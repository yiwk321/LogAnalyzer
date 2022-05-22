package analyzerListeners;

import java.util.Arrays;

public class StackTraceData {
	String exceptionName;
	StackTraceElement[] stackTrace;
	
	
	public StackTraceData(String anExceptionName, StackTraceElement[] aStackTrace) {
		exceptionName = anExceptionName;
		stackTrace = aStackTrace;
	}
	
	public String toString() {
		return exceptionName + " " + Arrays.toString(stackTrace);
	}
}
