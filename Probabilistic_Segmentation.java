/*RUN_TEST for adding a plugins for FIJI
 * The program will create an image and do an image function on the image.
 *
 */

import java.awt.AWTEvent;
import java.awt.TextField;
import java.util.*;

import ij.*;
import ij.process.*;
import ij.plugin.filter.*;
import ij.gui.*;


/**The algorithm distinguishes signal from noise, requiring basically no
 *parameter adjustment, and no prior knowledge about signal shape, making
 *it robust and versatile, and, most of all, very convenient.
 *Using the background filter option, it can do limited selection of small
 *features over large features. It cannot distinguish overlapping features
 *from one another. However, since the algorithm correctly identifies
 *signal with high fidelity, its output can be used to replace the possibly
 *unstable thresholding step of other algorithms. For example, to find
 * spot-like features, probabilisticSegmentation can be used to identify the
 * local intensity maxima in the image that correspond to actual signal.
 * 
 * Parameters : 
 * bgFilter:filter mask size for median filter to subtract
 *		    background. Filter size should be at least 1.5 times the diameter
 *		    of  circular features, or 3 times the diameter of  linear features.
 *			Must be odd.
 *		    Currently, stacks are filters independantly.
 *          Note that a local median filter is powerful and robust, but
 *          slows down the code significantly. The current implementation is a slow implementation. 
 *          Faster implementation TODO
 * NOTE : bgFilter is most likely the only parameter that you'll have to
 *        set.
 *          
 * fpExp : target maximum expected number of false positives per
 * 		   image.  
 * Note: For Gaussian noise and constant background, fpExp is
 *       exact. For poisson noise on top of a strongly variable
 *       background, fpExp might underestimate the number of false
 *       positive pixels. Normally, the false positives will be
 *       isolated, so if the features are sufficiently large, the false
 *       positives can easily be eliminated by size. Also note that if
 *       fpExp is very large or very small, the calculation for the
 *       binomial probabilities saturates, meaning that different
 *       extreme values of fpExp might yield the same segmentation
 *       results.
 * imgFilter: filter mask for determining true positives. Vector
 *		    containing the filter size for all the dimensions of the filter
 *		    mask. Must be odd.
 *          Note: a larger imgFilter will be able to detect features of
 *          lower SNR, as long as they're at least as large as the filter.
 *          However, a larger filter also leads to an overestimation of the
 *          feature size, which may have to be corrected via an erosion
 *          step. A good alternative value for imgFilter with larger
 *          features is [9 9]
 * Please be advise that Probabilistic_Segmentation uses diverse function from the apache commons math library
 * Commons Math is distributed under the terms of the Apache License, Version 2.0
 * Commons Math requires JDK 1.5+ and has no runtime dependencies.
 * @author Jonas Dorn, Jacques Boisvert
 *
 */
public class Probabilistic_Segmentation implements ExtendedPlugInFilter,DialogListener{
	private ImagePlus imp;

	private ImageStack segSt;//Segmentation stack.
	private ImagePlus segImg;//Segmentation window.

	//Debug stacks
	private ImageStack backgroundSt;
	private ImageStack meanSt;
	private ImageStack diffSt;


	private int nPasses = 1;
	private int pass;

	//private ImageStack stack;

	//THRESHOLD PARAM
	private static double fpExp = 0.5;
	private static double nseMult = 1.3;
	//IMAGE KERNEL PARAM
	private static int imgKernelWidth = 5;
	private static int imgKernelHeight = 5;
	//BACKGROUND KERNEL PARAM
	private static int bgKernelWidth = 5;
	private static int bgKernelHeight = 5;
	//BOOLEAN PARAM
	private static boolean doBackgroundSub = true;
	private static boolean doPoissonEstimation = false;
	private static boolean debug = true;
	private static int paddingType = 0;

	//ERROR MESSAGE
	private static final String evenKernelError = "Kernel must be of odd size";
	final double VERSION = 1.04;

	private int flags = DOES_ALL|CONVERT_TO_FLOAT;

	public void run(ImageProcessor ip){

		//Validate background/image filter.
		if(!validateBgKernel())
			return ;
		if(!validateImgKernel())
			return;

		int bgValue = 0;
		int height = imp.getHeight();
		int width = imp.getWidth();

		/**Threshold parameters
		 * 
		 */
		double pSinglePixel = 1 - MathFunction.normCDF(nseMult,0,1);
		int avgMaskSize = imgKernelWidth * imgKernelHeight; //* imgKernelDepth;
		double [] expectedFT = new double [avgMaskSize];
		for (int i = 0; i < avgMaskSize;i++){
			expectedFT[i] = ( 1-MathFunction.binoCDF(i, avgMaskSize, pSinglePixel) ) * height * width;
		}
		int idx = 0;
		for(int i = 0; i < avgMaskSize;i++){
			if (expectedFT[i] <= fpExp){
				idx = i;
				break;
			}
		}

		double fpThresh = (double)idx / avgMaskSize;
		/**ESTIMATE BACKGROUND
		 * 
		 */
		if (doBackgroundSub){

			ImageProcessor backgroundProcessor = Filter.medianFilter(ip,bgKernelWidth,bgKernelHeight,paddingType);

			if (debug){
				backgroundSt.addSlice(ip);
			}
			if (doPoissonEstimation){

			}else{
				//Subtract the image with the background
				subtract(ip,backgroundProcessor);
				imp.updateAndDraw();
			}
		}

		/**ESTIMATE NOISE
		 * 
		 */
		ImageProcessor diffIp = diffRow(ip,3);
		diffIp = diffCol(diffIp,3);

		if (debug){
			if(pass == 0){
				this.diffSt = new ImageStack(diffIp.getWidth(),diffIp.getHeight());
			}
			ImageProcessor dummy = new FloatProcessor(diffIp.getWidth(),diffIp.getHeight());
			dummy.copyBits(diffIp, 0, 0, Blitter.COPY);
			diffSt.addSlice(dummy);
		}
		float stdDiffImage = getStd(diffIp);
		//IJ.showMessage("Standard Deviation : "+ Float.toString(stdDiffImage));

		float noise = stdDiffImage/20;
		//IJ.showMessage("NOISE : " + Float.toString(noise));

		//IJ.showMessage("Fp Threshold : " + Double.toString(fpThresh));
		/**THRESHOLD
		 * 
		 */
		ImageProcessor segIP = threshold(ip,bgValue + noise * nseMult);
		segIP = Filter.meanFastFilter(segIP,imgKernelWidth,imgKernelHeight);
		if (debug){
			meanSt.addSlice(segIP);
		}
		segIP = binaryThreshold(segIP,fpThresh);
		segSt.addSlice(segIP);
		if (pass == nPasses-1){
			segImg.setStack("Segmentation Image",segSt);
			if(debug){
				ImagePlus backgroundImage = new ImagePlus("Median image",backgroundSt);
				backgroundImage.show();

				ImagePlus diffImage = new ImagePlus("Diff Image",diffSt);
				diffImage.show();


				ImagePlus meanImg = new ImagePlus("mean Image",meanSt);
				meanImg.show();
			}
			segImg.show();
		}
		pass++;
	}
	/**This method is called by ImageJ for initialization
	 * 
	 * @param arg Unused here. 
	 * @param imp The ImagePlus containing the image (or stack) to process.
	 * @return    The method returns flags (i.e., a bit mask) specifying the
	 *            capabilities (supported formats, etc.) and needs of the filter.
	 *            See PlugInFilter.java and ExtendedPlugInFilter in the ImageJ
	 *            sources for details.
	 */
	public int setup(java.lang.String arg, ImagePlus imp){

		this.imp = imp;
		try{
			if (imp.getType() == ImagePlus.COLOR_256 || imp.getType() == ImagePlus.COLOR_RGB){
				IJ.error("Image needs to be a grayscale image.");
				return DONE;
			}}
		catch(Exception e){
			IJ.noImage();
			return DONE;
		}
		segSt = new ImageStack(imp.getWidth(),imp.getHeight());
		segImg = new ImagePlus();
		backgroundSt = new ImageStack(imp.getWidth(),imp.getHeight());
		meanSt = new ImageStack(imp.getWidth(),imp.getHeight());
		return flags;	
	}

	/**Called by imageJ after the call to setup
	 * set up the parameter gui.
	 */
	public int showDialog(ImagePlus imp, String command, PlugInFilterRunner pfr) {
		GenericDialog gd = new GenericDialog("Probabilistic Segmentation Parameters");
		gd.addNumericField("False positive expectancy",fpExp,1);
		gd.addNumericField("Noise multiplicator",nseMult,1);
		gd.addMessage("Background Subtraction");
		String [] checkBoxLabel = {"Background Estimation","Poisson Estimation"};
		boolean [] defaultBoxValue = {doBackgroundSub,doPoissonEstimation};
		gd.addCheckboxGroup(1,2,checkBoxLabel,defaultBoxValue);
		String [] itemList = {"Antisymmetric","Symmetric"};
		gd.addChoice("Padding Option", itemList, "Antisymmetric");
		gd.addMessage("Background Filter mask");
		gd.addNumericField("x",bgKernelWidth,0);
		gd.addNumericField("y",bgKernelHeight,0);
		//param.addNumericField("z", 1, 3);
		gd.addMessage("Image Filter mask");
		gd.addNumericField("x",imgKernelWidth,0);
		gd.addNumericField("y",imgKernelHeight,0);
		//param.addNumericField("z",1,3);
		gd.addCheckbox("Debug", debug);
		gd.addDialogListener(this); 
		gd.showDialog();
		if(gd.wasCanceled())
			return DONE;
		flags = IJ.setupDialog(imp, flags);     // ask whether to process all slices of stack (if a stack)
		return flags;
	}

	/**Called after modifications to the dialog.
	 * Return true if valid
	 * 
	 */
	public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {

		Vector<?> numFields = gd.getNumericFields();
		TextField xBgKernel = (TextField)numFields.get(2);
		TextField yBgKernel = (TextField)numFields.get(3);
		TextField xImKernel = (TextField)numFields.get(4);
		TextField yImKernel = (TextField)numFields.get(5);
		if(!xBgKernel.getText().equals(yBgKernel.getText()) || !xImKernel.getText().equals(yImKernel.getText())){
			if(e.getSource() == xBgKernel)
				yBgKernel.setText(xBgKernel.getText());
			else if(e.getSource() == yBgKernel)
				xBgKernel.setText(yBgKernel.getText());
			else if(e.getSource() == xImKernel)
				yImKernel.setText(xImKernel.getText());
			else if(e.getSource() == yImKernel)
				xImKernel.setText(yImKernel.getText());
		}
		//DEFINE PARAMETER 
		fpExp = gd.getNextNumber();
		nseMult = gd.getNextNumber();
		bgKernelWidth = (int) gd.getNextNumber();
		bgKernelHeight = (int) gd.getNextNumber();
		imgKernelWidth = (int) gd.getNextNumber();
		imgKernelHeight = (int) gd.getNextNumber();
		doBackgroundSub = gd.getNextBoolean();
		doPoissonEstimation = gd.getNextBoolean();
		paddingType = gd.getNextChoiceIndex();//0 = antisymmetric, 1 = symmetric;
		debug = gd.getNextBoolean();

		return !gd.invalidNumber();
	}


	/**Function subtract calculates the subtraction of two float image processor
	 * NOTE : The initial image is changed
	 * @param destination - First image
	 * @param source - image use for the subtraction
	 * 
	 */
	public static void subtract(ImageProcessor destination, ImageProcessor source){
		int width = destination.getWidth();
		int height = destination.getHeight();
		float [] pix1 = (float [])destination.getPixels();
		float [] pix2 = (float [] ) source.getPixels();
		for(int row = 0;row < height;row++){
			int offset = row*width;
			for(int col = 0; col < width;col++){
				pix1[col+offset] = pix1[col+offset] - pix2[col+offset];
			}
		}
		destination.setPixels(pix1);
	}


	/**Function diffCol calculates the nth difference between columns
	 * Recursion is used for each subsequent difference.
	 * @param destination - Float image processor
	 * @param n - nth difference
	 * @return diffCol - Float image processor of the nth difference
	 */
	private static ImageProcessor diffCol(ImageProcessor destination, int n){
		int destWidth = destination.getWidth();
		ImageProcessor diffIP = new FloatProcessor(destWidth-1,destination.getHeight());
		int width = diffIP.getWidth();
		int height = diffIP.getHeight();
		float [] diffPix = (float [] ) diffIP.getPixels();
		float [] pix = (float [] ) destination.getPixels();

		for(int row = 0; row < height;row++){
			int offset = row*width;
			int diffOffset = row * destWidth;
			for(int col = 0; col < width;col++){
				diffPix[col+offset] = pix[col+diffOffset+1] - pix[col+diffOffset];
			}
		}
		if(n == 1)
			return diffIP;
		else
			return diffCol(diffIP,n-1);
	}

	/**Function diffRow calculates the nth difference between rows.
	 * Recursion is used for each subsequent difference. n-1,n-2...n-(n-1) 
	 * @param destination - Float image processor
	 * @param n - The nth difference
	 * @return diffRow - Float image  processor of the nth difference
	 */
	private static ImageProcessor diffRow(ImageProcessor destination, int n){
		int destWidth = destination.getWidth();
		ImageProcessor diffIP = new FloatProcessor(destWidth,destination.getHeight()-1);
		int width = diffIP.getWidth();
		int height = diffIP.getHeight();
		float [] diffPix = (float [] ) diffIP.getPixels();
		float [] pix = (float [] ) destination.getPixels();

		for(int row = 0; row < height;row++){
			int offset = row*width;
			for(int col = 0; col < width;col++){
				diffPix[col+offset] = pix[col+offset+width] - pix[col+offset];
			}
		}
		if(n == 1)
			return diffIP;
		else
			return diffRow(diffIP,n-1);
	}

	/**Function that calculate the standard deviation of an image. 
	 * 
	 * @param ip - Image processor containing the pixels value for the image
	 * @return the standard deviation
	 */
	private static float getStd(ImageProcessor ip){
		//cut-off is roughly at 3 sigma, see Danuser, 1992 or Rousseeuw & Leroy, 1987
		int k = 3;
		double magicNumber = Math.pow(1.4826,2);//see same publications
		float [] pix = (float []) ip.getPixels();
		//Find median
		float median = QuickSelect.select(pix,0,pix.length-1,pix.length/2+1);
		//IJ.showMessage(Float.toString(median));
		float [] res2 = new float [pix.length];
		int width = ip.getWidth();
		int height = ip.getHeight();

		//calculate squared residual.
		for(int row = 0; row < height;row++){
			int offset = row*width;
			for(int col = 0; col < width;col++){
				res2[col+offset] = (float)Math.pow( (pix[col+offset]-median),2);
			}
		}

		//Find the median of the residual
		float resMedian = QuickSelect.select(res2,0,res2.length-1,res2.length/2+1);
		//IJ.showMessage(Float.toString(resMedian));

		//Calculate the Weight for each pixel and if the pixel is not an outlier, add it to the sum.
		float sumRes = 0;
		int nInlier = 0;
		for(int row = 0; row < height; row++){
			int offset = row*width;
			for(int col = 0; col < width; col++){
				if(res2[col+offset] / (magicNumber * resMedian) <= (Math.pow(k,2))){
					sumRes += res2[col+offset];
					nInlier ++;
				}
			}
		}
		//IJ.showMessage(Integer.toString(nInlier));
		return (float)Math.sqrt(sumRes / (nInlier-4) );
	}

	/**Function that return a byte image where pix[i,j] is 1 if pix[i,j] > threshold and 0 otherwise
	 * 
	 * @param ip - float image processor to threshold
	 * @param thresh - threshold level
	 * @return byte image processor with value 0|1
	 */
	private static ImageProcessor threshold(ImageProcessor ip,double thresh){
		int height = ip.getHeight();
		int width = ip.getWidth();
		float [] pix = (float [] )ip.getPixels();
		byte [] bytePix = new byte [height*width];

		for(int row = 0 ; row < height; row++){
			int offset = row * width;
			for(int col = 0 ; col < width; col++){
				if(pix[offset + col] > thresh)
					bytePix[offset+col] = (byte) 1;
				else
					bytePix[offset+col] = 0;
			}
		}
		ImageProcessor binaryIP = new ByteProcessor(width,height,bytePix);
		return binaryIP;
	}

	/**Function that return a binary image where pix[i,j] is 255 if pix[i,j] > threshold and 0 otherwise
	 * 
	 * @param ip - float image processor to threshold
	 * @param thresh - threshold level
	 * @return binary image processor with value 0|255
	 * See also threshold.
	 */
	private static ImageProcessor binaryThreshold(ImageProcessor ip, double thresh){
		int height = ip.getHeight();
		int width = ip.getWidth();
		float [] pix = (float [] )ip.getPixels();
		byte [] bytePix = new byte [height*width];
		for(int row = 0 ; row < height; row++){
			int offset = row * width;
			for(int col = 0 ; col < width; col++){
				if(pix[offset + col] > thresh)
					bytePix[offset+col] = (byte) 255;
				else
					bytePix[offset+col] = 0;
			}
		}
		ImageProcessor binaryIP = new BinaryProcessor(new ByteProcessor(width,height,bytePix));
		return binaryIP;
	}

	/**Function that validate the background kernel input by the user
	 * 
	 * @return true if background kernel is > 3 and if odd
	 */
	private static boolean validateBgKernel(){
		String bgTooSmall = "Background Filter must have a width/height > 3";

		if(bgKernelWidth < 3){
			IJ.error(bgTooSmall);
			return false;
		}
		else if(bgKernelWidth % 2 == 0){
			IJ.error(evenKernelError);
			return false;
		}
		return true;
	}

	/**Function that validate the image kernel given by the user
	 * 
	 * @return true if the image kernel is > 3 and if odd
	 * @return false if the image kernel is < 3 and is even
	 */
	private static boolean validateImgKernel(){
		String imgTooSmall = "Image Filter must have a width/height > 3";

		if(imgKernelWidth < 3){
			IJ.error(imgTooSmall);
			return false;
		}
		else if(imgKernelWidth % 2 == 0){
			IJ.error(evenKernelError);
			return false;
		}
		return true;
	}

	/** This method is called by ImageJ to set the number of calls to run(ip)
	 *  corresponding to 100% of the progress bar */
	public void setNPasses(int nPasses) {
		this.nPasses = nPasses;
		this.pass = 0;
		// TODO Auto-generated method stub	
	}


}
