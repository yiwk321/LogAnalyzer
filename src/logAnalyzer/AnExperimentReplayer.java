package logAnalyzer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import fluorite.commands.EHICommand;
import fluorite.commands.EclipseCommand;
import fluorite.commands.FileOpenCommand;
import fluorite.commands.FileSaveCommand;
import fluorite.commands.PauseCommand;
import fluorite.commands.RunCommand;

public class AnExperimentReplayer extends AContextBasedReplayer {
	Map<String, Long> startTimeMap = new HashMap<>();
	Map<String, Long> endTimeMap = new HashMap<>();
	
	
	public AnExperimentReplayer(double multiplier, int defaultPauseTime) {
		super(multiplier, defaultPauseTime);
		readTimes();
	}
	
	protected void readTimes() {
		try {
			CSVReader cr = new CSVReader(new FileReader(new File("C:\\Users\\Zhizhou\\Desktop\\hermes\\timestamp for each participant.csv")));
			String[] line = cr.readNext();
			while ((line = cr.readNext()) != null) {
				startTimeMap.put(line[0], Long.parseLong(line[7]));
				endTimeMap.put(line[0], Long.parseLong(line[8]));
			}
			cr.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	protected long totalTimeSpent(String student){
		return endTimeMap.get(student)-startTimeMap.get(student);
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

			for (String student : data.keySet()) {
				String studentNum = new File(student).getName();
				if (studentNum.contains("pd") || Integer.parseInt(studentNum) >= 30 
//						|| studentNum.equals("25") || studentNum.equals("26")
						) {
					continue;
				}
				System.out.println("Generating DistributionData for student " + student);
				List<List<EHICommand>> nestedCommands = data.get(student);
				student = student.substring(student.lastIndexOf(File.separator)+1);
				List<String> line = new ArrayList<>();
				line.add(student);
				long totalTime = totalTimeSpent(studentNum);
				if (totalTime == 0) {
					continue;
				}
				long wallClockTime = wallClockTime(nestedCommands);
				line.add(format(totalTime));
				line.add(Math.round(Math.ceil((double)wallClockTime/DAY))+"");

				long[] restTime = restTime(nestedCommands, defaultPauseTime, studentNum);
				line.add(format(totalTime - restTime[0]));
				line.add(format(totalTime - restTime[1]));
				line.add(format(restTime[1]-restTime[0]));
				long compileActiveTime = retinaActiveTime(nestedCommands, studentNum);
				line.add(format(compileActiveTime));
				line.add(format(totalTime - restTime[0] - compileActiveTime));
				line.add(format(restTime[0]));
				line.add(format(restTime[1]));
				String[] nextLine = line.toArray(new String[1]);
				cw.writeNext(nextLine);
			}
			fw.close();
			cw.close();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}
	
	protected long[] restTime(List<List<EHICommand>> nestedCommands, long time, String student) {
		long[] restTime = new long[4];
		long startTimestamp = nestedCommands.get(0).get(1).getStartTimestamp();
		for (List<EHICommand> commands : nestedCommands) {
			for (EHICommand command : commands) {
//				if (command.getTimestamp() < startTimeMap.get(student) || command.getTimestamp() > endTimeMap.get(student)) {
//					continue;
//				}
				if (!isWorking(startTimestamp, command, student)) {
					continue;
				}
				if (command instanceof PauseCommand) {
					long pause = Long.parseLong(command.getDataMap().get("pause"));
					String prevType = command.getDataMap().get("prevType");
					String nextType = command.getDataMap().get("nextType");
					if (pause > pauseMap.get(prevType) && pause > nextPauseMap.get(nextType)) {
						restTime[0] += pause;
						restTime[2]++;
					}
					if (pause > time) {
						restTime[1] += pause;
						restTime[3]++;
					}
				}
			}
		}
		return restTime;
	}

	
	protected long retinaActiveTime(List<List<EHICommand>> nestedCommands, String student) {
		long totalTime = 0;
		long sessionStartTime = 0;
		long lastCompileTime = 0;
		for (List<EHICommand> commands : nestedCommands) {
			for (EHICommand command : commands) {
				if (command.getTimestamp() < startTimeMap.get(student) || command.getTimestamp() > endTimeMap.get(student)) {
					continue;
				}
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
	
	public void split(String assign, Map<String, List<List<EHICommand>>> commands, int dataSetNum) {
		try {
			if (dataSetNum <= 0 || dataSetNum >= commands.size()) {
				return;
			}
			List<String> keys = new ArrayList<>();
			keys.addAll(commands.keySet());
//			Collections.shuffle(keys);
//			List<String> dataSet = keys.subList(0, dataSetNum);
//			List<String> testSet = keys.subList(dataSetNum, keys.size());
			File csv = new File(assign+"Split"+(commands.size()-dataSetNum)+".csv");
			FileWriter fw;
			CSVWriter cw;
//			if (csv.exists()) {
//				csv.delete();
//			}
			int index = 1;
			while (csv.exists()) {
				csv = new File(assign+"Split"+(commands.size()-dataSetNum)+"("+index+").csv");
				index++;
			}
			csv.createNewFile();
			fw = new FileWriter(csv, true);
			cw = new CSVWriter(fw);
			List<String> header = new ArrayList<>();
			header.add("Run");
			header.add("Average diff");
			cw.writeNext(header.toArray(new String[1]));
			for (int i = 0; i < 10; i++) {
				Collections.shuffle(keys);
				List<String> dataSet = keys.subList(0, dataSetNum);
				List<String> testSet = keys.subList(dataSetNum, keys.size());
				createThreshold(assign, commands, dataSet, cw);
				List<String> line = new ArrayList<>();
				line.add((i+1)+"");
//				cw.writeNext(line.toArray(new String[1]));
//				line.add("Subject");
//				line.add("Real Time");
//				line.add("Work Time (context)");
//				line.add("Rest Time (context)");
//				line.add("False Positives (context)");
//				line.add("Work Time (fixed)");
//				line.add("Rest Time (fixed)");
//				line.add("False Positives (fixed)");
//				cw.writeNext(line.toArray(new String[1]));
//				line.clear();
				
				List<Long> fixedDiff = new ArrayList<>();
				List<Long> contextDiff = new ArrayList<>();
				List<Long> fixedFP = new ArrayList<>();
				List<Long> contextFP = new ArrayList<>();
				List<Long> realTime = new ArrayList<>();
				
				for (String student : testSet) {
					List<List<EHICommand>> nestedCommands = commands.get(student);
					student = new File(student).getName();
//					line.add(student);
					long totalTime = totalTimeSpent(student);
					realTime.add(totalTime);
//					line.add(format(totalTime));
					long[] restTime = restTime(nestedCommands, defaultPauseTime, student);
					fixedDiff.add(restTime[1]);
					contextDiff.add(restTime[0]);
					fixedFP.add(restTime[3]);
					contextFP.add(restTime[2]);
					
//					line.add(format(totalTime-restTime[0]));
//					line.add(format(restTime[0]));
//					line.add(restTime[2]+"");
//					
//					line.add(format(totalTime-restTime[1]));
//					line.add(format(restTime[1]));
//					line.add(restTime[3]+"");
//					cw.writeNext(line.toArray(new String[1]));
//					line.clear();
				}
//				line.add("Average");
//				line.add("");
//				line.add("");
//				long contextDiffSum = 0;
//				for (Long diff : contextDiff) {
//					contextDiffSum += diff;
//				}
//				double avgContextDiff = 1.0 * contextDiffSum / contextDiff.size();
//				line.add(format((long)avgContextDiff));
//				
//				long contextFPSum = 0;
//				for (Long fp : contextFP) {
//					contextFPSum += fp;
//				}
//				double avgContextFP= 1.0 * contextFPSum / contextFP.size();
//				line.add(avgContextFP+"");
//				line.add("");
//				
//				long fixedDiffSum = 0;
//				for (Long diff : fixedDiff) {
//					fixedDiffSum += diff;
//				}
//				double avgFixedDiff = 1.0 * fixedDiffSum / fixedDiff.size();
//				line.add(format((long)avgFixedDiff));
//				
//				long fixedFPSum = 0;
//				for (Long fp : fixedFP) {
//					fixedFPSum += fp;
//				}
//				double avgFixedFP= 1.0 * fixedFPSum / fixedFP.size();
//				line.add(avgFixedFP+"");
//				
//				cw.writeNext(line.toArray(new String[1]));
//				line.clear();
				
//				line.add("STD");
//				line.add("");
//				line.add("");
//				line.add(format((long)std(contextDiff, avgContextDiff)));
//				line.add(std(contextFP, avgContextFP)+"");
//				line.add("");
//				line.add(format((long)std(fixedDiff, avgFixedDiff)));
//				line.add(std(fixedFP, avgFixedFP)+"");
//				cw.writeNext(line.toArray(new String[1]));
//				line.clear();
//				cw.writeNext(line.toArray(new String[1]));
//				cw.writeNext(line.toArray(new String[1]));
				
//				double contextDiffSum = 0;
//				for (int j = 0; j < contextDiff.size(); j++) {
//					contextDiffSum += 1.0 * contextDiff.get(j) / realTime.get(j);
//				}
//				double avgContextDiff = 1.0 * contextDiffSum / contextDiff.size();
//				line.add(avgContextDiff*100+"%");
//				cw.writeNext(line.toArray(new String[1]));
				
				double fixedDiffSum = 0;
				for (int j = 0; j < contextDiff.size(); j++) {
					fixedDiffSum += 1.0 * fixedDiff.get(j) / realTime.get(j);
				}
				double avgFixedDiff = 1.0 * fixedDiffSum / contextDiff.size();
				line.add(avgFixedDiff*100+"%");
				cw.writeNext(line.toArray(new String[1]));
			}

			cw.close();
			fw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void createThreshold(String assign, Map<String, List<List<EHICommand>>> data, List<String> dataSet, CSVWriter cw) {
		String[] header = new String[TYPES.length+1];
		header[0] = "";
		for (int i = 0; i < TYPES.length; i++) {
			header[i+1] = TYPES[i];
		}
//		cw.writeNext(header);
		String[] nextLine = new String[header.length];
		assign = assign.substring(assign.lastIndexOf(File.separator)+1);
		MaxTenList[] prevMaxTenLists = new MaxTenList[TYPES.length];
		MaxTenList[] nextMaxTenLists = new MaxTenList[TYPES.length];
		for (int i = 0; i < prevMaxTenLists.length; i++) {
			prevMaxTenLists[i] = new MaxTenList();
			nextMaxTenLists[i] = new MaxTenList(); 
		}
		for (String student : dataSet) {
			List<List<EHICommand>> nestedCommands = data.get(student);
			if (nestedCommands.size() == 0) continue;
			student = student.substring(student.lastIndexOf(File.separator)+1);
			long startTimestamp = nestedCommands.get(0).get(1).getStartTimestamp();
			for (List<EHICommand> commands : nestedCommands) {
				for (EHICommand command : commands) {
					if (!isWorking(startTimestamp, command, student)) {
						continue;
					}
					if (command instanceof PauseCommand) {
						for (int i = 0; i < TYPES.length; i++) {
							if (command.getDataMap().get("prevType").equals(PauseCommand.TYPES[i])) {
								long pause = Long.parseLong(command.getDataMap().get("pause"));
								prevMaxTenLists[i].add(pause);
								break;
							}
						}
						for (int i = 0; i < TYPES.length; i++) {
							if (command.getDataMap().get("nextType").equals(PauseCommand.TYPES[i])) {
								long pause = Long.parseLong(command.getDataMap().get("pause"));
								nextMaxTenLists[i].add(pause);
								break;
							}
						}
					}
				}
			}
		}
		nextLine[0] = "Prev";
		for (int i = 0; i < prevMaxTenLists.length; i++) {
			MaxTenList list = prevMaxTenLists[i];
			if (list.size() == 0) {
				nextLine[i+1] = defaultPauseTime+"";
				pauseMap.put(TYPES[i], defaultPauseTime);
			} else {
				nextLine[i+1] = list.get(list.size()/2)+"";
				pauseMap.put(TYPES[i], (long)(list.get(list.size()/2) * multiplier));
//				nextLine[i+1] = list.get(0)+"";
//				pauseMap.put(TYPES[i], (long)(list.get(0) * multiplier));
			}
		}
//		cw.writeNext(nextLine);
		nextLine[0] = "Next";
		for (int i = 0; i < nextMaxTenLists.length; i++) {
			MaxTenList list = nextMaxTenLists[i];
			if (list.size() == 0) {
				nextLine[i+1] = defaultPauseTime+"";
				nextPauseMap.put(TYPES[i], defaultPauseTime);
//			} else if (i == 6) {
//				nextLine[i+1] = list.get(0)+"";
//				nextPauseMap.put(TYPES[i], list.get(0));
			} else {
				nextLine[i+1] = list.get(list.size()/2)+"";
				nextPauseMap.put(TYPES[i], (long)(list.get(list.size()/2) * multiplier));
//				nextLine[i+1] = list.get(0)+"";
//				nextPauseMap.put(TYPES[i], (long)(list.get(0) * multiplier));
			}
		}
//		cw.writeNext(nextLine);
	}
	
/*	public void createPauseDistribution(String assign, Map<String, List<List<EHICommand>>> data, String skip) {
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
				String studentNum = new File(student).getName();
				if (studentNum.contains("pd") || Integer.parseInt(studentNum) >= 30) {
					continue;
				}
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
				String studentNum = new File(student).getName();
				if (studentNum.contains("pd") || Integer.parseInt(studentNum) >= 30) {
					continue;
				}
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
	}*/
	
	public void analyzeAssignment(CountDownLatch latch, String assign, Map<String, Map<String, List<EHICommand>>> assignLogs) {
		new Thread(()->{
			System.out.println("Analyzing " + assign.substring(assign.lastIndexOf(File.separator)+1));
			Map<String, List<List<EHICommand>>> commands = new HashMap<>();
			for (String student : assignLogs.keySet()) {
				commands.put(student, new ArrayList<List<EHICommand>>(assignLogs.get(student).values()));
			}
//			split(assign, commands, 10);
//			split(assign, commands, 7);
//			createLocalCheckSuiteEvents(assign, commands);
//			createPauseDistribution(assign, commands);
//			readPauseTime();
//			createDistributionData(assign, commands);
//			createLocalCheckPassEvents(assign, commands);
//			createBreakDistributionData(assign, commands);
//			createAssignData(assign, commands);
			latch.countDown();
		}).start();
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
	
//	public void createPauseDistribution(String assign, Map<String, List<List<EHICommand>>> data) {
//		File csv = new File(assign+"PauseDistribution.csv");
//		System.out.println("Generating Pause Distribution for Assignment " + assign);
//		FileWriter fw;
//		try {
//			if (csv.exists()) {
//				csv.delete();
//			}
//			csv.createNewFile();
//			fw = new FileWriter(csv);
//			CSVWriter cw = new CSVWriter(fw);
//			String[] prev = {"Prev"};
//			cw.writeNext(prev);
//			String[] header = getPauseHeader();
//			cw.writeNext(header);
//
//			String[] nextLine = new String[header.length];
//			int[] sum = new int[PauseCommand.TYPES.length];
//			long[] sumPause = new long[sum.length];
//			assign = assign.substring(assign.lastIndexOf(File.separator)+1);
//			for (String student : data.keySet()) {
//				List<List<EHICommand>> nestedCommands = data.get(student);
//				if (nestedCommands.size() == 0) continue;
//				student = student.substring(student.lastIndexOf(File.separator)+1);
//				if (student.contains("pd") || Integer.parseInt(student) >= 30) {
//					continue;
//				}
//				List<String> retVal = new ArrayList<>();
//				int[] numCommmands = new int[sum.length];
//				long[] pauseTimes = new long[sum.length];
//				long[] max = new long[sum.length];
//				long[] min = new long[sum.length];
//				double[] mean = new double[sum.length];
//				List<List<Long>> pauses = new ArrayList<>();
//				for (int i = 0; i < PauseCommand.TYPES.length; i++) {
//					pauses.add(new ArrayList<>());
//				}
//				retVal.add(student);
//				for (List<EHICommand> commands : nestedCommands) {
//					for (EHICommand command : commands) {
//						if (!isWorking(command, student)) {
//							continue;
//						}
//						if (command instanceof PauseCommand) {
//							for (int i = 0; i < sum.length; i++) {
//								if (command.getDataMap().get("prevType").equals(PauseCommand.TYPES[i])) {
//									numCommmands[i]++;
//									long pause = Long.parseLong(command.getDataMap().get("pause"));
//									pauses.get(i).add(pause);
//									pauseTimes[i] += pause;
//									if (min[i] == 0 || min[i] > pause) {
//										min[i] = pause;
//									} 
//									if (max[i] < pause) {
//										max[i] = pause;
//									}
//									break;
//								}
//							}
//						}
//					}
//				}
//				for (int i = 0; i < sum.length; i++) {
//					retVal.add(numCommmands[i]+"");
//					if (numCommmands[i] == 0) {
//						retVal.add("0");
//					} else {
//						mean[i] = 1.0*pauseTimes[i]/numCommmands[i];
//						retVal.add(mean[i]+"");
//					}
//					retVal.add(min[i]+"");
//					retVal.add(max[i]+"");
//					retVal.add(std(pauses.get(i),mean[i])+"");
//					
//					sum[i] += numCommmands[i];
//					sumPause[i] += pauseTimes[i];
//				}
//				nextLine = retVal.toArray(new String[1]);
//				cw.writeNext(nextLine);
//			}
//			nextLine = new String[header.length];
//			nextLine[0] = "Sum";
//			for (int i = 0; i < sum.length; i++) {
//				nextLine[1+i*5] = sum[i]+"";
//				if (sum[i] == 0) {
//					nextLine[2+i*5] = 0+"";
//				} else {
//					nextLine[2+i*5] = sumPause[i]/sum[i]+"";
//				}
//			}
//			cw.writeNext(nextLine);
//			
//			String[] empty = {};
//			cw.writeNext(empty);
//			String[] next = {"next"};
//			cw.writeNext(next);
//			sum = new int[PauseCommand.TYPES.length];
//			
//			sumPause = new long[sum.length];
//			
//			for (String student : data.keySet()) {
//				List<List<EHICommand>> nestedCommands = data.get(student);
//				if (nestedCommands.size() == 0) continue;
//				student = student.substring(student.lastIndexOf(File.separator)+1);
//				if (student.contains("pd") || Integer.parseInt(student) >= 30) {
//					continue;
//				}
//				List<String> retVal = new ArrayList<>();
//				int[] numCommmands = new int[sum.length];
//				long[] pauseTimes = new long[sum.length];
//				long[] max = new long[sum.length];
//				long[] min = new long[sum.length];
//				double[] mean = new double[sum.length];
//				List<List<Long>> pauses = new ArrayList<>();
//				for (int i = 0; i < PauseCommand.TYPES.length; i++) {
//					pauses.add(new ArrayList<>());
//				}
//				retVal.add(student);
//				
//				for (List<EHICommand> commands : nestedCommands) {
//					for (EHICommand command : commands) {
//						if (!isWorking(command, student)) {
//							continue;
//						}
//						if (command instanceof PauseCommand) {
//							for (int i = 0; i < sum.length; i++) {
//								if (command.getDataMap().get("nextType").equals(PauseCommand.TYPES[i])) {
//									numCommmands[i]++;
//									long pause = Long.parseLong(command.getDataMap().get("pause"));
//									pauses.get(i).add(pause);
//									pauseTimes[i] += pause;
//									if (min[i] == 0 || min[i] > pause) {
//										min[i] = pause;
//									} 
//									if (max[i] < pause) {
//										max[i] = pause;
//									}
//									break;
//								}
//							}
//						}
//					}
//				}
//				for (int i = 0; i < sum.length; i++) {
//					retVal.add(numCommmands[i]+"");
//					if (numCommmands[i] == 0) {
//						retVal.add("0");
//					} else {
//						mean[i] = 1.0*pauseTimes[i]/numCommmands[i];
//						retVal.add(mean[i]+"");
//					}
//					retVal.add(min[i]+"");
//					retVal.add(max[i]+"");
//					retVal.add(std(pauses.get(i),mean[i])+"");
//					sum[i] += numCommmands[i];
//					sumPause[i] += pauseTimes[i];
//				}
//				nextLine = retVal.toArray(new String[1]);
//				cw.writeNext(nextLine);
//			}
//			nextLine = new String[header.length];
//			nextLine[0] = "Sum";
//			for (int i = 0; i < sum.length; i++) {
//				nextLine[1+i*5] = sum[i]+"";
//				if (sum[i] == 0) {
//					nextLine[2+i*5] = 0+"";
//				} else {
//					nextLine[2+i*5] = sumPause[i]/sum[i]+"";
//				}
//			}
//			cw.writeNext(nextLine);
//			
//			fw.close();
//			cw.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		} 
//	}

	private boolean isWorking(long startTimestamp, EHICommand command, String student) {
//		return command.getTimestamp() >= startTimeMap.get(student) && command.getTimestamp() <= endTimeMap.get(student);
		return command.getStartTimestamp() + command.getTimestamp() >= startTimestamp + startTimeMap.get(student) && command.getStartTimestamp() + command.getTimestamp() <= startTimestamp + endTimeMap.get(student);
	}
}
