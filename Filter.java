import ij.IJ;
import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;


public class Filter implements PlugInFilter{

	ImagePlus imp;

	private final static int antisym = 0;
	private final static int sym  = 1;
	public static ImageProcessor meanFastFilter(ImageProcessor ip, int filterWidth,int filterHeight){
		
		int height = ip.getHeight();
		int width = ip.getWidth();
		int extraPaddedWidth = (filterWidth/2) * 2;
		int extraPaddedHeight = (filterHeight/2) * 2;
		ip = ip.convertToFloat();
		ImageProcessor paddedIp = createPaddedImage(ip,width+extraPaddedWidth ,height+extraPaddedHeight,sym);
		float [] pix = (float [])paddedIp.getPixels();
		float [] meanPix = meanWidthFilter(pix,width+extraPaddedWidth,height+extraPaddedHeight,filterWidth);
		meanPix = meanHeightFilter(meanPix,width+extraPaddedWidth,height+extraPaddedHeight,filterHeight);
		
		ImageProcessor meanIP = new FloatProcessor(width,height,copyInterior(meanPix,width+extraPaddedWidth,height+extraPaddedHeight,width,height));
		return meanIP;
	}
	
	private static float [] copyInterior(float [] pix,int paddedWidth,int paddedHeight,int imageWidth,int imageHeight){
		float [] interiorPixels = new float[imageWidth*imageHeight];
		int extraWidth = paddedWidth - imageWidth;
		int extraHeight = paddedHeight - imageHeight;
		for(int row = 0; row<imageHeight;row++){
			int imageOffset = row*imageWidth;
			int paddedOffset = row*paddedWidth + ((extraHeight/2)*paddedWidth);
			for(int col = 0;col<imageWidth;col++){
				//COPY INTERIOR
				 interiorPixels[col+imageOffset] = pix[col+paddedOffset + extraWidth/2];
			}
		}

		return interiorPixels;
	}	
	
	public static float [] meanHeightFilter(float [] pix, int width, int height, int filterHeight){
		if(filterHeight == 1)
			return pix;
		float [] heightKernel = new float [filterHeight];
		float [] meanPix = new float[pix.length];
		int heightOffset = filterHeight/2 * width; 
		int cacheWidth = 0; 
		int kernelIdx = 0;
		float sum = 0;
		for(int col = 0; col < width; col++){
			for(int row = 0; row < height;row++){
				int offset = row*width;
				
				if(row == 0){
					kernelIdx = 0;
					sum = 0;
					cacheWidth = filterHeight/2 + 1; 
					for(int i = 0;i < cacheWidth;i++){
						sum += pix[offset+col+i*width];
						heightKernel[kernelIdx] = pix[offset+col+i*width];
						kernelIdx ++;
					}
					for(int i =cacheWidth;i < filterHeight;i++){
						heightKernel[i] = 0;
					}
				}else if(offset+col+heightOffset >= pix.length){
					cacheWidth--;
					sum -= heightKernel[kernelIdx];
					heightKernel[kernelIdx] = 0;
					if (kernelIdx < filterHeight-1)
						kernelIdx ++;
					else
						kernelIdx = 0;
				}else{
					if (cacheWidth < filterHeight)
						cacheWidth++;
					sum -= heightKernel[kernelIdx];
					sum += pix[offset+col + heightOffset];
					heightKernel[kernelIdx] = pix[offset+col + heightOffset];
					if (kernelIdx < filterHeight-1)
						kernelIdx ++;
					else
						kernelIdx = 0;
				}
				meanPix[offset+col] = sum / cacheWidth;
			}
		}
		return meanPix;
	}
	public static float [] meanWidthFilter(float [] pix, int width, int height, int filterWidth){
		if (filterWidth == 1){
			return pix;
		}
		float [] widthKernel = new float [filterWidth];
		float [] meanPix = new float[pix.length];
		
		int widthOffset = filterWidth/2;
		int cacheWidth = 0; 
		float sum = 0;
		int kernelIdx = 0;
		for(int row = 0; row < height; row++){
			int offset = row*width;
			for(int col = 0; col < width;col++){
				if (col == 0){
					kernelIdx = 0;
					sum = 0;
					cacheWidth = filterWidth/2 + 1; 
					for(int i = 0;i < cacheWidth;i++){
						sum += pix[offset+col+i];
						widthKernel[kernelIdx] = pix[offset+col+i];
						kernelIdx ++;
					}
					for(int i =cacheWidth;i <filterWidth;i++){
						widthKernel[i] = 0;
					}
				}else if(offset+col+widthOffset >= offset+width){
					try{
					cacheWidth--;
					sum -= widthKernel[kernelIdx];
					widthKernel[kernelIdx] = 0;
					if (kernelIdx < filterWidth-1)
						kernelIdx ++;
					else
						kernelIdx = 0;
					}catch(Exception e){
						IJ.showMessage("At Pixel : " + Integer.toString(offset + col));
					}
				}else{
					if (cacheWidth < filterWidth)
						cacheWidth++;
					sum -= widthKernel[kernelIdx];
					sum += pix[offset+col + widthOffset];
					widthKernel[kernelIdx] = pix[offset+col + widthOffset];
					if (kernelIdx < filterWidth-1)
						kernelIdx ++;
					else
						kernelIdx = 0;
				}
				meanPix[offset+col] = sum / cacheWidth;
			}
		}
		return meanPix;
	}
	//Filter a 32 bit image, with a float processor. If Image type is 8/16 bits, convert first with ImageJ convert
	public static ImageProcessor medianFilter(ImageProcessor ip,int filterWidth,int filterHeight,int paddingType){
		//Define height and width of the padded image.
		int extraWidth = (filterWidth / 2) * 2;//Take into account both side
		int extraHeight = (filterHeight /2) * 2;//
		int imageWidth = ip.getWidth();
		int imageHeight = ip.getHeight();
		int paddedWidth = imageWidth + extraWidth;
		int paddedHeight = imageHeight + extraHeight;
		int filterSize = filterWidth * filterHeight;

		ImageProcessor paddedProcessor = createPaddedImage(ip,paddedWidth,paddedHeight,paddingType);
		ImagePlus dum = new ImagePlus("paddedImage",paddedProcessor);
		dum.show();
		ImageProcessor medianProcessor;

		float [] medianArray = new float [imageWidth*imageHeight];
		float [] linearFilterArray = new float[filterSize];
		float [] paddedArray = (float [])paddedProcessor.getPixels();
		medianProcessor = new FloatProcessor(imageWidth,imageHeight);
		for(int row = 0;row < imageHeight;row++){
			int offset = row*imageWidth;
			int paddedOffset = row*paddedWidth + extraHeight/2 * paddedWidth;
			for(int col = 0; col < imageWidth;col++){
				int paddedColOffset = extraWidth/2 + col;

				//Create LINEAR KERNEL
				int filtIdx = 0;
				for(int i = 0;i < filterHeight;i++){
					for(int j = 0;j < filterWidth;j++){
						int filtColOffset = (j - extraWidth/2);
						int filtRowOffset = (i - extraHeight/2) * paddedWidth;
						linearFilterArray[filtIdx] = paddedArray[filtColOffset+filtRowOffset+paddedOffset+paddedColOffset];
						filtIdx++;
					}
				}
				medianArray[col+offset] = QuickSelect.select(linearFilterArray, 0, linearFilterArray.length-1, linearFilterArray.length/2+1);
			}
		}
		medianProcessor.setPixels(medianArray);
		return medianProcessor;
	}





	/**createPaddedImage creates an image with added border to decrease border problematic with filters.
	 * 
	 * @param ip - original image processor
	 * @param paddedWidth - The final padded image width
	 * @param paddedHeight - The final padded image height
	 * @param paddedOption - 0 , 1 - asymmetric or symmetric
	 * @return padded imageProcessor
	 */
	private static ImageProcessor createPaddedImage(ImageProcessor ip,int paddedWidth,int paddedHeight,int paddedOption){
		ImageProcessor paddedProcessor;
		int imageWidth = ip.getWidth();
		int extraWidth = paddedWidth - imageWidth;
		int imageHeight = ip.getHeight();
		int extraHeight = paddedHeight - imageHeight;

		//ImagePlus paddedImg;//For debugging -> possible to show padded image.

		//Cast image in 8bits/16bits in 32 bits images.
		float [] pixArray = (float [])ip.getPixels();
		paddedProcessor = new FloatProcessor(paddedWidth,paddedHeight);
		float [] paddedArray = new float [paddedWidth*paddedHeight];
		//Start by copying the interior of the image + first padded rows and columns.
		for(int row = 0; row<imageHeight;row++){
			int imageOffset = row*imageWidth;
			int paddedOffset = row*paddedWidth + ((extraHeight/2)*paddedWidth);

			for(int col = 0;col<imageWidth;col++){
				//COPY INTERIOR
				paddedArray[col+paddedOffset + extraWidth/2] = pixArray[col+imageOffset];
			}
		}

		//Fill top and bottom border
		for(int row = 0 ; row < extraHeight/2 ;row++){
			//Calculate the different index for the different offSets
			int topOffset = paddedWidth * (extraHeight/2 - 1 - row);
			int firstTopElementOffset = (extraHeight/2 + row) * paddedWidth;
			int secondTopElementOffset = firstTopElementOffset + paddedWidth;


			int botOffset = paddedWidth * (paddedHeight - extraHeight/2 + row);
			int firstBotElementOffset =  paddedWidth * (paddedHeight - extraHeight/2 - 1 - row);
			int secondBotElementOffset = firstBotElementOffset - paddedWidth;

			int colOffset = (extraWidth/2) - row ;

			for(int col = 0;col<paddedWidth - ( extraWidth/2 - row) ;col++){

				//FILL TOP
				int firstTopIdx = col+colOffset+firstTopElementOffset;
				int secondTopIdx = col+colOffset+secondTopElementOffset;
				int topIdx = col+colOffset+topOffset;
				float diff = paddedArray[secondTopIdx] - paddedArray[firstTopIdx];
				float value = 0;
				if (paddedOption == antisym)//Anti-symmetric
					value = (paddedArray[topIdx + paddedWidth] ) - diff;
				else if (paddedOption == sym)//Symmetric
					value = (paddedArray[topIdx + paddedWidth] ) + diff;
				paddedArray[topIdx] = (float)value;

				//FILL BOT
				int firstBotIdx = col+colOffset+firstBotElementOffset;
				int secondBotIdx = col+colOffset+secondBotElementOffset;
				int botIdx = col+colOffset+botOffset;

				diff = paddedArray[secondBotIdx] - paddedArray[firstBotIdx];
				if (paddedOption == antisym)//Anti-symmetric
					value = paddedArray[botIdx - paddedWidth] - diff;
				else if (paddedOption == sym)//Symmetric
					value = paddedArray[botIdx - paddedWidth] + diff;
				paddedArray[botIdx] = (float)value;

			}

		}

		//Fill left and right
		for(int col = 0; col<extraWidth/2 ; col++){
			int colOffset = extraWidth/2 -col;
			int rightColOffset = paddedWidth - 1 - extraWidth/2 +col;
			for(int row = 0;row<paddedHeight;row++){
				int rowOffset = row * paddedWidth;

				int currentLeftValueIdx = rowOffset+colOffset;
				int leftIdx = currentLeftValueIdx - 1;
				int firstLeftElementIdx = rowOffset+colOffset+col*2;
				int secondLeftElementIdx = firstLeftElementIdx +1;

				int currentRightValueIdx = rowOffset+rightColOffset;
				int rightIdx = currentRightValueIdx +1;
				int firstRightElementIdx = rowOffset+rightColOffset-col*2;
				int secondRightElementIdx = firstRightElementIdx - 1;

				float value = 0;
				//LEFT
				float diff = paddedArray[secondLeftElementIdx]  - paddedArray[firstLeftElementIdx];
				if (paddedOption == antisym)//Anti-symmetric
					value = paddedArray[currentLeftValueIdx]  - diff;
				else if(paddedOption == sym)//Symmetric
					value = paddedArray[currentLeftValueIdx]  + diff;
				paddedArray[leftIdx] = (float)value;
				//RIGHT
				diff = paddedArray[secondRightElementIdx] - paddedArray[firstRightElementIdx];
				if (paddedOption == antisym)
					value = paddedArray[currentRightValueIdx] - diff;
				else if(paddedOption == sym)
					value = paddedArray[currentRightValueIdx] + diff;
				paddedArray[rightIdx] = (float)value;
			}
		}
		paddedProcessor.setPixels(paddedArray);
		return paddedProcessor;
		//paddedImg = new ImagePlus("Background Image",paddedProcessor);
		//paddedImg.show();
	}


	@Override
	public int setup(String arg, ImagePlus imp) {
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
		return DOES_ALL;	
	}


	public void run(ImageProcessor ip) {
		ip = ip.convertToFloat();
		ImageProcessor medianFilterProcessor = medianFilter(ip,5,5,0);
		ImagePlus medianBackground = new ImagePlus("Background Image",medianFilterProcessor);
		medianBackground.show();
	}
	
	public static void main(String [] args){
		float [] a = {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25};
		float [] b = meanHeightFilter(a,5,5,3);
		Probabilistic_Segmentation.printArray(b);
		
	}
	
	/*
	private static float [] byteToFloat(byte [] arr){
		float [] arr2 = new float [arr.length];
		int length = arr.length;
		for(int i = 0;i < length;i++){
			arr2[i] = arr[i] &0xff;
		}
		return arr2;
	}
	*/
}
