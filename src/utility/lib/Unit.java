package utility.lib;

import utility.global.Config;

public class Unit {

	public static final int
		SAT = 1,
		BIT = 100,
		BTC = 100000000;
	
	public static final float
		SECONDS = 1f,
		MINUTES = 60f,
		PER_SECOND = 1f,
		PER_MINUTE = 60f;
		
	public static int bit(double bits) {
		return (int) (bits * BIT);
	}
	
	public static int btc(double btcs) {
		return (int) (btcs * BTC);
	}
	
	public static int euro(double euros) {
		return (int) (euros / Config.getFloat("BITCOIN_EUROS") * BTC);
	}
	
	public static float toBIT(int sat) {
		return sat / (float) BIT;
	}
	
	public static float toBTC(int sat) {
		return sat / (float) BTC;
	}
	
	public static float toEUR(int sat) {
		return Config.getFloat("BITCOIN_EUROS") * sat / BTC;
	}
	
	
	
	private Unit() {}
	
}
