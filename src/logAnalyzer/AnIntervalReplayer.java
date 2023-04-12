package logAnalyzer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;

import analyzer.logAnalyzer.ContextAndFixedTimes;
import analyzer.logAnalyzer.InsertsAndDeletes;
import difficultyPrediction.featureExtraction.ARatioBasedFeatureExtractor;
import difficultyPrediction.featureExtraction.ExtractRatiosBasedOnNumberOfEvents;
import difficultyPrediction.featureExtraction.RatioBasedFeatureExtractor;
import difficultyPrediction.predictionManagement.APredictionManager;
import difficultyPrediction.predictionManagement.DecisionTreeModel;
import difficultyPrediction.predictionManagement.PredictionManager;
import fluorite.commands.AssistCommand;
import fluorite.commands.CopyCommand;
import fluorite.commands.Delete;
import fluorite.commands.EHICommand;
import fluorite.commands.EclipseCommand;
import fluorite.commands.FileOpenCommand;
import fluorite.commands.Insert;
import fluorite.commands.InsertStringCommand;
import fluorite.commands.PasteCommand;
import fluorite.commands.PauseCommand;
import fluorite.commands.Replace;
import fluorite.commands.RunCommand;
import fluorite.util.EHLogReader;

public class AnIntervalReplayer {

	// For context based work time, a pause is considered a rest if pauseTime >
	// MULTIPLIER * context-based-threshold
	public static final double DEFAULT_MULTIPLIER = 1;
	// DEFAULT_THRESHOLD in minutes
	// For fixed work time, a pause is considered a rest if pauseTime >
	// DEFAULT_THRESHOLD
	// For context based work time, a threshold = DEFAULT_THRESHOLD if it is
	// undefined for that type of command
	public static final int DEFAULT_THRESHOLD = 5;
	// public static final long START_TIME = 0;
	// public static final long END_TIME = Long.MAX_VALUE;
	// Print traces messages
	public static final boolean DEFAULT_TRACE = false;
	public static final long FIVE_MIN = 5 * 60 * 1000;
	public static final String XML_FILE_ENDING = "\r\n</Events>";
	public static final String[] TYPES = { "Edit", "Debug", "Run", "IO", "Exception", "Request", "Web", "Save",
			"Gained Focus", "Lost Focus", "Terminate", "Difficulty", "Move Caret", "Open File", "Select", "Compile",
			"LocalChecks", "Other" };
	public static final long[] THRESHOLD = { 15109, 22531, 34266, 0, 9641, 0, 493000, 6921, 77564, 24868, 0, 50953,
			102979, 3984, 50202, 51718, 0, 79218 };
	public static final long[] NEXT_THRESHOLD = { 59079, 30031, 13407, 0, 19062, 0, 493000, 10125, 472825, 104170, 0,
			13780, 102797, 65204, 50202, 20110, 0, 58702 };

	private static Map<String, Long> pauseMap;
	private static Map<String, Long> nextPauseMap;
	private EHLogReader reader = new EHLogReader();

	static long defaultPauseTime = FIVE_MIN;
	double multiplier = 1;
	boolean trace = false;
	File currentStudent = new File("default");
	Map<String, List<EHICommand>> currentLogs = new TreeMap<>();
	Pattern pattern = Pattern.compile("(public |private |protected )(.*)( +static)? +(.+)( *)\\(.*\\)");
	Pattern previousPattern = Pattern.compile(".*(public |private |protected )(.*)( +static)? +(.+)( *)\\(.*\\)");
	static RatioBasedFeatureExtractor featureExtractor;
	static PredictionManager predictionManager;

	public AnIntervalReplayer() {
		this(DEFAULT_MULTIPLIER, DEFAULT_THRESHOLD, DEFAULT_TRACE);
	}

	public AnIntervalReplayer(double multiplier, int defaultPauseTime, boolean trace) {
		if (defaultPauseTime > 0) {
			this.defaultPauseTime = defaultPauseTime * 60000L;
		} else {
			this.defaultPauseTime = FIVE_MIN;
		}
		if (multiplier > 0) {
			this.multiplier = multiplier;
		}
		this.trace = trace;
		initPauseMap();
		initPredictionObjects();
	}
	public static void initPredictionObjects() {
		featureExtractor = new ARatioBasedFeatureExtractor(null);
		featureExtractor.setFeatureExtractionStrategy(new ExtractRatiosBasedOnNumberOfEvents());
		
		predictionManager = new APredictionManager(null);
		predictionManager.setPredictionStrategy(new DecisionTreeModel(predictionManager));
	}
	public static void initPauseMap() {
		if (pauseMap == null || nextPauseMap == null) {
			pauseMap = new HashMap<>();
			nextPauseMap = new HashMap<>();
			for (int i = 0; i < TYPES.length; i++) {
				pauseMap.put(TYPES[i], THRESHOLD[i] == 0 ? defaultPauseTime : THRESHOLD[i]);
				nextPauseMap.put(TYPES[i], NEXT_THRESHOLD[i] == 0 ? defaultPauseTime : NEXT_THRESHOLD[i]);
			}
		}
	}

	public long[] getWorkTime(File student, long start, long end) {
		initPauseMap();
		Map<String, List<EHICommand>> commandMap = readProject(student);
		long[] retVal = new long[2];
		if (commandMap == null) {
			if (trace) {
				System.err.println("Error: Cannot read student log");
			}
			retVal[0] = -1;
			retVal[1] = -1;
			return retVal;
		}
		List<List<EHICommand>> nestedCommands = getNestedCommands(commandMap, start, end);
		return getWorkTime(nestedCommands, start, end); // added by pd
		// long totalTime = totalTimeSpent(nestedCommands);
		// long[] restTime = restTime(nestedCommands, defaultPauseTime);
		// retVal[0] = totalTime - restTime[0];
		// retVal[1] = totalTime - restTime[1];
		// return retVal;
	}

	public long[] getWorkTime(List<List<EHICommand>> nestedCommands, long start, long end) {

		long[] retVal = new long[2];

		long totalTime = totalTimeSpent(nestedCommands);
		long[] restTime = restTime(nestedCommands, defaultPauseTime);
		retVal[0] = totalTime - restTime[0];
		retVal[1] = totalTime - restTime[1];
		return retVal;
	}

	public ContextAndFixedTimes contextAndFixedWorkTimesNested(List<List<EHICommand>> nestedCommands, long start,
			long end, int aFixedTimeThreshold) {

		// long[] retVal = new long[2];
		ContextAndFixedTimes retVal = new ContextAndFixedTimes();

		long totalTime = totalTimeSpent(nestedCommands);
		ContextAndFixedTimes restTimes = contextAndFixedRestTimesNested(nestedCommands, aFixedTimeThreshold);
		retVal.contextTime = totalTime - restTimes.contextTime;
		retVal.fixedTime = totalTime - restTimes.fixedTime;

		return retVal;
	}

	public ContextAndFixedTimes contextAndFixedWorkTimesFlat(List<EHICommand> commands, long start, long end,
			int aFixedTimeThreshold) {

		ContextAndFixedTimes retVal = new ContextAndFixedTimes();

		long totalTime = wallTimeSpent(commands);
		ContextAndFixedTimes restTimes = contextAndFixedRestTimesFlat(commands, aFixedTimeThreshold);
		retVal.contextTime = totalTime - restTimes.contextTime;
		retVal.fixedTime = totalTime - restTimes.fixedTime;
		return retVal;
	}

	public ContextAndFixedTimes contextAndFixedWorkTimes(List<EHICommand> commands, int from, int to,
			int aFixedTimeThreshold) {

		ContextAndFixedTimes retVal = new ContextAndFixedTimes();

		long totalTime = wallTimeSpent(commands, from, to);
		ContextAndFixedTimes restTimes = contextAndFixedRestTimes(commands, from, to, aFixedTimeThreshold);
		retVal.contextTime = totalTime - restTimes.contextTime;
		retVal.fixedTime = totalTime - restTimes.fixedTime;
		return retVal;
	}

	public ContextAndFixedTimes contextAndFixedWorkTimes(List<EHICommand> commands, int from, int to) {

		return contextAndFixedRestTimes(commands, from, to, defaultPauseTime);
	}

	public List<List<EHICommand>> getNestedCommands(Map<String, List<EHICommand>> commandMap, long start, long end) {
		List<List<EHICommand>> nestedCommands = new ArrayList<>();
		out: for (String logFile : commandMap.keySet()) {
			List<EHICommand> commands = commandMap.get(logFile);
			int startIndex = 0;
			int lastIndex = commands.size() - 1;
			EHICommand last = commands.get(lastIndex);
			long lastTimestamp = last.getStartTimestamp() + last.getTimestamp();
			if (lastTimestamp < start) {
				continue;
			}
			for (; startIndex < commands.size(); startIndex++) {
				EHICommand command = commands.get(startIndex);
				long timestamp = command.getStartTimestamp() + command.getTimestamp();
				if (end > 0 && timestamp > end) {
					break out;
				} else if (timestamp > start) {
					break;
				}
			}
			for (; lastIndex >= 0; lastIndex--) {
				EHICommand command = commands.get(lastIndex);
				long timestamp = command.getStartTimestamp() + command.getTimestamp();
				if (end == 0 || timestamp < end) {
					break;
				}
			}
			nestedCommands.add(commands.subList(startIndex, lastIndex + 1));
		}
		return nestedCommands;
	}

	public static InsertsAndDeletes insertsAndDeletesFlat(List<EHICommand> commands, long start, long end) {
		InsertsAndDeletes retVal = new InsertsAndDeletes();
		int lastIndex = commands.size() - 1;
		EHICommand last = commands.get(lastIndex);
		long lastTimestamp = last.getStartTimestamp() + last.getTimestamp();
		if (lastTimestamp < start) {
			return retVal;
		}
		for (int i = 0; i < commands.size(); i++) {
			EHICommand command = commands.get(i);
			long timestamp = command.getStartTimestamp() + command.getTimestamp();
			if (end > 0 && timestamp > end) {
				return retVal;
			} else if (timestamp > start) {
				if (command instanceof Insert) {
					retVal.numberOfInserts++;
					retVal.insertLength += command.getDataMap().get("text").length();
				} else if (command instanceof Delete) {
					retVal.numberOfDeletes++;
					retVal.deleteLength += command.getDataMap().get("text").length();
				} else if (isDeletePrevious(command)) {
					retVal.numberOfDeletes++;
					retVal.deleteLength++;
				}
			}
		}
		return retVal;
	}
	
	

	public static InsertsAndDeletes insertsAndDeletesFlat(List<EHICommand> commands, int from, int to) {
		InsertsAndDeletes retVal = new InsertsAndDeletes();
		// int lastIndex = to-1;
		// EHICommand last = commands.get(lastIndex);
		// long lastTimestamp = last.getStartTimestamp() + last.getTimestamp();
		// if (lastTimestamp < start) {
		// return retVal;
		// }
		for (int i = from; i < to; i++) {
			EHICommand command = commands.get(i);
			long timestamp = command.getStartTimestamp() + command.getTimestamp();

			if (command instanceof Insert) {
				retVal.numberOfInserts++;
				retVal.insertLength += command.getDataMap().get("text").length();
			} else if (command instanceof Delete) {
				retVal.numberOfDeletes++;
				retVal.deleteLength += command.getDataMap().get("text").length();
			} else if (isDeletePrevious(command)) {
				retVal.numberOfDeletes++;
				retVal.deleteLength++;
			}
		}

		return retVal;
	}

	public static InsertsAndDeletes insertsAndDeletesFlat(List<EHICommand> commands) {
		return insertsAndDeletesFlat(commands, 0, Integer.MAX_VALUE);
	}

	public int[] getEdits(File student, long start, long end) {
		initPauseMap();
		Map<String, List<EHICommand>> commandMap = readProject(student);
		int[] retVal = new int[4];
		if (commandMap == null) {
			if (trace) {
				System.err.println("Error: Cannot read student log");
			}
			retVal[0] = -1;
			retVal[1] = -1;
			retVal[2] = -1;
			retVal[3] = -1;
			return retVal;
		}
		out: for (String logFile : commandMap.keySet()) {
			List<EHICommand> commands = commandMap.get(logFile);
			int lastIndex = commands.size() - 1;
			EHICommand last = commands.get(lastIndex);
			long lastTimestamp = last.getStartTimestamp() + last.getTimestamp();
			if (lastTimestamp < start) {
				continue;
			}
			for (int i = 0; i < commands.size(); i++) {
				EHICommand command = commands.get(i);
				long timestamp = command.getStartTimestamp() + command.getTimestamp();
				if (end > 0 && timestamp > end) {
					break out;
				} else if (timestamp > start) {
					if (command instanceof Insert) {
						retVal[0]++;
						retVal[1] += command.getDataMap().get("text").length();
					} else if (command instanceof Delete) {
						retVal[2]++;
						retVal[3] += command.getDataMap().get("text").length();
					} else if (isDeletePrevious(command)) {
						retVal[2]++;
						retVal[3]++;
					}
				}
			}
		}
		return retVal;
	}

	public InsertsAndDeletes insertsAndDeletes(File student, long start, long end) {
		initPauseMap();
		Map<String, List<EHICommand>> commandMap = readProject(student);
		InsertsAndDeletes retVal = new InsertsAndDeletes();
		if (commandMap == null) {
			if (trace) {
				System.err.println("Error: Cannot read student log");
			}
			retVal.makeInvalid();
			return retVal;
		}
		for (String logFile : commandMap.keySet()) {
			InsertsAndDeletes fileInsertsAndDeletes = insertsAndDeletes(student, start, end);
			retVal.numberOfInserts += fileInsertsAndDeletes.numberOfInserts;
			retVal.insertLength += fileInsertsAndDeletes.insertLength;
			retVal.numberOfDeletes += fileInsertsAndDeletes.numberOfDeletes;
			retVal.deleteLength += fileInsertsAndDeletes.deleteLength;

		}
		return retVal;
	}

	public int runs(List<EHICommand> commands, int from, int to) {
		// int lastIndex = commands.size()-1;
		// EHICommand last = commands.get(lastIndex);
		int retVal = 0;
		// long lastTimestamp = last.getStartTimestamp() + last.getTimestamp();
		// if (lastTimestamp < start) {
		// return 0;
		// }
		for (int i = from; i < to; i++) {
			EHICommand command = commands.get(i);

			if (command instanceof RunCommand) {
				retVal++;

			}
		}
		return retVal;
	}

	public int getRuns(List<EHICommand> commands, long start, long end) {
		int lastIndex = commands.size() - 1;
		EHICommand last = commands.get(lastIndex);
		int retVal = 0;
		long lastTimestamp = last.getStartTimestamp() + last.getTimestamp();
		if (lastTimestamp < start) {
			return 0;
		}
		for (int i = 0; i < commands.size(); i++) {
			EHICommand command = commands.get(i);
			long timestamp = command.getStartTimestamp() + command.getTimestamp();
			if (end > 0 && timestamp > end) {
				return retVal;
			} else if (timestamp > start) {
				if (command instanceof RunCommand) {
					retVal++;
				}
			}
		}
		return retVal;
	}

	public int getRuns(File student, long start, long end) {
		if (pauseMap == null || nextPauseMap == null) {
			initPauseMap();
		}
		Map<String, List<EHICommand>> commandMap = readProject(student);
		int retVal = 0;
		if (commandMap == null) {
			if (trace) {
				System.err.println("Error: Cannot read student log");
			}
			return -1;
		}
		for (String logFile : commandMap.keySet()) {
			List<EHICommand> commands = commandMap.get(logFile);
			int fileRuns = getRuns(commands, start, end);
			retVal += fileRuns;
		}

		return retVal;
	}
	// public int getRuns(File student, long start, long end) {
	// if (pauseMap == null || nextPauseMap == null) {
	// initPauseMap();
	// }
	// Map<String, List<EHICommand>> commandMap = readProject(student);
	// int retVal = 0;
	// if (commandMap == null) {
	// if (trace) {
	// System.err.println("Error: Cannot read student log");
	// }
	// return -1;
	// }
	// out:
	// for (String logFile : commandMap.keySet()) {
	// List<EHICommand> commands = commandMap.get(logFile);
	// int lastIndex = commands.size()-1;
	// EHICommand last = commands.get(lastIndex);
	// long lastTimestamp = last.getStartTimestamp() + last.getTimestamp();
	// if (lastTimestamp < start) {
	// continue;
	// }
	// for (int i = 0; i < commands.size(); i++) {
	// EHICommand command = commands.get(i);
	// long timestamp = command.getStartTimestamp() + command.getTimestamp();
	// if (end > 0 && timestamp > end) {
	// break out;
	// } else if (timestamp > start) {
	// if (command instanceof RunCommand) {
	// retVal++;
	// }
	// }
	// }
	// }
	// return retVal;
	// }

	public long[] restTime(List<List<EHICommand>> nestedCommands, long time) {
		long[] restTime = new long[2];
		for (List<EHICommand> commands : nestedCommands) {
			for (int i = 0; i < commands.size() - 1; i++) {
				EHICommand command = commands.get(i);
				if (command instanceof PauseCommand) {
					long pause = Long.parseLong(command.getDataMap().get("pause"));
					String prevType = command.getDataMap().get("prevType");
					String nextType = command.getDataMap().get("nextType");
					if (prevType == null && nextType == null) {
						continue;
					}
					if (pause > pauseMap.get(prevType) && pause > nextPauseMap.get(nextType)) {
						restTime[0] += pause;
					}
					if (pause > time) {
						restTime[1] += pause;
					}
				}
			}
		}
		return restTime;
	}

	public ContextAndFixedTimes contextAndFixedRestTimesNested(List<List<EHICommand>> nestedCommands,
			long aFixedTimeThreshold) {
		ContextAndFixedTimes retVal = new ContextAndFixedTimes();
		for (List<EHICommand> commands : nestedCommands) {
			ContextAndFixedTimes fileRestTime = contextAndFixedRestTimesFlat(commands, aFixedTimeThreshold);
			retVal.contextTime += fileRestTime.contextTime;
			retVal.fixedTime += fileRestTime.fixedTime;

		}
		return retVal;
	}

	public ContextAndFixedTimes contextAndFixedRestTimesFlat(List<EHICommand> commands, long aFixedTimeThreshold) {
		return contextAndFixedRestTimes(commands, 0, commands.size(), aFixedTimeThreshold);
	}

	public ContextAndFixedTimes contextAndFixedRestTimesFlat(List<EHICommand> commands) {
		return contextAndFixedRestTimesFlat(commands, defaultPauseTime);
	}

	public ContextAndFixedTimes contextAndFixedRestTimes(List<EHICommand> commands, int from, int to,
			long aFixedTimeThreshold) {
		ContextAndFixedTimes retVal = new ContextAndFixedTimes();
		// long[] restTime = new long[2];
		for (int i = from; i < to - 1; i++) { // why to -1
			EHICommand command = commands.get(i);
			if (command instanceof PauseCommand) {
				long pause = Long.parseLong(command.getDataMap().get("pause"));
				String prevType = command.getDataMap().get("prevType");
				String nextType = command.getDataMap().get("nextType");
				if (prevType == null && nextType == null) {
					continue;
				}
				if (pause > pauseMap.get(prevType) && pause > nextPauseMap.get(nextType)) {
					retVal.contextTime += pause;
				}
				if (pause > aFixedTimeThreshold) {
					retVal.fixedTime += pause;
				}
			}
		}
		// }
		return retVal;
	}

	// public static long totalTimeSpent(List<List<EHICommand>> nestedCommands){
	// long projectTime = 0;
	// for(int k = 0; k < nestedCommands.size(); k++) {
	// List<EHICommand> commands = nestedCommands.get(k);
	// if (commands.size() == 0) {
	// continue;
	// }
	// int j = 0;
	// for(; j < commands.size(); j++) {
	// if (commands.get(j).getStartTimestamp() > 0 ||
	// commands.get(j).getTimestamp() > 0) {
	// break;
	// }
	// }
	// long timestamp1 = commands.get(j).getTimestamp() +
	// commands.get(j).getStartTimestamp();
	// EHICommand command2 = commands.get(commands.size()-1);
	// long timestamp2 = command2.getStartTimestamp() + command2.getTimestamp();
	// projectTime += timestamp2 - timestamp1;
	// }
	// return projectTime;
	// }
	public static long totalTimeSpent(List<List<EHICommand>> nestedCommands) {
		long projectTime = 0;
		for (int k = 0; k < nestedCommands.size(); k++) {
			List<EHICommand> commands = nestedCommands.get(k);
			long aFileTime = wallTimeSpent(commands);
			projectTime += aFileTime;
			// if (commands.size() == 0) {
			// continue;
			// }
			// int j = 0;
			// for(; j < commands.size(); j++) {
			// if (commands.get(j).getStartTimestamp() > 0 ||
			// commands.get(j).getTimestamp() > 0) {
			// break;
			// }
			// }
			// long timestamp1 = commands.get(j).getTimestamp() +
			// commands.get(j).getStartTimestamp();
			// EHICommand command2 = commands.get(commands.size()-1);
			// long timestamp2 = command2.getStartTimestamp() +
			// command2.getTimestamp();
			// projectTime += timestamp2 - timestamp1;
		}
		return projectTime;
	}

	public static long wallTimeSpent(List<EHICommand> commands) {
		long projectTime = 0;

		if (commands.size() == 0) {
			return 0;
		}
		int j = 0;
		for (; j < commands.size(); j++) {
			if (commands.get(j).getStartTimestamp() > 0 || commands.get(j).getTimestamp() > 0) {
				break;
			}
		}
		long timestamp1 = commands.get(j).getTimestamp() + commands.get(j).getStartTimestamp();
		EHICommand command2 = commands.get(commands.size() - 1);
		long timestamp2 = command2.getStartTimestamp() + command2.getTimestamp();
		projectTime += timestamp2 - timestamp1;

		return projectTime;
	}

	public static long wallTimeSpent(List<EHICommand> commands, int from, int to) {
		// long projectTime = 0;

		if (commands.size() == 0 || from >= to || from >= commands.size() || to >= commands.size()) {
			return 0;
		}

		long fromTimeStamp = commands.get(from).getTimestamp() + commands.get(from).getStartTimestamp();
		long toTimeStamp = commands.get(to).getTimestamp() + commands.get(to).getStartTimestamp();
		return toTimeStamp - fromTimeStamp;
	}

	protected File getLogFolder(File student) {
		if (trace) {
			System.out.println("Reading student " + student);
		}
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

	protected File[] getLogFiles(File logFolder) {
		File[] logFiles = logFolder.listFiles(File::isDirectory);

		if (logFiles != null && logFiles.length > 0) {
			logFiles = logFiles[0].listFiles((file) -> {
				if (file.getName().contains("copy.xml")) {
					file.delete();
					return false;
				}
				return file.getName().startsWith("Log") && file.getName().endsWith(".xml");
			});
		} else {
			logFiles = logFolder.listFiles((file) -> {
				if (file.getName().contains("copy.xml")) {
					file.delete();
					return false;
				}
				return file.getName().startsWith("Log") && file.getName().endsWith(".xml");
			});
		}
		if (logFiles == null || logFiles.length == 0) {
			return null;
		}
		Arrays.sort(logFiles);
		File lastLogFile = new File(logFiles[logFiles.length - 1].getPath());
		if (lastLogFile.exists()) {
			try {
				if (!lastLogFile.renameTo(lastLogFile)) {
					// File[] tempLogFiles = new File[logFiles.length-1];
					// for (int i = 0; i < logFiles.length-1; i++) {
					// tempLogFiles[i]= logFiles[i];
					// }
					// logFiles = tempLogFiles;
					File copy = new File(
							lastLogFile.getPath().substring(0, lastLogFile.getPath().length() - 4) + "copy.xml");
					copyFiles(lastLogFile, copy);
					logFiles[logFiles.length - 1] = copy;
				}
				;
			} catch (Exception e) {
				// File[] tempLogFiles = new File[logFiles.length-1];
				// for (int i = 0; i < logFiles.length-1; i++) {
				// tempLogFiles[i]= logFiles[i];
				// }
				// logFiles = tempLogFiles;
				try {
					File copy = new File(
							lastLogFile.getPath().substring(0, lastLogFile.getPath().length() - 4) + "copy.xml");
					copyFiles(lastLogFile, copy);
					logFiles[logFiles.length - 1] = copy;
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}
		refineLogFiles(logFiles);
		return logFiles;
	}

	protected Map<String, List<EHICommand>> readProject(File student) {
		if (student.equals(currentStudent)) {
			return currentLogs;
		}
		File logFolder = getLogFolder(student);
		if (logFolder == null) {
			return null;
		}

		File[] logFiles = getLogFiles(logFolder);
		if (logFiles == null) {
			System.err.println("No logs found for student " + student.getName());
			return null;
		}
		Map<String, List<EHICommand>> logs = new TreeMap<>();
		for (File logFile : logFiles) {
			List<EHICommand> ret = readOneLogFile(logFile);
			if (ret != null) {
				logs.put(logFile.getPath(), ret);
			}
		}
		currentStudent = student;
		currentLogs = logs;
		return logs;
	}

	protected List<EHICommand> readOneLogFile(File log) {
		String path = log.getPath();
		if (trace) {
			System.out.println("Reading file " + path);
		}
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
//			sortCommands(commands, 0, commands.size() - 1);
			commands.sort((a,b)->Long.compare(a.getTimestamp()+a.getStartTimestamp(), b.getTimestamp()+b.getStartTimestamp()));
			if (log.getName().contains("copy")) {
				log.delete();
			}
			return commands;
		} catch (Exception e) {
			System.err.println("Could not read file" + path + "\n" + e);
		}
		return null;
	}

	public void refineLogFiles(File[] logFiles) {
		try {
			for (File file : logFiles) {
				File lckFile = new File(file.getPath() + ".lck");
				if (lckFile.exists()) {
					BufferedReader reader = new BufferedReader(new FileReader(file));
					String lastLine = null;
					String currentLine = null;
					while ((currentLine = reader.readLine()) != null) {
						lastLine = currentLine;
					}
					if (lastLine != null && !lastLine.endsWith("</Events>")) {
						BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
						writer.write(XML_FILE_ENDING);
						writer.close();
					}
					reader.close();
					lckFile.delete();
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}

//	protected void sortCommands(List<EHICommand> commands, int start, int end) {
//		for (int i = 0; i < commands.size(); i++) {
//			if (commands.get(i) == null) {
//				commands.remove(i);
//				i--;
//			}
//		}
//		EHICommand command = null;
//		long cur = 0;
//		for (int i = 0; i < commands.size(); i++) {
//			command = commands.get(i);
//			cur = command.getStartTimestamp() + command.getTimestamp();
//			int j = i - 1;
//			while (j >= 0) {
//				if (commands.get(j).getStartTimestamp() + commands.get(j).getTimestamp() > cur) {
//					j--;
//				} else {
//					break;
//				}
//			}
//			if (j < i - 1) {
//				commands.remove(i);
//				commands.add(j + 1, command);
//			}
//		}
//	}

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

	public long getStartTime(File student) {
		File logFolder = getLogFolder(student);
		if (logFolder == null) {
			return -1;
		}
		File[] logs = getLogFiles(logFolder);
		if (logs == null || logs.length == 0) {
			System.err.println("Error: No logs found for " + student.getPath());
			return -1;
		}
		for (int i = 0; i < logs.length; i++) {
			List<EHICommand> commands = readOneLogFile(logs[i]);
			for (EHICommand command : commands) {
				if (command instanceof FileOpenCommand) {
					return command.getStartTimestamp() + command.getTimestamp();
				}
			}
		}
		System.err.println("Error: No FileOpenCommand found for " + student.getPath());
		return -1;
	}

	public long getEndTime(File student) {
		File logFolder = getLogFolder(student);
		if (logFolder == null) {
			return -1;
		}
		File[] logs = getLogFiles(logFolder);
		if (logs == null || logs.length == 0) {
			System.err.println("Error: No logs found for " + student.getPath());
			return -1;
		}
		for (int i = logs.length - 1; i >= 0; i--) {
			List<EHICommand> commands = readOneLogFile(logs[i]);
			for (int j = commands.size() - 1; j >= 0; j--) {
				EHICommand command = commands.get(j);
				if (command instanceof InsertStringCommand || command instanceof Insert
						|| command instanceof CopyCommand || command instanceof Delete || command instanceof Replace
						|| command instanceof PasteCommand || command instanceof AssistCommand
						|| (command instanceof EclipseCommand && command.getAttributesMap()
								.get(EclipseCommand.XML_ID_ATTR).toLowerCase().contains("delete"))) {
					return command.getStartTimestamp() + command.getTimestamp();
				}
			}
		}
		System.err.println("Error: No FileOpenCommand found for " + student.getPath());
		return -1;
	}

	public Map<String, List<String>> getDistanceAndProcedures(File student, long start, long end) {
		initPauseMap();
		Map<String, List<EHICommand>> commandMap = readProject(student);
		if (commandMap == null) {
			if (trace) {
				System.err.println("Error: Cannot read student log");
			}
			return new HashMap<>();
		}
		String[] currentFile = new String[2];
		currentFile[0] = "";
		currentFile[1] = "";
		List<List<EHICommand>> nestedCommands = new ArrayList<>();
		out: for (String logFile : commandMap.keySet()) {
			List<EHICommand> commands = commandMap.get(logFile);
			int startIndex = 0;
			int lastIndex = commands.size() - 1;
			EHICommand last = commands.get(lastIndex);
			long lastTimestamp = last.getStartTimestamp() + last.getTimestamp();
			if (lastTimestamp < start) {
				continue;
			}
			if (commands.get(1).getTimestamp() + commands.get(1).getStartTimestamp() > end) {
				break;
			}
			for (; startIndex < commands.size(); startIndex++) {
				EHICommand command = commands.get(startIndex);
				long timestamp = command.getStartTimestamp() + command.getTimestamp();
				if (command instanceof FileOpenCommand) {
					String path = command.getDataMap().get("filePath");
					if (path != null) {
						currentFile[0] = path.substring(path.lastIndexOf("src") + 4);
						String snapshot = command.getDataMap().get("snapshot");
						if (snapshot != null) {
							currentFile[1] = snapshot;
						} else {
							currentFile[1] = "";
						}
					}
				}
				if (end > 0 && timestamp > end) {
					break out;
				} else if (timestamp > start) {
					break;
				}
			}
			for (; lastIndex >= 0; lastIndex--) {
				EHICommand command = commands.get(lastIndex);
				long timestamp = command.getStartTimestamp() + command.getTimestamp();
				if (end == 0 || timestamp < end) {
					break;
				}
			}
			nestedCommands.add(commands.subList(startIndex, lastIndex + 1));
		}
		Map<String, List<String>> retVal = new HashMap<>();
		int[] offsets = { -1, -1 };
		for (List<EHICommand> list : nestedCommands) {
			for (EHICommand command : list) {
				if (command instanceof Insert || command instanceof Delete) {
					int offset = Integer.parseInt(command.getAttributesMap().get("offset"));
					if (offsets[0] == -1 || offset < offsets[0]) {
						offsets[0] = offset;
					}
					if (offsets[1] == -1 || offset > offsets[1]) {
						offsets[1] = offset;
					}
				}
				if (command instanceof FileOpenCommand) {
					if (!currentFile[1].equals("") && offsets[0] != -1) {
						if (retVal.containsKey(currentFile[0])) {
							List<String> procedureList = retVal.get(currentFile[0]);
							if (offsets[0] < Integer.parseInt(procedureList.get(0))) {
								procedureList.set(0, offsets[0] + "");
							}
							if (offsets[1] > Integer.parseInt(procedureList.get(1))) {
								procedureList.set(1, offsets[1] + "");
							}
						} else {
							List<String> aList = new ArrayList<>();
							aList.add(offsets[0] + "");
							aList.add(offsets[1] + "");
							aList.add(currentFile[1]);
							retVal.put(currentFile[0], aList);
						}
					}
					offsets[0] = -1;
					offsets[1] = -1;
					String path = command.getDataMap().get("filePath");
					if (path != null) {
						currentFile[0] = path.substring(path.lastIndexOf("src") + 4);
						String snapshot = command.getDataMap().get("snapshot");
						if (snapshot != null) {
							currentFile[1] = snapshot;
						} else {
							currentFile[1] = "";
						}
					}
				}
			}
		}
		if (!currentFile[1].equals("") && offsets[0] != -1) {
			if (retVal.containsKey(currentFile[0])) {
				List<String> procedureList = retVal.get(currentFile[0]);
				if (offsets[0] < Integer.parseInt(procedureList.get(0))) {
					procedureList.set(0, offsets[0] + "");
				}
				if (offsets[1] > Integer.parseInt(procedureList.get(1))) {
					procedureList.set(1, offsets[1] + "");
				}
			} else {
				List<String> aList = new ArrayList<>();
				aList.add(offsets[0] + "");
				aList.add(offsets[1] + "");
				aList.add(currentFile[1]);
				retVal.put(currentFile[0], aList);
			}
		}
		for (String file : retVal.keySet()) {
			List<String> list = retVal.get(file);
			int startOffset = Integer.parseInt(list.get(0));
			int endOffset = Integer.parseInt(list.get(1));
			list.set(0, endOffset - startOffset + "");
			list.remove(1);
			String content = list.get(1);
			list.remove(1);
			if (startOffset > content.length()) {
				continue;
			}
			Matcher matcher = pattern.matcher(
					content.substring(startOffset, endOffset > content.length() ? content.length() : endOffset));
			String procedure;
			while (matcher.find()) {
				procedure = matcher.group(4);
				if (!list.contains(procedure)) {
					list.add(procedure);
				}
			}
			for (int i = startOffset - 100 > 0 ? startOffset - 100 : 0; i >= 0; i = i - 100 > 0 ? i = i - 100 : 0) {
				Matcher aMatcher = pattern.matcher(content.substring(i, startOffset));
				if (aMatcher.find()) {
					list.add(aMatcher.group(4));
					break;
				}
				if (i == 0) {
					break;
				}
			}
		}
		return retVal;
	}

	public static boolean isDeletePrevious(EHICommand command) {
		return command instanceof EclipseCommand && (((EclipseCommand) command).getCommandID()
				.equals("eventLogger.styledTextCommand.DELETE_PREVIOUS")
				|| ((EclipseCommand) command).getCommandID().equals("org.eclipse.ui.edit.text.deletePreviousWord"));
	}

	public void copyFiles(File source, File dest) throws IOException {
		InputStream is = null;
		OutputStream os = null;
		try {
			if (!dest.exists()) {
				dest.getParentFile().mkdirs();
				dest.createNewFile();
			} else {
				dest.delete();
				dest.createNewFile();
			}
			is = new FileInputStream(source);
			os = new FileOutputStream(dest);
			byte[] buffer = new byte[1024];
			int length;
			while ((length = is.read(buffer)) > 0) {
				os.write(buffer, 0, length);
			}
			buffer = "</Events>".getBytes();
			os.write(buffer, 0, buffer.length);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (is != null) {
				is.close();
			}
			if (os != null) {
				os.close();
			}
		}
	}
}
