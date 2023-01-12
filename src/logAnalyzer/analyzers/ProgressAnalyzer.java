package logAnalyzer.analyzers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONObject;

import au.com.bytecode.opencsv.CSVWriter;
import fluorite.commands.EHICommand;
import fluorite.commands.PiazzaPostCommand;
import fluorite.commands.ZoomChatCommand;

public class ProgressAnalyzer extends LogAnalyzer<List<Integer>> {
	public String getSurfix() {
		return "Progress";
	}
	
	public void read(Entry<String, List<List<EHICommand>>> entry) {
		String student = entry.getKey();
		student = student.substring(student.lastIndexOf("(")+1, student.lastIndexOf(")"));
		List<Integer> list = getListFromMap(map, student);
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

	public void write(File course, Map<String, List<Float>> gradeMap) {
		List<String> header = new ArrayList<>();
		String[] stringArray = {};
		header.add("student");
		for (File path : getAssigns(course)) {
			String assign = path.getName();
//			headers.add(assign + " Work Time");
			header.add(assign + " First Piazza Post");
//			headers.add(assign + " Post Time");
			header.add(assign + " First OH");
//			headers.add(assign + " Zoom Time");
		}
		header.add("Total assign grade");
		header.add("Total quiz grade");
		header.add("Total exam grade");
		File output = getOutput(course);
		if (output.exists()) {
			output.delete();
		}
		List<String> nextLine = new ArrayList<>();
		try (CSVWriter cw = new CSVWriter(new FileWriter(output))) {
			cw.writeNext(header.toArray(stringArray));

			for (Entry<String, List<Integer>> entry: map.entrySet()) {
				nextLine.clear();
				nextLine.add(entry.getKey());
				for (int i : entry.getValue()) {
					nextLine.add(i+"%");
				}
				addGrades(nextLine, gradeMap.get(entry.getKey()));
				cw.writeNext(nextLine.toArray(stringArray));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}

	public File[] getAssigns(File course) {
		return course.listFiles((file)->{
			return file.isDirectory() && file.getName().startsWith("Assignment ");
		});
	}
}
