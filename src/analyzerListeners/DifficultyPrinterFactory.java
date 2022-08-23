package analyzerListeners;

public class DifficultyPrinterFactory {
static DifficultyPrinter exceptionPrinter;

public static DifficultyPrinter getDifficultyPrinter() {
	if (exceptionPrinter == null) {
		exceptionPrinter = new ADifficultyPrinter();
	}
	return exceptionPrinter;
}

public static void setDifficultyPrinter(DifficultyPrinter newVal) {
	exceptionPrinter = newVal;
}

}
