package tests;

import java.util.HashSet;
import java.util.Set;

public class Suite {
	Set<String> rc;
	Set<String> ec;
	int numPassed = 0;
	int ecPassed = 0;
	String name = "";
//	long passTime = 0;
//	long ecPassTime = 0;
	boolean printed = false;
	boolean ecPrinted = false;
	
	public Suite(String name, Set<String> tests) {
		this.name = name;
		this.rc = tests;
		ec = new HashSet<>();
	}
	
	public boolean pass(String test) {
		if (rc.contains(test)) {
			numPassed++;
//			if (passedRC()) {
//				passTime = time;
//			}
			return true;
		} else if (ec.contains(test)) {
			ecPassed++;
//			if (passedEC()) {
//				ecPassTime = time;
//			}
			return true;
		}
		return false;
	}
	
	public boolean printed() {
		return printed;
	}
	
	public void print() {
		printed = true;
	}
	
	public boolean ecPrinted() {
		return ecPrinted;
	}
	
	public void ecPrint() {
		ecPrinted = true;
	}
	
	public boolean passedRC() {
		return numPassed == rc.size();
	}
	
	public boolean passedEC() {
		return ecPassed == ec.size();
	}
	
	public String getName() {
		return name;
	}
	
	public void reset() {
		numPassed = 0;
		ecPassed = 0;
//		passTime = 0;
//		ecPassTime = 0;
		printed = false;
		ecPrinted = false;
	}
	
//	public long getPassTime() {
//		return passTime;
//	}
//	
//	public long getECPassTime() {
//		return ecPassTime;
//	}
	
	public boolean maybeAddEC(String test) {
		if (rc.contains(test)) {
			rc.remove(test);
			ec.add(test);
			return true;
		}
		return false;
	}
	
	public boolean hasEC() {
		return ec.size() > 0;
	}
	
	public boolean hasRC() {
		return rc.size() > 0;
	}
}
