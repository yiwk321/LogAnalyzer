package logAnalyzer.analyzers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import au.com.bytecode.opencsv.CSVWriter;
import fluorite.commands.BaseDocumentChangeEvent;
import fluorite.commands.EHICommand;
import fluorite.commands.PauseCommand;

public class SessionAnalyzer extends TimeOfDayAnalyzer {
	final static long THRESHOLD = 60 * 60 * 1000; 
	public void read(Entry<String, List<List<EHICommand>>> entry) {
		List<List<EHICommand>> nestedCommands = entry.getValue();
		String student = entry.getKey();
		student = student.substring(student.lastIndexOf("(") + 1, student.lastIndexOf(")"));
		boolean inSession = false;
		
		List<Integer> list = getListFromMap(map, student);
		if (list.size() == 0) {
			for (int i = 0; i < 24; i++) {
				list.add(0);
			}
		}

		for (List<EHICommand> commands : nestedCommands) {
			for (int i = 0; i < commands.size(); i++) {
				EHICommand command = commands.get(i);

				if (command instanceof PauseCommand) {
					long pause = Long.parseLong(command.getDataMap().get("pause"));
					if (pause > THRESHOLD) {
						inSession = false;
					}
				} else if (!inSession && command instanceof BaseDocumentChangeEvent) {
					int hour = getHour(command);
					list.set(hour, list.get(hour)+1);
					inSession = true;
				}
			}
			inSession = false;
		}	
	}

	public String getSurfix() {
		return "Session";
	}

	public void write(File course) {
		String[] header = {"student", "sessions"};
		List<String> nextLine = new ArrayList<>();
		int threshold = 5;
		
		try (CSVWriter cw = new CSVWriter(new FileWriter(getOutput(course)))) {
			cw.writeNext(header);

			for (Entry<String, List<Integer>> entry : map.entrySet()) {
				nextLine.clear();
				List<Integer> sessionCounts = entry.getValue();
				if (sessionCounts.stream().reduce(0, (a, b) -> a+b) < threshold) {
					continue;
				}
				nextLine.add(entry.getKey());
				for (int j = 0; j < sessionCounts.size(); j++) {
					nextLine.add(sessionCounts.get(j) + "");
				}
				cw.writeNext(nextLine.toArray(new String[1]));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
}
