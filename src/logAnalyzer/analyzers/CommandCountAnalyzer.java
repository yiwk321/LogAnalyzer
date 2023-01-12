package logAnalyzer.analyzers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import au.com.bytecode.opencsv.CSVWriter;
import fluorite.commands.AssistCommand;
import fluorite.commands.CopyCommand;
import fluorite.commands.Delete;
import fluorite.commands.EHICommand;
import fluorite.commands.FileOpenCommand;
import fluorite.commands.Insert;
import fluorite.commands.LocalCheckCommand;
import fluorite.commands.PasteCommand;
import fluorite.commands.Replace;
import fluorite.commands.RunCommand;
import fluorite.commands.WebCommand;

public class CommandCountAnalyzer extends LogAnalyzer<List<Integer>>{
	public String getSurfix() {
		return "CommandCount";
	}	
	public void read(Entry<String, List<List<EHICommand>>> entry) {
		List<List<EHICommand>> nestedCommands = entry.getValue();
		String student = entry.getKey();
		student = student.substring(student.lastIndexOf("(") + 1, student.lastIndexOf(")"));

		List<Integer> list = getListFromMap(map, student);
		if (list.isEmpty()) {
			for (int i = 0; i < 11; i++) {
				list.add(0);
			}
		}
		long localcheckTime = 0;
		long runTime = 0;
		for (List<EHICommand> commands : nestedCommands) {
			for (int i = 0; i < commands.size(); i++) {
				EHICommand command = commands.get(i);

				if (command instanceof WebCommand) {
					list.set(0, list.get(0) + 1);
				}
				if (command instanceof Insert) {
//					numCommands[1] += command.getDataMap().get("text").length();
					list.set(1, list.get(1) + command.getDataMap().get("text").length());
				}
//				if (command instanceof InsertStringCommand) {
//					list.set(1, list.get(1)+command.getDataMap().get("data").length());
//				}
				if (command instanceof Delete) {
//					numCommands[2] += command.getDataMap().get("text").length();
					list.set(2, list.get(2) + command.getDataMap().get("text").length());
				}
//				if (command instanceof EclipseCommand && ((EclipseCommand) command).getCommandID()
//						.equals("eventLogger.styledTextCommand.DELETE_PREVIOUS")) {
////					numCommands[2]++;
//					list.set(2, list.get(2)+1);
//				}
				if (command instanceof Replace) {
//					numCommands[3]++;
					list.set(5, list.get(5) + 1);
				}
				if (command instanceof CopyCommand) {
//					numCommands[4]++;
					list.set(3, list.get(3) + 1);
				}
				if (command instanceof PasteCommand) {
//					numCommands[5]++;
					list.set(4, list.get(4) + 1);
				}
				if (command instanceof FileOpenCommand) {
					list.set(6, list.get(6) + 1);
				}
				if (command instanceof RunCommand) {
					long timestamp = command.getTimestamp() + command.getStartTimestamp();
					if (runTime + 10000 < timestamp) {
						if (command.getAttributesMap().get("type").equals("Debug")) {
//							numCommands[7]++;
							list.set(8, list.get(8) + 1);
						} else {
//							numCommands[6]++;
							list.set(7, list.get(7) + 1);
						}
						runTime = timestamp;
					}
				}
				if (command instanceof LocalCheckCommand) {
					long timestamp = command.getTimestamp() + command.getStartTimestamp();
					if (localcheckTime < timestamp) {
//						numCommands[8]++;
						list.set(9, list.get(9) + 1);
						localcheckTime = timestamp;
					}
				}
				if (command instanceof AssistCommand) {
					list.set(10, list.get(10) + 1);
				}
			}

		}		
	}

	public void write(File course, Map<String, List<Float>> gradeMap) {
		String[] header = { "Student", "Web", "Insert", "Delete", "Copy", "Paste", "Replace", "FileOpen", "Run",
				"Debug", "LocalChecks", "Assist",
				"Total assign grade", "Total quiz grade", "Total exam grade"
				};
		List<String> nextLine = new ArrayList<>();
		try (CSVWriter cw = new CSVWriter(new FileWriter(getOutput(course)))) {
			cw.writeNext(header);

			for (Entry<String, List<Integer>> entry : map.entrySet()) {
				nextLine.clear();
				List<Integer> commandCounts = entry.getValue();
				nextLine.add(entry.getKey());
				for (int j = 0; j < commandCounts.size(); j++) {
					nextLine.add(commandCounts.get(j) + "");
				}
				addGrades(nextLine, gradeMap.get(entry.getKey()));
				cw.writeNext(nextLine.toArray(new String[1]));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
}
