package analyzerListeners;

public class ExceptionPrinterFactory {
static ExceptionPrinter exceptionPrinter;

public static ExceptionPrinter getExceptionPrinter() {
	if (exceptionPrinter == null) {
		exceptionPrinter = new AnExceptionPrinter();
	}
	return exceptionPrinter;
}

public static void setExceptionPrinter(ExceptionPrinter newVal) {
	exceptionPrinter = newVal;
}

}
