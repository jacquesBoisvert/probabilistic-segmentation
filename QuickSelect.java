import java.util.Random;

/**Implementation of a quickselect. Quickselect is a selection algorithm that uses the quicksort algorithm.
 * Like quicksort, quickselect choose one element as a pivot and partition the other elements in 2 classes, higher or lower,
 * based on the pivot. However, quickselect only recurses in one of the 2 classes, where quicksort would recurses in both groups.
 * 
 *------ EFFICIENCY ------ 
 *worst case performance : O(n^2)
 *Best case performance : O(n)
 *Average case performance : O(n)
 * @author Jacques Boisvert
 *Note : Quickselect is also known as Hoare's selection algorithm
 *Note : The array passed by reference is actually sort by the function 
 *		 when the function is searching for the k-order element. 
 */
public class QuickSelect {
	
/**Function partition is responsible to divide the elements situated between the border left and right in 2 classes, 
 * lower or higher elements.
 * Note : Short array are cast into int first with a binary AND operation &0xffff
 * @param arr - short array
 * @param left - Left border
 * @param right - Right border
 * @param pivot - pivot index
 * @return storeIdx -  new pivot index
 */
	private static int partition(short [] arr,int left,int right,int pivot){
		short pivotValue = arr[pivot];
		arr[pivot] = arr[right];//Put the pivot as the last element
		arr[right] = pivotValue;
		int storeIdx = left;
		for(int i = left;i<=right-1;i++){
			if( (arr[i] &0xffff) < (pivotValue &0xffff)){//short -> int
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
	
	/**Function partition is responsible to divide the elements situated between the border left and right in 2 classes, 
	 * lower or higher elements.
	 * 
	 * @param arr - int array
	 * @param left - Left border
	 * @param right - Right border
	 * @param pivot - pivot index
	 * @return storeIdx -  new pivot index
	 */
	private static int partition(int [] arr,int left,int right,int pivot){
		int pivotValue = arr[pivot];
		arr[pivot] = arr[right]; //Put the pivot as the last element
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
	
	/**Function partition is responsible to divide the elements situated between the border left and right in 2 classes, 
	 * lower or higher elements.
	 * @param arr - float array
	 * @param left - Left border
	 * @param right - Right border
	 * @param pivot - pivot index
	 * @return storeIdx -  new pivot index
	 */
	private static int partition(float [] arr,int left,int right,int pivot){
		float pivotValue = arr[pivot];
		arr[pivot] = arr[right]; //Put the pivot as the last element
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
	
	/**Function partition is responsible to divide the elements situated between the border left and right in 2 classes, 
	 * lower or higher elements.
	 * Note : Byte array are cast into int first with a binary AND operation &0xff
	 * @param arr - byte array
	 * @param left - Left border
	 * @param right - Right border
	 * @param pivot - pivot index
	 * @return storeIdx -  new pivot index
	 */
	private static int partition(byte [] arr,int left,int right,int pivot){
		byte pivotValue = arr[pivot];
		arr[pivot] = arr[right]; //Put the pivot as the last element
		arr[right] = pivotValue;
		int storeIdx = left;
		for(int i = left;i<=right-1;i++){
			if( ( arr[i]&0xff) < (pivotValue &0xff)){//byte -> int
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
	
	/** Select is an iterative function, that divides an array in subarray based on a pivot.
	 * Pivot selection is done randomly.  
	 * Note : Value are assumed to be in the range 0-65535, so when the comparison
	 * is going to be made in the partition function, the byte will be cast with a binary AND operation &0xffff
	 * @param arr - short array
	 * @param left - Left boundary - Usually when starting the algorithm, this value is 0. 
	 * @param right - Right boundary - Usually when starting the algorithm this value is arr.length-1.
	 * @param k - k-order value to be found - If looking for the median, starting value is arr.length/2 + 1.
	 * @return value of the k-order element.
	 */
	public static short select(short [] arr,int left, int right, int k ){
		while(!(left == right)){
			Random rand = new Random();
			int pivotIdx = rand.nextInt(right- left + 1) + left;//Select randomly pivot.
			int pivotNewIdx = partition(arr,left,right,pivotIdx);//Pivot has move. Update his position.
			int pivotDist = pivotNewIdx - left + 1;
			if (pivotDist == k){ //Found k-order value.
				return arr[pivotNewIdx];
			}
			else if (k < pivotDist) // k-order value is situation in left part of the array
				right = pivotNewIdx-1;
			else{ // k-order value is situated in the right part of the array
				left = pivotNewIdx+1;
				k = k-pivotDist;
			}		
		}
		return arr[left];//Only 1 element left
	}

	/** Select is an iterative function, that divides an array in subarray based on a pivot.
	 * Pivot selection is done randomly. 
	 * Note : Value are assumed to be in the range 0-255, so when the comparison
	 * is going to be made in the partition function, the byte will be cast with a binary AND operation &0xff
	 * @param arr - byte array
	 * @param left - Left boundary - Usually when starting the algorithm, this value is 0. 
	 * @param right - Right boundary - Usually when starting the algorithm this value is arr.length-1.
	 * @param k - k-order value to be found - If looking for the median, starting value is arr.length/2 + 1.
	 * @return value of the k-order element.
	 * 
	 */
	public static byte select(byte [] arr,int left, int right, int k ){
		while(!(left == right)){
			Random rand = new Random();
			int pivotIdx = rand.nextInt(right- left + 1) + left;
			int pivotNewIdx = partition(arr,left,right,pivotIdx);//Pivot has move. Update his position.
			int pivotDist = pivotNewIdx - left + 1;
			if (pivotDist == k){ //Found k-order value.
				return arr[pivotNewIdx];
			}
			else if (k < pivotDist)// k-order value is situation in left part of the array
				right = pivotNewIdx-1;
			else{ // k-order value is situated in the right part of the array
				left = pivotNewIdx+1;
				k = k-pivotDist;
			}		
		}
		return arr[left];//Only 1 element left
	}
	
	/** Select is an iterative function, that divides an array in subarray based on a pivot.
	 * Pivot selection is done randomly. 
	 * 
	 * @param arr - float array
	 * @param left - Left boundary - Usually when starting the algorithm, this value is 0. 
	 * @param right - Right boundary - Usually when starting the algorithm this value is arr.length-1.
	 * @param k - k-order value to be found - If looking for the median, starting value is arr.length/2 + 1.
	 * @return value of the k-order element.
	 */	
	public static float select(float [] arr,int left, int right, int k ){
		while(!(left == right)){
			Random rand = new Random();
			int pivotIdx = rand.nextInt(right- left + 1) + left;
			int pivotNewIdx = partition(arr,left,right,pivotIdx);//Pivot has move. Update his position.
			int pivotDist = pivotNewIdx - left + 1;
			if (pivotDist == k){ //Found k-order value.
				return arr[pivotNewIdx];
			}
			else if (k < pivotDist)
				right = pivotNewIdx-1;
			else{ // k-order value is situated in the right part of the array
				left = pivotNewIdx+1;
				k = k-pivotDist;
			}		
		}
		return arr[left];//Only 1 element left
	}
	
	/** Select is an iterative function, that divides an array in subarray based on a pivot.
	 * Pivot selection is done randomly. 
	 * 
	 * @param arr - int array
	 * @param left - Left boundary - Usually when starting the algorithm, this value is 0. 
	 * @param right - Right boundary - Usually when starting the algorithm this value is arr.length-1.
	 * @param k - k-order value to be found - If looking for the median, starting value is arr.length/2 + 1.
	 * @return value of the k-order element.
	 */
	public static int select(int [] arr,int left, int right, int k ){
		while(!(left == right)){
			Random rand = new Random();
			int pivotIdx = rand.nextInt(right- left + 1) + left;//Select randomly pivot.
			int pivotNewIdx = partition(arr,left,right,pivotIdx);//Pivot has move. Update his position.
			int pivotDist = pivotNewIdx - left + 1;//Calculate the distance
			if (pivotDist == k){//Found k-order value.
				return arr[pivotNewIdx];
			}
			else if (k < pivotDist)// k-order value is situation in left part of the array
				right = pivotNewIdx-1;
			else{ // k-order value is situated in the right part of the array
				left = pivotNewIdx+1;
				k = k-pivotDist;
			}		
		}
		return arr[left];//Only 1 element left
	}
}
 