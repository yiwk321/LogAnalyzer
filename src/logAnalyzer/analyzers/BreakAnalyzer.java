package logAnalyzer.analyzers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONObject;

import au.com.bytecode.opencsv.CSVWriter;
import fluorite.commands.BalloonCommand;
import fluorite.commands.EHICommand;
import fluorite.commands.PauseCommand;
import fluorite.commands.PiazzaPostCommand;

public class BreakAnalyzer extends LogAnalyzer<List<String[]>>{
	public String getSurfix() {
		return "Breaks";
	}
	
	public void read(Entry<String, List<List<EHICommand>>> entry) {
		String student = entry.getKey();
		student = student.substring(student.lastIndexOf("(")+1, student.lastIndexOf(")"));
		List<String[]> list = getListFromMap(map, student);
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

	public void write(File course, Map<String, List<Float>> gradeMap) {
		String[] headers = {"student", "type", "cause", "breaktime", "next command", "folders"
				//				   "numBreakAfterOHRequest", "numNoBreakAfterOHRequest", 
				//				   "numBreakAfterOHSession", "numNoBreakAfterOHSession"
//				, "Total assign grade", "Total quiz grade", "Total exam grade"
		};
		File output = getOutput(course);
		
		try (CSVWriter cw = new CSVWriter(new FileWriter(output))) {
			cw.writeNext(headers);

			for (Entry<String, List<String[]>> entry: map.entrySet()) {
				for (String[] line : entry.getValue()) {
					cw.writeNext(line);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
