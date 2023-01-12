package logAnalyzer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import analyzer.extension.replayView.FileUtility;
import fluorite.commands.EHICommand;
import fluorite.util.EHLogReader;

public class LogReader {
	static EHLogReader reader = new EHLogReader();

	
	public static Map<String, List<List<EHICommand>>> readAssignment(File assign) {
		System.out.println("Reading assignment " + assign);
		if (!assign.exists()) {
			System.out.println("Folder " + assign + " does not exist");
			return null;
		}
		Map<String, List<List<EHICommand>>> logs = new TreeMap<>();
		for (File student : assign.listFiles((parent, fileName) -> {
			return fileName.contains(",") && fileName.contains("(");
		})) {
			List<List<EHICommand>> ret = readStudent(student);
			if (ret != null && !ret.isEmpty()) {
				logs.put(student.getPath(), ret);
			}
		}
		return logs;
	}

	public static List<List<EHICommand>> readStudent(File student) {
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
				return file.getName().startsWith("Log") && file.getName().endsWith(".xml");
			});
		} else {
			logFiles = logFolder.listFiles((file) -> {
				return file.getName().startsWith("Log") && file.getName().endsWith(".xml");
			});
		}
		if (logFiles == null) {
			System.out.println("No logs found for student " + student.getName());
			return null;
		}
		List<List<EHICommand>> logs = new ArrayList<>();
		for (File logFile : logFiles) {
			if (logFile.length() == 0) {
				logFile.delete();
				continue;
			}
			List<EHICommand> ret = readOneLogFile(logFile);
			if (ret != null) {
				logs.add(ret);
			} else {
				System.err.println("Need to append <Events>");
			}
		}
		return logs;
	}

	public static List<EHICommand> readOneLogFile(File log) {
		List<EHICommand> retVal = readOneLogFileWthoutAppending(log, false);
		return retVal;
	}

	public static List<EHICommand> readOneLogFileWthoutAppending(File log, boolean printError) {
		String path = log.getPath();
		if (!log.exists()) {
			System.err.println("log does not exist:" + path);
			return null;
		}
		if (!path.endsWith(".xml")) {
			System.err.println("log is not in xml format:" + path);
			return null;
		}
		try {
			List<EHICommand> commands = reader.readAll(path);
			return commands;
		} catch (Exception e) {
			if (printError) {
				System.err.println("Could not read file" + path + "\n" + e);
				e.printStackTrace();
			}
		}
		return null;
	}

	public static File getProjectFolder(File folder) {
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
