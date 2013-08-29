import java.util.Random;


public class QuickSelect {

	private static int partition(short [] arr,int left,int right,int pivot){
		short pivotValue = arr[pivot];
		arr[pivot] = arr[right];
		arr[right] = pivotValue;
		int storeIdx = left;
		for(int i = left;i<=right-1;i++){
			if( (arr[i] &0xffff) < (pivotValue &0xffff)){
				short tmp = arr[i];
				arr[i] = arr[storeIdx];
				arr[storeIdx] = tmp;
				storeIdx++;
			}
		}
		arr[right] = arr[storeIdx];
		arr[storeIdx] = pivotValue;


		return storeIdx;
	}

	private static int partition(int [] arr,int left,int right,int pivot){
		int pivotValue = arr[pivot];
		arr[pivot] = arr[right];
		arr[right] = pivotValue;
		int storeIdx = left;
		for(int i = left;i<=right-1;i++){
			if(arr[i] < pivotValue){
				int tmp = arr[i];
				arr[i] = arr[storeIdx];
				arr[storeIdx] = tmp;
				storeIdx++;
			}
		}
		arr[right] = arr[storeIdx];
		arr[storeIdx] = pivotValue;


		return storeIdx;
	}

	private static int partition(float [] arr,int left,int right,int pivot){
		float pivotValue = arr[pivot];
		arr[pivot] = arr[right];
		arr[right] = pivotValue;
		int storeIdx = left;
		for(int i = left;i<=right-1;i++){
			if(arr[i] < pivotValue){
				float tmp = arr[i];
				arr[i] = arr[storeIdx];
				arr[storeIdx] = tmp;
				storeIdx++;
			}
		}
		arr[right] = arr[storeIdx];
		arr[storeIdx] = pivotValue;


		return storeIdx;
	}
	
	private static int partition(byte [] arr,int left,int right,int pivot){
		byte pivotValue = arr[pivot];
		arr[pivot] = arr[right];
		arr[right] = pivotValue;
		int storeIdx = left;
		for(int i = left;i<=right-1;i++){
			if( ( arr[i]&0xff) < (pivotValue &0xff)){
				byte tmp = arr[i];
				arr[i] = arr[storeIdx];
				arr[storeIdx] = tmp;
				storeIdx++;
			}
		}
		arr[right] = arr[storeIdx];
		arr[storeIdx] = pivotValue;


		return storeIdx;
	}
	public static short select(short [] arr,int left, int right, int k ){
		while(!(left == right)){
			Random rand = new Random();
			int pivotIdx = rand.nextInt(right- left + 1) + left;
			int pivotNewIdx = partition(arr,left,right,pivotIdx);
			int pivotDist = pivotNewIdx - left + 1;
			if (pivotDist == k){
				return arr[pivotNewIdx];
			}
			else if (k < pivotDist)
				right = pivotNewIdx-1;
			else{
				left = pivotNewIdx+1;
				k = k-pivotDist;
			}		
		}
		return arr[left];//Only 1 element left
	}

	public static byte select(byte [] arr,int left, int right, int k ){
		while(!(left == right)){
			Random rand = new Random();
			int pivotIdx = rand.nextInt(right- left + 1) + left;
			int pivotNewIdx = partition(arr,left,right,pivotIdx);
			int pivotDist = pivotNewIdx - left + 1;
			if (pivotDist == k){
				return arr[pivotNewIdx];
			}
			else if (k < pivotDist)
				right = pivotNewIdx-1;
			else{
				left = pivotNewIdx+1;
				k = k-pivotDist;
			}		
		}
		return arr[left];//Only 1 element left
	}
	
	public static float select(float [] arr,int left, int right, int k ){
		while(!(left == right)){
			Random rand = new Random();
			int pivotIdx = rand.nextInt(right- left + 1) + left;
			int pivotNewIdx = partition(arr,left,right,pivotIdx);
			int pivotDist = pivotNewIdx - left + 1;
			if (pivotDist == k){
				return arr[pivotNewIdx];
			}
			else if (k < pivotDist)
				right = pivotNewIdx-1;
			else{
				left = pivotNewIdx+1;
				k = k-pivotDist;
			}		
		}
		return arr[left];//Only 1 element left
	}
	
	public static int select(int [] arr,int left, int right, int k ){
		while(!(left == right)){
			Random rand = new Random();
			int pivotIdx = rand.nextInt(right- left + 1) + left;
			int pivotNewIdx = partition(arr,left,right,pivotIdx);
			int pivotDist = pivotNewIdx - left + 1;
			if (pivotDist == k){
				return arr[pivotNewIdx];
			}
			else if (k < pivotDist)
				right = pivotNewIdx-1;
			else{
				left = pivotNewIdx+1;
				k = k-pivotDist;
			}		
		}
		return arr[left];//Only 1 element left
	}
}
 