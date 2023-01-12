package logAnalyzer.analyzers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
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

public class HolidayAnalyzer extends LogAnalyzer<List<Integer>>{
	String[][] holidays = { 
			{ "2016-09-05" }, { "2016-10-20", "2016-10-24" }, { "2016-11-23", "2016-11-28" },

			{ "2017-01-16" }, { "2017-03-11", "2017-03-20" }, { "2017-04-14" },

			{ "2017-09-04" }, { "2017-10-19", "2017-10-23" }, { "2017-11-22", "2017-11-27" },

			{ "2018-01-15" }, { "2018-03-10", "2018-03-19" }, { "2018-03-30" },

			{ "2018-09-03" }, { "2018-10-18", "2018-10-22" }, { "2018-11-21", "2018-11-26" },

			{ "2019-01-21" }, { "2019-03-09", "2019-03-18" }, { "2019-04-19" },

			{ "2019-09-02" }, { "2019-10-17", "2019-10-21" }, { "2019-11-27", "2019-12-02" },

			{ "2020-01-20" }, { "2020-03-07", "2020-03-15" }, { "2020-04-10" },

			{ "2020-09-07" },

			{ "2021-01-18" }, { "2021-02-15", "2021-02-16" }, { "2021-03-11", "2021-03-15" }, { "2021-04-02" },
			{ "2021-04-05" },

			{ "2021-05-31" },

			{ "2021-09-06" }, { "2021-10-21", "2021-10-25" }, { "2021-11-24", "2021-11-30" },

			{ "2022-01-17" }, { "2022-03-11", "2022-03-21" }, { "2022-04-14", "2022-04-18" },

			{ "2022-09-05", "2022-09-07" }, { "2022-09-26" }, { "2022-10-20", "2022-10-24" },
			{ "2022-11-23", "2022-11-28" }, };
	List<List<Date>> holidayDates = new ArrayList<>();
	SimpleDateFormat holidayDF = new SimpleDateFormat("yyyy-MM-dd");
	
	public String getSurfix() {
		return "Holiday";
	}
	
	public HolidayAnalyzer() {
		holidayDF.setTimeZone(TimeZone.getTimeZone("America/New_York"));
		for (String[] holiday : holidays) {
			List<Date> dates = new ArrayList<>();
			try {
				dates.add(holidayDF.parse(holiday[0]));
				if (holiday.length == 2) {
					dates.add(holidayDF.parse(holiday[1]));
				}
			} catch (ParseException e) {
				e.printStackTrace();
			}
			holidayDates.add(dates);
		}
	}
	
	public void read(Entry<String, List<List<EHICommand>>> entry) {
		String student = entry.getKey();
		student = student.substring(student.lastIndexOf("(") + 1, student.lastIndexOf(")"));
		List<Integer> list = getListFromMap(map, student);
		if (list.size() == 0) {
			for (int i = 0; i < 2; i++) {
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
				if (isHoliday(command)) {
					list.set(1, list.get(1) + 1);
				} else {
					list.set(0, list.get(0) + 1);
				}
			}
		}		
	}

	public boolean isHoliday(EHICommand command) {
		long timestamp = command.getTimestamp() + command.getStartTimestamp();
		if (timestamp == 0) {
			timestamp = command.getTimestamp2();
		}
		Date commandDate = new Date(timestamp);

		for (List<Date> dates : holidayDates) {
			Date startDate = dates.get(0);
			Date endDate = dates.size() == 2 ? dates.get(1) : new Date(startDate.getTime() + 24 * 3600 * 1000);
			if (commandDate.after(startDate) && commandDate.before(endDate)) {
				return true;
			}
		}
		return false;
	}
	
	public void write(File course, Map<String, List<Float>> gradeMap) {
		String[] header = { "Student", "School day", "Holiday"
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
