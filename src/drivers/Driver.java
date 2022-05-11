package drivers;

import java.io.File;
import java.util.Scanner;
import logAnalyzer.AContextBasedReplayer;
import logAnalyzer.ASemesterReplayer;
import logAnalyzer.AnAssignmentReplayer;
import logAnalyzer.AnExperimentReplayer;
import logAnalyzer.Replayer;
import mains.Main;

public class Driver {
//	private static String classFolderPath = "E:\\testdata\\Fall2020";
//	private static String experimentalClassFolderPath = "C:\\Users\\Zhizhou\\OneDrive\\UNC CH\\Junior 1st Sem\\hermes\\git\\Hermes\\Hermes\\data\\ExperimentalData";
	public static Replayer replayer;
	static String path = "";
	static boolean isRead = false;
	public static double multiplier = 1;
	public static int defaultPauseTime = -1;
	static String generateFolderName = "Generated";
	static int generateCommandType = Replayer.LOCALCHECK;
	
	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		chooseMode(scanner);
		enterPath(scanner);
		out:
		while(true) {
			System.out.println("Enter Command: read/generate/analyze/change/delete/quit");
			switch (scanner.nextLine().toLowerCase()) {
			case "generate":
			case "g":
				generate();
				break;
			case "analyze":
			case "a":
				analyze();
				break;
			case "change":
			case "c":
				enterPath(scanner);
				break;
			case "delete":
			case "d":
				delete();
				break;
			case "read":
			case "r":
				read();
				break;
			case "quit":
			case "q":
				break out;
			default:
				System.out.println("Command Not Recognized, Enter Again");
				break;
			}
		}
		scanner.close();
		System.exit(0);
	}
	
	public static void generate() {
		System.out.println("Generating LocalChecks log");
//		String[] args = {path};
//		Main.main(args);
		if (!isRead) read();
//		replayer.createExtraCommand("Generated", Replayer.LOCALCHECK);
//		replayer.createExtraCommand("Generated", Replayer.PAUSE);
		replayer.createExtraCommand(generateFolderName, generateCommandType);
		isRead = false;
	}
	
	public static void analyze() {
		if (!isRead) read();
		replayer.analyze();
	}
	
	public static void delete() {
		replayer.delete(path);
	}
	
	public static void read() {
		if (new File(path).exists()) {
			replayer.readLogs(path);
			isRead = true;
		} 
	}
	
	public static void chooseMode(Scanner scanner) {
		while (true){
			System.out.println("Choose mode: semester/assign/intell/experiment");
			String input = scanner.nextLine();
			switch (input.toLowerCase()) {
			case "assign":
			case "a":
				replayer = new AnAssignmentReplayer();
				return;
			case "semester":
			case "s":
				replayer = new ASemesterReplayer();
				return;
			case "intell":
			case "i":
				System.out.println("Enter pause time multiplier (default is 1):");
				while (true) {
					try {
						String string = scanner.nextLine();
						if (string.isEmpty()) {
							break;
						}
						multiplier = Double.parseDouble(string);
						break;
					} catch (Exception e) {
						System.out.println("Multiplier must be double or empty line");
					}
				}
				System.out.println("Enter default pause time (default is 5 min):");
				while (true) {
					try {
						String string = scanner.nextLine();
						if (string.isEmpty()) {
							break;
						}
						defaultPauseTime = Integer.parseInt(string);
						break;
					} catch (Exception e) {
						System.out.println("Default pause time must be int or empty line");
					}
				}
				replayer = new AContextBasedReplayer(multiplier, defaultPauseTime);
				return;
			case "experiment":
			case "e":
//				System.out.println("Enter pause time multiplier (default is 1):");
//				while (true) {
//					try {
//						String string = scanner.nextLine();
//						if (string.isEmpty()) {
//							break;
//						}
//						multiplier = Double.parseDouble(string);
//						break;
//					} catch (Exception e) {
//						System.out.println("Multiplier must be double or empty line");
//					}
//				}
//				System.out.println("Enter default pause time (default is 5 min):");
//				while (true) {
//					try {
//						String string = scanner.nextLine();
//						if (string.isEmpty()) {
//							break;
//						}
//						defaultPauseTime = Integer.parseInt(string);
//						break;
//					} catch (Exception e) {
//						System.out.println("Default pause time must be int or empty line");
//					}
//				}
				replayer = new AnExperimentReplayer(multiplier, defaultPauseTime);
				return;
			default:
				System.out.println("Command Not Recognized, Enter Again");
				break;
			}
		}
	}
	
	public static void enterPath(Scanner scanner) {
		isRead = false;
		while (true) {
			System.out.println("Enter Folder Path:");
			path = scanner.nextLine();
			if (new File(path).exists()) {
				return;
			} 
			System.out.println("Path Does Not Exist, Enter Again");
		}
	}
}
















