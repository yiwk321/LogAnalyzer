package logAnalyzer.analyzers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.Map.Entry;

import au.com.bytecode.opencsv.CSVWriter;
import fluorite.commands.EHICommand;
import fluorite.commands.PauseCommand;

public class WeekDayAnalyzer extends LogAnalyzer<List<Integer>>{
	SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd-u-HH-mm-ss-SSS 'ET'");

	public String getSurfix() {
		return "WeekDay";
	}	
	
	public WeekDayAnalyzer() {
		df.setTimeZone(TimeZone.getTimeZone("America/New_York"));
	}

	public void read(Entry<String, List<List<EHICommand>>> entry) {
		String student = entry.getKey();
		student = student.substring(student.lastIndexOf("(") + 1, student.lastIndexOf(")"));
		List<Integer> list = getListFromMap(map, student);
		if (list.size() == 0) {
			for (int i = 0; i < 7; i++) {
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
				int weekDay = getWeekDay(command);
				list.set(weekDay - 1, list.get(weekDay - 1) + 1);
			}
		}
	}
	
	public int getWeekDay(EHICommand command) {
		String date = getDate(command);
		return Integer.parseInt(date.charAt(11) + "");
	}
	
	public String getDate(EHICommand command) {
		long timestamp = command.getTimestamp() + command.getStartTimestamp();
		if (timestamp == 0) {
			timestamp = command.getTimestamp2();
		}
		return df.format(new Date(timestamp));
	}

	public void write(File course, Map<String, List<Float>> gradeMap) {
		String[] header = { "Student", "Mon", "Tue", "Wed", "Thur", "Fri", "Sat", "Sun"
				, "Total assign grade", "Total quiz grade", "Total exam grade"};
//		File grades = getGrades(course);
//		if (grades == null) {
//			return;
//		}
		List<String> nextLine = new ArrayList<>();
		int threshold = getThreshold();
		
		try (CSVWriter cw = new CSVWriter(new FileWriter(getOutput(course)))) {
			cw.writeNext(header);
			
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
