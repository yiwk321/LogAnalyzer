package logAnalyzer.analyzers;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import analyzer.extension.replayView.FileUtility;
import fluorite.commands.EHICommand;
import fluorite.util.EHLogReader;
import logAnalyzer.LogReader;

public class AggregateAnalyzer {
	List<LogAnalyzer<List<Object>>> logAnalyzers = new ArrayList<>(); 
	Map<String, List<Float>> gradeMap = new HashMap<>();
	PiazzaAndZoomAnalyzer piazzaAndZoomAnalyzer = null;
	EHLogReader reader = new EHLogReader();

	public void register(LogAnalyzer analyzer) {
		logAnalyzers.add(analyzer);
	}
	
	public static void main(String[] args) {
		for (String arg : args) {
			AggregateAnalyzer aggregatePiazzaZoomGrades = new AggregateAnalyzer();
//			aggregatePiazzaZoomGrades.register(new BreakAnalyzer());
//			aggregatePiazzaZoomGrades.register(new ProgressAnalyzer());
			aggregatePiazzaZoomGrades.register(new WeekDayAnalyzer());
			aggregatePiazzaZoomGrades.register(new HolidayAnalyzer());
			aggregatePiazzaZoomGrades.register(new TimeOfDayAnalyzer());
			aggregatePiazzaZoomGrades.register(new CommandCountAnalyzer());
			aggregatePiazzaZoomGrades.register(new SessionAnalyzer());
//			aggregatePiazzaZoomGrades.register(new PiazzaAndZoomAnalyzer());
			
			File course = new File(arg);
			aggregatePiazzaZoomGrades.readLogs(course);
			aggregatePiazzaZoomGrades.write(course);
		}
		System.out.println("Done");
	}
	
	public void register(PiazzaAndZoomAnalyzer piazzaAndZoomAnalyzer) {
		this.piazzaAndZoomAnalyzer = piazzaAndZoomAnalyzer;
	}
	
	Set<String> badLogs = new HashSet<>();
	
	public String getStudentName(String path) {
		return path.substring(path.lastIndexOf(File.separator)+1);
	}
	
	public String getPathForAssign(File assign, String student) {
		return assign.getAbsolutePath() + File.separator + student;
	}
 
	public void readLogs(File course) {
		if (piazzaAndZoomAnalyzer != null) {
			piazzaAndZoomAnalyzer.read(course);
		}
		readGrades(course);
		File[] assigns = getAssigns(course);
		Map<File, Map<String, List<List<EHICommand>>>> allLogs = new HashMap<>();
		for (File assign : assigns) {
			Map<String, List<List<EHICommand>>> logMap = LogReader.readAssignment(assign);
			if (logMap.size() == 0) {
				continue;
			}
			allLogs.put(assign, logMap);
			if (assign.getName().contains("-")) {
				continue;
			}
			Map<String, Integer> sizeMap = new HashMap<>();
			int threshold = 0;
			for (Entry<String, List<List<EHICommand>>> entry : logMap.entrySet()) {
				int sum = 0;
				for (List<EHICommand> commands : entry.getValue()) {
					sum += commands.size();
				}
				threshold += sum;
				sizeMap.put(entry.getKey(), sum);
			}
			threshold = threshold / logMap.size() / 5;
			for (Entry<String, Integer> entry : sizeMap.entrySet()) {
				if (entry.getValue() <= threshold) {
					badLogs.add(getStudentName(entry.getKey()));
				}
			}
		}
		
		
		for (Entry<File, Map<String, List<List<EHICommand>>>> entry : allLogs.entrySet()) {
			for (String student: badLogs) {
				entry.getValue().remove(getPathForAssign(entry.getKey(), student));
			}
			
			for (Entry<String, List<List<EHICommand>>> entry2 : entry.getValue().entrySet()) {
				for (LogAnalyzer<List<Object>> logAnalyzer : logAnalyzers) {
					logAnalyzer.read(entry2);
				}
			}
		}
	}
	
	public void readGrades(File course) {
		File grades = getGrades(course);
		if (grades == null) {
			return;
		}
		String[] lines = FileUtility.readFile(grades).toString().split("\\R");
		String line1 = lines[0];
		String[] assignments = line1.substring(line1.indexOf("\"")).split("\",\"");
		
		for (int i = 1; i < lines.length; i++) {
			String[] line = lines[i].split(",");
			String onyen = line[2];
			float assignSum = 0;
			float quizSum = 0;
			float examSum = 0;
			for (int j = 0; j < assignments.length; j++) {
				String name = assignments[j];
				String grade = line[j + 3];
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
			}
			List<Float> list = getListFromMap(gradeMap, onyen);
			list.add(assignSum);
			list.add(quizSum);
			list.add(examSum);
		}
		
	}
	
	public <T> List<T> getListFromMap(Map<String, List<T>> map, String onyen) {
		if (map.containsKey(onyen)) {
			return map.get(onyen);
		}
		List<T> list = new ArrayList<>();
		map.put(onyen, list);
		return list;
	}
	
	public File getGrades(File course) {
		File grades = new File(course, "gradebook_exportAnon.csv");
		if (grades.exists()) {
			return grades;
		}
		return null;
	}

	public File[] getAssigns(File course) {
		return course.listFiles((file) -> {
			return file.isDirectory() && file.getName().startsWith("Assignment ");
		});
	}

	public void write(File course) {
		if (piazzaAndZoomAnalyzer != null) {
			piazzaAndZoomAnalyzer.write(course, gradeMap);
		}
		for (LogAnalyzer<List<Object>> logAnalyzer : logAnalyzers) {
			logAnalyzer.write(course, gradeMap);
		}
	}
}
