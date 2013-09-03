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

public class Probabilistic_Segmentation implements PlugInFilter,DialogListener{
	ImagePlus imp;
	GenericDialog param;
	double MAX_VALUE;
	double MIN_VALUE;

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

	public void run(ImageProcessor ip){

		//Image is a 8/16/32 bit grayscale image.
		//Convert to 32 bit grayscale image if necessary.
		if(imp.getType() == ImagePlus.GRAY16 ||imp.getType() == ImagePlus.GRAY8)
			ip =ip.convertToFloat();
		if(!showDialog())
			return;
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

			if(debug){
				ImagePlus backgroundImage = new ImagePlus("Median image",backgroundProcessor);
				backgroundImage.show();
			}

			if (doPoissonEstimation){

			}else{
				//Subtract the image with the background
				subtract(ip,backgroundProcessor);
				imp.setProcessor(ip);
				imp.updateAndDraw();
			}
		}

		/**ESTIMATE NOISE
		 * 
		 */
		ImageProcessor diffIp = diffRow(ip,3);
		diffIp = diffCol(diffIp,3);

		if (debug){
			ImageProcessor dummy = new FloatProcessor(diffIp.getWidth(),diffIp.getHeight());
			dummy.copyBits(diffIp, 0, 0, Blitter.COPY);
			ImagePlus diffImage = new ImagePlus("Diff Image",dummy);
			diffImage.show();
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
		if(debug){
			ImagePlus dum = new ImagePlus("threshold image",segIP);
			dum.show();
			return;
		}
		segIP = Filter.meanFastFilter(segIP,imgKernelWidth,imgKernelHeight);
		if (debug){
			ImagePlus meanImg = new ImagePlus("mean Image",segIP);
			meanImg.show();
		}
		segIP = binaryThreshold(segIP,fpThresh);
		ImagePlus segImg = new ImagePlus("Segmentation Image",segIP);
		segImg.show();
	}
	public int setup(java.lang.String arg, ImagePlus imp){
		this.imp = imp;
		this.MIN_VALUE = 0;
		try{
			if (imp.getType() == ImagePlus.COLOR_256 || imp.getType() == ImagePlus.COLOR_RGB){
				IJ.error("Image needs to be a grayscale image.");
				return DONE;
			}}
		catch(Exception e){
			IJ.noImage();
			return DONE;
		}

		this.MAX_VALUE = Math.pow(2,imp.getBitDepth()) - 1;
		return DOES_ALL;	
	}

	public boolean showDialog(){
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
			return false;
		return true;
	}

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


	public static void main(String [] args){
		float [] dummy = {8,50,94,138,180,54,87,121,155,188,112,135,160,185,208,170,183,199,215,228,216,220,226,232,236};
		printArray(dummy);
		float med = QuickSelect.select(dummy, 0,dummy.length-1, dummy.length/2+1);
		System.out.println("Median Found : " + med);
		printArray(dummy);
		System.out.println("Median Found : " + med);
		System.out.println();
		for(int i = 0;i<10;i++){
			int [] arr = randomArray(0,40,25);
			printArray(arr);
			int median = QuickSelect.select(arr,0,arr.length-1,arr.length/2+1);
			System.out.println("Median found : " + median);
			printArray(arr);
			System.out.println();
		}
	}


	public static void printArray(int [] array){
		System.out.print("[ " );
		for(int i = 0; i<array.length;i++){
			if (i == array.length-1)
				System.out.print(array[i]);
			else
				System.out.print(array[i] + " , "); 
		}
		System.out.println(" ]");
	}
	public static void printArray(float [] array){
		System.out.print("[ " );
		for(int i = 0; i<array.length;i++){
			if (i == array.length-1)
				System.out.print(array[i]);
			else
				System.out.print(array[i] + " , "); 
		}
		System.out.println(" ]");
	}
	public static void printArray(short [] array){
		System.out.print("[ " );
		for(int i = 0; i<array.length;i++){
			if (i == array.length-1)
				System.out.print(array[i]);
			else
				System.out.print(array[i] + " , "); 
		}
		System.out.println(" ]");
	}
	public static int [] randomArray(int startRange,int endRange,int n){
		int [] arr = new int [n];
		for(int i = 0; i < n;i++){
			arr[i] = startRange + (int)(Math.random() * ((endRange - startRange)+1));
		}
		return arr;
	}

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
	public static ImageProcessor diff(ImageProcessor destination,int n){
		ImageProcessor diffProcessor = new FloatProcessor(destination.getWidth()-1,destination.getHeight()-1);
		int height = diffProcessor.getHeight();
		int width = diffProcessor.getWidth();
		int destWidth = destination.getWidth();
		float [] diffPix = (float [])diffProcessor.getPixels();
		float [] pix = (float [] )destination.getPixels();

		for(int row = 0; row < height;row++){
			int offset = row * width;
			int diffOffset = row * destWidth;
			for(int col = 0; col < width;col++){
				//dim 1
				float diff = pix[col+diffOffset+1] - pix[col+diffOffset];
				//dim 2
				diffPix[col+offset] = diff + (pix[col+diffOffset+destWidth] - pix[col+diffOffset]);
			}
		}
		if(n == 1)
			return diffProcessor;
		else
			return diff(diffProcessor,n-1);
	}

	private static float getStd(ImageProcessor ip){
		int k = 3;
		double magicNumber = Math.pow(1.4826,2);
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

}
