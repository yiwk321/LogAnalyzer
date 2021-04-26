package logAnalyzer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import fluorite.commands.EHICommand;
import fluorite.commands.LocalCheckCommand;
import fluorite.commands.PauseCommand;

public class AnIntellAssignReplayer extends AnAssignmentReplayer{
	protected Map<String, Long> pauseMap = new HashMap<>();
	protected Map<String, Long> nextPauseMap = new HashMap<>();
	long defaultPauseTime = FIVE_MIN;
	double multiplier = 1;
	public static final String[] TYPES = {"Edit", "Debug", "Run", "IO", "Exception", 
			  "Request", "Web", "Save", "Gained Focus", 
			  "Lost Focus", "Terminate", "Difficulty", 
			  "Move Caret", "Open File", "Select", "Compile", 
//			  "LocalChecks",
			  "Other"}; 
	public static String outputFolder = "E:\\submissions\\Distribution";
	
	public AnIntellAssignReplayer(double multiplier, int defaultPauseTime) {
		super();
		if (defaultPauseTime > 0) {
			this.defaultPauseTime = defaultPauseTime*60000L;
		} else {
			this.defaultPauseTime = FIVE_MIN;
		}
		if (multiplier > 0) {
			this.multiplier = multiplier;
		}
		readPauseTime();
	}
	
	protected void readPauseTime() {
		try {
			CSVReader cr = new CSVReader(new FileReader("C:\\Users\\Zhizhou\\OneDrive\\UNC CH\\Junior 1st Sem\\hermes\\git\\Hermes\\Hermes\\data\\ExperimentalData\\ExperimentPauseDistribution.csv"));
			String[] line = null;
			while ((line = cr.readNext()) != null) {
				if (line[0].equals("Sum")) {
					break;
				}
			}
			for (int i = 0; i < TYPES.length; i++) {
				String s = line[4+i*5];
				long pause = defaultPauseTime;
				if (s.length() > 0) {
					try {
						pause = Long.parseLong(s);
						if (pause > 0) {
							pauseMap.put(TYPES[i], (long)(pause * multiplier));
						} else {
							pauseMap.put(TYPES[i], defaultPauseTime);
						}
					} catch (Exception e) {
						System.out.println("reading pause time for " + TYPES[i] + " but read " + s);
						pauseMap.put(TYPES[i], defaultPauseTime);
					}
				} else {
					pauseMap.put(TYPES[i], defaultPauseTime);
				}
			}
			pauseMap.put("LocalChecks", defaultPauseTime);
			while ((line = cr.readNext()) != null) {
				if (line[0].equals("Sum")) {
					break;
				}
			}
			for (int i = 0; i < TYPES.length; i++) {
				String s = line[4+i*5];
				long pause = defaultPauseTime;
				if (s.length() > 0) {
					try {
						pause = Long.parseLong(s);
						if (pause > 0) {
							nextPauseMap.put(TYPES[i], (long)(pause * multiplier));
						} else {
							nextPauseMap.put(TYPES[i], defaultPauseTime);
						}
					} catch (Exception e) {
						System.out.println("reading pause time for " + TYPES[i] + " but read " + s);
						nextPauseMap.put(TYPES[i], defaultPauseTime);
					}
				} else {
					nextPauseMap.put(TYPES[i], defaultPauseTime);
				}
			}
			nextPauseMap.put("LocalChecks", defaultPauseTime);
			cr.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
//	protected long restTime(List<List<EHICommand>> nestedCommands) {
//		long restTime = 0;
//		for (List<EHICommand> commands : nestedCommands) {
//			for (EHICommand command : commands) {
//				if (command instanceof PauseCommand) {
//					long pause = Long.parseLong(command.getDataMap().get("pause"));
//					String prevType = command.getDataMap().get("prevType");
//					String nextType = command.getDataMap().get("nextType");
//					if (pause > pauseMap.get(prevType) && pause > nextPauseMap.get(nextType)) {
//						restTime += pause;
//					}
//				}
//			}
//		}
//		return restTime;
//	}
	
	protected long[] restTime(List<List<EHICommand>> nestedCommands, long time) {
		long[] restTime = new long[2];
		for (List<EHICommand> commands : nestedCommands) {
			for (EHICommand command : commands) {
				if (command instanceof PauseCommand) {
					long pause = Long.parseLong(command.getDataMap().get("pause"));
					String prevType = command.getDataMap().get("prevType");
					String nextType = command.getDataMap().get("nextType");
					if (pause > pauseMap.get(prevType) && pause > nextPauseMap.get(nextType)) {
						restTime[0] += pause;
					}
					if (pause > time) {
						restTime[1] += pause;
					}
				}
			}
		}
		return restTime;
	}
	
//	protected void keepMax3(long[] pauseTimes, long pause) {
//		if (pause > pauseTimes[0]) {
//			pauseTimes[1] = pauseTimes[0];
//			pauseTimes[2] = pauseTimes[1];
//			pauseTimes[0] = pause;
//		} else if (pause > pauseTimes[1]) {
//			pauseTimes[2] = pauseTimes[1];
//			pauseTimes[1] = pause;
//		} else if (pause > pauseTimes[2]) {
//			pauseTimes[2] = pause;
//		}
//	}
//	
//	protected void readPauseTime() {
//		try {
//			CSVReader cr = new CSVReader(new FileReader("C:\\Users\\Zhizhou\\OneDrive\\UNC CH\\Junior 1st Sem\\hermes\\git\\Hermes\\Hermes\\data\\ExperimentalData\\ExperimentPauseDistribution.csv"));
//			cr.readNext();
//			String[] line = cr.readNext();
//			long[][] pauseTimes = new long[TYPES.length][3];
//			while ((line = cr.readNext()) != null) {
//				if (line[0].equals("Sum")) {
//					break;
//				}
//				for (int i = 0; i < TYPES.length; i++) {
//					String s = line[4+i*5];
//					long pause = defaultPauseTime;
//					if (s.length() > 0) {
//						try {
//							pause = Long.parseLong(s);
//							keepMax3(pauseTimes[i], pause);
//						} catch (Exception e) {
//							System.out.println("reading pause time for " + TYPES[i] + " but read " + s);
//							keepMax3(pauseTimes[i], 0);
//						}
//					} else {
//						keepMax3(pauseTimes[i], 0);
//					}
//				}
//			}
//			
//			for (int i = 0; i < TYPES.length; i++) {
//				long pause = pauseTimes[i][1];
//				if (pause > 0) {
//					pauseMap.put(TYPES[i], (long)(pause * multiplier));
//				} else {
//					pauseMap.put(TYPES[i], defaultPauseTime);
//				}
//			}
//			pauseMap.put("LocalChecks", defaultPauseTime);
//			cr.close();
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
	
	public void createDistributionData(String assign, Map<String, List<List<EHICommand>>> data) {
		File temp = new File(assign);
		assign = temp.getName();
		String course = temp.getParentFile().getName();
		File csv = new File(outputFolder + File.separator + course + File.separator + assign +"Distribution.csv");
		FileWriter fw;
		try {
			if (!csv.getParentFile().exists()) {
				csv.getParentFile().mkdirs();
			}
			if (csv.exists()) {
				csv.delete();
			}
			csv.createNewFile();
			fw = new FileWriter(csv);
			CSVWriter cw = new CSVWriter(fw);
			String[] header = getHeader();
			cw.writeNext(header);

//			assign = assign.substring(assign.lastIndexOf(File.separator)+1);
			for (String student : data.keySet()) {
				System.out.println("Generating DistributionData for student " + student);
				List<List<EHICommand>> nestedCommands = data.get(student);
				student = student.substring(student.lastIndexOf(File.separator)+1);
				List<String> line = new ArrayList<>();
				line.add(student);
				long totalTime = totalTimeSpent(nestedCommands);
				if (totalTime == 0) {
					continue;
				}
				long wallClockTime = wallClockTime(nestedCommands);
				line.add(convertToHourMinuteSecond(totalTime));
				line.add(Math.round(Math.ceil((double)wallClockTime/DAY))+"");

				long[] restTime = restTime(nestedCommands, defaultPauseTime);
				line.add(convertToHourMinuteSecond(totalTime - restTime[0]));
				line.add(convertToHourMinuteSecond(totalTime - restTime[1]));
				line.add(convertToHourMinuteSecond(restTime[1]-restTime[0]));
				line.add(convertToHourMinuteSecond(restTime[0]));
				line.add(convertToHourMinuteSecond(restTime[1]));
				
				String[] nextLine = line.toArray(new String[1]);
				cw.writeNext(nextLine);
			}
			fw.close();
			cw.close();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}
	
	public void analyzeAssignment(CountDownLatch latch, String assign, Map<String, Map<String, List<EHICommand>>> assignLogs) {
		new Thread(()->{
			System.out.println("Analyzing " + assign.substring(assign.lastIndexOf(File.separator)+1));
			Map<String, List<List<EHICommand>>> commands = new HashMap<>();
			for (String student : assignLogs.keySet()) {
				commands.put(student, new ArrayList<List<EHICommand>>(assignLogs.get(student).values()));
			}
//			createDistributionData(assign, commands);
//			createLocalCheckPassEvents(assign, commands);
			createBreakDistributionData(assign, commands);
//			createPauseDistribution(assign, commands);
//			createAssignData(assign, commands);
			latch.countDown();
		}).start();
	}

	protected String[] getHeader() {
		String[] header = new String[8];
		header[0] = "Student";
		header[1] = "Total Time Spent";
		header[2] = "Number of Days";
		header[3] = "Active Time(lab)";
		header[4] = "Active Time(5min)";
		header[5] = "Difference";
		header[6] = "Rest Time(lab)";
		header[7] = "Rest Time(5min)";
		return header;
	}
	
	protected String[] getEventHeader() {
		String[] header = new String[4];
		header[0] = "case_id";
		header[1] = "timestamp";
		header[2] = "activity";
		header[3] = "user";
		return header;
	}
	
	protected String[] getBreakHeader() {
		String[] header = new String[2+PauseCommand.RANGES.length];
		header[0] = "Type";
		header[1] = "Threshold";
		for (int i = 0; i < PauseCommand.RANGES.length; i++) {
			header[i+2] = PauseCommand.RANGES[i];
		}
		return header;
	}
	
	public void createBreakDistributionData(String assign, Map<String, List<List<EHICommand>>> data) {
		File temp = new File(assign);
		assign = temp.getName();
		String course = temp.getParentFile().getName();
		File csv = new File(outputFolder + File.separator + course + File.separator + assign + "BreakDistribution.csv");
		FileWriter fw;
		try {
			if (!csv.getParentFile().exists()) {
				csv.getParentFile().mkdirs();
			}
			if (csv.exists()) {
				csv.delete();
			}
			csv.createNewFile();
			fw = new FileWriter(csv);
			CSVWriter cw = new CSVWriter(fw);
			String[] header = getBreakHeader();
			cw.writeNext(header);

			int[][] prevBreaks = new int[TYPES.length+1][PauseCommand.RANGES.length];
			int[][] nextBreaks = new int[TYPES.length+1][PauseCommand.RANGES.length];
			Map<String, Integer> typeMap = new HashMap<>();
			for (int i = 0; i < TYPES.length; i++) {
				typeMap.put(TYPES[i], i);
			}
			typeMap.put("LocalChecks", TYPES.length);
			Map<String, Integer> rangeMap = new HashMap<>();
			for (int i = 0; i < PauseCommand.RANGES.length; i++) {
				rangeMap.put(PauseCommand.RANGES[i], i);
			}
			for (String student : data.keySet()) {
				System.out.println("Generating BreakDistribution for student " + student);
				List<List<EHICommand>> nestedCommands = data.get(student);
				for (List<EHICommand> commands : nestedCommands) {
					for (EHICommand command: commands) {
						if (command instanceof PauseCommand) {
							String prevType = command.getDataMap().get("prevType");
							String nextType = command.getDataMap().get("nextType");
							long pause = Long.parseLong(command.getDataMap().get("pause"));
							if (pause > pauseMap.get(prevType) && pause > nextPauseMap.get(nextType)) {								
								String range = command.getAttributesMap().get("range");
								if (range == null) {
									range = ((PauseCommand)command).getRange(pause);
								}
								prevBreaks[typeMap.get(prevType)][rangeMap.get(range)]++;
								nextBreaks[typeMap.get(nextType)][rangeMap.get(range)]++;
							}
						}
					}
				}
			}
			String[] prev = {"prev"};
			cw.writeNext(prev);
			int[] sum = new int[PauseCommand.RANGES.length];
			for (int i = 0; i < TYPES.length; i++) {
				String[] nextLine = new String[PauseCommand.RANGES.length+2];
				nextLine[0] = TYPES[i];
				nextLine[1] = pauseMap.get(TYPES[i])+"";
				for (int j = 0; j < PauseCommand.RANGES.length; j++) {
					nextLine[j+2] = prevBreaks[i][j]+""; 
					sum[j] += prevBreaks[i][j];
				}
				cw.writeNext(nextLine);
			}
			String[] sumLine = new String[sum.length+2];
			sumLine[0] = "Sum";
			sumLine[1] = "";
			for (int i = 0; i < sum.length; i++) {
				sumLine[i+2] = sum[i]+"";
			}
			cw.writeNext(sumLine);
			cw.writeNext(new String[0]);
			String[] next = {"next"};
			cw.writeNext(next);
			sum = new int[PauseCommand.RANGES.length];
			for (int i = 0; i < TYPES.length; i++) {
				String[] nextLine = new String[PauseCommand.RANGES.length+2];
				nextLine[0] = TYPES[i];
				nextLine[1] = pauseMap.get(TYPES[i])+"";
				for (int j = 0; j < PauseCommand.RANGES.length; j++) {
					nextLine[j+2] = nextBreaks[i][j]+""; 
					sum[j] += nextBreaks[i][j];
				}
				cw.writeNext(nextLine);
			}
			sumLine = new String[sum.length+2];
			sumLine[0] = "Sum";
			sumLine[1] = "";
			for (int i = 0; i < sum.length; i++) {
				sumLine[i+2] = sum[i]+"";
			}
			cw.writeNext(sumLine);
			fw.close();
			cw.close();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}
	
	public void createLocalCheckPassEvents(String assign, Map<String, List<List<EHICommand>>> data) {
		File temp = new File(assign);
		assign = temp.getName();
		String course = temp.getParentFile().getName();
		File csv = new File(outputFolder + File.separator + course + File.separator + assign +"LocalCheckPassEvents.csv");
//		File csv = new File(assign+"LocalCheckPassEvents.csv");
		FileWriter fw;
		try {
			if (!csv.getParentFile().exists()) {
				csv.getParentFile().mkdirs();
			}
			if (csv.exists()) {
				csv.delete();
			}
			csv.createNewFile();
			fw = new FileWriter(csv);
			CSVWriter cw = new CSVWriter(fw);
			String[] header = getEventHeader();
			cw.writeNext(header);

//			assign = assign.substring(assign.lastIndexOf(File.separator)+1);
			for (String student : data.keySet()) {
				System.out.println("Generating LocalCheckPassEvents for student " + student);
				List<List<EHICommand>> nestedCommands = data.get(student);
				student = student.substring(student.lastIndexOf(File.separator)+1);
				String id = assign + '_' + student;
				long time = 0;
				Map<String, EHICommand> localCheckEvents = new HashMap<>();
				for (int i = 0; i < nestedCommands.size(); i++) {
					List<EHICommand> commands = nestedCommands.get(i);
					long lastTimestamp = 0;
					boolean paused = false;
					for (int j = 0; j < commands.size(); j++) {
						EHICommand command = commands.get(j);
						long timestamp = command.getStartTimestamp() + command.getTimestamp();
						if (timestamp == 0) {
							continue;
						}
						if (lastTimestamp != 0) {
							if (command instanceof PauseCommand) {
								long pause = Long.parseLong(command.getDataMap().get("pause"));
								String prevType = command.getDataMap().get("prevType");
								String nextType = command.getDataMap().get("nextType");
								if (pause < pauseMap.get(prevType) || pause < nextPauseMap.get(nextType)) {
									time += pause;
								}
								paused = true;
							} else if (!paused) {
								time += timestamp - lastTimestamp;
							}
						}

						if (command instanceof LocalCheckCommand) {
							String testcase = command.getDataMap().get("testcase");
//							String type = command.getDataMap().get("type");
//							if (!localCheckEvents.containsKey(testcase) 
//							|| (!localCheckEvents.get(testcase).getDataMap().get("type").equals("passed") 
//							&& !localCheckEvents.get(testcase).getDataMap().get("type").equals(type))) {
								command.setTimestamp2(time);
								localCheckEvents.put(testcase, command);
//							}
						}
						lastTimestamp = timestamp;
					}
				}
				List<EHICommand> events = new ArrayList<>();
				events.addAll(localCheckEvents.values());
				sortEvents(events);
				List<String> line = new ArrayList<>();
				for (EHICommand event : events) {
					line.clear();
					line.add(id);
					line.add(convertToHourMinuteSecond(event.getTimestamp2()));
					line.add(event.getDataMap().get("testcase") + "_" + event.getDataMap().get("type"));
					line.add(student);
					String[] nextLine = line.toArray(new String[1]);
					cw.writeNext(nextLine);
				}
			}
			fw.close();
			cw.close();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}
	
	public void sortEvents(List<EHICommand> commands){
		EHICommand command = null;
		long cur = 0;
		for(int i = 0; i < commands.size(); i++) {
			command = commands.get(i);
			cur = command.getTimestamp2();
			int j = i-1;
			while (j >= 0){
				if (commands.get(j).getTimestamp2()> cur) {
					j--;
				} else {
					break;
				}
			}
			if (j < i-1) {
				commands.remove(i);
				commands.add(j+1, command);
			}
		}
	}
}
