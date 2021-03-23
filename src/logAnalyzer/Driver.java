package logAnalyzer;

import java.io.File;
import java.util.Scanner;

public class Driver {
//	E:\testdata\Fall2020
//	C:\Users\Zhizhou\OneDrive\UNC CH\Junior 1st Sem\hermes\git\Hermes\Hermes\data\ExperimentalData
//	private static String classFolderPath = "E:\\testdata\\Fall2020";
//	private static String experimentalClassFolderPath = "C:\\Users\\Zhizhou\\OneDrive\\UNC CH\\Junior 1st Sem\\hermes\\git\\Hermes\\Hermes\\data\\ExperimentalData";
	private static Replayer replayer;
	static String path = "";
	
	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		chooseMode(scanner);
		enterPath(scanner);
		out:
		while(true) {
			System.out.println("Enter Command: generate/analyze/change/delete/quit");
			switch (scanner.nextLine().toLowerCase()) {
			case "generate":
				generate();
				break;
			case "analyze":
				analyze();
				break;
			case "change":
				enterPath(scanner);
				break;
			case "delete":
				delete();
				break;
			case "quit":
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
		replayer.createExtraCommand("Generated", Replayer.LOCALCHECK);
	}
	
	public static void analyze() {
		replayer.analyze();
	}
	
	public static void delete() {
		replayer.delete(path);
	}
	
	public static void chooseMode(Scanner scanner) {
		while (true){
			System.out.println("Choose mode: semester/assign");
			String input = scanner.nextLine();
			if (input.contains("assign")) {
				replayer = new AnAssignmentReplayer();
				return;
			} else if (input.contains("semester")){
				replayer = new ASemesterReplayer();
				return;
			}
			System.out.println("Command Not Recognized, Enter Again");
		}
	}
	
	public static void enterPath(Scanner scanner) {
		while (true) {
			System.out.println("Enter Folder Path:");
			path = scanner.nextLine();
			if (new File(path).exists()) {
				replayer.readLogs(path);
				return;
			} 
			System.out.println("Path Does Not Exist, Enter Again");
		}
	}
}

















