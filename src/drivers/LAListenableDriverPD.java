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

//			"C:\\Users\\dewan\\Downloads\\Assignment1A\\Assignment 1"
//			"C:\\Users\\dewan\\Downloads\\Assignment2A\\Assignment 2",
//			"C:\\Users\\dewan\\Downloads\\Assignment3A\\Assignment 3",
//			"C:\\Users\\dewan\\Downloads\\Assignment4A\\Assignment 4",
//			"C:\\Users\\dewan\\Downloads\\Assignment1_1A\\Assignment 1_1",
//			"C:\\Users\\dewan\\Downloads\\Assignment2_1A\\Assignment 2_1"
										};
	
	public static ReplayerListener[] replayerListeners = {
//			CommandPrinterFactory.getCommandPrinter(),
			ExceptionPrinterFactory.getExceptionPrinter()
	};
	
	public static void main(String[] args) {

		boolean generate = true;
		boolean delete = true;
//		boolean generate = false;

		for (String folder: folders) {
			
			ListenableFolderAnalyzer.analyzeFolder(folder, generate, delete, replayerListeners);
		}

	}
}