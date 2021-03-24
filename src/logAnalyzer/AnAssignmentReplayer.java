package logAnalyzer;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import fluorite.commands.EHICommand;

public class AnAssignmentReplayer extends Replayer {
	protected Map<String, Map<String, List<EHICommand>>> allLogs;
	
	public void readLogs(String path) {
//		if (root != null && root.getPath().equals(path)) {
//			return;
//		}
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

	public void createExtraCommandAssignment(CountDownLatch latch, String assign, Map<String, Map<String, List<EHICommand>>> assignLog, String surfix, int mode) {
		Map<String, List<String[]>> localCheckEvents = null;
		if (mode == LOCALCHECK) {
			localCheckEvents = readLocalCheckEvents(assign);
		}
		for (String student : assignLog.keySet()) {
			createExtraCommandStudent(latch, assignLog.get(student), surfix, mode, localCheckEvents == null ? null : localCheckEvents.get(student));
		}
	}
	
	public Map<String, Map<String, List<EHICommand>>> readAssignment(File assign) {
		System.out.println("Reading assignment " + assign);
		if (!assign.exists()) {
			System.out.println("Folder " + assign + " does not exist");
			return null;
		}
		Map<String, Map<String, List<EHICommand>>> logs = new TreeMap<>();
		for (File student : assign.listFiles(File::isDirectory)) {
			Map<String, List<EHICommand>> ret = readStudent(student);
			if (ret != null) {
				logs.put(student.getPath(), ret);
			}
		}
		return logs;
	}
	
	public void analyze(CountDownLatch latch) {
		analyzeAssignment(latch, root.getPath(), allLogs);
	}
	
	public void analyzeAssignment(CountDownLatch latch, String assign, Map<String, Map<String, List<EHICommand>>> assignLogs) {
		new Thread(()->{
			System.out.println("Analyzing " + assign.substring(assign.lastIndexOf(File.separator)+1));
			Map<String, List<List<EHICommand>>> commands = new HashMap<>();
			for (String student : assignLogs.keySet()) {
				commands.put(student, new ArrayList<List<EHICommand>>(assignLogs.get(student).values()));
			}
			createDistributionData(assign, commands);
			createPauseDistribution(assign, commands);
			createAssignData(assign, commands);
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
