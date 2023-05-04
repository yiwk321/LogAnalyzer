package logAnalyzer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.json.JSONObject;

import analyzer.extension.replayView.FileUtility;
import fluorite.commands.EHICommand;
import fluorite.commands.EclipseCommand;
import generators.PiazzaCommandGenerator;
import generators.ZoomChatCommandGenerator;

public class AnAssignmentReplayer extends Replayer {
	protected Map<String, Map<String, List<EHICommand>>> allLogs;
	protected Set<String> zoomChatters;
	protected Set<String> piazzaPosters;
	protected Set<String> assignmentSubmitters;
	protected Set<String> nonAssignmentSubmitters;
//	protected Set<String> piazzaNonAssignmentSubmitters;
//	protected Set<String> zoomNonAssignmentSubmitters;

	protected JSONObject piazzaPosts;
	protected File piazzaPostFile;
	protected String piazzaPostsString;
	protected File zoomChatsFolder;

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

	public void createExtraCommand(CountDownLatch latch, String surfix, int mode, boolean appendAllRemainingCommands) {
		createExtraCommandAssignment(latch, root.getPath(), allLogs, surfix, mode, appendAllRemainingCommands);
	}

	HashMap<String, List<Long>> sessionTimeMap = new HashMap<>();

	public  void findAllAssignmentSubmitters(Map<String, Map<String, List<EHICommand>>> assignLog) {
		Set<String> retVal = new HashSet();
		for (String student : assignLog.keySet()) {
			int aLeftParenIndex = student.indexOf("(");
			String aStudentName = student.substring(aLeftParenIndex+1, student.length() - 1);
			String aNormalizedStudentName = normalizeName(aStudentName);
			if (aNormalizedStudentName != null) {
				retVal.add(aNormalizedStudentName);
			} else {
				System.err.println("Illegal name:" + aStudentName);
			}
//			retVal.add(aNormalizedStudentName);
		}

		assignmentSubmitters = retVal;
	}
//	static Pattern fakeNamePattern = Pattern.compile("([a-zA-Z]+ [a-zA-Z]+ [a-zA-Z]*)");
	static String fakeNameExpression = "[a-zA-Z]+ [a-zA-Z']+";

	public static String normalizeName(String aName) {
		String aNameStringComponents[] = aName.split(":");
//		if (aNameStringComponents.length > 1) {
//			System.out.println("Name has :" + aName);
//		}
		String[] aNameComponents = aNameStringComponents[0].split("\\[");
		String retVal = aNameComponents[0].trim();
		if (!retVal.matches(fakeNameExpression)) {
			return null;
		}
		if (retVal.contains(":") || retVal.contains("Uh")) {
			System.out.println("Midified Name has :" + aName);
		}
		return retVal;
	}
	public void findAllNonAssignmentSubmitters( ) {
		Set<String> retVal = new HashSet();
		retVal.addAll(piazzaPosters);
		retVal.addAll(zoomChatters);
		retVal.removeAll(assignmentSubmitters);
		nonAssignmentSubmitters = retVal;
//		System.out.println("Submitters :" + assignmentSubmitters );
//		System.out.println("Non submitters: " + nonAssignmentSubmitters);
		
	}

	public void findAllPiazzaAndZoomStudents(String assign) {
		piazzaPostFile = findPiazzaPostFile(assign);
		piazzaPosts = null;
		if (piazzaPostFile != null && piazzaPostFile.exists()) {
			piazzaPostsString = FileUtility.readFile(piazzaPostFile).toString();
			piazzaPosts = new JSONObject(piazzaPostsString);
			piazzaPosters = PiazzaCommandGenerator.findAllPiazzaPosters(piazzaPosts);

		}
		zoomChatsFolder = findZoomChatsFolder(assign);
		zoomChatters = ZoomChatCommandGenerator.findAllZoomChatters(zoomChatsFolder);
	}
	
	protected boolean isSynthesized(File aStudent) {
		if (synthesizedStudents == null) {
			return false;
		}
		for (File aSynthesizedStudent:synthesizedStudents) {
			if (aStudent.equals(aSynthesizedStudent)) {
				return true;
			}
		}
		return false;
	}

	public void createExtraCommandAssignment(CountDownLatch latch, String assign,
			Map<String, Map<String, List<EHICommand>>> assignLog, String surfix, int mode,
			boolean appendAllRemainingCommands) {
		Map<String, List<String[]>> localCheckEvents = null;
		if (mode == LOCALCHECK) {
			localCheckEvents = readLocalCheckEvents(assign);
		}
//		 piazzaPostFile = findPiazzaPostFile(assign);
//		 piazzaPosts = null;
//		if (piazzaPostFile != null && piazzaPostFile.exists()) {
//			piazzaPostsString = FileUtility.readFile(piazzaPostFile).toString();
//			piazzaPosts = new JSONObject(piazzaPostsString);
//			piazzaPosters = PiazzaCommandGenerator.findAllPiazzaPosters(piazzaPosts);
//
//		}
//		 zoomChatsFolder = findZoomChatsFolder(assign);
//		zoomChatters = ZoomChatCommandGenerator.findAllZoomChatters(zoomChatsFolder);
		for (String student : assignLog.keySet()) {
			File aStudentFile = new File(student);
//			createExtraCommandStudent(latch, assignLog.get(student), student, surfix, mode, localCheckEvents == null ? null : localCheckEvents.get(student));
			List<String[]> studentLocalCheckEvents = localCheckEvents == null ? null : localCheckEvents.get(student);
			if (studentLocalCheckEvents == null) {
				studentLocalCheckEvents = new ArrayList<>();
			}
			boolean modifiedAppendRemaniningCommands = appendAllRemainingCommands || isSynthesized(new File(student));
//			createChainedExtraCommandsStudent(latch, assignLog.get(student), student, surfix, mode,
//					studentLocalCheckEvents, piazzaPosts, zoomChatsFolder, sessionTimeMap, appendAllRemainingCommands);
			createChainedExtraCommandsStudent(latch, assignLog.get(student), student, surfix, mode,
					studentLocalCheckEvents, piazzaPosts, zoomChatsFolder, sessionTimeMap, modifiedAppendRemaniningCommands);

		
		}
	}

	public File findPiazzaPostFile(String assign) {
		File[] files = new File(assign).getParentFile().listFiles((parent, fileName) -> {
			return fileName.contains("ByAuthorPosts") && fileName.endsWith(".json");
		});
		if (files.length > 0) {
			return files[files.length - 1];
		}
		return null;
	}

	public File findZoomChatsFolder(String assign) {
		File[] files = new File(assign).getParentFile().listFiles((parent, fileName) -> {
			return fileName.equals("ZoomChatsAnon");
		});
		if (files.length > 0) {
			return files[files.length - 1];
		}
		return null;
	}

//	protected List<EHICommand> createEmptyLog() {
//		List<EHICommand> emptyLog = new ArrayList<>();
//		EclipseCommand command = new EclipseCommand("", 0);
//		command.setTimestamp(0);
//		command.setStartTimestamp(0);
//		emptyLog.add(command);
//		emptyLog.add(command);
//		emptyLog.add(command);
//		return emptyLog;
//	}
//
//	protected File createEmptyLogFile(File student, List<EHICommand> log) {
//		File logFolder = null;
//		File submission = new File(student, "Submission attachment(s)");
//		if (submission.exists()) {
//			logFolder = getProjectFolder(submission);
//			if (logFolder != null) {
//				logFolder = new File(logFolder, "Logs" + File.separator + "Eclipse");
//			}
//		}
//		if (logFolder == null) {
//			return null;
//		}
//		logFolder.mkdirs();
//		File emptyLogFile = new File(logFolder, "Log2010-01-01-00-00-00-000.xml");
////		emptyLogFile.createNewFile();
//		return emptyLogFile;
////		Map<String, List<EHICommand>> emptyMap = new HashMap<>();
////
////		emptyMap.put(emptyLogFile.getPath(), log);
////		logs.put(student.getPath(), emptyMap);
//	}
	
	public static File createEmptySubmission(File assign, String aStudent) {
		String[] aNames = aStudent.split(" ");
		String aSrcLogFileName
		=  aNames[1] + ", " + aNames[0] + "(" + aStudent + ")" +
		 "/Submission attachment(s)/Synthesized/src";
		File aSrcFile = new File (assign, aSrcLogFileName);
		aSrcFile.mkdirs();
		File aStudentSubmission = aSrcFile.getParentFile().getParentFile().getParentFile();
//		String aLogFileName
//			=  aNames[1] + "," + aNames[0] + "(" + aStudent + ")" +
//			 "/Submission attachment(s)/Synthesized/Logs/Eclipse";
//		
//		
//		File aLogFile = new File(assign, aLogFileName);
		
//		return aLogFile;
		return aStudentSubmission;
		
	}
	
	public static List<File> createEmptySubmissions(File assign, Collection<String> aStudents) {
		List<File> retVal = new ArrayList();
		for (String aStudent:aStudents) {
			File aFile = createEmptySubmission(assign, aStudent);
			retVal.add(aFile);
		}
		return retVal;
	}
	
	


	public Map<String, Map<String, List<EHICommand>>> readAssignment(File assign) {
		System.out.println("Reading assignment " + assign);
		if (!assign.exists()) {
			System.out.println("Folder " + assign + " does not exist");
			return null;
		}
		Map<String, Map<String, List<EHICommand>>> logs = new TreeMap<>();
		for (File student : assign.listFiles((parent, fileName) -> {
			return fileName.contains(",") && fileName.contains("(");
		})) {
			Map<String, List<EHICommand>> ret = readStudentCreatingPossiblyEmptyLog(student);
			if (ret == null) {
				continue;
			}
			logs.put(student.getPath(), ret);
			
//			Map<String, List<EHICommand>> ret = readStudent(student);
//			if (ret != null && !ret.isEmpty()) {
//				logs.put(student.getPath(), ret);
//			} else {
//
//				List<EHICommand> emptyLog = createEmptyLog();
//				File emptyLogFile = createEmptyLogFile(student, emptyLog);
//				if (emptyLogFile == null) {
//					continue;
//				}
//				Map<String, List<EHICommand>> emptyMap = new HashMap<>();
//				emptyMap.put(emptyLogFile.getPath(), emptyLog);
//				logs.put(student.getPath(), emptyMap);
//
//			}
		}
		findAllAssignmentSubmitters(logs);
		findAllPiazzaAndZoomStudents(assign.getPath());
		findAllNonAssignmentSubmitters();
		List<File> anEmptySumbissions = createEmptySubmissions(assign, nonAssignmentSubmitters);
		for (File anEmptySubmission:anEmptySumbissions) {
			Map<String, List<EHICommand>> ret = readStudentCreatingPossiblyEmptyLog(anEmptySubmission);
			if (ret == null) {
				continue;
			}
			logs.put(anEmptySubmission.getPath(), ret);
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
//			createAssignTimeline(assign, commands);
			createSessionTimeMap(assign, sessionTimeMap);
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
