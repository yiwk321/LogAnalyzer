package drivers;

import analyzer.extension.replayView.ReplayListener;
import analyzerListeners.CommandPrinterFactory;
import analyzerListeners.ExceptionPrinterFactory;
import logAnalyzer.AContextBasedReplayer;
import logAnalyzer.ASemesterReplayer;
import logAnalyzer.AnAssignmentReplayer;
import logAnalyzer.AnExperimentReplayer;
import logAnalyzer.ReplayerListener;

public class LAListenableDriverPD extends Driver{
	public static String[] folders = {
//										"E:\\submissions\\533",
//										"E:\\submissions\\524",
//										"C:\\Users\\Zhizhou\\OneDrive\\UNC CH\\Junior 1st Sem\\hermes\\git\\Hermes\\Hermes\\data\\ExperimentalData",
//										"D:\\Assignment 4",
//										"D:\\Assignment 4\\Bruno, Adrian(abruno)\\Submission attachment(s)",
//										"E:\\Test\\Assignment 4"
			"C:\\Users\\dewan\\Downloads\\Assignment0A\\Assignment 0"
										};
	
	public static ReplayerListener[] replayerListeners = {
//			CommandPrinterFactory.getCommandPrinter(),
			ExceptionPrinterFactory.getExceptionPrinter()
	};
	
	public static void main(String[] args) {

		boolean generate = true;
//		boolean generate = false;

		for (String folder: folders) {
			
			ListenableFolderAnalyzer.analyzeFolder(folder, true, true, replayerListeners);
		}

	}
}