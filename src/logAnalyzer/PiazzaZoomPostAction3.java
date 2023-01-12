package logAnalyzer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.json.JSONObject;

import java.util.Map.Entry;

import analyzer.extension.replayView.FileUtility;
import au.com.bytecode.opencsv.CSVWriter;
import fluorite.commands.EHICommand;
import fluorite.commands.PauseCommand;
import fluorite.commands.PiazzaPostCommand;
import fluorite.commands.ZoomChatCommand;
import fluorite.commands.ZoomSessionStartCommand;
import fluorite.util.EHLogReader;

public class PiazzaZoomPostAction3 {
	public static String[] assigns = {
			"H:\\CompPaper\\301ss21\\Assignment 1",
			"H:\\CompPaper\\301ss21\\Assignment 2",
			"H:\\CompPaper\\301ss21\\Assignment 3",
			"H:\\CompPaper\\301ss21\\Assignment 4",
			};
	public static String outputFile = "H:\\CompPaper\\301ss21\\PiazzaZoomPostAction3.csv";
//	public static String[] assigns = {
//			"F:\\CompPaper\\524f21\\Fall2021\\Assignment 1",
//			"F:\\CompPaper\\524f21\\Fall2021\\Assignment 2",
//			"F:\\CompPaper\\524f21\\Fall2021\\Assignment 3",
//			"F:\\CompPaper\\524f21\\Fall2021\\Assignment 4",
//			"F:\\CompPaper\\524f21\\Fall2021\\Assignment 5",
//			"F:\\CompPaper\\524f21\\Fall2021\\Assignment 6",
//			};
//	public static String outputFile = "F:\\CompPaper\\524f21\\PiazzaZoomPostAction.csv";
	EHLogReader reader = new EHLogReader();
	//[0:numBreakAfterPiazzaQuestion], [1:numNoBreakAfterPiazzaQuestion], 
	//[2:numBreakAfterOHRequest], [3:numNoBreakAfterOHRequest],
	//[4:numBreakAfterOHSession], [5:numNoBreakAfterOHSession],
	Map<String, List<Integer>> map = new HashMap<>();
	final long TEN_MIN = 600000L; 
	
	public static void main(String[] args) {
		PiazzaZoomPostAction3 obj = new PiazzaZoomPostAction3();
		obj.categorizePiazzaPosts();
		obj.write();
	}
	
	
	public void categorizePiazzaPosts() {
		for (String assign : assigns) {
			for (Entry<String, List<List<EHICommand>>> entry: readAssignment(new File(assign)).entrySet()) {
				String student = entry.getKey();
				student = student.substring(student.lastIndexOf("(")+1, student.lastIndexOf(")"));
				List<Integer> list = getListFromMap(map, student);
				List<List<EHICommand>> nestedCommands = entry.getValue();
//				boolean hasPiazzaPost = false;
//				boolean hasOfficeHour = false;
				EHICommand piazzaPostCommand = null;
				int pizzaPostI = -1;
				int piazzaPostJ = -1;
				EHICommand zoomChatCommand = null;
				int zoomChatI = -1;
				int zoomChatJ = -1;
				for (int i = 0; i < nestedCommands.size() && (piazzaPostCommand == null || zoomChatCommand == null); i++) {
					List<EHICommand> commands = nestedCommands.get(i);
					for (int j = 1; j < commands.size(); j++) {
						EHICommand command = commands.get(j);
						if (piazzaPostCommand == null && command instanceof PiazzaPostCommand) {
							JSONObject post = new JSONObject(command.getDataMap().get("piazza_post"));
							if (post.getString("root_subject").equals("Protocol for Synchronous Interaction") || post.getBoolean("is_office_hour_request") || post.getString("type").equals("question")) {
								piazzaPostCommand = command;
								pizzaPostI = i;
								piazzaPostJ = j;
							} 
						} 
						if (zoomChatCommand == null && command instanceof ZoomChatCommand) {
							zoomChatCommand = command;
							zoomChatI = i;
							zoomChatJ = j;
						}
					}
				}
				long workTime = -1;
//				list.add((int)(workTime/1000));
				if (piazzaPostCommand == null) {
					list.add(100);
				} else {
					workTime = totalTimeSpent(nestedCommands);
					List<List<EHICommand>> nestedCommands2 = new ArrayList<>();
					for (int i = 0; i < pizzaPostI; i++) {
						nestedCommands2.add(nestedCommands.get(i));
					}
					nestedCommands2.add(nestedCommands.get(pizzaPostI).subList(0, piazzaPostJ+1));
					long postTime = totalTimeSpent(nestedCommands2);
					list.add((int)(1.0 * postTime/workTime*100));
//					list.add((int)(1.0 * postTime/1000));
				}
				if (zoomChatCommand == null) {
					list.add(100);
				} else {
					if (workTime == -1) {
						workTime = totalTimeSpent(nestedCommands);
					}
					List<List<EHICommand>> nestedCommands2 = new ArrayList<>();
					for (int i = 0; i < zoomChatI; i++) {
						nestedCommands2.add(nestedCommands.get(i));
					}
					nestedCommands2.add(nestedCommands.get(zoomChatI).subList(0, zoomChatJ+1));
					long zoomTime = totalTimeSpent(nestedCommands2);
					list.add((int)(1.0 * zoomTime/workTime*100));
//					list.add((int)(zoomTime/1000));
				}
			}
		}
	}
	
	protected long totalTimeSpent(List<List<EHICommand>> nestedCommands){
		long projectTime = 0;
		try {
			for(int k = 0; k < nestedCommands.size(); k++) {
				List<EHICommand> commands = nestedCommands.get(k);
				if (commands.size() == 0) {
					continue;
				}
				int j = 0;
				for(; j < commands.size(); j++) {
					if (commands.get(j).getStartTimestamp() > 0 || commands.get(j).getTimestamp() > 0) {
						break;
					}
				}
				long timestamp1 = commands.get(j).getTimestamp() + commands.get(j).getStartTimestamp();
				EHICommand command2 = commands.get(commands.size()-1);
				long timestamp2 = command2.getStartTimestamp() + command2.getTimestamp();
				projectTime += timestamp2 - timestamp1;
			}
		} catch (Exception e) {
			// TODO: handle exception
			return 0;
		}
		
		return projectTime;
	}

	public void write() {
//		String[] headers = {"student", "numBreakAfterPiazzaQuestion", "numNoBreakAfterPiazzaQuestion", 
//									   "numBreakAfterOHRequest", "numNoBreakAfterOHRequest", 
//									   };
		List<String> headers = new ArrayList<>();
		String[] stringArray = {};
		headers.add("student");
		for (String path : assigns) {
			String assign = path.substring(path.lastIndexOf("\\")+1);
//			headers.add(assign + " Work Time");
			headers.add(assign + " First Piazza Post");
//			headers.add(assign + " Post Time");
			headers.add(assign + " First OH");
//			headers.add(assign + " Zoom Time");
		}
		File output = new File(outputFile);
		if (output.exists()) {
			output.delete();
		}
		List<String> nextLine = new ArrayList<>();
		try (CSVWriter cw = new CSVWriter(new FileWriter(output))) {
			cw.writeNext(headers.toArray(stringArray));

			for (Entry<String, List<Integer>> entry: map.entrySet()) {
				nextLine.clear();
				nextLine.add(entry.getKey());
				for (int i : entry.getValue()) {
					nextLine.add(i+"%");
				}
				cw.writeNext(nextLine.toArray(stringArray));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public List<Integer> getListFromMap(Map<String, List<Integer>> map, String onyen) {
		if (map.containsKey(onyen)) {
			return map.get(onyen);
		}
		List<Integer> list = new ArrayList<>();
		map.put(onyen, list);
		return list;
	}
	
	public Map<String, List<List<EHICommand>>> readAssignment(File assign) {
		System.out.println("Reading assignment " + assign);
		if (!assign.exists()) {
			System.out.println("Folder " + assign + " does not exist");
			return null;
		}
		Map<String, List<List<EHICommand>>> logs = new TreeMap<>();
		for (File student : assign.listFiles((parent, fileName)->{
			return fileName.contains(",") && fileName.contains("(");
		})) {
			List<List<EHICommand>> ret = readStudent(student);
			if (ret != null && !ret.isEmpty()) {
				logs.put(student.getPath(), ret);
			} 
		}
		return logs;
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
	
	public List<List<EHICommand>> readStudent(File student) {
//		System.out.println("Reading student " + student);
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
	
	public List<EHICommand> readOneLogFile(File log) {
		List<EHICommand> retVal = readOneLogFileWthoutAppending(log, false);
		return retVal;
	}
	
	public List<EHICommand> readOneLogFileWthoutAppending(File log, boolean printError) {
		String path = log.getPath();
		if (path.contains("Log2021-06-20-20-25-46-388.xml")) {
			System.out.println("Pausing");
		}
//		System.out.println("Reading file " + path);
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
}