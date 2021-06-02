package drivers;

import java.io.File;
import java.util.Scanner;

import logAnalyzer.AContextBasedReplayer;
import logAnalyzer.ASemesterReplayer;
import logAnalyzer.AnAssignmentReplayer;

public class Driver2 extends Driver{
	public static String[] folderPath = {
										"E:\\submissions\\533",
										"E:\\submissions\\524"};
	public static String prefix = "Assignment ";
	public static int[] assignments = {
										1,
										2,
										3,
										4,
										5,
										6
										};
	
	public static void main(String[] args) {
		Scanner s = new Scanner(System.in);
		chooseMode(s);
		System.out.println("Generate logs? (Y/N):");
		boolean generate = true;
		while (true) {
				switch (s.nextLine().toLowerCase()) {
				case "y":
				case "yes":
				case "generate":
				case "g":
					generate = true;
					break;
				case "n":
				case "no":
					generate = false;
					break;
				default:
					System.out.println("Must be yes or no");
					break;
				}
				break;
		}
		s.close();
		if (replayer.getClass() == AContextBasedReplayer.class) {
			replayer = new AContextBasedReplayer(multiplier, defaultPauseTime);
		} else if (replayer.getClass() == AnAssignmentReplayer.class) {
			replayer = new AnAssignmentReplayer();
		} else if (replayer.getClass() == ASemesterReplayer.class) {
			replayer = new ASemesterReplayer();
		}
		for (String folder : folderPath) {
			for (int assign : assignments) {
				path = folder + File.separator + prefix + assign;
				isRead = false;
				if (generate) {
					delete();
					generate();
				}
				analyze();
			}
		}
		System.exit(0);
	}
}