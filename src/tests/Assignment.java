package tests;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Assignment {
	Set<Suite> suites;
	String assignment;
	boolean printed = false;
	boolean ecPrinted = false;
	
	public Assignment(String assignment) {
		this.assignment = assignment;
		suites = new HashSet<>();
	}
	
	public Set<Suite> getSuites(){
		return suites;
	}
	
	public void addSuite(Suite suite) {
		suites.add(suite);
	}
	
	public void addEC(String test) {
		for (Suite suite : suites) {
			if (suite.maybeAddEC(test)) {
				return;
			}
		}
	}
	
	public void reset() {
		printed = false;
		ecPrinted = false;
		for (Suite suite : suites) {
			suite.reset();
		}
	}
	
	public List<Suite> pass(String test) {
		List<Suite> list = new ArrayList<>();
		for (Suite suite : suites) {
			if (suite.pass(test) && ((suite.hasRC() && suite.passedRC() && !suite.printed()) || (suite.hasEC() && suite.passedEC() && !suite.ecPrinted()))) {
				list.add(suite);
			}
		}
		if (list.size() == 0) {
			return null;
		}
		return list;
	}
	
	public boolean rcPassed() {
		for (Suite suite : suites) {
			if (suite.hasRC() && !suite.passedRC()) {
				return false;
			}
		}
		return true;
	}
	
	public boolean ecPassed() {
		boolean hasEC = false;
		for (Suite suite : suites) {
			if (suite.hasEC()) {
				hasEC = true;
			}
		}
		if (!hasEC) {
			return false;
		}
		for (Suite suite : suites) {
			if (suite.hasEC() && !suite.passedEC()) {
				return false;
			}
		}
		return true;
	}
	
	public boolean printed() {
		return printed;
	}
	
	public void print() {
		printed = true;
	}
	
	public void ecPrint() {
		ecPrinted = true;
	}
	
	public boolean ecPrinted() {
		return ecPrinted;
	}
}
