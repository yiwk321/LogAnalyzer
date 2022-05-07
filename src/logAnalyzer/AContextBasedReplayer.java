package logAnalyzer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import fluorite.commands.EHICommand;
import fluorite.commands.EclipseCommand;
import fluorite.commands.FileOpenCommand;
import fluorite.commands.FileSaveCommand;
import fluorite.commands.LocalCheckCommand;
import fluorite.commands.PauseCommand;
import fluorite.commands.RunCommand;
import tests.Assignment;
import tests.Suite;

public class AContextBasedReplayer extends AnAssignmentReplayer{
	protected Map<String, Long> pauseMap = new HashMap<>();
	protected Map<String, Long> nextPauseMap = new HashMap<>();
	long defaultPauseTime = FIVE_MIN;
	double multiplier = 1;
	
	public static final String[] TYPES = {"Edit", "Debug", "Run", "IO", "Exception", 
			"Request", "Web", "Save", "Gained Focus", 
			"Lost Focus", "Terminate", "Difficulty", 
			"Move Caret", "Open File", "Select", "Compile", 
			"LocalChecks", 
			"Other"}; 
	public static final String[] RANGES = {"1s-2s","2s-5s","5s-10s","10s-20s","20s-30s",
			"30s-1m","1m-2m","2m-5m","5m-10m","10m-20m",
			"20m-30m","30m-1h",">1h"};
	public static final long[] THRESHOLD = {15109, 22531, 34266, 0, 9641, 0, 493000, 6921, 77564,
			24868, 0, 50953, 102979, 3984, 50202, 51718, 0, 79218};
	public static final long[] NEXT_THRESHOLD = {59079, 30031, 13407, 0, 19062, 0, 493000, 10125, 
			472825, 104170, 0, 13780, 102797, 65204, 50202, 20110, 0, 58702};
	public static final String PAUSE_TIME_DISTRIBUTION = "C:\\Users\\Zhizhou\\OneDrive\\UNC CH\\Junior 1st Sem\\hermes\\git\\Hermes\\Hermes\\data\\ExperimentalData\\ExperimentPauseDistribution.csv";
	
	public static String outputFolder = "E:\\submissions\\Distribution";
	
	public AContextBasedReplayer(double multiplier, int defaultPauseTime) {
		super();
		if (defaultPauseTime > 0) {
			this.defaultPauseTime = defaultPauseTime*60000L;
		} else {
			this.defaultPauseTime = FIVE_MIN;
		}
		if (multiplier > 0) {
			this.multiplier = multiplier;
		}
		initPauseMap();
	}
	
	protected void initPauseMap() {
		pauseMap = new HashMap<>();
		nextPauseMap = new HashMap<>();
		for (int i = 0; i < TYPES.length; i++) {
			pauseMap.put(TYPES[i], THRESHOLD[i]==0?defaultPauseTime:THRESHOLD[i]);
			nextPauseMap.put(TYPES[i], NEXT_THRESHOLD[i]==0?defaultPauseTime:NEXT_THRESHOLD[i]);
		}
	}
	
	protected void readPauseTime() {
		try {
			CSVReader cr = new CSVReader(new FileReader(PAUSE_TIME_DISTRIBUTION));
			String[] line = null;
			while ((line = cr.readNext()) != null) {
				if (line[0].equals("Sum")) {
					break;
				}
			}
			for (int i = 0; i < TYPES.length; i++) {
				String s = line[4+i*5];
				long pause = 0;
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
			for (int i = 0; i < commands.size()-1; i++) {
				EHICommand command = commands.get(i);
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
	
	public void createAssignData2(String assign, Map<String, List<List<EHICommand>>> data) {
		File csv = new File(assign+"2.csv");
		FileWriter fw;
		try {
			if (csv.exists()) {
				csv.delete();
			}
			csv.createNewFile();
			fw = new FileWriter(csv);
			CSVWriter cw = new CSVWriter(fw);
			String[] header = {"Student", "Start Time", "End Time", "Wall Time", 
							   "Total Time Spent", "Active Time (5min)", "Rest Time (5min)", 
							   "Active Time (context)", "Rest Time (context)"};
			cw.writeNext(header);
			
			assign = assign.substring(assign.lastIndexOf(File.separator)+1);
			for (String student : data.keySet()) {
				System.out.println("Generating AssignData for student " + student);
				List<List<EHICommand>> nestedCommands = data.get(student);
				student = student.substring(student.lastIndexOf(File.separator)+1);
				
				List<String> retVal = new ArrayList<>();
				retVal.add(student);
				long wallClockTime = wallClockTime(nestedCommands);
				EHICommand c1 = null;
				for (int j = 0; j < nestedCommands.get(0).size(); j++) {
					c1 = nestedCommands.get(0).get(j);
					if (c1.getStartTimestamp() > 0 || c1.getTimestamp() > 0) {
						break;
					}
				}
				long startTime = 0;
				if (c1 != null) {
					startTime = c1.getStartTimestamp() + c1.getTimestamp();
				}
				retVal.add(new Date(startTime).toString());
				retVal.add(new Date(startTime + wallClockTime).toString());
				retVal.add(format(wallClockTime));

				long totalTime = totalTimeSpent(nestedCommands);
				if (totalTime == 0) {
					continue;
				}
				long[] restTime = restTime(nestedCommands, FIVE_MIN);
				retVal.add(format(totalTime));
				retVal.add(format(totalTime - restTime[1]));
				retVal.add(format(restTime[1]));
				retVal.add(format(totalTime - restTime[0]));
				retVal.add(format(restTime[0]));
				String[] nextLine = retVal.toArray(new String[1]);
				cw.writeNext(nextLine);
			}
			fw.close();
			cw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
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
			Map<String, Long> rcPassTime = localCheckRegularCredit(assign, data);

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
				line.add(format(totalTime));
				line.add(Math.round(Math.ceil((double)wallClockTime/DAY))+"");

				long[] restTime = restTime(nestedCommands, defaultPauseTime);
				line.add(format(totalTime - restTime[0]));
				line.add(format(totalTime - restTime[1]));
				line.add(format(restTime[1]-restTime[0]));
				long compileActiveTime = compileActiveTime(nestedCommands);
				line.add(format(compileActiveTime));
				line.add(format(totalTime - restTime[0] - compileActiveTime));
				line.add(format(restTime[0]));
				line.add(format(restTime[1]));
				Map<String, List<String>> events = localCheckEventsDistribution(nestedCommands);
				if (rcPassTime.containsKey(student)) {
					line.add(format(rcPassTime.get(student)));
				} else {
					line.add("NaN");
				}
				line.add(events.get("Pass").toString());
				line.add(events.get("Partial").toString());
				line.add(events.get("Fail").toString());
				String[] nextLine = line.toArray(new String[1]);
				cw.writeNext(nextLine);
			}
			fw.close();
			cw.close();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}
	
	protected Map<String, List<String>> localCheckEventsDistribution(List<List<EHICommand>> nestedCommands) {
		Map<String, List<String>> events = new HashMap<>();
		List<String> pass = new ArrayList<>();
		List<String> partial = new ArrayList<>();
		List<String> fail = new ArrayList<>();
		
		Map<String, EHICommand> localCheckEvents = new HashMap<>();
		for (List<EHICommand> commands: nestedCommands) {
			for (EHICommand command: commands) {
				if (command instanceof LocalCheckCommand) {
					String testcase = command.getDataMap().get("testcase");
					localCheckEvents.put(testcase, command);
				}
			}
		}
		for (String testcase : localCheckEvents.keySet()) {
			String type = localCheckEvents.get(testcase).getDataMap().get("type");
			if (type.equals("passed")) {
				pass.add(testcase);
			} else if (type.contains("partial")) {
				partial.add(testcase);
			} else if (type.contains("fail")) {
				fail.add(testcase);
			}
		}
		events.put("Pass", pass);
		events.put("Partial", partial);
		events.put("Fail", fail);
		return events;
	}
	
//	protected long compileActiveTime(List<List<EHICommand>> nestedCommands) {
//		long totalTime = 0;
//		long sessionStartTime = 0;
//		long lastCompileTime = 0;
//		for (List<EHICommand> commands : nestedCommands) {
//			for (EHICommand command : commands) {
//				if (command instanceof RunCommand || command instanceof FileSaveCommand) {
//					long time = command.getStartTimestamp() + command.getTimestamp();
//					if (sessionStartTime == 0) {
//						sessionStartTime = time;
//					} else if (time - lastCompileTime > TEN_MIN*3) {
//						totalTime += lastCompileTime - sessionStartTime;
//						sessionStartTime = time;
//					}
//					lastCompileTime = time;
//				}
//			}
//		}
//		totalTime += lastCompileTime - sessionStartTime;
//		return totalTime;
//	}
	
	protected long compileActiveTime(List<List<EHICommand>> nestedCommands) {
		long totalTime = 0;
		long sessionStartTime = 0;
		long lastCompileTime = 0;
		for (List<EHICommand> commands : nestedCommands) {
			for (EHICommand command : commands) {
				if (command instanceof RunCommand || command instanceof FileSaveCommand || 
					(command instanceof EclipseCommand && (((EclipseCommand)command).getCommandID().contains("save"))) || 
					command instanceof FileOpenCommand) {
					long time = command.getStartTimestamp() + command.getTimestamp();
					if (sessionStartTime == 0) {
						sessionStartTime = time;
					} else if (time - lastCompileTime > TEN_MIN*3) {
						totalTime += lastCompileTime - sessionStartTime;
						sessionStartTime = time;
					}
					lastCompileTime = time;
				}
			}
			totalTime += lastCompileTime - sessionStartTime;
			sessionStartTime = 0;
			lastCompileTime = 0;
		}
		return totalTime;
	}
	
	public void analyzeAssignment(CountDownLatch latch, String assign, Map<String, Map<String, List<EHICommand>>> assignLogs) {
		new Thread(()->{
			System.out.println("Analyzing " + assign.substring(assign.lastIndexOf(File.separator)+1));
			Map<String, List<List<EHICommand>>> commands = new HashMap<>();
			for (String student : assignLogs.keySet()) {
				commands.put(student, new ArrayList<List<EHICommand>>(assignLogs.get(student).values()));
			}
//			createLocalCheckSuiteEvents(assign, commands);
//			createDistributionData(assign, commands);
//			createLocalCheckPassEvents(assign, commands);
//			createBreakDistributionData(assign, commands);
//			createPauseDistribution(assign, commands);
			createAssignData2(assign, commands);
			latch.countDown();
		}).start();
	}

	protected String[] getHeader() {
		String[] header = new String[14];
		header[0] = "Student";
		header[1] = "Total Time Spent";
		header[2] = "Number of Days";
		header[3] = "Active Time(Context-Based)";
		header[4] = "Active Time(5min)";
		header[5] = "Difference";
		header[6] = "Active Time(Retina)";
		header[7] = "Difference";
		header[8] = "Rest Time(Context-Based)";
		header[9] = "Rest Time(5min)";
		header[10] = "Regular Credit Milestone Time";
		header[11] = "Pass";
		header[12] = "Partial";
		header[13] = "Fail";
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
				nextLine[1] = nextPauseMap.get(TYPES[i])+"";
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
	
	public void createLocalCheckEvents(String assign, Map<String, List<List<EHICommand>>> data) {
		File temp = new File(assign);
		assign = temp.getName();
		String course = temp.getParentFile().getName();
		File csv = new File(outputFolder + File.separator + course + File.separator + assign +"LocalCheckEvents.csv");
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
			Set<Long> workingSet = new HashSet<>();
//			assign = assign.substring(assign.lastIndexOf(File.separator)+1);
			for (String student : data.keySet()) {
				System.out.println("Generating LocalCheckEvents for student " + student);
				List<List<EHICommand>> nestedCommands = data.get(student);
				student = student.substring(student.lastIndexOf(File.separator)+1);
				String id = assign + '_' + student;
				long time = 0;
				int numEdits = 0;
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
							if (numEdits > 0) {
								workingSet.add(time);
							}
							numEdits = 0;
							String testcase = command.getDataMap().get("testcase");
//							String type = command.getDataMap().get("type");
//							if (!localCheckEvents.containsKey(testcase) 
//							|| (!localCheckEvents.get(testcase).getDataMap().get("type").equals("passed") 
//							&& !localCheckEvents.get(testcase).getDataMap().get("type").equals(type))) {
								command.setTimestamp2(time);
								localCheckEvents.put(testcase, command);
//							}
						} else if (getEventType(command).equals("Edit")) {
							numEdits++;
						}
						lastTimestamp = timestamp;
					}
				}
				List<EHICommand> events = new ArrayList<>();
				events.addAll(localCheckEvents.values());
				sortEvents(events);
				List<String> line = new ArrayList<>();
				long lastTime = 0;
				for (EHICommand event : events) {
					if (lastTime != event.getTimestamp2() && workingSet.contains(lastTime)) {
						workingSet.remove(lastTime);
						line.clear();
						line.add(id);
						line.add(format(lastTime+1));
						line.add("Working");
						line.add(student);
						String[] nextLine = line.toArray(new String[1]);
						cw.writeNext(nextLine);
					}
					line.clear();
					line.add(id);
					line.add(format(event.getTimestamp2()));
					line.add(event.getDataMap().get("testcase") + "_" + event.getDataMap().get("type"));
					line.add(student);
					String[] nextLine = line.toArray(new String[1]);
					cw.writeNext(nextLine);
					lastTime = event.getTimestamp2();
				}
			}
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
			Set<Long> workingSet = new HashSet<>();
//			assign = assign.substring(assign.lastIndexOf(File.separator)+1);
			for (String student : data.keySet()) {
				System.out.println("Generating LocalCheckPassEvents for student " + student);
				List<List<EHICommand>> nestedCommands = data.get(student);
				student = student.substring(student.lastIndexOf(File.separator)+1);
				String id = assign + '_' + student;
				long time = 0;
				int numEdits = 0;
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

						if (command instanceof LocalCheckCommand && command.getDataMap().get("type").equals("passed") ) {
							if (numEdits > 0) {
								workingSet.add(time);
							}
							numEdits = 0;
							String testcase = command.getDataMap().get("testcase");
//							String type = command.getDataMap().get("type");
//							if (!localCheckEvents.containsKey(testcase) 
//							|| (!localCheckEvents.get(testcase).getDataMap().get("type").equals("passed") 
//							&& !localCheckEvents.get(testcase).getDataMap().get("type").equals(type))) {
								command.setTimestamp2(time);
								localCheckEvents.put(testcase, command);
//							}
						} else if (getEventType(command).equals("Edit")) {
							numEdits++;
						}
						lastTimestamp = timestamp;
					}
				}
				List<EHICommand> events = new ArrayList<>();
				events.addAll(localCheckEvents.values());
				sortEvents(events);
				List<String> line = new ArrayList<>();
				long lastTime = 0;
				for (EHICommand event : events) {
					if (lastTime != event.getTimestamp2() && workingSet.contains(lastTime)) {
						workingSet.remove(lastTime);
						line.clear();
						line.add(id);
						line.add(format(lastTime+1));
						line.add("Working");
						line.add(student);
						String[] nextLine = line.toArray(new String[1]);
						cw.writeNext(nextLine);
					}
					line.clear();
					line.add(id);
					line.add(format(event.getTimestamp2()));
					line.add(event.getDataMap().get("testcase") + "_" + event.getDataMap().get("type"));
					line.add(student);
					String[] nextLine = line.toArray(new String[1]);
					cw.writeNext(nextLine);
					lastTime = event.getTimestamp2();
				}
			}
			fw.close();
			cw.close();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}
	
	public void createLocalCheckSuiteEvents(String assign, Map<String, List<List<EHICommand>>> data) {
		File temp = new File(assign);
		assign = temp.getName();
		String course = temp.getParentFile().getName();
		File csv = new File(outputFolder + File.separator + course + File.separator + assign +"LocalCheckSuiteEvents(FactorySuite).csv");
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
			Assignment assignment = assignMap.get(assign);
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

						if (command instanceof LocalCheckCommand && command.getDataMap().get("type").equals("passed") ) {
							String testcase = command.getDataMap().get("testcase");
							command.setTimestamp2(time);
							localCheckEvents.put(testcase, command);
						}
						lastTimestamp = timestamp;
					}
				}
				List<EHICommand> events = new ArrayList<>();
				events.addAll(localCheckEvents.values());
				sortEvents(events);
				List<String> line = new ArrayList<>();
				assignment.reset();
				for (EHICommand event : events) {
					List<Suite> suites = null;
					String[] nextLine = null;
					long timestamp = event.getTimestamp2();
					if ((suites = assignment.pass(event.getDataMap().get("testcase"))) != null) {
						for (Suite s : suites) {
							line.clear();
							line.add(id);
							if (s.hasEC() && s.passedEC()) {
								line.add(format(timestamp));
								line.add(s.getName()+"(EC)");
								s.ecPrint();
							} else if (s.hasRC() && s.passedRC()){
								line.add(format(timestamp));
								line.add(s.getName());
								s.print();
							}
							line.add(student);
							nextLine = line.toArray(new String[1]);
							cw.writeNext(nextLine);
						}
						if (assignment.rcPassed() && !assignment.printed()) {
							line.clear();
							line.add(id);
							line.add(format(timestamp));
							line.add("All Regular Credit Passed");
							line.add(student);
							nextLine = line.toArray(new String[1]);
							cw.writeNext(nextLine);
							assignment.print();
						}
						if (assignment.ecPassed() && !assignment.ecPrinted()) {
							line.clear();
							line.add(id);
							line.add(format(timestamp));
							line.add("All Extra Credit Passed");
							line.add(student);
							nextLine = line.toArray(new String[1]);
							cw.writeNext(nextLine);
							assignment.ecPrint();
						}
						if (assignment.rcPassed() && assignment.ecPassed()) {
							line.clear();
							line.add(id);
							line.add(format(timestamp));
							line.add("All Suites Passed");
							line.add(student);
							nextLine = line.toArray(new String[1]);
							cw.writeNext(nextLine);
							assignment.ecPrint();
						}
					}
				}
			}
			fw.close();
			cw.close();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}
	
	public Map<String, Long> localCheckRegularCredit(String assign, Map<String, List<List<EHICommand>>> data) {
		Map<String, Long> rcPassTime = new HashMap<>();
			Assignment assignment = assignMap.get(assign);
			for (String student : data.keySet()) {
				System.out.println("Generating LocalCheckPassEvents for student " + student);
				List<List<EHICommand>> nestedCommands = data.get(student);
				student = student.substring(student.lastIndexOf(File.separator)+1);
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

						if (command instanceof LocalCheckCommand && command.getDataMap().get("type").equals("passed") ) {
							String testcase = command.getDataMap().get("testcase");
							command.setTimestamp2(time);
							localCheckEvents.put(testcase, command);
						}
						lastTimestamp = timestamp;
					}
				}
				List<EHICommand> events = new ArrayList<>();
				events.addAll(localCheckEvents.values());
				sortEvents(events);
				assignment.reset();
				
				for (EHICommand event : events) {
					List<Suite> suites = null;
					long timestamp = event.getTimestamp2();
					if ((suites = assignment.pass(event.getDataMap().get("testcase"))) != null) {
						for (Suite s : suites) {
							if (s.hasEC() && s.passedEC()) {
							} else if (s.hasRC() && s.passedRC()){
							}
						}
						if (assignment.rcPassed() && !assignment.printed()) {
							rcPassTime.put(student, timestamp);
						}
					}
				}
			}
			return rcPassTime;
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
	
	public void createPauseDistribution(String assign, Map<String, List<List<EHICommand>>> data) {
		File csv = new File(assign+"PauseDistribution.csv");
		System.out.println("Generating Pause Distribution for Assignment " + assign);
		FileWriter fw;
		try {
			if (csv.exists()) {
				csv.delete();
			}
			csv.createNewFile();
			fw = new FileWriter(csv);
			CSVWriter cw = new CSVWriter(fw);
			String[] header = new String[PauseCommand.TYPES.length+1];
			header[0] = "";
			for (int i = 0; i < PauseCommand.TYPES.length; i++) {
				header[i+1] = PauseCommand.TYPES[i];
			}
			cw.writeNext(header);

			String[] nextLine = new String[header.length];
			
			assign = assign.substring(assign.lastIndexOf(File.separator)+1);
			long[] max = new long[PauseCommand.TYPES.length];
			for (String student : data.keySet()) {
				List<List<EHICommand>> nestedCommands = data.get(student);
				if (nestedCommands.size() == 0) continue;
				student = student.substring(student.lastIndexOf(File.separator)+1);
				for (List<EHICommand> commands : nestedCommands) {
					for (EHICommand command : commands) {
						if (command instanceof PauseCommand) {
							for (int i = 0; i < max.length; i++) {
								if (command.getDataMap().get("prevType").equals(PauseCommand.TYPES[i])) {
									long pause = Long.parseLong(command.getDataMap().get("pause"));
									if (max[i] < pause) {
										max[i] = pause;
									}
									break;
								}
							}
						}
					}
				}
			}
			nextLine = new String[header.length];
			nextLine[0] = "Prev";
			for (int i = 0; i < max.length; i++) {
				nextLine[i+1] = format(max[i]);
			}
			cw.writeNext(nextLine);
			
			
			max = new long[PauseCommand.TYPES.length];
			for (String student : data.keySet()) {
				List<List<EHICommand>> nestedCommands = data.get(student);
				if (nestedCommands.size() == 0) continue;
				student = student.substring(student.lastIndexOf(File.separator)+1);
				for (List<EHICommand> commands : nestedCommands) {
					for (EHICommand command : commands) {
						if (command instanceof PauseCommand) {
							for (int i = 0; i < max.length; i++) {
								if (command.getDataMap().get("nextType").equals(PauseCommand.TYPES[i])) {
									long pause = Long.parseLong(command.getDataMap().get("pause"));
									if (max[i] < pause) {
										max[i] = pause;
									}
									break;
								}
							}
						}
					}
				}
			}
			nextLine = new String[header.length];
			nextLine[0] = "Next";
			for (int i = 0; i < max.length; i++) {
				nextLine[i+1] = format(max[i]);
			}
			cw.writeNext(nextLine);
			
			fw.close();
			cw.close();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}
}
