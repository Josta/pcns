package utility.global;

/** Tool for measuring elapsed real time **/
public class StopWatch {

	private static long startTime;
	
	private StopWatch() {}
	
	public static void start(String label) {
		start();
		System.out.println(label);
	}
	
	public static void start() {
		startTime = System.nanoTime();
	}
	
	public static double measure() {
		long stopTime = System.nanoTime();
		double runtime =  (stopTime - startTime) / 1000000.0;
		System.out.println(" (Runtime: " + runtime + "ms)");
		startTime = stopTime;
		return runtime;
	}
	
}
