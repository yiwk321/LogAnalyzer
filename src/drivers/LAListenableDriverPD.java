package drivers;

import java.util.Date;

import analyzer.extension.replayView.ReplayListener;
import analyzerListeners.CommandPrinterFactory;
import analyzerListeners.DifficultyPrinterFactory;
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
//			"C:\\Users\\dewan\\Downloads\\Assignment 4A\\Assignment 4"

//			"C:\\Users\\dewan\\Downloads\\Assignment 1A\\Assignment 1"
//			"C:\\Users\\dewan\\Downloads\\Assignment 2A\\Assignment 2",
//			"C:\\Users\\dewan\\Downloads\\Assignment 3A\\Assignment 3",
//			"C:\\Users\\dewan\\Downloads\\Assignment 4A\\Assignment 4",
//			"C:\\Users\\dewan\\Downloads\\Assignment 1_1A\\Assignment 1_1",
//			"C:\\Users\\dewan\\Downloads\\Assignment 2_1A\\Assignment 2_1"
			"F:\\Hermes Data\\Assignment 0"
										};
	
	public static ReplayerListener[] replayerListeners = {
//			CommandPrinterFactory.getCommandPrinter(),
//			ExceptionPrinterFactory.getExceptionPrinter()
			DifficultyPrinterFactory.getDifficultyPrinter()

	};
	
	public static void main(String[] args) {

		boolean generate = true;
		boolean delete = true;
//		boolean generate = false;
//		boolean delete = false;

//		Date aDate = new Date(1621961225203L);
//		System.out.println ("date:" + aDate);

		for (String folder: folders) {
			
			ListenableFolderAnalyzer.analyzeFolder(folder, generate, delete, replayerListeners);
		}

	}
}