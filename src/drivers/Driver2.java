package drivers;

import java.io.File;

import logAnalyzer.AContextBasedReplayer;
import logAnalyzer.ASemesterReplayer;
import logAnalyzer.AnAssignmentReplayer;
import logAnalyzer.AnExperimentReplayer;
import logAnalyzer.RemoveCopiedLogs;
import logAnalyzer.Replayer;

public class Driver2 extends Driver{ 
	public static String[] courses = {
//										"H:\\CompPaper\\524f21\\Assignment 1",
//										"H:\\CompPaper\\524f21\\Assignment 2",
//										"H:\\CompPaper\\524f21\\Assignment 3",
//										"H:\\CompPaper\\524f21\\Assignment 4",
//										"H:\\CompPaper\\524f21\\Assignment 5",
//										"H:\\CompPaper\\524f21\\Assignment 6",
//										"H:\\CompPaper\\533s22",
//										"H:\\CompPaper\\401f16",
//										"H:\\CompPaper\\401f17",
										"H:\\CompPaper\\401f18",
//										"H:\\CompPaper\\524f19",
//										"H:\\CompPaper\\524f20",
//										"H:\\CompPaper\\533s18",
//										"H:\\CompPaper\\533s19",
//										"H:\\CompPaper\\533s20",
//										"H:\\CompPaper\\533s21",
//										"H:\\CompPaper\\533s22\\Assignment 2",
//										"H:\\CompPaper\\533s22\\Assignment 3",
//										"H:\\CompPaper\\533s22\\Assignment 4",
//										"H:\\CompPaper\\533s22\\Assignment 5",
//										"H:\\CompPaper\\533s22\\Assignment 6",
//										"H:\\CompPaper\\533s22\\Assignment 7",
										};
	public static String prefix = "Assignment ";
//	public static int[] assignments = {
//			0,
//										1,
//										2,
//										3,
//										4,
//										5,
//										6,
//										7
//										};
	static boolean generate = true;
	static boolean isLastAssignment = false;
	

	static boolean analyze = true;
//	static boolean generate = false;
	static String generateFolderName = "Generated";
	static int generateCommandType = Replayer.LOCALCHECK;
	public static void setCourses(String[] aCourses) {
		courses = aCourses;
	}
	public static void setAnalyze(boolean newVal) {
		analyze =  newVal;
	}
	public static void hasLastAssignment(boolean isLastAssignment) {
		Driver2.isLastAssignment = isLastAssignment;
	}
	
	public static void main(String[] args) {
		for (String course : courses) {
			File courseFolder = new File(course);
			File[] assigns = courseFolder.listFiles((file)->{
				return file.isDirectory() && file.getName().startsWith("Assignment ");
			});
			String[] folders = new String[assigns.length];
			for (int i = 0; i < assigns.length; i++) {
				folders[i] = assigns[i].getPath(); 
			}
			new RemoveCopiedLogs().removeCopiedLogs(folders);
			for (int i = 0; i < folders.length; i++) {
				String folder = folders[i];
//				replayer = new AContextBasedReplayer(multiplier, defaultPauseTime);
				replayer = new AnAssignmentReplayer();

				path = folder;
				isRead = false;
				if (generate) {
					delete();
					generate(i == folders.length-1 && isLastAssignment);
//					generate(false);
				}
				if (analyze) {
				analyze();
				}
			}
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