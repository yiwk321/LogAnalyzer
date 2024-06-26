package drivers;

import logAnalyzer.AContextBasedReplayer;
import logAnalyzer.ASemesterReplayer;
import logAnalyzer.AnAssignmentReplayer;
import logAnalyzer.AnExperimentReplayer;

public class LADriverPD extends Driver{
	public static String[] folders = {
//										"E:\\submissions\\533",
//										"E:\\submissions\\524",
//										"C:\\Users\\Zhizhou\\OneDrive\\UNC CH\\Junior 1st Sem\\hermes\\git\\Hermes\\Hermes\\data\\ExperimentalData",
//										"D:\\Assignment 4",
//										"D:\\Assignment 4\\Bruno, Adrian(abruno)\\Submission attachment(s)",
//										"E:\\Test\\Assignment 4"
			"C:\\Users\\dewan\\Downloads\\Assignment0A\\Assignment 0"
										};
	public static String prefix = "Assignment ";
	public static int[] assignments = {
										0
//										1,
//										2,
//										3,
//										4,
//										5,
//										6
										};
	
	public static void main(String[] args) {

		boolean generate = true;
//		boolean generate = false;

		for (String folder: folders) {
//			replayer = new AContextBasedReplayer(multiplier, defaultPauseTime);
			replayer = new AnAssignmentReplayer();

			path = folder;
			isRead = false;
			if (generate) {
				delete();
				try {
				generate(false);  
				} catch (Exception e) {
					e.printStackTrace();
				}
				
			}
			analyze();
		}
//			replayer = new AnExperimentReplayer(multiplier, defaultPauseTime);
//		}
//		for (String folder : folders) {
//			for (int assign : assignments) {
//				path = folder + File.separator + prefix + assign;
//				path = folderPath[0];
//				isRead = false;
//				if (generate) {
//					delete();
//					generate();
//				}
//				analyze();
//			}
//		}
		System.exit(0);
	}
}