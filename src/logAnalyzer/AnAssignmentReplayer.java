package logAnalyzer;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;

import fluorite.commands.EHICommand;
import fluorite.commands.EclipseCommand;

public class AnAssignmentReplayer extends Replayer {
	protected Map<String, Map<String, List<EHICommand>>> allLogs;

	public AnAssignmentReplayer() {
		System.setProperty("user.timezone", "America/New_York");
	}

	public void readLogs(String path) {
		root = new File(path);
		allLogs = readAssignment(root);
	}

	public int countStudents() {
		return allLogs.size();
	}

	public int countAssignments() {
		return 1;
	}

	public void createExtraCommand(CountDownLatch latch, String surfix, int mode) {
		createExtraCommandAssignment(latch, root.getPath(), allLogs, surfix, mode);
	}

	public void createExtraCommandAssignment(CountDownLatch latch, String assign,
			Map<String, Map<String, List<EHICommand>>> assignLog, String surfix, int mode) {
		Map<String, List<String[]>> localCheckEvents = null;
		if (mode == LOCALCHECK) {
			localCheckEvents = readLocalCheckEvents(assign);
		}
		File piazzaPostFile = findPiazzaPostFile(assign);
		File zoomChatsFolder = findZoomChatsFolder(assign);
		for (String student : assignLog.keySet()) {
//			createExtraCommandStudent(latch, assignLog.get(student), student, surfix, mode, localCheckEvents == null ? null : localCheckEvents.get(student));
			createChainedExtraCommandsStudent(latch, assignLog.get(student), student, surfix, mode,
					localCheckEvents == null ? null : localCheckEvents.get(student), piazzaPostFile, zoomChatsFolder);

		}
	}

	public File findPiazzaPostFile(String assign) {
		File[] files = new File(assign).listFiles((parent, fileName) -> {
			return fileName.startsWith("ByAuthorPosts") && fileName.endsWith(".json");
		});
		if (files.length > 0) {
			return files[files.length - 1];
		}
		return null;
	}

	public File findZoomChatsFolder(String assign) {
		File[] files = new File(assign).listFiles((parent, fileName) -> {
			return fileName.equals("ZoomChatsAnon");
		});
		if (files.length > 0) {
			return files[files.length - 1];
		}
		return null;
	}

	public Map<String, Map<String, List<EHICommand>>> readAssignment(File assign) {
		System.out.println("Reading assignment " + assign);
		if (!assign.exists()) {
			System.out.println("Folder " + assign + " does not exist");
			return null;
		}
		Map<String, Map<String, List<EHICommand>>> logs = new TreeMap<>();
		for (File student : assign.listFiles((parent, fileName)->{
			return fileName.contains(",") && fileName.contains("(");
		})) {
			Map<String, List<EHICommand>> ret = readStudent(student);
			if (ret != null && !ret.isEmpty()) {
				logs.put(student.getPath(), ret);
			} else {
				Map<String, List<EHICommand>> emptyMap = new HashMap<>();
				List<EHICommand> emptyLog = new ArrayList<>();
				EclipseCommand command = new EclipseCommand("", 0);
				command.setTimestamp(0);
				command.setStartTimestamp(0);
				emptyLog.add(command);
				emptyLog.add(command);
				emptyLog.add(command);

				File logFolder = null;
				File submission = new File(student,"Submission attachment(s)");
				if (submission.exists()) {
					logFolder = getProjectFolder(submission);
					if (logFolder != null) {
						logFolder = new File(logFolder, "Logs"+File.separator+"Eclipse");
					}
				}
				if (logFolder == null) {
					continue;
				}
				logFolder.mkdirs();
				File emptyLogFile = new File(logFolder, "Log2010-01-01-00-00-00-000.xml");
//				emptyLogFile.createNewFile();
				
				emptyMap.put(emptyLogFile.getPath(), emptyLog);
				logs.put(student.getPath(), emptyMap);

			}
		}
		return logs;
	}

	public void analyze(CountDownLatch latch) {
		analyzeAssignment(latch, root.getPath(), allLogs);
	}

	public void analyzeAssignment(CountDownLatch latch, String assign,
			Map<String, Map<String, List<EHICommand>>> assignLogs) {
		new Thread(() -> {
			System.out.println("Analyzing " + assign.substring(assign.lastIndexOf(File.separator) + 1));
			Map<String, List<List<EHICommand>>> commands = new HashMap<>();
			for (String student : assignLogs.keySet()) {
				commands.put(student, new ArrayList<List<EHICommand>>(assignLogs.get(student).values()));
			}
//			createDistributionData(assign, commands);
//			createPauseDistribution(assign, commands);
//			createAssignData(assign, commands);
//			createEvents(assign, commands);
			createAssignTimeline(assign, commands);

			latch.countDown();
		}).start();
	}

	public void delete(String path) {
		deleteAssign(new File(path));
	}

	public void deleteAssign(File assign) {
		if (assign.isDirectory()) {
			for (File student : assign.listFiles(File::isDirectory)) {
				deleteStudent(student);
			}
		}
	}

}
