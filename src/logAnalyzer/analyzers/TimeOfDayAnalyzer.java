package logAnalyzer.analyzers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import au.com.bytecode.opencsv.CSVWriter;
import fluorite.commands.EHICommand;
import fluorite.commands.PauseCommand;

public class TimeOfDayAnalyzer extends WeekDayAnalyzer {
	public String getSurfix() {
		return "TimeOfDay";
	}		
	public void read(Entry<String, List<List<EHICommand>>> entry) {
		String student = entry.getKey();
		student = student.substring(student.lastIndexOf("(") + 1, student.lastIndexOf(")"));
		List<Integer> list = getListFromMap(map, student);
		if (list.size() == 0) {
			for (int i = 0; i < 24; i++) {
				list.add(0);
			}
		}
		List<List<EHICommand>> nestedCommands = entry.getValue();
		for (int i = 0; i < nestedCommands.size(); i++) {
			List<EHICommand> commands = nestedCommands.get(i);
			for (int j = 1; j < commands.size(); j++) {
				EHICommand command = commands.get(j);
				if (command instanceof PauseCommand) {
					continue;
				}
				int hour = getHour(command);
				list.set(hour, list.get(hour) + 1);
			}
		}
	}
	
	public int getHour(EHICommand command) {
		String date = getDate(command);
		return Integer.parseInt(date.substring(13, 15));
	}
	
	public void write(File course, Map<String, List<Float>> gradeMap) {
		List<String> header = new ArrayList<>();
		header.add("Student");
		for (int i = 0; i < 24; i++) {
			header.add(i + "");
		}
		header.add("Total assign grade");
		header.add("Total quiz grade");
		header.add("Total exam grade");

		List<String> nextLine = new ArrayList<>();
		int threshold = getThreshold();
		
		try (CSVWriter cw = new CSVWriter(new FileWriter(getOutput(course)))) {
			cw.writeNext(header.toArray(new String[1]));

			for (Entry<String, List<Integer>> entry : map.entrySet()) {
				nextLine.clear();
				List<Integer> commandCounts = entry.getValue();
				if (commandCounts.stream().reduce(0, (a, b) -> a+b) < threshold) {
					continue;
				}
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
