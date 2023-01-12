package logAnalyzer;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import analyzer.extension.replayView.FileUtility;

public class RemoveCopiedLogs {
	static String[] assigns = {
			"F:\\Hermes Data\\301ss21\\Assignment 1", 
			"F:\\Hermes Data\\301ss21\\Assignment 2",
			"F:\\Hermes Data\\301ss21\\Assignment 3",
			"F:\\Hermes Data\\301ss21\\Assignment 4",
			};
	Set<String> logs = new HashSet<>();
	
	public static void main(String[] args) {
		RemoveCopiedLogs removeCopiedLogs = new RemoveCopiedLogs();
		removeCopiedLogs.removeCopiedLogs(assigns);
	}
	
	public void removeCopiedLogs(String[] assigns) {
		for (String assignString : assigns) {
			File assign = new File(assignString);
			System.out.println("Reading assignment " + assign);
			if (!assign.exists()) {
				System.out.println("Folder " + assign + " does not exist");
				return;
			}
			for (File student : assign.listFiles((parent, fileName)->{
				return fileName.contains(",") && fileName.contains("(");
			})) {
				File[] logFiles = getLogFiles(student);
				if (logFiles==null) {
					continue;
				}
				for (File logFile : logFiles) {
					if (logFile.getName().endsWith(".lck")) {
						logFile.delete();
						continue;
					} 
					if (logs.contains(logFile.getName())) {
						logFile.delete();
						System.out.println("Deleted " + logFile.getPath());
						if (logFile.getParent().endsWith("Generated")) {
							File originalLog = new File(logFile.getParentFile().getParent(), logFile.getName());
							originalLog.delete();
							File lckFile = new File(originalLog.getPath()+".lck");
							System.out.println("Deleted " + originalLog.getPath());
							if (lckFile.exists()) {
								lckFile.delete();
							}
						} else {
							File logFolder = logFile.getParentFile();
							File[] generated = logFolder.listFiles((file)->{
								return file.isDirectory();
							});
							if (generated.length == 1) {
								File generatedLog = new File(logFile.getParent(), "\\Generated\\" + logFile.getName());
								generatedLog.delete();
								System.out.println("Deleted " + generatedLog.getPath());
							}
						}
					} else {
						logs.add(logFile.getName());
					}
				}
			}
		}
	}
	
	public File[] getLogFiles(File student) {
		System.out.println("Reading student " + student);
		if (!student.exists()) {
			System.out.println("Folder " + student + " does not exist");
			return null;
		}
		File logFolder = null;
		File submission = new File(student, "Submission attachment(s)");
		if (submission.exists()) {
			logFolder = getProjectFolder(submission);
			if (logFolder != null) {
				logFolder = new File(logFolder, "Logs" + File.separator + "Eclipse");
			} else if (FileUtility.unzip(submission)) {
				logFolder = getProjectFolder(submission);
				if (logFolder != null) {
					logFolder = new File(logFolder, "Logs" + File.separator + "Eclipse");
				}
			}
		} else {
			logFolder = new File(student, "Eclipse");
			if (!logFolder.exists()) {
				logFolder = getProjectFolder(student);
				if (logFolder != null) {
					logFolder = new File(logFolder, "Logs" + File.separator + "Eclipse");
				}
			}
		}
		if (logFolder == null || !logFolder.exists()) {
			System.out.println("No logs found for student " + student.getName());
			return null;
		}
		File[] logFiles = logFolder.listFiles(File::isDirectory);
		if (logFiles != null && logFiles.length > 0) {
			logFiles = logFiles[0].listFiles((file) -> {
				return file.getName().startsWith("Log");
			});
		} else {
			logFiles = logFolder.listFiles((file) -> {
				return file.getName().startsWith("Log");
			});
		}
		if (logFiles == null) {
			System.out.println("No logs found for student " + student.getName());
			return null;
		}
		return logFiles;
	}
	
	public File getProjectFolder(File folder) {
		for (File file : folder.listFiles(File::isDirectory)) {
			if (file.getName().equals("src")) {
				return folder;
			}
		}
		for (File file : folder.listFiles(File::isDirectory)) {
			if ((file = getProjectFolder(file)) != null) {
				return file;
			}
		}
		return null;
	}
}
