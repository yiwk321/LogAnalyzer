package logAnalyzer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class LogAnalyzerLoggerFactory {
	static LogAnalyzerAssignmentMetrics logAnalyzerAssignmentMetrics = new LogAnalyzerAssignmentMetrics();
	static FileWriter specificLogger;

	public static LogAnalyzerAssignmentMetrics getLogAnalyzerAssignmentMetrics() {
		return logAnalyzerAssignmentMetrics;
	}

	public static FileWriter getSpecificLogger() {
		return specificLogger;
	}

//	public LogAnalyzerLoggerFactory(File aFolder) {
//		try {
//			createSpecificLoggerAndMetrics(aFolder);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
	
	public static void closeLoggerAndMetrics() {
		logMessage(logAnalyzerAssignmentMetrics.toString());
		try {
		if (specificLogger != null) {
			specificLogger.close();
		}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static void createLoggerAndMetrics(File aFile) throws IOException {
//		File folder = new File(folderName);
		
		File aBaseFolder = aFile;
		String[] aNameParts = aFile.getName().split("\\.");
		String aFileNameWithoutSuffix = aNameParts[0];
//		File specificLoggerFile = new File(aBaseFolder, aFile.getName() + " Log.csv");
		File specificLoggerFile = new File(aBaseFolder, aFileNameWithoutSuffix + " Log.csv");

//		File specificLoggerFile = new File(aFile.getParentFile(), aFile.getName() + " Log.csv");
		if (!specificLoggerFile.exists()) {
			specificLoggerFile.createNewFile();
		}
		specificLogger = new FileWriter(specificLoggerFile);
	}
	
	public static  void logMessage(String aMessage) {
		try {
			specificLogger.write(aMessage);
		
		specificLogger.flush();
		} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		logAnalyzerAssignmentMetrics = new LogAnalyzerAssignmentMetrics();

	}

}
