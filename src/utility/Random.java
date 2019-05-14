package utility;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import utility.lib.Lists;

public class Random {
	
	private java.util.Random rand;
	
	public Random() {
		rand = new java.util.Random(System.currentTimeMillis());
	}
	
	public Random(long seed) {
		rand = new java.util.Random(seed);
	}

	public java.util.Random get() {
		return rand;
	}
	
	public int getInt(int limit) {
		return rand.nextInt(limit);
	}
	
	public <T> T getOne(List<T> list) {
		return list.get(rand.nextInt(list.size()));
	}
	
	public <T> T getOne(Collection<T> coll) {
	    int num = rand.nextInt(coll.size());
	    for(T t: coll) if (--num < 0) return t;
	    throw new AssertionError();
	}
	
	public <T> List<T> getDistinct(List<T> list, int n) {
		if (n > list.size()) {
			throw new IllegalArgumentException();
		}
		ArrayList<T> result = new ArrayList<>();
		for (int index : getDistictInts(n, list.size())) {
			result.add(list.get(index));
		}
		return result;
		
	} 
	
	public long getLong() {
		return rand.nextLong();
	}
	
	public float getFloat() {
		return rand.nextFloat();
	}
	
	public int getIntBetween(int low, int high) {
		return low + (int) (rand.nextFloat() * (high - low));
	}
	
	public long getLongBetween(long low, long high) {
		return low + (long) (rand.nextDouble() * (high - low));
	}
	
	public int[] getDistictInts(int n, int limit) {
		if (n >= limit) {
			throw new IllegalArgumentException();
		}
        Set<Integer> picked = new HashSet<>();
        while (picked.size() < n) {
            picked.add(rand.nextInt(limit));
        }
        List<Integer> list = new ArrayList<>(picked);
        Collections.shuffle(list, rand);
        return Lists.array(list);
    }

	public double getDouble() {
		return rand.nextDouble();
	}
	
}
