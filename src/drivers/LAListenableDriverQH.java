package drivers;

import analyzer.extension.replayView.ReplayListener;
import analyzerListeners.CommandPrinterFactory;
import analyzerListeners.ExceptionGathererFactory;
import analyzerListeners.ExceptionPrinterFactory;
import logAnalyzer.AContextBasedReplayer;
import logAnalyzer.ASemesterReplayer;
import logAnalyzer.AnAssignmentReplayer;
import logAnalyzer.AnExperimentReplayer;
import logAnalyzer.ReplayerListener;

public class LAListenableDriverQH extends Driver{
	public static String[] folders = {
//										"E:\\submissions\\533",
//										"E:\\submissions\\524",
//										"C:\\Users\\Zhizhou\\OneDrive\\UNC CH\\Junior 1st Sem\\hermes\\git\\Hermes\\Hermes\\data\\ExperimentalData",
//										"D:\\Assignment 4",
//										"D:\\Assignment 4\\Bruno, Adrian(abruno)\\Submission attachment(s)",
//										"E:\\Test\\Assignment 4"
			"/Users/jean-hong/Downloads/Assignment0"
										};
	
	public static ReplayerListener[] replayerListeners = {
//			CommandPrinterFactory.getCommandPrinter(),
			ExceptionGathererFactory.getExceptionGatherer()
	};
	
	public static void main(String[] args) {

		boolean generate = true;
//		boolean generate = false;

		for (String folder: folders) {
			
			ListenableFolderAnalyzer.analyzeFolder(folder, true, true, replayerListeners);
		}
		
		System.out.println(ExceptionGathererFactory.getExceptionGatherer().getStackTraces());

	}
}