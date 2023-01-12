package logAnalyzer;

import static org.junit.Assert.fail;

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
import fluorite.commands.ZoomSessionEndCommand;
import fluorite.util.EHLogReader;

public class PiazzaZoomPostAction2 {
	public static String[] assigns = {
			"H:\\CompPaper\\301ss21\\Assignment 1",
			"H:\\CompPaper\\301ss21\\Assignment 2",
			"H:\\CompPaper\\301ss21\\Assignment 3",
			"H:\\CompPaper\\301ss21\\Assignment 4",
			};
	public static String outputFile = "H:\\CompPaper\\301ss21\\PiazzaZoomPostAction2.csv";
//	public static String[] assigns = {
//			"H:\\CompPaper\\524f21\\Fall2021\\Assignment 1",
//			"H:\\CompPaper\\524f21\\Fall2021\\Assignment 2",
//			"H:\\CompPaper\\524f21\\Fall2021\\Assignment 3",
//			"H:\\CompPaper\\524f21\\Fall2021\\Assignment 4",
//			"H:\\CompPaper\\524f21\\Fall2021\\Assignment 5",
//			"H:\\CompPaper\\524f21\\Fall2021\\Assignment 6",
//			};
//	public static String outputFile = "H:\\CompPaper\\524f21\\PiazzaZoomPostAction.csv";
	EHLogReader reader = new EHLogReader();
	//[0:numBreakAfterPiazzaQuestion], [1:numNoBreakAfterPiazzaQuestion], 
	//[2:numBreakAfterOHRequest], [3:numNoBreakAfterOHRequest],
	//[4:numBreakAfterOHSession], [5:numNoBreakAfterOHSession],
	Map<String, List<String[]>> map = new HashMap<>();
//	Map<String, List<String[]>> zoomMap = new HashMap<>();
	final long TEN_MIN = 600000L; 
	
	public static void main(String[] args) {
		PiazzaZoomPostAction2 obj = new PiazzaZoomPostAction2();
		obj.categorizePiazzaPosts();
		obj.write();
		System.out.println("Done");
	}
	
	
	public void categorizePiazzaPosts() {
		for (String assign : assigns) {
			for (Entry<String, List<List<EHICommand>>> entry: readAssignment(new File(assign)).entrySet()) {
				String student = entry.getKey();
				student = student.substring(student.lastIndexOf("(")+1, student.lastIndexOf(")"));
				List<String[]> list = getListFromMap(map, student);
				List<List<EHICommand>> nestedCommands = entry.getValue();
				for (int i = 0; i < nestedCommands.size(); i++) {
//					EHICommand lastZoomChatCommand = null;
//					int lastZoomChatCommandIdx = 0;
					List<EHICommand> commands = nestedCommands.get(i);
					for (int j = 1; j < commands.size(); j++) {
						EHICommand command = commands.get(j);
						if (command instanceof PiazzaPostCommand) {
							JSONObject post = new JSONObject(command.getDataMap().get("piazza_post"));
							String content = post.getString("content").replaceAll("\\R", "\t");
							String folders = post.getJSONArray("folders").toString();
							String cause = "";
							String contentLowercase = content.toLowerCase();
							if (contentLowercase.contains("localcheck") || contentLowercase.contains("check ") || contentLowercase.contains("checks ")) {
								cause = "LocalCheck";
							} else if (contentLowercase.contains("checkstyle")) {
								cause = "Checkstyle";
							} else if (contentLowercase.contains("quiz")) {
								cause = "Quiz";
							} else if (folders.contains("logistics")) {
								cause = "Logistics";
							} else if (folders.contains("grading")) {
								cause = "Grading";
							} else {
								cause = "Programming Question";
							}
							
							if (post.getString("root_subject").equals("Protocol for Synchronous Interaction") 
							 || post.getBoolean("is_office_hour_request") || post.getString("type").equals("question")) {
								if (j + 1 < commands.size()) {
									EHICommand nextCommand = commands.get(j+1);
									if (nextCommand instanceof PauseCommand) {
										nextCommand = commands.get(j+2);
									}
									long breakTime = nextCommand.getTimestamp() - command.getTimestamp();
									
									String[] line = {student, "Piazza", cause, (breakTime/1000)+"", 
											nextCommand.getName(), folders, content};
									list.add(line);
								} else {
									String[] line = {student, "Piazza", cause, "-1", "End of session", 
											folders, content};
									list.add(line);
									break;
								}
							} 
						}
//						if (command instanceof ZoomSessionEndCommand) {
//							if (j + 1 < commands.size()) {
//								EHICommand nextCommand = commands.get(j+1);
//								if (nextCommand instanceof PauseCommand && j + 2 < commands.size()) {
//									nextCommand = commands.get(j+2);
//								}
//								long breakTime = nextCommand.getTimestamp() - command.getTimestamp();
//								
//								String[] line = {student, "Zoom", (breakTime/1000)+"", nextCommand.getName()};
//								list.add(line);
//							} else {
////								list.get(2).increment();
//								String[] line = {student, "Zoom", "-1", "End of session"};
//								list.add(line);
//								break;
//							}
////						} else {
////							lastZoomChatCommand = null;
//						}
					}
					
				}
			}
		}
	}
	//[0:numBreakAfterPiazzaQuestion], [1:numNoBreakAfterPiazzaQuestion], [2:numBreakAfterOHRequest], [3:numNoBreakAfterOHRequest]

	public void write() {
		String[] headers = {"student", "type", "cause", "breaktime", "next command", "folders"
//									   "numBreakAfterOHRequest", "numNoBreakAfterOHRequest", 
//									   "numBreakAfterOHSession", "numNoBreakAfterOHSession"
									   };
		File output = new File(outputFile);
		if (output.exists()) {
			output.delete();
		}
//		List<String> nextLine = new ArrayList<>();
		try (CSVWriter cw = new CSVWriter(new FileWriter(output))) {
			cw.writeNext(headers);

			for (Entry<String, List<String[]>> entry: map.entrySet()) {
//				nextLine.clear();
//				nextLine.add(entry.getKey());
				for (String[] line : entry.getValue()) {
//					for (int i = 0; i < line.length; i++) {
//						line[i] = quote(line[i]);
//					}
					cw.writeNext(line);
				}
				
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public List<String[]> getListFromMap(Map<String, List<String[]>> map, String onyen) {
		if (map.containsKey(onyen)) {
			return map.get(onyen);
		}
		List<String[]> list = new ArrayList<>();
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
//	
//	public String quote(String s) {
//		return "\"" + s + "\"";
//	}
}
