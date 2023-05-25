package drivers;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import logAnalyzer.AContextBasedReplayer;
import logAnalyzer.ASemesterReplayer;
import logAnalyzer.AnAssignmentReplayer;
import logAnalyzer.AnExperimentReplayer;
import logAnalyzer.LogAnalyzerLoggerFactory;
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
	static boolean hasLastAssignment = false;
	

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
	public static void setHasLastAssignment(boolean newVal) {
		Driver2.hasLastAssignment = newVal;
	}
	public static boolean hasLastAssignment() {
		return hasLastAssignment;
	}
	
	static Map<String, Integer> lastAssignmentMissing = new HashMap();
	static int currentAssignmentNumber = 0;
	public static void missingCurrentAssignment(String aStudent) {
		lastAssignmentMissing.put(aStudent, currentAssignmentNumber);
	}
	
	static int numAssignments;
	public static boolean isMissingPreviousAssignment(String aStudent) {
		Integer aPreviousAssignmentNumber = 
				lastAssignmentMissing.get(aStudent);
		return (aPreviousAssignmentNumber != null) &&
				(aPreviousAssignmentNumber == currentAssignmentNumber - 1);
	}
	
	public static boolean isLastAssignment() {
		return (currentAssignmentNumber == (numAssignments - 1)) && hasLastAssignment();
	}
	
	
	public static void main(String[] args) {
		for (String course : courses) {
			File courseFolder = new File(course);
			File[] assigns = courseFolder.listFiles((file)->{
				return file.isDirectory() && file.getName().startsWith("Assignment ");
			});
			String[] folders = new String[assigns.length];
			numAssignments = assigns.length;

			for (int i = 0; i < assigns.length; i++) {
				folders[i] = assigns[i].getPath(); 
			}
			new RemoveCopiedLogs().removeCopiedLogs(folders);
			for (int i = 0; i < folders.length; i++) {
				currentAssignmentNumber = i;
				String folder = folders[i];
//				replayer = new AContextBasedReplayer(multiplier, defaultPauseTime);
				replayer = new AnAssignmentReplayer();

				path = folder;
				isRead = false;
				if (generate) {
					delete();
					generate(i == folders.length-1 && hasLastAssignment);
//					generate(false);
				}
				if (analyze) {
				analyze();
				}
				LogAnalyzerLoggerFactory.closeLoggerAndMetrics();
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