package logAnalyzer;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import fluorite.commands.EHICommand;

public class ASemesterReplayer extends AnAssignmentReplayer {
	protected Map<String, Map<String, Map<String, List<EHICommand>>>> allLogs;
	
	public void readLogs(String path) {
		if (root != null && root.getPath().equals(path)) {
			return;
		}
		root = new File(path);
		allLogs = readSemester(root);
	}
	
	public int countStudents() {
		int count = 0;
		for (String assign: allLogs.keySet()) {
			count += allLogs.get(assign).size();
		}
		return count;
	}
	
	public int countAssignments() {
		return allLogs.size();
	}
	
	public void createExtraCommand(CountDownLatch latch, String surfix, int mode) {
		createExtraCommandSemester(latch, surfix, mode);
	}

	public void createExtraCommandSemester(CountDownLatch latch, String surfix, int mode) {
		for (String assign : allLogs.keySet()) {
			createExtraCommandAssignment(latch, assign, allLogs.get(assign), surfix, mode);
		}
	}
	
	
	public Map<String, Map<String, Map<String, List<EHICommand>>>> readSemester(File semester) {
		System.out.println("Reading Semester " + semester);
		if (!semester.exists()) {
			System.out.println("Folder " + semester + " does not exist");
			System.exit(0);
		}
		Map<String, Map<String, Map<String, List<EHICommand>>>> logs = new TreeMap<>();
		for (File assign : semester.listFiles(File::isDirectory)) {
			Map<String, Map<String, List<EHICommand>>> ret = readAssignment(assign);
			if (ret != null) {
				logs.put(assign.getPath(), ret);
			}
		}
		return logs;
	}
	
	public void analyze(CountDownLatch latch) {
		analyzeSemester(latch);
	}
	
	public void analyzeSemester(CountDownLatch latch) {
		for (String assign : allLogs.keySet()) {
			analyzeAssignment(latch, assign, allLogs.get(assign));
		}
	}
	
	public void delete(String path) {
		deleteSemester(new File(path));
	}
	
	public void deleteSemester(File semester) {
		if (semester.isDirectory()) {
			for (File assign : semester.listFiles(File::isDirectory)) {
				deleteAssign(assign);
			}
		}
	}
}
