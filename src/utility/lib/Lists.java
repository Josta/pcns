package utility.lib;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/** Library with list/array helpers **/
public class Lists {
	
	private Lists() {}
	
	/** Turns a collection into an array **/
	public static int[] array(Collection<Integer> list) {
		return list.stream().mapToInt(i->i).toArray();
	}
	
	/** Turns a collection into an array **/
	public static long[] longArray(Collection<Long> list) {
		return list.stream().mapToLong(i->i).toArray();
	}
	
	public static String[] strArray(Stream<String> stream) {
		return stream.toArray(String[]::new);
	}
	
	/** Concatenates two arrays **/
	public static int[] concat(int[] a, int[] b) {
		return IntStream.concat(Arrays.stream(a), Arrays.stream(b)).toArray();
	}
	
	public static boolean isPrefix(int[] prefix, int[] array) {
		if (prefix.length > array.length)
			return false;
		for (int i = 0; i < prefix.length; i++)
			if (prefix[i] != array[i]) return false;
		return true;
	}
	
	/** Reverts the order of the array **/
	public static int[] revert(int[] a) {
		for(int i = 0; i < a.length / 2; i++) {
		    int temp = a[i];
		    a[i] = a[a.length - i - 1];
		    a[a.length - i - 1] = temp;
		}
		return a;
	}
	
	public static <T> T concat(T a, T b) {
	    if (!a.getClass().isArray() || !b.getClass().isArray()) {
	        throw new IllegalArgumentException();
	    }

	    Class<?> resCompType;
	    Class<?> aCompType = a.getClass().getComponentType();
	    Class<?> bCompType = b.getClass().getComponentType();

	    if (aCompType.isAssignableFrom(bCompType)) {
	        resCompType = aCompType;
	    } else if (bCompType.isAssignableFrom(aCompType)) {
	        resCompType = bCompType;
	    } else {
	        throw new IllegalArgumentException();
	    }

	    int aLen = Array.getLength(a);
	    int bLen = Array.getLength(b);

	    @SuppressWarnings("unchecked")
	    T result = (T) Array.newInstance(resCompType, aLen + bLen);
	    System.arraycopy(a, 0, result, 0, aLen);
	    System.arraycopy(b, 0, result, aLen, bLen);        

	    return result;
	}

	/** Returns an array filled with 'value' **/
	public static int[] initIntArray(int size, int value) {
		int[] arr = new int[size];
		Arrays.fill(arr, value);
		return arr;
	}
	
	/** Returns an array filled with 'value' **/
	public static long[] initLongArray(int size, long value) {
		long[] arr = new long[size];
		Arrays.fill(arr, value);
		return arr;
	}

	/** Returns an array filled with 'value' **/
	public static byte[] initByteArray(int size, byte value) {
		byte[] arr = new byte[size];
		Arrays.fill(arr, value);
		return arr;
	}
	
	/** Returns an array filled with 'value' **/
	public static double[] initDoubleArray(int size, double value) {
		double[] arr = new double[size];
		Arrays.fill(arr, value);
		return arr;
	}
	
	/** Returns an array filled with 'value' **/
	public static boolean[] initBoolArray(int size, boolean value) {
		boolean[] arr = new boolean[size];
		Arrays.fill(arr, value);
		return arr;
	}
	
	/** Returns an array filled with 'false' **/
	public static boolean[] initBoolArray(int size) {
		return initBoolArray(size, false);
	}
	
	/** Returns an array where each item is created by a lambda function getting the index **/
	public static <T> ArrayList<T> initArray(int size, Function<Integer, T> initializer) {
		ArrayList<T> list = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			list.add(initializer.apply(i));
		}
		return list;
	}
	
	/** Removes loops in a path **/
	public static int[] shortenPath(int[] path) {
		for (int a = 0; a < path.length; a++) {
			for (int b = path.length - 1; b > a; b--) {
				if (path[a] == path[b]) {
					int[] partA = Arrays.copyOfRange(path, 0, a),
						  partB = Arrays.copyOfRange(path, b, path.length);
					return Lists.concat(partA, partB);
				}
			}
		}
		return path;
	}
	
	/** Returns the sum of an array, ignoring max_values **/
	public static long sum(int[] array) {
		long sum = 0;
		for (int i : array) {
			if (i < Integer.MAX_VALUE)
			sum += i;
		}
		return sum;
	}

	/** Returns the average of an array, ignoring max_values **/
	public static float average(int[] array) {
		long sum = 0, count = 0;
		for (int i : array) {
			if (i < Integer.MAX_VALUE) {
				count++;
				sum += i;
			}
		}
		return sum / count;
	}
	
	/** Returns the average of an array, ignoring max_values **/
	public static float average(long[] array) {
		long sum = 0, count = 0;
		for (long i : array) {
			if (i < Long.MAX_VALUE) {
				count++;
				sum += i;
			}
		}
		return sum / count;
	}

	public static boolean intersect(int[] a, int[] b) {
		for (int x : a) {
			for (int y : b) {
				if (x == y) {
					return true;
				}
			}
		}
		return false;
	}

	public static int countTrue(boolean[] arr) {
		int count = 0;
		for (boolean bool : arr) {
			if (bool) count++;
		}
		return count;
	}
	
	public static IntStream intStream(Collection<Integer> c) {
        return c.stream().mapToInt(Integer::intValue);
    }

	public static LinkedList<Long> linkedList(long[] values) {
		LinkedList<Long> list = new LinkedList<>();
		for (long value : values)
			list.add(value);
		return list;
	}
	
	public static LinkedList<Integer> linkedList(int[] values) {
		LinkedList<Integer> list = new LinkedList<>();
		for (int value : values)
			list.add(value);
		return list;
	}
	
}
