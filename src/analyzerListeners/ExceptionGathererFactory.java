package analyzerListeners;

public class ExceptionGathererFactory {
static ExceptionGatherer exceptionGatherer;

public static ExceptionGatherer getExceptionGatherer() {
	if (exceptionGatherer == null) {
		exceptionGatherer = new AnExceptionGatherer();
	}
	return exceptionGatherer;
}

public static void setExceptionGatherer(ExceptionGatherer newVal) {
	exceptionGatherer = newVal;
}

}
