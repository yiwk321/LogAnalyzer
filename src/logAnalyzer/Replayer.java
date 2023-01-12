package logAnalyzer;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.json.JSONObject;

import analyzer.extension.replayView.FileUtility;
import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import dayton.ellwanger.helpbutton.exceptionMatcher.ExceptionMatcher;
import dayton.ellwanger.helpbutton.exceptionMatcher.JavaExceptionMatcher;
import dayton.ellwanger.helpbutton.exceptionMatcher.PrologExceptionMatcher;
import dayton.ellwanger.helpbutton.exceptionMatcher.SMLExceptionMatcher;
import fluorite.commands.CheckStyleCommand;
import fluorite.commands.ConsoleInput;
import fluorite.commands.ConsoleOutput;
import fluorite.commands.ConsoleOutputCommand;
import fluorite.commands.CopyCommand;
import fluorite.commands.Delete;
import fluorite.commands.DifficultyCommand;
import fluorite.commands.EHExceptionCommand;
import fluorite.commands.EHICommand;
import fluorite.commands.EclipseCommand;
import fluorite.commands.ExceptionCommand;
import fluorite.commands.FileOpenCommand;
import fluorite.commands.GetHelpCommand;
import fluorite.commands.Insert;
import fluorite.commands.InsertStringCommand;
import fluorite.commands.LocalCheckCommand;
import fluorite.commands.PasteCommand;
import fluorite.commands.Replace;
import fluorite.commands.RequestHelpCommand;
import fluorite.commands.PauseCommand;
import fluorite.commands.PiazzaPostCommand;
import fluorite.commands.RunCommand;
import fluorite.commands.ShellCommand;
import fluorite.commands.WebCommand;
import fluorite.commands.ZoomChatCommand;
import fluorite.util.EHLogReader;
import generators.ChainedCommandGenerator;
import generators.CheckstyleCommandGenerator;
import generators.CommandGenerator;
import generators.LocalCheckCommandGenerator;
import generators.PauseCommandGenerator;
import generators.SourceCodeOnRunGenerator;
import generators.WebCommandGenerator;
import sun.tools.jar.resources.jar;
import tests.Assignment;
import tests.Suite;
import util.misc.Common;

public abstract class Replayer {
	public static final int PAUSE = 0;
	public static final int LOCALCHECK = 1;
	public static final int WEB = 2;
	public static final String LOCALCHECK_EVENTS = "LocalChecks Logs";
	public static final String REST_INSESSION = "Rest(In Session)";
	public static final String REST_ENDSESSION = "Rest(End Session)";
	public static final String REST_LOSEFOCUS = "Rest(Lose Focus)";
	public static final String ECLIPSE_LOST_FOCUS = "ECLIPSE_LOST_FOCUS";
	public static final String ECLIPSE_CLOSED = "ECLIPSE_CLOSED";
	public static final String XML_START1 = "<Events startTimestamp=\"";
	public static final String XML_START2 = "\" logVersion=\"";
	public static final String XML_VERSION = "1.0.0.202008151525";
	public static final String XML_START3 = "\">\r\n";
	public static final String XML_FILE_ENDING = "\r\n</Events>";
	public static final String XML_FILE_ENDING_WITHOUT_NEW_LINE = "</Events>";
	public static final long ONE_SECOND = 1000;
	public static final long ONE_MIN = 60 * 1000;
	public static final long TEN_MIN = 10 * ONE_MIN;
	public static final long FIVE_MIN = 5 * ONE_MIN;
	public static final long HALF_MIN = ONE_MIN / 2;
	public static final long TWO_MIN = 2 * ONE_MIN;
	public static final long DAY = 24 * 60 * ONE_MIN;
	public static final long[] REST = { ONE_SECOND, 2 * ONE_SECOND, 5 * ONE_SECOND, 10 * ONE_SECOND, 15 * ONE_SECOND,
			HALF_MIN, ONE_MIN, TWO_MIN, FIVE_MIN, TEN_MIN, 2 * TEN_MIN, 3 * TEN_MIN, 9 * FIVE_MIN, 6 * TEN_MIN };
	private ExceptionMatcher[] ems = { JavaExceptionMatcher.getInstance(), PrologExceptionMatcher.getInstance(),
			SMLExceptionMatcher.getInstance() };
	private Map<String, String> logToWrite;
	private EHLogReader reader;
	protected File root;
	protected Map<String, Assignment> assignMap;
	static Pattern suitePattern = Pattern.compile("^(\\w*) - \\[(.*)\\]");
	static Pattern assignPattern = Pattern.compile("Assignment \\d");
	static Pattern ecPattern = Pattern.compile("Assignment \\d Extra Credit.*");
	static Pattern assignSuitePattern = Pattern.compile("[FS]\\d*Assignment\\d*Suite.*");
	public static final String suite = "E:\\submissions\\524\\Suite Test Mapping.txt";
	SimpleDateFormat df;

//	protected List<CommandGenerator> commandGenerators = new ArrayList();
	
	public Replayer() {
		logToWrite = new TreeMap<>();
		reader = new EHLogReader();
	}

	public abstract void readLogs(String path);

	public List<List<EHICommand>> readStudentNestedList(File student) {
		Map<String, List<EHICommand>> aMap = readStudent(student);
		List<List<EHICommand>> retVal = new ArrayList(aMap.values());
		return retVal;

	}

	public Map<String, List<EHICommand>> readStudent(File student) {
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
//		refineLogFiles(logFolder);
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
		Map<String, List<EHICommand>> logs = new TreeMap<>();
		for (File logFile : logFiles) {
			if (logFile.length() == 0) {
				logFile.delete();
				continue;
			}
			List<EHICommand> ret = readOneLogFile(logFile);
			if (ret != null) {
				logs.put(logFile.getPath(), ret);
			} else {
				System.err.println("Need to append <Events>");
			}
		}
		return logs;
	}

	public List<EHICommand> readOneLogFileWthoutAppending(File log, boolean printError) {
		String path = log.getPath();
		if (path.contains("Log2021-06-20-20-25-46-388.xml")) {
			System.out.println("Pausing");
		}
		System.out.println("Reading file " + path);
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
			sortCommands(commands);
			return commands;
		} catch (Exception e) {
			if (printError) {
				System.err.println("Could not read file" + path + "\n" + e);
				e.printStackTrace();
			}
		}
		return null;
	}

	public List<EHICommand> readOneLogFile(File log) {
		List<EHICommand> retVal = readOneLogFileWthoutAppending(log, false);
		if (retVal == null) {
			appendEvents(log);
			return readOneLogFileWthoutAppending(log, true);
		}
		return retVal;
//		String path = log.getPath();
//		System.out.println("Reading file " + path);
//		if (!log.exists()) {
//			System.err.println("log does not exist:" + path);
//			return null;
//		}
//		if (!path.endsWith(".xml")) {
//			System.err.println("log is not in xml format:" + path);
//			return null;
//		}
//		try {
//			List<EHICommand> commands = reader.readAll(path);
//			sortCommands(commands);
//			return commands;
//		} catch (Exception e) {
//			System.err.println("Could not read file" + path + "\n"+ e);
//			e.printStackTrace();
//		}
//		return null;
	}

	static void appendEvents(File logFile) {
		String aFileText = "";
		try {
			aFileText = Common.readFile(logFile).toString();

			if (aFileText.endsWith(XML_FILE_ENDING)) {
				return; // already done this or some other problem
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))){
			if (aFileText.endsWith(System.lineSeparator())) {
				writer.write(XML_FILE_ENDING_WITHOUT_NEW_LINE);
			} else {
				writer.write(XML_FILE_ENDING);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void refineLogFile(File logFile) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(logFile));
			String lastLine = null;
			String currentLine = null;
			while ((currentLine = reader.readLine()) != null) {
				lastLine = currentLine;
			}
			if (lastLine != null && !lastLine.endsWith("</Events>")) {
//					BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true));
//					writer.write(XML_FILE_ENDING);
//					writer.close();
				appendEvents(logFile);
			}
			reader.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
//	public void refineLogFiles(File logFolder){
//		try {
//			for (File file : logFolder.listFiles((filename)->{return filename.getName().endsWith(".lck");})) {
//				File logFile = new File(file.getParent(), file.getName().substring(0, file.getName().indexOf(".lck")));
//				BufferedReader reader = new BufferedReader(new FileReader(logFile));
//				String lastLine = null;
//				String currentLine = null;
//				while((currentLine = reader.readLine()) != null) {
//					lastLine = currentLine;
//				}
//				if (lastLine != null && !lastLine.endsWith("</Events>")) {
////					BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true));
////					writer.write(XML_FILE_ENDING);
////					writer.close();
//					appendEvents(logFile);
//				}	
//				reader.close();
//				file.delete();
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		} 
//	}

	protected Map<String, List<String[]>> readLocalCheckEvents(String assign) {
		Map<String, List<String[]>> localCheckEvents = new TreeMap<>();
		Pattern pattern = Pattern.compile("(\\d)+");
		Matcher logMatcher = null;
		Matcher eventsMatcher = null;
		try {
			File folder = new File(LOCALCHECK_EVENTS);
			File[] files = folder.listFiles((f) -> {
				return f.getName().endsWith(".csv");
			});
			for (File file : files) {
				logMatcher = pattern.matcher(assign.substring(assign.lastIndexOf(File.separator)));
				eventsMatcher = pattern.matcher(file.getName());
				if (!(logMatcher.find() && eventsMatcher.find()
						&& logMatcher.group(0).equals(eventsMatcher.group(0)))) {
					continue;
				}
				System.out.println("Reading LocalChecks file " + file.getPath());
				CSVReader cr = new CSVReader(new FileReader(file));
				String[] nextLine = cr.readNext();
				List<String[]> studentLC = null;
				while ((nextLine = cr.readNext()) != null) {
					String studentFolder = assign + File.separator;
					for (int i = 0; i < nextLine.length - 3; i++) {
						if (nextLine[i + 3].isEmpty()) {
							continue;
						}
						if (i > 0) {
							studentFolder += ",";
						}
						studentFolder += nextLine[i + 3];
					}
					if (!localCheckEvents.containsKey(studentFolder)) {
						studentLC = new ArrayList<>();
						localCheckEvents.put(studentFolder, studentLC);
					}
					studentLC.add(nextLine);
				}
				cr.close();
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return localCheckEvents;
	}

	public String readWebContent(String aURL) {
		String content = "";
		Scanner sc = null;
		try {
			URL url = new URL(aURL);
			URLConnection con = url.openConnection();
			con.setReadTimeout(5000);
			sc = new Scanner(con.getInputStream());
			StringBuffer sb = new StringBuffer();
			while (sc.hasNext()) {
				sb.append(sc.next());
			}
			content = sb.toString();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (sc != null) {
				sc.close();
			}
		}
		if (content.equals("") && !aURL.substring(0, 5).equals("https")) {
			aURL = aURL.replaceFirst("http", "https");
			return readWebContent(aURL);
		}

		return content;
	}

	public void createExtraCommand(String surfix, int mode, boolean appendAllRemainingCommands) {
		logToWrite.clear();
		CountDownLatch latch = new CountDownLatch(countStudents());
		createExtraCommand(latch, surfix, mode, appendAllRemainingCommands);
		try {
			latch.await();
			writeLogs(surfix);
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			System.out.println("Done!");
		}
	}

	public abstract void createExtraCommand(CountDownLatch latch, String surfix, int mode, boolean appendAllRemainingCommands);

	public void createExtraCommandStudent(CountDownLatch aLatch, Map<String, List<EHICommand>> aStudentLog,
			String aStudent, String surfix, int mode, List<String[]> localCheckEvents) {
//		CommandGenerator cg = new ChainedCommandGenerator(this, aLatch, aStudent, aStudentLog, localCheckEvents);
//		if (mode == LOCALCHECK && localCheckEvents != null) {
////			cg = new LocalCheckCommandGenerator(this, latch, student, studentLog, localCheckEvents);
////			cg = new SourceCodeOnRunGenerator(this, latch, student, studentLog, localCheckEvents);
//			cg = new PauseCommandGenerator(this, latch, aStudentLog);
//			commandGenerators.add(cg);
//			cg = new CheckstyleCommandGenerator(this, latch, student, aStudentLog);
//			commandGenerators.add(cg);
//
//
//		} else if (mode == WEB) {
//			cg = new WebCommandGenerator(this, latch, aStudentLog, getLogFolder(new File(student)).getParentFile());
//		} else {
//			cg = new PauseCommandGenerator(this, latch, aStudentLog);
//		}
//		new Thread(cg).start();
	}

	protected CountDownLatch latch;
	Map<String, List<EHICommand>> commandMap;
	String student;

	public void createChainedExtraCommandsStudent(CountDownLatch aLatch, Map<String, List<EHICommand>> aStudentLog,
			String aStudent, String aSuffix, int mode, List<String[]> localCheckEvents, JSONObject piazzaPosts,
			File zoomChatsFolder, HashMap<String, List<Long>> sessionTimeMap, boolean appendAllRemainingCommands) {
		CommandGenerator cg = new ChainedCommandGenerator(this, aLatch, aStudent, aStudentLog, localCheckEvents,
				piazzaPosts, zoomChatsFolder, sessionTimeMap, appendAllRemainingCommands);

//		latch = aLatch;
//		student = aStudent;
//		commandMap = aStudentLog;
////		commandGenerators.add(new PauseCommandGenerator(this, null, aStudentLog));
//		commandGenerators.add(new LocalCheckCommandGenerator(this, latch, aStudent, aStudentLog, localCheckEvents));
//		commandGenerators.add(new CheckstyleCommandGenerator(this, latch, aStudent, aStudentLog));
//		commandGenerators.add(new LocalCheckCommandGenerator(this, latch, student, studentLog, localCheckEvents));
//
//		CommandGenerator cg;
//		if (mode == LOCALCHECK && localCheckEvents != null) {
////			cg = new LocalCheckCommandGenerator(this, latch, student, studentLog, localCheckEvents);
////			cg = new SourceCodeOnRunGenerator(this, latch, student, studentLog, localCheckEvents);
//			cg = new CheckstyleCommandGenerator(this, aLatch, student, studentLog, localCheckEvents);
//			commandGenerators.add(cg);
//
//
//		} else if (mode == WEB) {
//			cg = new WebCommandGenerator(this, aLatch, studentLog, getLogFolder(new File(student)).getParentFile());
//		} else {
//			cg = new PauseCommandGenerator(this, aLatch, studentLog);
//		}
		new Thread(cg).start();
	}

	protected File getLogFolder(File student) {
		if (!student.exists()) {
			System.err.println("Folder " + student + " does not exist");
			return null;
		}
		File logFolder = null;
		File submission = new File(student, "Submission attachment(s)");
		if (submission.exists()) {
			logFolder = getProjectFolder(submission);
			if (logFolder != null) {
				logFolder = new File(logFolder, "Logs" + File.separator + "Eclipse");
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
			System.err.println("No logs found for student " + student.getName());
			return null;
		}
		return logFolder;
	}

	public abstract int countStudents();

	public abstract int countAssignments();

	public synchronized void updateLogMap(String path, String logContent) {
		logToWrite.put(path, logContent);
	}

	private void writeLogs(String surfix) {
		for (String fileName : logToWrite.keySet()) {
			try {
				File file = new File(fileName);
				if (file.getParentFile().getName().contains("Eclipse")) {
					file = new File(file.getParent() + File.separator + surfix + File.separator + file.getName());
				}
				System.out.println("Writing to file " + file.getPath());
				if (file.exists()) {
					file.delete();
				}
				file.getParentFile().mkdirs();
				file.createNewFile();
//				BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
				OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
				writer.write(logToWrite.get(fileName));
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		logToWrite.clear();
	}

	public void analyze() {
		CountDownLatch latch = new CountDownLatch(countAssignments());
		analyze(latch);
		try {
			latch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			System.out.println("Done!");
		}
	}

	public abstract void analyze(CountDownLatch latch);

	public void createSessionTimeMap(String assign, Map<String, List<Long>> sessionTimeMap) {
		String folder = new File(assign).getParent();
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(new File(folder, "ZoomSessionTimesSeconds.txt")))) {
			for (Entry<String, List<Long>> studentSessions: sessionTimeMap.entrySet()) {
				if (studentSessions.getValue().size() == 0) {
					continue;
				}
				String student = studentSessions.getKey();
//				student = student.substring(student.lastIndexOf(File.pathSeparator));
				bw.write(student + ":" + studentSessions.getValue().size() + System.lineSeparator());
				long total = 0;
				for (long sessionTime : studentSessions.getValue()) {
//					bw.write(convertToHourMinuteSecond(sessionTime) + System.lineSeparator());
					bw.write((sessionTime / 1000) + System.lineSeparator());
					total += sessionTime;
				}
				bw.write("Total: " + (total / 1000) + System.lineSeparator());
			}
		} catch (IOException e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(new File(folder, "ZoomSessionTimes.txt")))) {
			for (Entry<String, List<Long>> studentSessions: sessionTimeMap.entrySet()) {
				if (studentSessions.getValue().size() == 0) {
					continue;
				}
				String student = studentSessions.getKey();
				long total = 0;
//				student = student.substring(student.lastIndexOf(File.pathSeparator));
				bw.write(student + ":" + studentSessions.getValue().size() + System.lineSeparator());
				for (long sessionTime : studentSessions.getValue()) {
					bw.write(convertToHourMinuteSecond(sessionTime) + System.lineSeparator());
//					bw.write((sessionTime / 1000) + System.lineSeparator());
					total += sessionTime;
				}
				bw.write("Total: " + convertToHourMinuteSecond(total) + System.lineSeparator());
			}
		} catch (IOException e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	
	public void createAssignTimeline(String assign, Map<String, List<List<EHICommand>>> data) {
		notifyNewAssignment(assign, data);
		for (String key : data.keySet()) {
			System.out.println(key);
		}
		df = new SimpleDateFormat("MM/dd/yyyy hh:mma 'ET'");
		TimeZone edt = TimeZone.getTimeZone("America/New_York");
		df.setTimeZone(edt);
//		try {
//		assign = assign.substring(assign.lastIndexOf(File.separator)+1);
		for (Entry<String, List<List<EHICommand>>> student : data.entrySet()) {

			createStudentTimeline(student.getKey(), student.getValue());
		}
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
	}

	public void createStudentTimeline(String student, List<List<EHICommand>> nestedCommands) {
		File timeline = new File(student + File.separator + "Timeline.txt");

		try {
			if (timeline.exists()) {
				timeline.delete();
			}
			if (!timeline.createNewFile()) {
				return;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		try (FileWriter fw = new FileWriter(timeline)) {
			System.out.println("Generating AssignTimeline for student " + student);
			student = student.substring(student.lastIndexOf(File.separator) + 1);
			List<List<EHICommand>> difficulties = new ArrayList<>();
			List<EHICommand> difficultySessionCommands = new ArrayList<>();
			boolean hasDifficulty = false;
			EHICommand currentFile = null;
			for (List<EHICommand> commands : nestedCommands) {
				long lastTime = -1;
				long curTime = -1;
				for (int i = 0; i < commands.size(); i++) {
					EHICommand command = commands.get(i);
					if (i > 0) {
						lastTime = curTime;
					}
					if (command != null) {
						curTime = command.getTimestamp() + command.getStartTimestamp();
					}
					if (command instanceof FileOpenCommand) {
						currentFile = command;
					}
					if (hasDifficulty && (command instanceof CheckStyleCommand
							|| command instanceof ConsoleOutput
							|| command instanceof ConsoleInput || command instanceof WebCommand
							|| command instanceof RunCommand || command instanceof FileOpenCommand)) {
						difficultySessionCommands.add(command);
					}
					if (hasDifficulty && command instanceof Insert) {
						while (++i < commands.size() && commands.get(i) instanceof Insert) {
							((Insert) command).combine(commands.get(i));
						}
						difficultySessionCommands.add(command);
					}
					if (hasDifficulty && command instanceof Delete) {
						while (++i < commands.size() && commands.get(i) instanceof Delete) {
							((Delete) command).combine(commands.get(i));
						}
						difficultySessionCommands.add(command);
					}
					if (command instanceof PiazzaPostCommand) {
//						JSONObject post = new JSONObject(command.getDataMap().get("piazza_post"));
//						if (post.getBoolean("is_office_hour_request")) {
//							difficulties.add(difficultySessionCommands);
//							difficultySessionCommands = new ArrayList<>();
//						}
						if (!hasDifficulty && currentFile != null) {
							difficultySessionCommands.add(currentFile);
						}
						hasDifficulty = true;
						difficultySessionCommands.add(command);
//						JSONObject post = new JSONObject(command.getDataMap().get("piazza_post"));
//						if(post.getString("content").contains("Hey I would like to join OHs this morning to solve an issue regarding receiving errors for the wrong w")) {
//							int a =0;
//						}
					}

					if (command instanceof RequestHelpCommand) {
//						difficulties.add(difficultySessionCommands);
//						difficultySessionCommands = new ArrayList<>();
						if (!hasDifficulty && currentFile != null) {
							difficultySessionCommands.add(currentFile);
						}
						hasDifficulty = true;
						difficultySessionCommands.add(command);
					}
					if (command instanceof ZoomChatCommand) {
//						if (i + 1 < commands.size() && !(commands.get(i+1) instanceof ZoomChatCommand)) {
//							difficulties.add(difficultySessionCommands);
//							difficultySessionCommands = new ArrayList<>();
//							hasDifficulty = false;
//						}
						if (!hasDifficulty && currentFile != null) {
							difficultySessionCommands.add(currentFile);
						}
						hasDifficulty = true;
						difficultySessionCommands.add(command);
					}
//					if (command instanceof PauseCommand) {
//					}
//					if (command instanceof WebCommand) {
//					}
//					if (command instanceof CheckStyleCommand) {
//					}
					if (command instanceof ExceptionCommand) {
						if (!hasDifficulty && currentFile != null) {
							difficultySessionCommands.add(currentFile);
						}
						hasDifficulty = true;
						difficultySessionCommands.add(command);
					}
					if (command instanceof RunCommand
//							&& command.getAttributesMap().get("type").equals("Run")
						) {
						boolean noException = true;
						int endIdx = i + 10 < commands.size() ? i + 10 : commands.size();
						int j = i + 1;
						for (; j < endIdx; j++) {
							if (commands.get(i) instanceof ExceptionCommand) {
								noException = false;
								break;
							}
							if (!(command instanceof RunCommand)) {
								break;
							}
						}
						if (noException) {
							if (hasDifficulty) {
								difficulties.add(difficultySessionCommands);
								difficultySessionCommands = new ArrayList<>();
								hasDifficulty = false;
							} else {
								difficultySessionCommands.clear();
							}
						} else {
							if (!hasDifficulty && currentFile != null) {
								difficultySessionCommands.add(currentFile);
							}
							hasDifficulty = true;
							for (int k = 0; k <= j; k++) {
								difficultySessionCommands.add(commands.get(k));
							}
						}
						i = j + 1;
					}
					if (command instanceof LocalCheckCommand) {
						String type = command.getDataMap().get("type");
						if (type.equals("fail_decline")|| type.equals("fail_growth")) {
							if (!hasDifficulty && currentFile != null) {
								difficultySessionCommands.add(currentFile);
							}
							hasDifficulty = true;
							difficultySessionCommands.add(command);
							if (i + 1 < commands.size() && !(commands.get(i+1) instanceof LocalCheckCommand)) {
								for (EHICommand difficultyCommand : difficultySessionCommands) {
									if (!(difficultyCommand instanceof LocalCheckCommand && hasDifficulty)) {
										difficulties.add(difficultySessionCommands);
										hasDifficulty = false;
										break;
									}
								}
								difficultySessionCommands = new ArrayList<>();
							}
						}
					}
				}
			}
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < difficulties.size(); i++) {
				sb.append("Difficulty " + (i + 1) + System.lineSeparator());
				for (EHICommand command : difficulties.get(i)) {
					sb.append(getCommandString(command));
					sb.append(System.lineSeparator());
				}
				sb.append(System.lineSeparator());
			}
			fw.write(sb.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
//	public void createPiazzaStats(String assign, Map<String, List<List<EHICommand>>> data) {
//		df = new SimpleDateFormat("MM/dd/yyyy hh:mma 'ET'");
//		TimeZone edt = TimeZone.getTimeZone("America/New_York");
//		df.setTimeZone(edt);
//		File piazzaStats = new File(student + File.separator + "Timeline.txt");
//
//		for (Entry<String, List<List<EHICommand>>> entry : data.entrySet()) {
//			String student = entry.getKey();
//			List<List<EHICommand>> nestedCommands = entry.getValue();
//
//			try {
//				if (timeline.exists()) {
//					timeline.delete();
//				}
//				if (!timeline.createNewFile()) {
//					return;
//				}
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//			try (FileWriter fw = new FileWriter(timeline)) {
//				System.out.println("Generating AssignTimeline for student " + student);
//				student = student.substring(student.lastIndexOf(File.separator) + 1);
//				List<List<EHICommand>> difficulties = new ArrayList<>();
//				List<EHICommand> difficultySessionCommands = new ArrayList<>();
//				boolean hasDifficulty = false;
//				EHICommand currentFile = null;
//				for (List<EHICommand> commands : nestedCommands) {
//					long lastTime = -1;
//					long curTime = -1;
//					for (int i = 0; i < commands.size(); i++) {
//						EHICommand command = commands.get(i);
//						if (i > 0) {
//							lastTime = curTime;
//						}
//						if (command != null) {
//							curTime = command.getTimestamp() + command.getStartTimestamp();
//						}
//						if (command instanceof FileOpenCommand) {
//							currentFile = command;
//						}
//						if (hasDifficulty && (command instanceof CheckStyleCommand
//								|| command instanceof ConsoleOutput
//								|| command instanceof ConsoleInput || command instanceof WebCommand
//								|| command instanceof RunCommand || command instanceof FileOpenCommand)) {
//							difficultySessionCommands.add(command);
//						}
//						if (hasDifficulty && command instanceof Insert) {
//							while (++i < commands.size() && commands.get(i) instanceof Insert) {
//								((Insert) command).combine(commands.get(i));
//							}
//							difficultySessionCommands.add(command);
//						}
//						if (hasDifficulty && command instanceof Delete) {
//							while (++i < commands.size() && commands.get(i) instanceof Delete) {
//								((Delete) command).combine(commands.get(i));
//							}
//							difficultySessionCommands.add(command);
//						}
//						if (command instanceof PiazzaPostCommand) {
////							JSONObject post = new JSONObject(command.getDataMap().get("piazza_post"));
////							if (post.getBoolean("is_office_hour_request")) {
////								difficulties.add(difficultySessionCommands);
////								difficultySessionCommands = new ArrayList<>();
////							}
//							if (!hasDifficulty && currentFile != null) {
//								difficultySessionCommands.add(currentFile);
//							}
//							hasDifficulty = true;
//							difficultySessionCommands.add(command);
////							JSONObject post = new JSONObject(command.getDataMap().get("piazza_post"));
////							if(post.getString("content").contains("Hey I would like to join OHs this morning to solve an issue regarding receiving errors for the wrong w")) {
////								int a =0;
////							}
//						}
//
//						if (command instanceof RequestHelpCommand) {
////							difficulties.add(difficultySessionCommands);
////							difficultySessionCommands = new ArrayList<>();
//							if (!hasDifficulty && currentFile != null) {
//								difficultySessionCommands.add(currentFile);
//							}
//							hasDifficulty = true;
//							difficultySessionCommands.add(command);
//						}
//						if (command instanceof ZoomChatCommand) {
////							if (i + 1 < commands.size() && !(commands.get(i+1) instanceof ZoomChatCommand)) {
////								difficulties.add(difficultySessionCommands);
////								difficultySessionCommands = new ArrayList<>();
////								hasDifficulty = false;
////							}
//							if (!hasDifficulty && currentFile != null) {
//								difficultySessionCommands.add(currentFile);
//							}
//							hasDifficulty = true;
//							difficultySessionCommands.add(command);
//						}
////						if (command instanceof PauseCommand) {
////						}
////						if (command instanceof WebCommand) {
////						}
////						if (command instanceof CheckStyleCommand) {
////						}
//						if (command instanceof ExceptionCommand) {
//							if (!hasDifficulty && currentFile != null) {
//								difficultySessionCommands.add(currentFile);
//							}
//							hasDifficulty = true;
//							difficultySessionCommands.add(command);
//						}
//						if (command instanceof RunCommand
////								&& command.getAttributesMap().get("type").equals("Run")
//							) {
//							boolean noException = true;
//							int endIdx = i + 10 < commands.size() ? i + 10 : commands.size();
//							int j = i + 1;
//							for (; j < endIdx; j++) {
//								if (commands.get(i) instanceof ExceptionCommand) {
//									noException = false;
//									break;
//								}
//								if (!(command instanceof RunCommand)) {
//									break;
//								}
//							}
//							if (noException) {
//								if (hasDifficulty) {
//									difficulties.add(difficultySessionCommands);
//									difficultySessionCommands = new ArrayList<>();
//									hasDifficulty = false;
//								} else {
//									difficultySessionCommands.clear();
//								}
//							} else {
//								if (!hasDifficulty && currentFile != null) {
//									difficultySessionCommands.add(currentFile);
//								}
//								hasDifficulty = true;
//								for (int k = 0; k <= j; k++) {
//									difficultySessionCommands.add(commands.get(k));
//								}
//							}
//							i = j + 1;
//						}
//						if (command instanceof LocalCheckCommand) {
//							String type = command.getDataMap().get("type");
//							if (type.equals("fail_decline")|| type.equals("fail_growth")) {
//								if (!hasDifficulty && currentFile != null) {
//									difficultySessionCommands.add(currentFile);
//								}
//								hasDifficulty = true;
//								difficultySessionCommands.add(command);
//								if (i + 1 < commands.size() && !(commands.get(i+1) instanceof LocalCheckCommand)) {
//									for (EHICommand difficultyCommand : difficultySessionCommands) {
//										if (!(difficultyCommand instanceof LocalCheckCommand && hasDifficulty)) {
//											difficulties.add(difficultySessionCommands);
//											hasDifficulty = false;
//											break;
//										}
//									}
//									difficultySessionCommands = new ArrayList<>();
//								}
//							}
//						}
//					}
//				}
//				StringBuilder sb = new StringBuilder();
//				for (int i = 0; i < difficulties.size(); i++) {
//					sb.append("Difficulty " + (i + 1) + System.lineSeparator());
//					for (EHICommand command : difficulties.get(i)) {
//						sb.append(getCommandString(command));
//						sb.append(System.lineSeparator());
//					}
//					sb.append(System.lineSeparator());
//				}
//				fw.write(sb.toString());
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
//	}
//
//	public void createStudentTimeline(String student, List<List<EHICommand>> nestedCommands) {
//		
//	}

	public String getCommandString(EHICommand command) {
		long timestamp = command.getStartTimestamp() + command.getTimestamp();
		
		String retVal = df.format(timestamp) + "\t" + command.getName() + ": ";
		if (df.format(timestamp).equals("05/26/2021 12:33PM ET")) {
			int a = 0;
		}
		Map<String, String> dataMap = command.getDataMap();
		if (command instanceof ExceptionCommand) {
			return retVal + dataMap.get(ExceptionCommand.XML_Exception_Tag).trim();
		}
		if (command instanceof CheckStyleCommand) {
			return retVal + dataMap.get("CSVRow");
		}
		if (command instanceof ZoomChatCommand) {
			return retVal + dataMap.get("chat");
		}
		if (command instanceof ConsoleOutput) {
			return retVal + dataMap.get(ConsoleOutput.XML_Output_Tag).replaceAll("[\r\n]+", "\r\n").trim();
		}
		if (command instanceof ConsoleInput) {
			return retVal + dataMap.get(ConsoleInput.XML_Output_Tag).trim();
		}
		if (command instanceof LocalCheckCommand) {
			return retVal + dataMap.get("testcase") + "," + dataMap.get("type");
		}
		if (command instanceof PiazzaPostCommand) {
			JSONObject post = new JSONObject(dataMap.get("piazza_post"));
			return retVal + post.getString("subject") + "," + post.getString("content");
		}
		if (command instanceof RequestHelpCommand) {
			return retVal + dataMap.get("error-type") + "," + dataMap.get("error-message") + "," + dataMap.get("output");
		}
		if (command instanceof RunCommand) {
			dataMap = command.getAttributesMap();
			return df.format(timestamp) + "\t" + dataMap.get("type") + ": " + dataMap.get("kind") 
					+ ", projectName: " + dataMap.get("projectName")
					+ ", className: " + dataMap.get("className"); 
		}
		if (command instanceof WebCommand) {
			return retVal + dataMap.get("keyword") + ", " + dataMap.get("URL");
		}
		if (command instanceof Insert || command instanceof Delete) {
//			return retVal + "offset: " + command.getAttributesMap().get("offset") + ", text: " + dataMap.get("text");
//		}
//		if (command instanceof Delete) {
//			return retVal + "offset: " + command.getAttributesMap().get("offset") + ", text: " + dataMap.get("text");
			return retVal;
		}
		if (command instanceof FileOpenCommand) {
			return retVal + System.lineSeparator() + dataMap.get("snapshot");
		}
		return "";
	}

	public void createAssignData(String assign, Map<String, List<List<EHICommand>>> data) {
		notifyNewAssignment(assign, data);

		File csv = new File(assign + ".csv");
		FileWriter fw;
		File csv2 = new File(assign + "Event.csv");
		FileWriter fw2;
		int[] sum = new int[11];
		try {
			if (csv.exists()) {
				csv.delete();
			}
			csv.createNewFile();
			fw = new FileWriter(csv);
			CSVWriter cw = new CSVWriter(fw);
			String[] header = { "Student", "Total Time Spent", "Active Time", "Rest Time", "Wall Clock Time", "Pause",
					"Web", "Insert", "Delete", "Replace", "Copy", "Paste", "Run", "Exception", "LocalChecks",
					"Exception Breakdown" };
			cw.writeNext(header);

			if (csv2.exists()) {
				csv2.delete();
			}
			csv2.createNewFile();
			fw2 = new FileWriter(csv2);
			CSVWriter cw2 = new CSVWriter(fw2);
			List<String[]> output = new ArrayList<>();
			String[] header2 = { "case_id", "timestamp", "activity", "user" };
			cw2.writeNext(header2);
			assign = assign.substring(assign.lastIndexOf(File.separator) + 1);
			for (String student : data.keySet()) {
				System.out.println("Generating AssignData for student " + student);
				List<List<EHICommand>> nestedCommands = data.get(student);
				student = student.substring(student.lastIndexOf(File.separator) + 1);

				List<String> retVal = new ArrayList<>();
				retVal.add(student);
				long totalTime = totalTimeSpent(nestedCommands);
				if (totalTime == 0) {
					continue;
				}
				long[] restTime = restTime(nestedCommands, FIVE_MIN, Long.MAX_VALUE);
				long wallClockTime = wallClockTime(nestedCommands);
				retVal.add(format(totalTime));
				retVal.add(format(totalTime - restTime[0]));
				retVal.add(format(restTime[0]));
				retVal.add(format(wallClockTime));
				int[] numCommands = new int[10];
				List<String> breakdownList = new ArrayList<>();
				StringBuffer list = new StringBuffer();
				notifyNewStudent(student, nestedCommands, totalTime, wallClockTime, restTime);
				int aSessionNumber = -1;
				for (List<EHICommand> commands : nestedCommands) {
					long lastTime = -1;
					long curTime = -1;
					aSessionNumber++;
					notifyNewSession(aSessionNumber);
					for (int i = 0; i < commands.size(); i++) {
						int aStartCommandIndex = i;
						String aRestType = null;
						EHICommand command = commands.get(i);
						EHICommand aStartCommand = command;
						boolean aRestInSession = false;
						String aRestKind = null;
						String aCommandTypeChar = "";
						String anEventTypeString = "";
						String aText = null;

						if (i > 0) {
							lastTime = curTime;
						}
						if (command != null) {
							curTime = command.getTimestamp() + command.getStartTimestamp();
						}
						if (lastTime - curTime > FIVE_MIN) {
							addOneLine(output, assign, lastTime, REST_INSESSION, student);
							aRestInSession = true;
							aRestKind = REST_INSESSION;
						}
						if (command instanceof ShellCommand
								&& ((ShellCommand) command).getAttributesMap().get("type").equals(ECLIPSE_LOST_FOCUS)) {
							addOneLine(output, assign, curTime, REST_LOSEFOCUS, student);
							aRestKind = REST_LOSEFOCUS;
						} else if (command instanceof ShellCommand
								&& ((ShellCommand) command).getAttributesMap().get("type").equals(ECLIPSE_CLOSED)) {
							addOneLine(output, assign, curTime, REST_ENDSESSION, student);
							aRestKind = REST_ENDSESSION;
						} else if (command instanceof InsertStringCommand || command instanceof CopyCommand
								|| command instanceof Delete || command instanceof Insert
								|| (command instanceof EclipseCommand && ((EclipseCommand) command).getCommandID()
										.equals("eventLogger.styledTextCommand.DELETE_PREVIOUS"))
								|| command instanceof Replace || command instanceof PasteCommand
								|| command instanceof ExceptionCommand || command instanceof RunCommand
								|| command instanceof ConsoleOutputCommand || command instanceof ConsoleInput
								|| command instanceof RequestHelpCommand || command instanceof GetHelpCommand
								|| command instanceof LocalCheckCommand) {

							anEventTypeString = getEventType(command).toString();
//							addOneLine(output, assign, curTime, getEventType(command), student);
							addOneLine(output, assign, curTime, anEventTypeString, student);

						}
						if (command instanceof PauseCommand) {
							numCommands[0]++;
							aCommandTypeChar = "A";
							list.append(aCommandTypeChar);
//							list.append("A");

						}
						if (command instanceof WebCommand) {
							numCommands[1]++;
							aCommandTypeChar = "W";
							list.append(aCommandTypeChar);
//							list.append("W");
						}
//						if (command instanceof InsertStringCommand) {
//							numCommands[2]++;
//							list.append("I");
//						}
						if (command instanceof Insert) {
							aText = command.getDataMap().get("text");
							numCommands[2] += command.getDataMap().get("text").length();
						}
						if (command instanceof Delete) {
//							numCommands[3]++;
							numCommands[3] += command.getDataMap().get("text").length();
							aCommandTypeChar = "D";
							list.append(aCommandTypeChar);
//							list.append("D");
						}
						if (command instanceof EclipseCommand && ((EclipseCommand) command).getCommandID()
								.equals("eventLogger.styledTextCommand.DELETE_PREVIOUS")) {
							numCommands[3]++;
						}
						if (command instanceof Replace) {
							numCommands[4]++;
							aCommandTypeChar = "R";
							list.append(aCommandTypeChar);
//							list.append("R");
						}
						if (command instanceof CopyCommand) {
							numCommands[5]++;
							aCommandTypeChar = "C";
							list.append(aCommandTypeChar);
//							list.append("C");
						}
						if (command instanceof PasteCommand) {
							numCommands[6]++;
							aCommandTypeChar = "P";
							list.append(aCommandTypeChar);
//							list.append("P");
						}
						if (command instanceof RunCommand) {
							numCommands[7]++;
							aCommandTypeChar = "U";
							list.append(aCommandTypeChar);
//							list.append("U");
						}
						if (command instanceof LocalCheckCommand) {
							numCommands[9]++;
							aCommandTypeChar = "L";
							list.append(aCommandTypeChar);
//							list.append("L");
						}
						if (command instanceof ExceptionCommand || command instanceof EHExceptionCommand) {
							if (isException(command)) {
								if (command instanceof EHExceptionCommand) {
									ExceptionCommand ex = new ExceptionCommand(
											command.getDataMap().get("exceptionString"), "");
									ex.setTimestamp(command.getTimestamp());
									ex.setStartTimestamp(command.getStartTimestamp());
									aStartCommand = ex;
								}
								numCommands[8]++;
								aCommandTypeChar = "E";
								list.append(aCommandTypeChar);
//								list.append("E");
								if (i < commands.size() - 1) {
									command = commands.get(i + 1);
								} else {
									continue;
								}
								while (!(command instanceof InsertStringCommand || command instanceof Replace
										|| command instanceof Delete || command instanceof CopyCommand
										|| command instanceof PasteCommand)) {
									if (command instanceof RunCommand || command instanceof ConsoleOutputCommand
											|| command instanceof ConsoleInput || command instanceof RequestHelpCommand
											|| command instanceof GetHelpCommand || command instanceof ExceptionCommand
											|| (command instanceof ShellCommand && ((ShellCommand) command)
													.getAttributesMap().get("type").equals("ECLIPSE_LOST_FOCUS"))
											|| (command instanceof ShellCommand && ((ShellCommand) command)
													.getAttributesMap().get("type").equals("ECLIPSE_CLOSED"))
											|| command instanceof LocalCheckCommand) {
										if (i > 0) {
											lastTime = curTime;
										}
										curTime = command.getTimestamp() + command.getStartTimestamp();
										if (lastTime - curTime > FIVE_MIN) {
											addOneLine(output, assign, lastTime, REST_INSESSION, student);
										}
										if (command instanceof ShellCommand && ((ShellCommand) command)
												.getAttributesMap().get("type").equals(ECLIPSE_LOST_FOCUS)) {
											addOneLine(output, assign, curTime, REST_LOSEFOCUS, student);
										} else if (command instanceof ShellCommand && ((ShellCommand) command)
												.getAttributesMap().get("type").equals(ECLIPSE_CLOSED)) {
											addOneLine(output, assign, curTime, REST_ENDSESSION, student);
										} else {
											addOneLine(output, assign, curTime, getEventType(command).toString(),
													student);
										}
									}
									if ((command instanceof ExceptionCommand || command instanceof EHExceptionCommand)
											&& isException(command)) {
										if (command instanceof EHExceptionCommand) {
											ExceptionCommand ex = new ExceptionCommand(
													command.getDataMap().get("exceptionString"), "");
											ex.setTimestamp(command.getTimestamp());
											ex.setStartTimestamp(command.getStartTimestamp());
											if (i > 0) {
												lastTime = curTime;
											}
											curTime = command.getTimestamp() + command.getStartTimestamp();
											if (lastTime - curTime > FIVE_MIN) {
												addOneLine(output, assign, lastTime, REST_INSESSION, student);
											}
											addOneLine(output, assign, curTime, getEventType(ex).toString(), student);
										}
										numCommands[8]++;
										aCommandTypeChar = "E";
										list.append(aCommandTypeChar);
//										list.append("E");
									}
									i++;
									if (i + 1 < commands.size()) {
										command = commands.get(i + 1);
									} else {
										break;
									}

								}
								breakdownList.add(countConsecutiveCommands(list));
								list = new StringBuffer();

//								notifyNewCommandInSession(
//										aSessionNumber, 
//										i, 
//										command, 
//										aCommandTypeChar, 
//										aRestType, 
//										aText);
							}
						}
						notifyNewCommandInSession(aStartCommandIndex, curTime, aStartCommand, aCommandTypeChar,
								anEventTypeString, aRestInSession, aRestType, aText, i, command);
					}

				}
				if (list.length() != 0) {
					breakdownList.add(countConsecutiveCommands(list));
				}
				for (int i = 0; i < numCommands.length; i++) {
					retVal.add(i + 5, numCommands[i] + "");
					sum[i] += numCommands[i];
					sum[numCommands.length] += numCommands[i];
				}
				for (int i = 0; i < breakdownList.size(); i++) {
					retVal.add(i + 5 + numCommands.length, breakdownList.get(i));
				}
				String[] nextLine = retVal.toArray(new String[1]);
				cw.writeNext(nextLine);
			}
			List<String> retVal = new ArrayList<>();
			retVal.add("Sum");
			retVal.add("");
			retVal.add("");
			retVal.add("");
			retVal.add("");
			for (int i = 0; i < sum.length; i++) {
				retVal.add(sum[i] + "");
			}
			String[] nextLine = retVal.toArray(new String[1]);
			cw.writeNext(nextLine);
			cw2.writeAll(output);
			fw.close();
			cw.close();

			fw2.close();
			cw2.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void commandCount(String assign, Map<String, List<List<EHICommand>>> data) {
		File csv = new File(assign + "CommandCount.csv");
		FileWriter fw;
		try {
			if (csv.exists()) {
				csv.delete();
			}
			csv.createNewFile();
			fw = new FileWriter(csv);
			CSVWriter cw = new CSVWriter(fw);
			String[] header = { "Student", "Web", "Insert", "Delete", "Replace", "Copy", "Paste", "Run", "Debug", "LocalChecks"};
			cw.writeNext(header);

			assign = assign.substring(assign.lastIndexOf(File.separator) + 1);
			for (String student : data.keySet()) {
				System.out.println("Generating AssignData for student " + student);
				List<List<EHICommand>> nestedCommands = data.get(student);
				student = student.substring(student.lastIndexOf(File.separator) + 1);

				List<String> retVal = new ArrayList<>();
				retVal.add(student);
				int[] numCommands = new int[9];
				List<String> breakdownList = new ArrayList<>();
				long localcheckTime = 0;
				for (List<EHICommand> commands : nestedCommands) {
					for (int i = 0; i < commands.size(); i++) {
						EHICommand command = commands.get(i);

						if (command instanceof WebCommand) {
							numCommands[0]++;
						}
						if (command instanceof Insert) {
							numCommands[1] += command.getDataMap().get("text").length();
						}
						if (command instanceof Delete) {
							numCommands[2] += command.getDataMap().get("text").length();
						}
						if (command instanceof EclipseCommand && ((EclipseCommand) command).getCommandID()
								.equals("eventLogger.styledTextCommand.DELETE_PREVIOUS")) {
							numCommands[2]++;
						}
						if (command instanceof Replace) {
							numCommands[3]++;
						}
						if (command instanceof CopyCommand) {
							numCommands[4]++;
						}
						if (command instanceof PasteCommand) {
							numCommands[5]++;
						}
						if (command instanceof RunCommand) {
							if (command.getAttributesMap().get("type").equals("Debug")) {
								numCommands[7]++;
							} else {
								numCommands[6]++;
							}
						}
						if (command instanceof LocalCheckCommand) {
							long timestamp = command.getTimestamp() + command.getStartTimestamp();
							if (localcheckTime < timestamp) {
								numCommands[8]++;
								localcheckTime = timestamp;
							}
						}
					}

				}
				retVal.add(student);
				for (int i = 0; i < numCommands.length; i++) {
					retVal.add(numCommands[i]+"");
				}
				String[] nextLine = retVal.toArray(new String[1]);
				cw.writeNext(nextLine);
			}
			fw.close();
			cw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void createDistributionData(String assign, Map<String, List<List<EHICommand>>> data) {
		File csv = new File(assign + "Distribution.csv");
		FileWriter fw;
		try {
			if (csv.exists()) {
				csv.delete();
			}
			csv.createNewFile();
			fw = new FileWriter(csv);
			CSVWriter cw = new CSVWriter(fw);
			String[] header = getHeader();
			cw.writeNext(header);

			String[] sum = new String[header.length];
			sum[0] = "Sum";
			int[] restSum = new int[REST.length];
			long[] restTimeSum = new long[REST.length];
			for (int i = 1; i < sum.length; i++) {
				sum[i] = "";
			}
			assign = assign.substring(assign.lastIndexOf(File.separator) + 1);
			for (String student : data.keySet()) {
				System.out.println("Generating DistributionData for student " + student);
				List<List<EHICommand>> nestedCommands = data.get(student);
				student = student.substring(student.lastIndexOf(File.separator) + 1);
				List<String> retVal = new ArrayList<>();
				retVal.add(student);
				long totalTime = totalTimeSpent(nestedCommands);
				if (totalTime == 0) {
					continue;
				}
				long wallClockTime = wallClockTime(nestedCommands);
				retVal.add(format(totalTime));
				retVal.add(format(wallClockTime));

				long[] restTime = { 0, 0 };
				for (int i = 0; i < REST.length; i++) {
					if (i < REST.length - 1) {
						restTime = restTime(nestedCommands, REST[i], REST[i + 1]);
					} else {
						restTime = restTime(nestedCommands, REST[i], Long.MAX_VALUE);
					}
					retVal.add(format(totalTime - restTime[0]));
					retVal.add(format(restTime[0]));
					retVal.add(restTime[1] + "");
					retVal.add(getTime(restTime[1] == 0 ? 0 : 1.0 * restTime[2] / restTime[1]));
					restSum[i] += restTime[1];
					restTimeSum[i] += restTime[2];
				}

				String[] days = daysSpent(nestedCommands);
				if (days != null) {
					for (int i = 0; i < days.length; i++) {
						retVal.add(days[i]);
					}
				}

				String[] nextLine = retVal.toArray(new String[1]);
				cw.writeNext(nextLine);
			}
			int sum2 = 0;
			for (int i = 0; i < restSum.length; i++) {
				sum[4 * i + 5] = restSum[i] + "";
				sum2 += restSum[i];
				sum[4 * i + 6] = getTime(restSum[i] == 0 ? 0 : 1.0 * restTimeSum[i] / restSum[i]);
			}
			sum[1] = sum2 + "";
			cw.writeNext(sum);
			fw.close();
			cw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void createPauseDistribution(String assign, Map<String, List<List<EHICommand>>> data) {
		File csv = new File(assign + "PauseDistribution.csv");
		System.out.println("Generating Pause Distribution for Assignment " + assign);
		FileWriter fw;
		try {
			if (csv.exists()) {
				csv.delete();
			}
			csv.createNewFile();
			fw = new FileWriter(csv);
			CSVWriter cw = new CSVWriter(fw);
			String[] prev = { "Prev" };
			cw.writeNext(prev);
			String[] header = getPauseHeader();
			cw.writeNext(header);

			String[] nextLine = new String[header.length];
			int[] sum = new int[PauseCommand.TYPES.length];
			long[] sumPause = new long[sum.length];
			assign = assign.substring(assign.lastIndexOf(File.separator) + 1);
			for (String student : data.keySet()) {
				List<List<EHICommand>> nestedCommands = data.get(student);
				if (nestedCommands.size() == 0)
					continue;
				student = student.substring(student.lastIndexOf(File.separator) + 1);
				List<String> retVal = new ArrayList<>();
				int[] numCommmands = new int[sum.length];
				long[] pauseTimes = new long[sum.length];
				long[] max = new long[sum.length];
				long[] min = new long[sum.length];
				double[] mean = new double[sum.length];
				List<List<Long>> pauses = new ArrayList<>();
				for (int i = 0; i < PauseCommand.TYPES.length; i++) {
					pauses.add(new ArrayList<>());
				}
				retVal.add(student);
				for (List<EHICommand> commands : nestedCommands) {
					for (EHICommand command : commands) {
						if (command instanceof PauseCommand) {
							for (int i = 0; i < sum.length; i++) {
								if (command.getDataMap().get("prevType").equals(PauseCommand.TYPES[i])) {
									numCommmands[i]++;
									long pause = Long.parseLong(command.getDataMap().get("pause"));
									pauses.get(i).add(pause);
									pauseTimes[i] += pause;
									if (min[i] == 0 || min[i] > pause) {
										min[i] = pause;
									}
									if (max[i] < pause) {
										max[i] = pause;
									}
									break;
								}
							}
						}
					}
				}
				for (int i = 0; i < sum.length; i++) {
					retVal.add(numCommmands[i] + "");
					if (numCommmands[i] == 0) {
						retVal.add("0");
					} else {
						mean[i] = 1.0 * pauseTimes[i] / numCommmands[i];
						retVal.add(mean[i] + "");
					}
					retVal.add(min[i] + "");
					retVal.add(max[i] + "");
					retVal.add(std(pauses.get(i), mean[i]) + "");

					sum[i] += numCommmands[i];
					sumPause[i] += pauseTimes[i];
				}
				nextLine = retVal.toArray(new String[1]);
				cw.writeNext(nextLine);
			}
			nextLine = new String[header.length];
			nextLine[0] = "Sum";
			for (int i = 0; i < sum.length; i++) {
				nextLine[1 + i * 5] = sum[i] + "";
				if (sum[i] == 0) {
					nextLine[2 + i * 5] = 0 + "";
				} else {
					nextLine[2 + i * 5] = sumPause[i] / sum[i] + "";
				}
			}
			cw.writeNext(nextLine);

			String[] empty = {};
			cw.writeNext(empty);
			String[] next = { "next" };
			cw.writeNext(next);
			sum = new int[PauseCommand.TYPES.length];

			sumPause = new long[sum.length];

			for (String student : data.keySet()) {
				List<List<EHICommand>> nestedCommands = data.get(student);
				if (nestedCommands.size() == 0)
					continue;
				student = student.substring(student.lastIndexOf(File.separator) + 1);
				List<String> retVal = new ArrayList<>();
				int[] numCommmands = new int[sum.length];
				long[] pauseTimes = new long[sum.length];
				long[] max = new long[sum.length];
				long[] min = new long[sum.length];
				double[] mean = new double[sum.length];
				List<List<Long>> pauses = new ArrayList<>();
				for (int i = 0; i < PauseCommand.TYPES.length; i++) {
					pauses.add(new ArrayList<>());
				}
				retVal.add(student);

				for (List<EHICommand> commands : nestedCommands) {
					for (EHICommand command : commands) {
						if (command instanceof PauseCommand) {
							for (int i = 0; i < sum.length; i++) {
								if (command.getDataMap().get("nextType").equals(PauseCommand.TYPES[i])) {
									numCommmands[i]++;
									long pause = Long.parseLong(command.getDataMap().get("pause"));
									pauses.get(i).add(pause);
									pauseTimes[i] += pause;
									if (min[i] == 0 || min[i] > pause) {
										min[i] = pause;
									}
									if (max[i] < pause) {
										max[i] = pause;
									}
									break;
								}
							}
						}
					}
				}
				for (int i = 0; i < sum.length; i++) {
					retVal.add(numCommmands[i] + "");
					if (numCommmands[i] == 0) {
						retVal.add("0");
					} else {
						mean[i] = 1.0 * pauseTimes[i] / numCommmands[i];
						retVal.add(mean[i] + "");
					}
					retVal.add(min[i] + "");
					retVal.add(max[i] + "");
					retVal.add(std(pauses.get(i), mean[i]) + "");
					sum[i] += numCommmands[i];
					sumPause[i] += pauseTimes[i];
				}
				nextLine = retVal.toArray(new String[1]);
				cw.writeNext(nextLine);
			}
			nextLine = new String[header.length];
			nextLine[0] = "Sum";
			for (int i = 0; i < sum.length; i++) {
				nextLine[1 + i * 5] = sum[i] + "";
				if (sum[i] == 0) {
					nextLine[2 + i * 5] = 0 + "";
				} else {
					nextLine[2 + i * 5] = sumPause[i] / sum[i] + "";
				}
			}
			cw.writeNext(nextLine);

			fw.close();
			cw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void createWebStats(String assign, File folder, Map<String, List<List<EHICommand>>> data) {
		File csv = new File(folder, assign + "WebStats.csv");
		FileWriter fw;
		File csv2 = new File(folder, assign + "WebSearches.csv");
		FileWriter fw2;
		try {
			if (csv.exists()) {
				csv.delete();
			}
			if (csv2.exists()) {
				csv2.delete();
			}
			csv.createNewFile();
			csv2.createNewFile();
			fw = new FileWriter(csv);
			fw2 = new FileWriter(csv2);
			CSVWriter cw = new CSVWriter(fw);
			CSVWriter cw2 = new CSVWriter(fw2);
			String[] header = { "Title", "URL", "# of Visits", "Provided?" };
//			String[] header2 = {"ID", "Search Word", "Title", "URL", "Sequence", "Last Page of the Search?", "Pasted Text"};
			String[] header2 = { "Search Word" };
			cw.writeNext(header);
			cw2.writeNext(header2);

			Map<String, Integer> urls = new HashMap<>();
			Map<String, String> titles = new HashMap<>();
			Map<String, List<String>> searches = new LinkedHashMap<>();
			Map<String, List<String>> contents = new HashMap<>();
			for (String student : data.keySet()) {
				System.out.println("Generating WebStats for student " + student);
				EHICommand lastSearch = null;
				List<List<EHICommand>> nestedCommands = data.get(student);
				for (List<EHICommand> commands : nestedCommands) {
					for (int i = 0; i < commands.size(); i++) {
						EHICommand command = commands.get(i);
						if (command instanceof WebCommand) {
							String url = command.getDataMap().get("URL");
							if (command.getAttributesMap().get("type").equals("Search")) {
								lastSearch = command;
								if (!searches.containsKey(lastSearch.getDataMap().get("keyword"))) {
									searches.put(lastSearch.getDataMap().get("keyword"), new ArrayList<>());
								}
							}
							if (command.getAttributesMap().get("type").equals("Search Result")
									|| command.getAttributesMap().get("type").equals("Stack Overflow")) {
								String searchWord = "null";
								if (lastSearch != null) {
									searchWord = lastSearch.getDataMap().get("keyword");
								}
								if (!searches.containsKey(searchWord)) {
									searches.put(searchWord, new ArrayList<>());
								}
								searches.get(searchWord).add(url);
//								if (!contents.containsKey(url)) {
//									List<String> list = new ArrayList<>();
//									if (!url.contains("google.com")) {
//										list.add(readWebContent(url));
////										list.add(readWebContent2(url));
//										contents.put(url, list);
//									}
//								}
							}
							if (command.getAttributesMap().get("type").contains("Provided")) {
//								if (!searches.containsKey("Provided")) {
//									searches.put("Provided", new ArrayList<>());
//								}
//								searches.get("Provided").add(url);
//								if (!contents.containsKey(url)) {
//									List<String> list = new ArrayList<>();
//									list.add(readWebContent(url));
////									list.add(readWebContent2(url));
//									contents.put(url, list);
//								}
							}
							urls.merge(url, 1, Integer::sum);
							titles.put(url, command.getDataMap().get("keyword"));
						}
						if (command instanceof PasteCommand) {
							outer: for (int j = i - 1; j >= 0 && j > i - 20; j--) {
								EHICommand command2 = commands.get(j);
								String pastedText = "";
								if (command2 instanceof InsertStringCommand || command2 instanceof Replace) {
									if (command2 instanceof InsertStringCommand) {
										pastedText = command2.getDataMap().get(InsertStringCommand.XML_Data_Tag);
									} else {
										pastedText = command2.getDataMap().get("insertedText");
									}
									if (pastedText.length() > 10) {
										for (String url : contents.keySet()) {
											List<String> list = contents.get(url);
											String pastedText2 = pastedText.replaceAll("\\s", "");
											if (list.get(0).contains(pastedText2)) {
												for (int k = 1; k < list.size(); k++) {
													if (list.get(k).replaceAll("\\s", "").equals(pastedText2)) {
														break outer;
													}
												}
												list.add(pastedText);
												break outer;
											}
										}
									}
								}
							}
						}
					}
				}
				for (String s : searches.keySet()) {
					List<String> nextLine = new ArrayList<>();
//					nextLine.add(student);
					if (s.contains(" - Google Search")) {
						s = s.substring(0, s.indexOf(" - Google Search"));
					}
					nextLine.add(s);
					cw2.writeNext(nextLine.toArray(new String[1]));

//					if (searches.get(s).size() == 0) {
//						nextLine.add("No Result");
//						cw2.writeNext(nextLine.toArray(new String[1]));
//						continue;
//					}
//					for (int i = 0; i < searches.get(s).size(); i++) {
//						String url = searches.get(s).get(i);
//						nextLine.add(titles.get(url));
//						nextLine.add(url);
//						nextLine.add(i+1+"");
//						nextLine.add(i == searches.get(s).size()-1 ? "Last" : "");
//						if (contents.containsKey(url)) {
//							List<String> list = contents.get(url);
//							for (int j = 1; j < list.size(); j++) {
//								nextLine.add(list.get(j));
//							}
//						}
//						cw2.writeNext(nextLine.toArray(new String[1]));
//						nextLine = new ArrayList<>();
//						nextLine.add(student);
//						nextLine.add("");
//					}
				}
				searches.clear();
			}
			Map<String, Integer> sortedMap = new LinkedHashMap<>();
			urls.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
					.forEachOrdered(x -> sortedMap.put(x.getKey(), x.getValue()));
			for (String s : sortedMap.keySet()) {
				String[] nextLine = { titles.get(s), s, sortedMap.get(s) + "", isProvided(s) ? "Provided" : "" };
				cw.writeNext(nextLine);
			}
			fw.close();
			cw.close();

			fw2.close();
			cw2.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public double std(List<Long> pauses, double mean) {
		double sum = 0;
		for (Long l : pauses) {
			sum += Math.pow((double) l - mean, 2);
		}
		return Math.sqrt(sum / pauses.size());
	}

	public void addOneLine(List<String[]> output, String assign, long time, String type, String pid) {
		String[] nextLine = new String[4];
		nextLine[0] = assign + " " + pid;
		Date date = new Date(time);
		DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		nextLine[1] = df.format(date);
		nextLine[2] = type;
		nextLine[3] = pid;
		if (nextLine != null) {
			output.add(nextLine);
		}
	}

	public void addOneLine(List<String[]> output, String assign, long time, String type, String pid, long duration) {
		String[] nextLine = new String[5];
		nextLine[0] = assign + " " + pid;
		Date date = new Date(time);
		DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		nextLine[1] = df.format(date);
		nextLine[2] = type;
		nextLine[4] = pid;
		nextLine[3] = convertToHourMinuteSecond(duration);
		if (nextLine != null) {
			output.add(nextLine);
		}
	}

	public void addOneLine(List<String[]> output, String pid, long runTime, long runDuration, long debugTime,
			long debugDuration) {
		String[] nextLine = new String[5];
		nextLine[0] = pid;
		Date date = new Date(runTime);
		Date date2 = new Date(debugTime);
		DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		nextLine[1] = df.format(date);
		nextLine[2] = convertToHourMinuteSecond(runDuration);
		nextLine[3] = df.format(date2);
		nextLine[4] = convertToHourMinuteSecond(debugDuration);
		if (nextLine != null) {
			output.add(nextLine);
		}
	}

	public String countConsecutiveCommands(StringBuffer list) {
		char lastChar = ' ';
		char curChar = ' ';
		int count = 1;
		StringBuffer retVal = new StringBuffer();
		for (int i = 0; i < list.length(); i++) {
			if (i != 0) {
				lastChar = curChar;
			}
			curChar = list.charAt(i);
			if (curChar == lastChar) {
				count++;
			} else if (i != 0) {
				retVal.append(lastChar + "" + count);
				count = 1;
			}
		}
		retVal.append(curChar + " " + count);
		return retVal.toString();
	}

	public void sortNestedCommands(List<List<EHICommand>> nestedCommands) {
		for (int i = 0; i < nestedCommands.size(); i++) {
			List<EHICommand> commands = nestedCommands.get(i);
			if (commands == null || commands.size() < 2) {
				nestedCommands.remove(i);
				i--;
			} else if (commands.size() > 2) {
				sortCommands(commands);
			}
		}
	}

	public void sortCommands(List<EHICommand> commands) {
		for (int i = 0; i < commands.size(); i++) {
			if (commands.get(i) == null) {
				commands.remove(i);
				i--;
			}
		}
		EHICommand command = null;
		long cur = 0;
		for (int i = 0; i < commands.size(); i++) {
			command = commands.get(i);
			cur = command.getStartTimestamp() + command.getTimestamp();
			int j = i - 1;
			while (j >= 0) {
				if (commands.get(j).getStartTimestamp() + commands.get(j).getTimestamp() > cur) {
					j--;
				} else {
					break;
				}
			}
			if (j < i - 1) {
				commands.remove(i);
				commands.add(j + 1, command);
			}
		}
	}

	protected long getLogFileCreationTime(File file) {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS");
		try {
			return df.parse(file.getName().substring(3, 27)).getTime();
		} catch (ParseException e) {
			e.printStackTrace();
			return 0;
		}
	}

	public String[] getPauseHeader() {
		List<String> retVal = new ArrayList<>();
		retVal.add("ID");
		for (String s : PauseCommand.TYPES) {
			retVal.add(s);
			retVal.add("Avg. Time");
			retVal.add("Shortest Time");
			retVal.add("Longest Time");
			retVal.add("STD");
		}
		return retVal.toArray(new String[1]);
	}

	public static EventType getEventType(EHICommand command) {
		if (command instanceof InsertStringCommand || command instanceof CopyCommand || command instanceof Delete
				|| command instanceof Replace || command instanceof PasteCommand) {
//			return "Edit";
			return EventType.Edit;

		}
		if (command instanceof RunCommand || command instanceof ConsoleOutputCommand || command instanceof ConsoleInput
				|| command instanceof EHExceptionCommand) {
//			return "IO";
			return EventType.IO;
		}
		if (command instanceof ExceptionCommand) {
//			return "Exception";
			return EventType.Exception;
		}
		if (command instanceof RequestHelpCommand || command instanceof GetHelpCommand) {
//		        	return "Request";
			return EventType.Request;

		}
		if (command instanceof LocalCheckCommand) {
//			return "LocalCheck";
			return EventType.LocalCheck;

		}
//		return "Other";
		return EventType.Other;

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

	protected String[] getHeader() {
		String[] header = new String[4 * REST.length + 5];
		header[0] = "Student";
		header[1] = "Total Time Spent";
		header[2] = "Wall Clock Time";
		for (int i = 0; i < REST.length; i++) {
			String t = getTime(REST[i]);
			header[3 + i * 4] = "Active Time(" + t + ")";
			header[4 + i * 4] = "Rest Time(" + t + ")";
			header[5 + i * 4] = "# of Rests(" + t + ")";
			header[6 + i * 4] = "Avg. Rest Time(" + t + ")";
		}
		header[header.length - 2] = "# of Days";
		header[header.length - 1] = "Time Spent Each Day";
		return header;
	}

	protected String getTime(long t) {
		long ret = t / ONE_SECOND;
		if (ret < 60) {
			return ret + "s";
		}
		ret /= 60;
		if (ret < 60) {
			return ret + "m";
		}
		double ret2 = ret / 60.0;
		return ret2 + "h";
	}

	protected String getTime(double t) {
		double ret = t / ONE_SECOND;
		DecimalFormat df = new DecimalFormat("#.###");
		if (ret < 60) {
			return df.format(ret);
		}
		ret /= 60;
		if (ret < 60) {
			return df.format(ret);
		}
		ret /= 60.0;
		return df.format(ret);
	}

	protected long totalTimeSpent(List<List<EHICommand>> nestedCommands) {
		long projectTime = 0;
		for (int k = 0; k < nestedCommands.size(); k++) {
			List<EHICommand> commands = nestedCommands.get(k);
			if (commands.isEmpty()) {
				continue;
			}
			int j = 0;
			for (; j < commands.size(); j++) {
				if (commands.get(j).getStartTimestamp() > 0 || commands.get(j).getTimestamp() > 0) {
					break;
				}
			}
			if (j == commands.size()) {
				j--;
			}
			long timestamp1 = commands.get(j).getTimestamp() + commands.get(j).getStartTimestamp();
			EHICommand command2 = commands.get(commands.size() - 1);
			long timestamp2 = command2.getStartTimestamp() + command2.getTimestamp();
			projectTime += timestamp2 - timestamp1;
		}
		return projectTime;
	}

	protected long[] restTime(List<List<EHICommand>> nestedCommands, long time, long time2) {
		long[] restTime = { 0, 0, 0 };
		for (List<EHICommand> commands : nestedCommands) {
			for (EHICommand command : commands) {
				if (command instanceof PauseCommand) {
					long pause = Long.parseLong(command.getDataMap().get("pause"));
					if (pause > time) {
						restTime[0] += pause;
						if (pause < time2) {
							restTime[1]++;
							restTime[2] += pause;
						}
					}
				}
			}
		}
		return restTime;
	}

	protected long wallClockTime(List<List<EHICommand>> nestedCommands) {
		long wallClockTime = 0;
		EHICommand c1 = null;
		EHICommand c2 = null;
		if (nestedCommands.size() == 0) {
			return 0;
		}
		for (int j = 0; j < nestedCommands.get(0).size(); j++) {
			c1 = nestedCommands.get(0).get(j);
			if (c1.getStartTimestamp() > 0 || c1.getTimestamp() > 0) {
				break;
			}
		}
		if (c1 == null) {
			return 0;
		}
		c2 = nestedCommands.get(nestedCommands.size() - 1)
				.get(nestedCommands.get(nestedCommands.size() - 1).size() - 1);
		wallClockTime = c2.getStartTimestamp() + c2.getTimestamp() - c1.getStartTimestamp() - c1.getTimestamp();
		return wallClockTime;
	}

	protected String[] daysSpent(List<List<EHICommand>> nestedCommands) {
		List<String> retVal = new ArrayList<>();
		int days = 1;
		long startTime = 0;
		long endTime = 0;
		List<EHICommand> commands = nestedCommands.get(0);
		EHICommand command = null;
		List<List<EHICommand>> nestedCommands2 = new ArrayList<>();
		List<EHICommand> commands2 = null;
		for (int i = 1; i < commands.size(); i++) {
			command = commands.get(i);
			if (command.getStartTimestamp() != 0) {
				break;
			}
		}
		startTime = command.getStartTimestamp();
		startTime -= startTime % DAY;
		commands = nestedCommands.get(nestedCommands.size() - 1);
		endTime = command.getStartTimestamp() + command.getTimestamp();
		endTime = endTime - endTime % DAY + DAY;
		long timeStamp = 0;
		for (int i = 0; i < nestedCommands.size(); i++) {
			commands = nestedCommands.get(i);
			commands2 = new ArrayList<>();
			nestedCommands2.add(commands2);
			for (int j = 0; j < commands.size(); j++) {
				command = commands.get(j);
				timeStamp = command.getStartTimestamp() + command.getTimestamp();
				if (timeStamp == 0) {
					continue;
				}
				if (timeStamp > startTime && timeStamp < (startTime + DAY)) {
					commands2.add(command);
				} else if (timeStamp >= (startTime + DAY)) {
					days++;
					startTime = timeStamp - timeStamp % DAY;
					if (nestedCommands2.size() > 0 && nestedCommands2.get(0).size() > 0) {
						retVal.add(format(totalTimeSpent(nestedCommands2)));
					}
					nestedCommands2 = new ArrayList<>();
					commands2 = new ArrayList<>();
					nestedCommands2.add(commands2);
					commands2.add(command);
				}
			}
		}
		if (nestedCommands2.size() > 0 && nestedCommands2.get(0).size() > 0) {
			retVal.add(format(totalTimeSpent(nestedCommands2)));
		}
		retVal.add(0, days + "");
		return retVal.toArray(new String[1]);
	}

	protected String format(long timeSpent) {
		boolean negative = false;
		if (timeSpent < 0) {
			negative = true;
			timeSpent = -1 * timeSpent;
		}
		long hour = timeSpent / 3600000;
		long minute = timeSpent % 3600000 / 60000;
		long second = timeSpent % 60000 / 1000;
		return (negative ? "-" : "") + String.format("%d:%02d:%02d", hour, minute, second);
//		return negative?"-":"" + hour + ":" + (minute>9?minute:"0"+minute) + ":" + (second>9?second:("0"+second));
	}

	protected boolean isException(EHICommand command) {
		String output = "";
		String language = "";
		if (command instanceof EHExceptionCommand) {
			output = command.getDataMap().get("outputString");
		} else {
			output = command.getDataMap().get("exceptionString");
			language = command.getDataMap().get("language");
		}
		for (ExceptionMatcher em : ems) {
			if ((language.equals("") || language.equals(em.getLanguage())) && em.isException(output)) {
				return true;
			}
		}
		return false;
	}

	public boolean isProvided(String s) {
		for (String url : WebCommand.PROVIDED_URL) {
			if (s.equals(url)) {
				return true;
			}
		}
		return false;
	}

	public abstract void delete(String path);

	public void deleteStudent(File student) {
		System.out.println("Deleting student " + student);
		if (!student.exists()) {
			System.out.println("Folder " + student + " does not exist");
			return;
		}
		File logFolder = null;
		File submission = new File(student, "Submission attachment(s)");
		if (submission.exists()) {
			logFolder = new File(getProjectFolder(submission), "Logs" + File.separator + "Eclipse");
		} else {
			logFolder = new File(student, "Eclipse");
			if (!logFolder.exists()) {
				logFolder = new File(getProjectFolder(student), "Logs" + File.separator + "Eclipse");
			}
		}
		if (!logFolder.exists()) {
			System.out.println("No logs found for student " + student.getName());
			return;
		}
		File[] generatedLogs = logFolder.listFiles(File::isDirectory);
		if (generatedLogs != null && generatedLogs.length > 0) {
			for (File file : generatedLogs) {
				FileUtility.deleteFolder(file);
			}
		}
	}

	public void mapTestToSuite() {
		assignMap = new HashMap<>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(suite)));
			String line = "";
			Assignment assign = null;
			Matcher matcher = null;
			while ((line = br.readLine()) != null) {
				if (ecPattern.matcher(line).matches()) {
					if ((line = br.readLine()) != null) {
						for (String test : line.split(", ")) {
							if (assign != null) {
								assign.addEC(test);
							}
						}
						;
					}
				} else if (assignPattern.matcher(line).matches()) {
					if (!assignMap.containsKey(line)) {
						assign = new Assignment(line);
						assignMap.put(line, assign);
					}
				} else if (assignSuitePattern.matcher(line).matches()) {
					continue;
				} else if ((matcher = suitePattern.matcher(line)).matches()) {
//					Matcher matcher = suitePattern.matcher(line);
//					matcher.matches();
					Suite suite = new Suite(matcher.group(1),
							new HashSet<String>(Arrays.asList(matcher.group(2).split(", "))));
					if (assign != null) {
						assign.addSuite(suite);
					}
				}
			}
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void createEvents(String assign, Map<String, List<List<EHICommand>>> data) {
		File csv = new File(assign + "Event.csv");
		FileWriter fw;
		List<String[]> output = new ArrayList<>();
		try {
			if (csv.exists()) {
				csv.delete();
			}
			csv.createNewFile();
			fw = new FileWriter(csv);
			CSVWriter cw = new CSVWriter(fw);
//			String[] header = {"case_id", "timestamp", "activity", "user"};
//			String[] header = {"case_id", "timestamp", "activity", "duration", "user"};
			String[] header = { "student", "First Run", "Run Duration", "First Debug", "Debug Duration" };
			cw.writeNext(header);
			for (String student : data.keySet()) {
				System.out.println("Writing " + assign + " student " + student + "to " + csv.getName());
				List<List<EHICommand>> nestedCommands = data.get(student);
//				List<List<Command>> breakdown = fixBreakdown(nestedCommands);
				List<List<Command>> breakdown = runs(nestedCommands);
				boolean firstRun = false;
				boolean fisrtDebug = false;
				long firstRunTime = -1;
				long firstRunDuration = -1;
				long firstDebugTime = -1;
				long firstDebugDuration = -1;
				for (List<Command> list : breakdown) {
					for (Command command : list) {
//						if (command.getType().equals("Exception")) {
//							addOneLine(output, "",  command.getTime(), command.getType(), student.substring(student.lastIndexOf(File.separator)+1));
//						} else {
//						if (command.getType().equals("Fixing")) 
						String type = command.getType();
						if (!firstRun && type.equals("Run")) {
//							addOneLine(output, "",  command.getTime(), command.getType(), student.substring(student.lastIndexOf(File.separator)+1), command.getDuration());
							firstRunTime = command.getTime();
							firstRunDuration = command.getDuration();
							firstRun = true;
						}
						if (!fisrtDebug && type.equals("Debug")) {
//							addOneLine(output, "",  command.getTime(), command.getType(), student.substring(student.lastIndexOf(File.separator)+1), command.getDuration());
							firstDebugTime = command.getTime();
							firstDebugDuration = command.getDuration();
							fisrtDebug = true;
						}
//							addOneLine(output, "",  command.getTime(), command.getType(), student.substring(student.lastIndexOf(File.separator)+1), command.getDuration());
//						}
//						addOneLine(output, "",  command.getTime(), command.getType(), student.substring(student.lastIndexOf(File.separator)+1));
					}
					addOneLine(output, student.substring(student.lastIndexOf(File.separator) + 1), firstRunTime,
							firstRunDuration, firstDebugTime, firstDebugDuration);
				}
			}
			cw.writeAll(output);
			fw.close();
			cw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public List<List<Command>> fixBreakdown(List<List<EHICommand>> nestedCommands) {
		List<List<Command>> exceptionToFixList = new ArrayList<>();
		List<Command> commandList = null;
//		boolean fixing = false;
//		int fixOffset = -1;
//		int range = 100;
//		long exceptionTime = -1;
//		long skipException = -1;
//		long skipRun = -1;
		for (int i = 0; i < nestedCommands.size(); i++) {
			boolean fixing = false;
			int fixOffset = -1;
			int range = 200;
//			long exceptionTime = -1;
			long skipException = -1;
			long skipRun = -1;
			List<EHICommand> commands = nestedCommands.get(i);
			for (int j = 0; j < commands.size(); j++) {
				EHICommand command = commands.get(j);
				long time = command.getStartTimestamp() + command.getTimestamp();
				if (command instanceof ExceptionCommand) {
					if (commandList == null) {
						commandList = new ArrayList<>();
					}
					if (time > skipException) {
						commandList.add(new Command("Exception", time));
						fixing = true;
						fixOffset = -1;
						skipException = time + 10000;
//						exceptionTime = time;
					}
				}
				if (command instanceof Insert || command instanceof Replace || command instanceof Delete) {
					if (fixing && commandList != null) {
						int offset = Integer.parseInt(command.getAttributesMap().get("offset"));
						if (fixOffset == -1) {
							fixOffset = offset;
//							if (time < exceptionTime + 1000) {
//								commandList.add(new Command("Fixing", time));
//							} else {
//								commandList.add(new Command("Fixing", exceptionTime+1000));
//							}
							commandList.add(new Command("Fixing", time));
						}
						if (Math.abs(offset - fixOffset) <= range) {
							fixOffset = offset;
						} else {
							fixing = false;
							commandList.add(new Command("Editing", time));
						}
					}
				}
				if (command instanceof FileOpenCommand && fixing && fixOffset != -1) {
					fixing = false;
					fixOffset = -1;
				}
				if (commandList != null && command instanceof PauseCommand
						&& Long.parseLong(command.getDataMap().get("pause")) > 5 * 60 * 1000) {
//					if (fixing && fixOffset == -1 && !commandList.isEmpty() && !commandList.get(commandList.size()-1).getType().equals("Pause")) {
//						commandList.add(new Command("Fixing", exceptionTime+1000));
//					}

					Command aCommand = new Command("Break", time);
					aCommand.setDuration(Long.parseLong(command.getDataMap().get("pause")));
					commandList.add(aCommand);
//					exceptionTime = time + aCommand.getDuration();
				}
				if (commandList != null && command instanceof RunCommand) {
					if (time > skipRun) {
						if (command.getAttributesMap().get("type").equals("Debug")) {
							commandList.add(new Command("Debug", time));
						} else {
							commandList.add(new Command("Run", time));
						}
						skipRun = time + 10000;
					}
				}
				if (commandList != null && command instanceof WebCommand) {
					commandList.add(new Command("Website", time));
				}
				if (commandList != null && command instanceof ConsoleOutput) {
					if (time > skipException) {
						commandList.add(new Command("Fixed", time));
						removeExceptionRunFixed(commandList);
						if (!commandList.isEmpty()) {
							exceptionToFixList.add(commandList);
							commandList = null;
						}
					}
				}
			}
			if (commandList != null && !commandList.isEmpty()) {
				commandList.add(new Command("EndSession", commands.get(commands.size() - 1).getStartTimestamp()
						+ commands.get(commands.size() - 1).getTimestamp()));
				removeExceptionRunFixed(commandList);
				if (!commandList.isEmpty()) {
					exceptionToFixList.add(commandList);
					commandList = null;
				}
			}
		}
		return exceptionToFixList;
	}

	public List<List<Command>> runs(List<List<EHICommand>> nestedCommands) {
		List<List<Command>> exceptionToFixList = new ArrayList<>();
		List<Command> commandList = new ArrayList<>();
		exceptionToFixList.add(commandList);
		long lastRunTime = -1;
		boolean isDebug = false;
		for (int i = 0; i < nestedCommands.size(); i++) {
//			long skipRun = -1;
			boolean skipRun = false;
			List<EHICommand> commands = nestedCommands.get(i);
			for (int j = 0; j < commands.size(); j++) {
				EHICommand command = commands.get(j);
				long time = command.getStartTimestamp() + command.getTimestamp();
				if (command instanceof RunCommand) {
//					if (time > skipRun) {
					lastRunTime = time;
					if (!skipRun) {
						if (command.getAttributesMap().get("type").equals("Debug")) {
//							commandList.add(new Command("Debug", time));
							isDebug = true;
						} else {
//							commandList.add(new Command("Run", time));
						}
						commandList.add(new Command("Run", time));
//						skipRun = time + 60000;
						skipRun = true;
					}
				}
				if (command instanceof Insert || command instanceof Replace || command instanceof Delete
						|| (command instanceof EclipseCommand
								&& (((EclipseCommand) command).getCommandID()
										.equals("eventLogger.styledTextCommand.DELETE_PREVIOUS")
										|| ((EclipseCommand) command).getCommandID()
												.equals("org.eclipse.ui.edit.text.deletePreviousWord"))
								|| (command instanceof PauseCommand
										&& Long.parseLong(command.getDataMap().get("pause")) > 5 * 60000))) {
					if (skipRun && !commandList.isEmpty()) {
						Command command2 = commandList.get(commandList.size() - 1);
						command2.setDuration(lastRunTime - command2.getTime());
						if (isDebug) {
							command2.setType("Debug");
						}
					}
					skipRun = false;
					isDebug = false;
				}
				if (command instanceof ExceptionCommand || command instanceof ConsoleOutput) {
					lastRunTime = time;
				}
			}
		}
		return exceptionToFixList;
	}

	public void removeExceptionRunFixed(List<Command> list) {
		for (int i = 0; i < list.size() - 2; i++) {
			if (isExcptionRunFixed(list.get(i), list.get(i + 1), list.get(i + 2))) {
				list.remove(i + 2);
				list.remove(i + 1);
				list.remove(i);
				i--;
			}
		}
		for (int i = 0; i < list.size() - 1; i++) {
			if (isExcptionFixed(list.get(i), list.get(i + 1))) {
				list.remove(i + 1);
				list.remove(i);
				i--;
			}
		}
		for (int i = 0; i < list.size() - 1; i++) {
			if (isBreakFixing(list.get(i), list.get(i + 1))
					&& list.get(i + 1).getTime() < list.get(i).getTime() + list.get(i).getDuration() - 10000) {
				list.remove(i + 1);
			}
		}
		sortCommand(list);
		for (int i = 0; i < list.size() - 1; i++) {
			Command command = list.get(i);
			if (!command.getType().equals("EndSession") && !command.getType().equals("Exception")
					&& !command.getType().equals("Break") && !command.getType().equals("Debug")
					&& !command.getType().equals("Run")) {
				command.setDuration(list.get(i + 1).getTime() - command.getTime());
			}
		}
	}

	public void sortCommand(List<Command> commands) {
		Command command = null;
		long cur = 0;
		for (int i = 0; i < commands.size(); i++) {
			command = commands.get(i);
			cur = command.getTime();
			int j = i - 1;
			while (j >= 0) {
				if (commands.get(j).getTime() > cur) {
					j--;
				} else {
					break;
				}
			}
			if (j < i - 1) {
				commands.remove(i);
				commands.add(j + 1, command);
			}
		}
	}

	public boolean isExcptionRunFixed(Command c1, Command c2, Command c3) {
		return c1.getType().equals("Exception") && c2.getType().equals("Run") && c3.getType().equals("Fixed");
	}

	public boolean isExcptionFixed(Command c1, Command c2) {
		return c1.getType().equals("Exception") && c2.getType().equals("Fixed");
	}

	public boolean isBreakFixing(Command c1, Command c2) {
		return c1.getType().equals("Break") && c2.getType().equals("Fixing");
	}

	protected String convertToHourMinuteSecond(long timeSpent) {
		int hour = (int) (timeSpent / 3600000);
		int minute = (int) (timeSpent % 3600000 / 60000);
		int second = (int) (timeSpent % 60000 / 1000);
		return hour + ":" + (minute < 10 ? "0" + minute : minute) + ":" + (second < 10 ? "0" + second : second);
	}

	List<ReplayerListener> replayerListeners = new ArrayList();

	public void addReplayerListener(ReplayerListener aListener) {
		replayerListeners.add(aListener);
	}

	void notifyNewStudent(String aStudent, List<List<EHICommand>> aNestedCommandList, long aTotalTimeSpent,
			long aWallClockTime, long[] aRestTimes) {
		for (ReplayerListener aListener : replayerListeners) {
			aListener.newStudent(aStudent, aNestedCommandList, aTotalTimeSpent, aWallClockTime, aRestTimes);
		}
	}

	void notifyNewAssignment(String anAssignment, Map<String, List<List<EHICommand>>> anAssignmentData) {
		for (ReplayerListener aListener : replayerListeners) {
			aListener.newAssignment(anAssignment, anAssignmentData);
		}
	}

	void notifyNewSession(int aSessionNumber) {
		for (ReplayerListener aListener : replayerListeners) {
			aListener.newSession(aSessionNumber);
		}
	}

	void notifyNewCommandInSession(int aStartCommandIndex, long aCommandTime, EHICommand aStartCommand,
			String aStartCommandTypeChar, String anEventTypeString, boolean anInSession, String aRestType, String aText,
			int anEndCommandIndex, EHICommand anEndCommand) {
		for (ReplayerListener aListener : replayerListeners) {
//		  
//			 aListener.newCommandInSession(aSession, aCommandIndex, aCommand, aCommandTypeChar, aRestType, aText);
			aListener.newCommandInSession(aStartCommandIndex, aCommandTime, aStartCommand, aStartCommandTypeChar,
					anEventTypeString, anInSession, aRestType, aText, anEndCommandIndex, anEndCommand);
		}
	}
//		protected  List<EHICommand> addCommands(int aSessionIndex, List<EHICommand> commands, long nextStartTime) {
//			for (CommandGenerator aCommandGenerator:commandGenerators) {
//				commands = aCommandGenerator.addCommands(aSessionIndex, commands, nextStartTime);
//			}
//			return commands;
//		}

//	 @Override
//		public void run() {
//			try {
//				if (latch != null) {
//					System.out.println(Thread.currentThread().getName() + " started");
//				}
////				for (String fileName : commandMap.keySet()) {
//				String[] keyset = commandMap.keySet().toArray(new String[0]);
//				for (int j = 0; j < commandMap.size(); j++) {
//					String fileName = keyset[j];
////					List<EHICommand> commands = removePauseCommands(commandMap.get(fileName));
//					List<EHICommand> commands = commandMap.get(fileName);
//					File file = new File(fileName);
//					if (commands.size() < 2) {
//						continue;
//					}
//					long startTimestamp = getLogFileCreationTime(file);
//					if (commands.get(commands.size()-1).getStartTimestamp() == 0) {
//						for (EHICommand command : commands) {
//							command.setStartTimestamp(startTimestamp);
//						}
//					}
//					List<EHICommand> newCommands = null;
//					if (j == commandMap.size()-1) {
//						newCommands = addCommands(j, commands, Long.MAX_VALUE);
//					} else {
//						List<EHICommand> nextCommands = commandMap.get(keyset[j+1]);
//						long nextStartTime = -1;
//						for(int k = 0; k < nextCommands.size(); k++) {
//							if (nextCommands.get(k).getStartTimestamp() > 0 || nextCommands.get(k).getTimestamp() > 0) {
//								nextStartTime = nextCommands.get(k).getStartTimestamp();
//								break;
//							}
//						}
//						newCommands = addCommands(j, commands, nextStartTime);
//					}
//					StringBuffer buf = new StringBuffer();
//					long aStartTime = newCommands.get(0).getTimestamp2();
////					buf.append(Replayer.XML_START1 + getLogFileCreationTime(file) + Replayer.XML_START2 + Replayer.XML_VERSION + Replayer.XML_START3);
//					buf.append(Replayer.XML_START1 + aStartTime + Replayer.XML_START2 + Replayer.XML_VERSION + Replayer.XML_START3);
//
//					int i = 0;
//					while(i < newCommands.size() && (newCommands.get(i) instanceof DifficultyCommand || newCommands.get(i) instanceof PauseCommand)) {
//						i++;
//					}
//					for (; i < newCommands.size();i++) {
//						try {
//							buf.append(newCommands.get(i).persist());
//						}catch (Exception e) {
//							i--;
//						}
//					}
//					buf.append(Replayer.XML_FILE_ENDING);
////					replayer.updateLogMap(fileName, buf.toString());
//					String aNewFileName = CommandGenerator.newFileName(fileName, aStartTime);
//					updateLogMap(aNewFileName, buf.toString());
//
//				}
//			} catch (Exception e) {
//				e.printStackTrace();
//			}finally {
//				if (latch != null) {
//					latch.countDown();
//					System.out.println(Thread.currentThread().getName() + " finished, " + latch.getCount() + " threads remaining");
//				}
//			}
//		} 

}

class Command {
	String type;
	long time;
	long duration;

	public Command(String type, long time) {
		this.type = type;
		this.time = time;
	}

	public String getType() {
		return type;
	}

	public long getTime() {
		return time;
	}

	public long getDuration() {
		return duration;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

	public void setType(String type) {
		this.type = type;
	}

}