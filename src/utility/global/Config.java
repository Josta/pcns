package utility.global;

import java.util.HashMap;

/**
 * Tool for storing/retrieving configuration values.
 * @author Josua
 */
public class Config {

	private static HashMap<String, String> overwrite = new HashMap<String, String>();
	
	private Config() {}
	
	public static String get(String key) {
		return overwrite.get(key);
	}
	
	public static void set(String key, String value) {
		overwrite.put(key, value);
	}
	
	public static void set(String key, boolean value) {
		overwrite.put(key, String.valueOf(value));
	}
	
	public static void set(String key, float value) {
		overwrite.put(key, String.valueOf(value));
	}
	
	public static void set(String key, long value) {
		overwrite.put(key, String.valueOf(value));
	}
	
	public static boolean getBoolean(String key) {
		return Boolean.parseBoolean(get(key));
	}

	public static int getInt(String key) {
		return Integer.parseInt(get(key));
	}

	public static double getDouble(String key) {
		return Double.parseDouble(get(key));
	}

	public static float getFloat(String key) {
		return Float.parseFloat(get(key));
	}

	public static long getLong(String key) {
		return Long.parseLong(get(key));
	}

	
}
