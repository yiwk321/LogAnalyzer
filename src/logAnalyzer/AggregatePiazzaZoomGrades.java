package logAnalyzer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONObject;
import analyzer.extension.replayView.FileUtility;
import au.com.bytecode.opencsv.CSVWriter;
import fluorite.commands.BalloonCommand;
import fluorite.commands.EHICommand;
import fluorite.commands.PauseCommand;
import fluorite.commands.PiazzaPostCommand;
import fluorite.commands.ZoomChatCommand;
import fluorite.util.EHLogReader;

public class AggregatePiazzaZoomGrades {
	Map<String, List<Float>> gradesMap = new HashMap<>();
	Map<String, List<Integer>> piazzaPostMap = new HashMap<>();
	Map<String, List<Integer>> zoomChatMap = new HashMap<>();
	List<JSONObject> piazzaPosts;
	Map<String, Integer> zoomSessionMap = new HashMap<>();
	Map<String, Integer> zoomTimeMap = new HashMap<>();
	Map<String, List<String[]>> breakMap = new HashMap<>();
	Map<String, List<Integer>> progressMap = new HashMap<>();
	Map<String, List<Long>> workTimeMap = new HashMap<>();

	EHLogReader reader = new EHLogReader();
	
	public static void main(String[] args) {
		for (String arg : args) {
			AggregatePiazzaZoomGrades aggregatePiazzaZoomGrades = new AggregatePiazzaZoomGrades();
			File course = new File(arg);
			aggregatePiazzaZoomGrades.readHelp(course);
			aggregatePiazzaZoomGrades.readLogs(course);
			aggregatePiazzaZoomGrades.write(course);
//			aggregatePiazzaZoomGrades.writeBreaks(course);
//			aggregatePiazzaZoomGrades.writeProgress(course);
		}
		System.out.println("Done");
	}
	
	public File getA1(File course) {
		File a1 = new File(course, "Assignment 1");
		if (a1.exists()) {
			return a1;
		} 
		return null;
	}
	
	public File getPiazza(File course) {
		File[] files = course.listFiles((file) -> {
			return file.getName().endsWith(".json");
		});
		if (files.length >= 1) {
			return files[0];
		}
		return null;
	}
	
	public File getGrades(File course) {
		File grades = new File(course, "gradebook_exportAnon.csv");
		if (grades.exists()) {
			return grades;
		} 
		return null;
	}
	
	public File getZoomTimes(File course) {
		File zoomTime = new File(course, "ZoomSessionTimesSeconds.txt");
		if (zoomTime.exists()) {
			return zoomTime;
		} 
		return null;
	}
	
	public File getOutput(File course) {
		File output = new File(course.getParent(), "\\stats\\" + course.getName() + " Stats.csv");
		if (output.exists()) {
			output.delete();
		}
		return output;
	}
	
	public File getBreakOutput(File course) {
		File output = new File(course.getParent(), "\\stats\\" + course.getName() + " Breaks.csv");
		if (output.exists()) {
			output.delete();
		}
		return output;
	}
	
	public void readLogs(File course) {
		File[] assigns = getAssigns(course);
		for (File assign : assigns) {
			for (Entry<String, List<List<EHICommand>>> entry: readAssignment(assign).entrySet()) {
				getWorkTimes(entry);
				readBreaks(entry);
				readProgress(entry);
			}
		}
	}
	
	public void getWorkTimes(Entry<String, List<List<EHICommand>>> entry) {
		String student = entry.getKey();
		student = student.substring(student.lastIndexOf("(")+1, student.lastIndexOf(")"));
		List<Long> list = getListFromMap(workTimeMap, student);
		List<List<EHICommand>> nestedCommands = entry.getValue();
		long totalTime = totalTimeSpent(nestedCommands);
		long restTime = restTime(nestedCommands, 5 * 60 * 1000L, Long.MAX_VALUE)[0];
		long worktime = totalTimeSpent(nestedCommands) - restTime(nestedCommands, 5 * 60 * 1000L, Long.MAX_VALUE)[0];
		if (worktime < 0) {
			System.out.println(worktime);
		}
		list.add(worktime);
	}
	
	public long getWorkTime(List<List<EHICommand>> nestedCommands) {
		return totalTimeSpent(nestedCommands) - restTime(nestedCommands, 5 * 60 * 1000L, Long.MAX_VALUE)[0];
	}
	
	protected long[] restTime(List<List<EHICommand>> nestedCommands, long time, long time2) {
		long[] restTime = {0,0,0};
		for (int i = 0; i < nestedCommands.size(); i++) {
			List<EHICommand> commands = nestedCommands.get(i);
			EHICommand last = null;
			EHICommand cur = null;
			int k = 0;
			for(; k < commands.size(); k++) {
				if (commands.get(k).getStartTimestamp() > 0 && commands.get(k).getTimestamp() > 1) {
					break;
				}
			}
			for(; k < commands.size(); k++) {
				if (cur != null) {
					last = cur;
				}
				cur = commands.get(k);
				if (last != null && last.getTimestamp() > 0) {
					long diff = cur.getStartTimestamp() + cur.getTimestamp() - last.getTimestamp() - last.getStartTimestamp();
//					if (diff > 1402740000) {
//						System.out.println(diff);
//					}
					if (diff > time) {
						restTime[0] += diff;
						if (diff < time2) {
							restTime[1]++;
							restTime[2] += diff;
						}
					}
				}
			}
		}
		return restTime;
	}
	
	public void readBreaks(Entry<String, List<List<EHICommand>>> entry) {
		String student = entry.getKey();
		student = student.substring(student.lastIndexOf("(")+1, student.lastIndexOf(")"));
		List<String[]> list = getListFromMap(breakMap, student);
		List<List<EHICommand>> nestedCommands = entry.getValue();
		for (int i = 0; i < nestedCommands.size(); i++) {
//			EHICommand lastZoomChatCommand = null;
//			int lastZoomChatCommandIdx = 0;
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
						int k = j+1;
						if (k < commands.size()) {
							EHICommand nextCommand = commands.get(k);
							while ((nextCommand instanceof PauseCommand || nextCommand instanceof PiazzaPostCommand || nextCommand instanceof BalloonCommand) && k+1 < commands.size()) {
								k++;
								nextCommand = commands.get(k);
							}
							if (k+1 < commands.size()) {
								long breakTime = nextCommand.getTimestamp() - command.getTimestamp();
								
								String[] line = {student, "Piazza", cause, (breakTime/1000)+"", 
										nextCommand.getName(), folders, 
//										content
										};
								list.add(line);
							} else {
								break;
							}
						} else {
							String[] line = {student, "Piazza", cause, "-1", "End of session", 
									folders, 
//									content
									};
							list.add(line);
							break;
						}
					} 
				}
//				if (command instanceof ZoomSessionEndCommand) {
//					if (j + 1 < commands.size()) {
//						EHICommand nextCommand = commands.get(j+1);
//						if (nextCommand instanceof PauseCommand && j + 2 < commands.size()) {
//							nextCommand = commands.get(j+2);
//						}
//						long breakTime = nextCommand.getTimestamp() - command.getTimestamp();
//						
//						String[] line = {student, "Zoom", (breakTime/1000)+"", nextCommand.getName()};
//						list.add(line);
//					} else {
////						list.get(2).increment();
//						String[] line = {student, "Zoom", "-1", "End of session"};
//						list.add(line);
//						break;
//					}
////				} else {
////					lastZoomChatCommand = null;
//				}
			}
			
		}
	}
	
	public void readProgress(Entry<String, List<List<EHICommand>>> entry) {
		String student = entry.getKey();
		student = student.substring(student.lastIndexOf("(")+1, student.lastIndexOf(")"));
		List<Integer> list = getListFromMap(progressMap, student);
		List<List<EHICommand>> nestedCommands = entry.getValue();
//		boolean hasPiazzaPost = false;
//		boolean hasOfficeHour = false;
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
//		list.add((int)(workTime/1000));
		if (piazzaPostCommand == null) {
			list.add(100);
		} else {
			workTime = getWorkTime(nestedCommands);
			List<List<EHICommand>> nestedCommands2 = new ArrayList<>();
			for (int i = 0; i < pizzaPostI; i++) {
				nestedCommands2.add(nestedCommands.get(i));
			}
			nestedCommands2.add(nestedCommands.get(pizzaPostI).subList(0, piazzaPostJ+1));
			long postTime = getWorkTime(nestedCommands2);
			list.add((int)(1.0 * postTime/workTime*100));
//			list.add((int)(1.0 * postTime/1000));
		}
		if (zoomChatCommand == null) {
			list.add(100);
		} else {
			if (workTime == -1) {
				workTime = getWorkTime(nestedCommands);
			}
			List<List<EHICommand>> nestedCommands2 = new ArrayList<>();
			for (int i = 0; i < zoomChatI; i++) {
				nestedCommands2.add(nestedCommands.get(i));
			}
			nestedCommands2.add(nestedCommands.get(zoomChatI).subList(0, zoomChatJ+1));
			long zoomTime = getWorkTime(nestedCommands2);
			list.add((int)(1.0 * zoomTime/workTime*100));
//			list.add((int)(zoomTime/1000));
		}
	}
	
	public void writeBreaks(File course) {
		String[] headers = {"student", "type", "cause", "breaktime", "next command", "folders"
//									   "numBreakAfterOHRequest", "numNoBreakAfterOHRequest", 
//									   "numBreakAfterOHSession", "numNoBreakAfterOHSession"
									   };
		File output = getBreakOutput(course);
		if (output.exists()) {
			output.delete();
		}
//		List<String> nextLine = new ArrayList<>();
		try (CSVWriter cw = new CSVWriter(new FileWriter(output))) {
			cw.writeNext(headers);

			for (Entry<String, List<String[]>> entry: breakMap.entrySet()) {
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
	
	public File[] getAssigns(File course) {
		return course.listFiles((file)->{
			return file.isDirectory() && file.getName().startsWith("Assignment ");
		});
//		String[] folders = new String[assigns.length];
//		for (int i = 0; i < assigns.length; i++) {
//			folders[i] = assigns[i].getPath(); 
//		}
	}

	public void readHelp(File course) {
		JSONObject piazzaPostsJson = FileUtility.readJSON(getPiazza(course));
		File[] studentFolders = getA1(course).listFiles((file)-> {
			return file.isDirectory() && !file.getName().equals("ZoomChatsAnon");
		});
		for (File studentFolder: studentFolders) {
			String folderName = studentFolder.getName();
			String onyen = folderName.substring(folderName.indexOf("(")+1, folderName.length()-1);
			
			List<Integer> list = getListFromMap(zoomChatMap, onyen);
			File[] zoomSession = studentFolder.listFiles((file)-> {
				return !file.isDirectory() && file.getName().contains("ZoomSession");
			});
			File zoom = null;
			if (zoomSession.length == 1) {
				zoom = zoomSession[0];
				zoomSessionMap.put(onyen, Integer.parseInt(zoom.getName().substring(0, zoom.getName().indexOf("Zoom"))));
			} else {
				zoomSessionMap.put(onyen, 0);
			}
			
			int numOH = 0;
			int numDiary = 0;
			int numQuestion = 0;
			int numPosts = 0;
			for (JSONObject post : findPiazzaPosts(onyen, piazzaPostsJson)) {
				numPosts++;
				if (post.getString("root_subject").equals("Protocol for Synchronous Interaction") || post.getBoolean("is_office_hour_request")) {
					numOH++;
				} 
				if (post.getBoolean("root_is_diary")) {
					numDiary++;
				}
				if (post.getString("type").equals("question")) {
					numQuestion++;
				}
			}
			list = getListFromMap(piazzaPostMap, onyen);
			list.add(numPosts-numOH-numDiary);
			list.add(numDiary);
			list.add(numQuestion);
		}
		File zoomtime = getZoomTimes(course);
		if (zoomtime == null) {
			return;
		}
		String[] zoomSessionTimesString = FileUtility.readFile(zoomtime).toString().split("\\R");
		String onyen = null;
		for (int i = 0; i < zoomSessionTimesString.length; i++) {
			String s = zoomSessionTimesString[i];
			if (s.length() > 15) {
				onyen = s.substring(s.lastIndexOf("(")+1, s.lastIndexOf(")"));
			} else if (s.startsWith("Total: ")) {
				zoomTimeMap.put(onyen, Integer.parseInt(s.substring(7)));
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
					if (commands.get(j).getStartTimestamp() > 0 && commands.get(j).getTimestamp() > 1) {
						break;
					}
				}
				if (j >= commands.size()) {
					continue;
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
		if (projectTime > 10000000L * 1000L) {
			System.out.println(projectTime);
		}
		return projectTime;
	}

	public File getProgressOutput(File course) {
		File output = new File(course.getParent(), "\\stats\\" + course.getName() + " Progress.csv");
		if (output.exists()) {
			output.delete();
		}
		return output;
	}
	
	public void writeProgress(File course) {
//		String[] headers = {"student", "numBreakAfterPiazzaQuestion", "numNoBreakAfterPiazzaQuestion", 
//									   "numBreakAfterOHRequest", "numNoBreakAfterOHRequest", 
//									   };
		List<String> headers = new ArrayList<>();
		String[] stringArray = {};
		headers.add("student");
		for (File path : getAssigns(course)) {
			String assign = path.getName();
//			headers.add(assign + " Work Time");
			headers.add(assign + " First Piazza Post");
//			headers.add(assign + " Post Time");
			headers.add(assign + " First OH");
//			headers.add(assign + " Zoom Time");
		}
		File output = getProgressOutput(course);
		if (output.exists()) {
			output.delete();
		}
		List<String> nextLine = new ArrayList<>();
		try (CSVWriter cw = new CSVWriter(new FileWriter(output))) {
			cw.writeNext(headers.toArray(stringArray));

			for (Entry<String, List<Integer>> entry: progressMap.entrySet()) {
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
	
	private List<JSONObject> findPiazzaPosts(String onyen, JSONObject piazzaPostsJson) {
		List<JSONObject> list = new ArrayList<>();
		String author = onyen + "(" + onyen + "@live.unc.edu)";
		if (!piazzaPostsJson.has(author)) {
			return list;
		}
		JSONArray posts = piazzaPostsJson.getJSONArray(author);
		for (Object post : posts) {
			if (!(post instanceof JSONObject)) {
				continue;
			}
			JSONObject postJson = (JSONObject)post;
			list.add(postJson);
		}
		return list;
	}
	
	protected String convertToHourMinuteSecond(long timeSpent) {
		int hour = (int) (timeSpent / 3600000);
		int minute = (int) (timeSpent % 3600000 / 60000);
		int second = (int) (timeSpent % 60000 / 1000);
		return hour + ":" + (minute < 10 ? "0" + minute : minute) + ":" + (second < 10 ? "0" + second : second);
	}
	
	public <T> List<T> getListFromMap(Map<String, List<T>> map, String onyen) {
		if (map.containsKey(onyen)) {
			return map.get(onyen);
		}
		List<T> list = new ArrayList<>();
		map.put(onyen, list);
		return list;
	}
	
	public void write(File course) {
		String[] headers = {"#piazza posts", "#diary", "#question", "# OH", "total OH time", "total OH time(s)", "total work time", "total work time(s)"};
		System.out.println(course.getPath());
		File grades = getGrades(course);
		if (grades == null) {
			return;
		}
		String[] lines = FileUtility.readFile(grades).toString().split("\\R");
		String line1 = lines[0];
		String[] firstLine1 = line1.substring(0, line1.indexOf("\"")).split(",");
		String[] firstLine2 = line1.substring(line1.indexOf("\"")).split("\",\"");
		List<String> nextLine = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			nextLine.add(firstLine1[i]);
		}
		for (int i = 0; i < headers.length; i++) {
			nextLine.add(headers[i]);
		}
		for (int i = 0; i < firstLine2.length; i++) {
			nextLine.add(firstLine2[i]);
		}
		nextLine.add("Total assign grade");
		nextLine.add("Total quiz grade");
		nextLine.add("Total exam grade");
		
		try (CSVWriter cw = new CSVWriter(new FileWriter(getOutput(course)))) {
			cw.writeNext(nextLine.toArray(headers));

			for (int i = 1; i < lines.length; i++) {
				nextLine.clear();
				String[] line = lines[i].split(",");
				String onyen = line[2];
				if (onyen.contains("Bobby")) {
					System.out.println(onyen);
				}
				for (int j = 0; j < 3; j++) {
					nextLine.add(line[j]);
				}
				List<Integer> piazzaPosts = getListFromMap(piazzaPostMap, onyen);
				if (piazzaPosts.size() == 0) {
					for (int j = 0; j < 3; j++) {
						nextLine.add("0");
					}
				} else {
					for (int j = 0; j < 3; j++) {
						nextLine.add(piazzaPosts.get(j)+"");
					}
				}
				
				Integer zommSessions = zoomSessionMap.get(onyen);
				nextLine.add(zommSessions == null ? "0" : zommSessions.toString());
				nextLine.add(convertToHourMinuteSecond(zoomTimeMap.get(onyen) == null ? 0 : zoomTimeMap.get(onyen)*1000L));
				nextLine.add(zoomTimeMap.get(onyen) == null ? "0" : zoomTimeMap.get(onyen)+"");
				List<Long> times = getListFromMap(workTimeMap, onyen);
				long totalTime = 0;
				for (long time: times) {
					totalTime += time;
				}
				nextLine.add(convertToHourMinuteSecond(totalTime));
				nextLine.add((int)(totalTime / 1000) + "");
				float assignSum = 0;
				float quizSum = 0;
				float examSum = 0;
				for (int j = 0; j < firstLine2.length; j++) {
					String name = firstLine2[j];
					String grade = line[j+3];
					if (grade.isEmpty()) {
						grade = "0";
					} 
					float gradef = Float.parseFloat(grade);
					if (name.contains("Assignment")) {
						assignSum += gradef;
					} else if (name.startsWith("Midterm") || name.startsWith("Final")) {
						examSum += gradef;
					} else {
						quizSum += gradef;
					}
					nextLine.add(grade);
				}
				nextLine.add(assignSum+"");
				nextLine.add(quizSum+"");
				nextLine.add(examSum+"");
				cw.writeNext(nextLine.toArray(headers));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
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
