package logAnalyzer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import analyzer.Analyzer;
import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import fluorite.commands.ConsoleInput;
import fluorite.commands.ConsoleOutputCommand;
import fluorite.commands.CopyCommand;
import fluorite.commands.Delete;
import fluorite.commands.EHExceptionCommand;
import fluorite.commands.EHICommand;
import fluorite.commands.ExceptionCommand;
import fluorite.commands.GetHelpCommand;
import fluorite.commands.InsertStringCommand;
import fluorite.commands.PasteCommand;
import fluorite.commands.PauseCommand;
import fluorite.commands.Replace;
import fluorite.commands.RequestHelpCommand;
import fluorite.commands.RunCommand;
import fluorite.commands.ShellCommand;
import fluorite.commands.WebCommand;

public class AExperimentalReplayer extends AReplayer{
	private Map<String, List<List<EHICommand>>> data;
	private static final int THREAD_LIM = 6;
	private static final String TIMESTAMP = "C:\\Users\\Zhizhou\\Desktop\\timestamp for each participant.csv";
	private Map<String, Long[]> timestamps = new HashMap<>();
	
	public AExperimentalReplayer(Analyzer anAnalyzer) {
		super(anAnalyzer);
		analyzer = anAnalyzer;
		data = new HashMap<>();
		System.setProperty("http.agent", "Chrome");
	}
	
	public void readTimestamp() {
		try {
			SimpleDateFormat df = new SimpleDateFormat("HH:mm");
			File timestampFile = new File(TIMESTAMP);
			CSVReader cr = new CSVReader(new FileReader(timestampFile));
			cr.readNext();
			String[] token = null;
			while ((token = cr.readNext()) != null) {
				long startTime = df.parse(token[4]).getTime();
				if (startTime < 15*3600*1000) {
					startTime += 12*3600*1000;
				}
				long endTime = df.parse(token[5]).getTime();
				if (endTime < 15*3600*1000) {
					endTime += 12*3600*1000;
				}
				Long[] times = {startTime, endTime};
				timestamps.put(token[0], times);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void createPauseCommandLogs(String classFolderPath) {
		File folder = new File(classFolderPath);
		if (!folder.exists()) {
			System.out.println("Class Folder does not exist");
			System.exit(0);
		}
		File[] students = folder.listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				return pathname.isDirectory();
			}
		});
		latch = new CountDownLatch(students.length);
		for (int j = 0; j < students.length; j++) {
			File studentFolder  = students[j];
			File logFolder = new File(studentFolder,"Eclipse");
			if (!logFolder.exists()) {
				latch.countDown();
				continue;
			}
			System.out.println("Reading " + studentFolder.getName());
			File[] logs = new File(logFolder.getPath()).listFiles(new FileFilter() {
				public boolean accept(File pathname) {
					return pathname.getName().startsWith("Log") && pathname.getName().endsWith(".xml");
				}
			});
			Thread thread = new Thread(new Runnable() {
				public void run() {
					if (logs == null) {
						synchronized (this) {
							threadCount--;
						}
						latch.countDown();
						return;
					}
//					if (!studentFolder.getName().equals("16")) {
//						synchronized (this) {
//							threadCount--;
//						}
//						latch.countDown();
//						return;
//					}
					try {
						File browser = new File(studentFolder, "Browser");
						List<EHICommand> webCommands = null;
						Iterator<EHICommand> webCommandIterator = null;
						if (browser.exists() && browser.listFiles().length != 0) {
							webCommands = readWebCommands(browser.listFiles()[0]);
							if (webCommands != null) {
								webCommandIterator = webCommands.iterator();
							}
						}
						EHICommand webCommand = null;
						for (File file : logs) {
							
							long startTimestamp = getLogFileCreationTime(file);
							long logStartTimestamp = startTimestamp;
//							long endTimestamp = Long.MAX_VALUE;
//							if (timestamps.containsKey(studentFolder.getName())) {
//								startTimestamp = startTimestamp / DAY * DAY + timestamps.get(studentFolder.getName())[0] - 60*ONE_MIN;
//								endTimestamp = startTimestamp / DAY * DAY + timestamps.get(studentFolder.getName())[1] - 59*ONE_MIN;
//							}
							List<EHICommand> commands = readOneLogFile(file.getPath(), analyzer);
							if (commands.size() < 2) {
								continue;
							}
							List<EHICommand> newCommands = new ArrayList<>();
							EHICommand last = null;
							EHICommand cur = null;
							if (webCommand == null) {
//								webCommand = maybeAddWebCommandBeforeLogs(webCommandIterator, startTimestamp, newCommands);
								webCommand = webCommandIterator.next();
							}
							long timestamp = 0;
							if (webCommand != null) {
								timestamp = webCommand.getTimestamp() - logStartTimestamp;
							}
							for (EHICommand command : commands) {
//								if (command.getTimestamp() + logStartTimestamp < startTimestamp || command.getTimestamp() + logStartTimestamp > endTimestamp) {
//									continue;
//								}
								command.setStartTimestamp(logStartTimestamp);
								if (cur == null) {
									cur = command;
									newCommands.add(command);
								} else {
									last = cur;
									cur = command;
									while (webCommand != null && timestamp >= last.getTimestamp() && timestamp <= cur.getTimestamp()) {
										webCommand.setStartTimestamp(logStartTimestamp);
										webCommand.setTimestamp(timestamp);
										maybeAddPauseCommand(newCommands, last,	webCommand);
										if (webCommandIterator.hasNext()) {
											last = webCommand;
											webCommand = webCommandIterator.next();
											timestamp = webCommand.getTimestamp() - logStartTimestamp;
										} else {
											webCommand = null;
											break;
										}
									}
									maybeAddPauseCommand(newCommands, last, cur);
								}
							}
							
							String logContent = XML_START1 + getLogFileCreationTime(file) + XML_START2 + XML_VERSION + XML_START3;
							for (EHICommand c : newCommands) {
								logContent += c.persist();
							}
							logContent += XML_FILE_ENDING;
							try {
								File newLog = new File(file.getParent()+File.separator+"RestOld"+File.separator+file.getName());
								if (newLog.exists()) {
									newLog.delete();
								}
								if (newCommands.size() == 0) {
									continue;
								}
								newLog.getParentFile().mkdirs();
								newLog.createNewFile();
								BufferedWriter writer = new BufferedWriter(new FileWriter(newLog, true));
								System.out.println("Writing to file " + newLog.getPath());
								writer.write(logContent);
								writer.close();
								System.out.println("Finished writing to file " + newLog.getPath());
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
						
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						synchronized (this) {
							threadCount--;
						}
						latch.countDown();
					}
				}							
			});
			while(true) {
				if (threadCount > THREAD_LIM) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				
				if (threadCount <= THREAD_LIM) {
					synchronized (this) {
						threadCount++;
						thread.start();
						break;
					}
				}
			}
		}
		try {
			latch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			System.exit(0);
		}
	}

	public void analyzeFolder(String classFolderPath) {
		File folder = new File(classFolderPath);
		if (!folder.exists()) {
			System.exit(0);
		}
		File[] participants = folder.listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				return pathname.isDirectory();
			}
		});
		for (int i = 0; i < participants.length; i++) {
			File participantFolder = new File(participants[i],"Eclipse");
			if (!participantFolder.exists()) {
				continue;
			}
			System.out.println("Reading student " + participantFolder.getName());
			data.put(participants[i].getName(), replayLogs(participantFolder.getPath(), analyzer));
		}
		List<List<EHICommand>> nestedCommands = data.get("26");
		for (int i = 0; i < nestedCommands.size(); i++) {
			for (EHICommand command:nestedCommands.get(i)) {
				if (command instanceof ExceptionCommand) {
					System.out.println(command);
				}
			}
		}
//		createAssignData("Experiment", folder);
//		createDistributionData("Experiment", folder);
		createPrevPauseDistribution("Experiment", folder);
//		createNextPauseDistribution("Experiment", folder);
//		createWebStats("Experiment", folder);
		System.exit(0);
	}

	private void createAssignData(String assign, File folder) {
		File csv = new File(folder,assign+".csv");
		FileWriter fw;
		File csv2 = new File(folder,assign+"Event.csv");
		FileWriter fw2;
		int[] sum = new int[10];
		try {
			if (csv.exists()) {
				csv.delete();
			}
			csv.createNewFile();
			fw = new FileWriter(csv);
			CSVWriter cw = new CSVWriter(fw);
			String[] header = {"Student", "Total Time Spent", "Active Time", "Rest Time", "Wall Clock Time", "Pause", "Web Visit", "Insert", "Delete", "Replace", "Copy", "Paste", "Run", "Exception", "Exception Breakdown"};
			cw.writeNext(header);

			if (csv2.exists()) {
				csv2.delete();
			}
			csv2.createNewFile();
			fw2 = new FileWriter(csv2);
			CSVWriter cw2 = new CSVWriter(fw2);
			String[] header2 = {"case_id", "timestamp", "activity", "user"};
			cw2.writeNext(header2);

			for (String student : data.keySet()) {
				System.out.println("Writing " + assign + " student " + student + "to " + csv.getName());
				List<List<EHICommand>> nestedCommands = data.get(student);
				List<String> retVal = new ArrayList<>();
				retVal.add(student);
				long totalTime = totalTimeSpent(nestedCommands);
				long[] restTime = restTime(nestedCommands, FIVE_MIN, Long.MAX_VALUE);
				long wallClockTime = wallClockTime(nestedCommands);
				retVal.add(convertToHourMinuteSecond(totalTime));
				retVal.add(convertToHourMinuteSecond(totalTime - restTime[0]));
				retVal.add(convertToHourMinuteSecond(restTime[0]));
				retVal.add(convertToHourMinuteSecond(wallClockTime));
				int[] numCommands = new int[9];
				List<String> breakdownList = new ArrayList<>();
				String list = "";
				for (List<EHICommand> commands : nestedCommands) {
					long lastTime = -1;
					long curTime = -1;
					for (int i = 0; i < commands.size(); i++) {
						EHICommand command = commands.get(i);

						if (i > 0) {
							lastTime = curTime;
						}
						if (command != null) {
							curTime = command.getTimestamp() + command.getStartTimestamp();
						}
						if (lastTime - curTime > 5*60*1000) {
							writeOneLine(cw2, assign, lastTime, REST_INSESSION, student);
						}
						if (command instanceof ShellCommand && ((ShellCommand)command).getAttributesMap().get("type").equals(ECLIPSE_LOST_FOCUS)) {
							writeOneLine(cw2, assign, curTime, REST_LOSEFOCUS, student);
						} else if (command instanceof ShellCommand && ((ShellCommand)command).getAttributesMap().get("type").equals(ECLIPSE_CLOSED)) {
							writeOneLine(cw2, assign, curTime, REST_ENDSESSION, student);
						} else if (command instanceof InsertStringCommand || command instanceof CopyCommand ||
								command instanceof Delete ||
								command instanceof Replace || command instanceof PasteCommand || command instanceof ExceptionCommand ||
								command instanceof RunCommand || command instanceof ConsoleOutputCommand || command instanceof ConsoleInput ||
								command instanceof RequestHelpCommand || command instanceof GetHelpCommand) {
							writeOneLine(cw2, assign, curTime, getEventType(command), student);
						}
						if (command instanceof PauseCommand) {
							numCommands[0]++;
							list += "A";
						}
						if (command instanceof WebCommand) {
							numCommands[1]++;
							list += "W";
						}
						if (command instanceof InsertStringCommand) {
							numCommands[2]++;
							list += "I";
						}
						if (command instanceof Delete) {
							numCommands[3]++;
							list += "D";
						}
						if (command instanceof Replace) {
							numCommands[4]++;
							list += "R";
						}
						if (command instanceof CopyCommand) {
							numCommands[5]++;
							list += "C";
						}
						if (command instanceof PasteCommand) {
							numCommands[6]++;
							list += "P";
						}
						if (command instanceof RunCommand) {
							numCommands[7]++;
							list += "U";
						}
						if (command instanceof ExceptionCommand || command instanceof EHExceptionCommand) {
							if(isException(command)) {
								if (command instanceof EHExceptionCommand) {
									ExceptionCommand ex = new ExceptionCommand(command.getDataMap().get("exceptionString"),"");
									ex.setTimestamp(command.getTimestamp());
									ex.setStartTimestamp(command.getStartTimestamp());
								}
								numCommands[8]++;
								list += "E";
								if (i < commands.size()-1) {
									command = commands.get(i+1);
								} else {
									continue;
								}
								while(!(command instanceof InsertStringCommand || command instanceof Replace 
										|| command instanceof Delete 
										|| command instanceof CopyCommand || command instanceof PasteCommand)) {
									if (command instanceof RunCommand || command instanceof ConsoleOutputCommand || command instanceof ConsoleInput ||
											command instanceof RequestHelpCommand || command instanceof GetHelpCommand || command instanceof ExceptionCommand ||
											(command instanceof ShellCommand && ((ShellCommand)command).getAttributesMap().get("type").equals("ECLIPSE_LOST_FOCUS")) ||
											(command instanceof ShellCommand && ((ShellCommand)command).getAttributesMap().get("type").equals("ECLIPSE_CLOSED"))) {
										if (i > 0) {
											lastTime = curTime;
										}
										curTime = command.getTimestamp() + command.getStartTimestamp();
										if (lastTime - curTime > 5*60*1000) {
											writeOneLine(cw2, assign, lastTime, REST_INSESSION, student);
										}
										if (command instanceof ShellCommand && ((ShellCommand)command).getAttributesMap().get("type").equals(ECLIPSE_LOST_FOCUS)) {
											writeOneLine(cw2, assign, curTime, REST_LOSEFOCUS, student);
										} else if (command instanceof ShellCommand && ((ShellCommand)command).getAttributesMap().get("type").equals(ECLIPSE_CLOSED)) {
											writeOneLine(cw2, assign, curTime, REST_ENDSESSION, student);
										} else {
											writeOneLine(cw2, assign, curTime, getEventType(command), student);
										}
									}
									if ((command instanceof ExceptionCommand || command instanceof EHExceptionCommand) && isException(command)) {
										if (command instanceof EHExceptionCommand) {
											ExceptionCommand ex = new ExceptionCommand(command.getDataMap().get("exceptionString"),"");
											ex.setTimestamp(command.getTimestamp());
											ex.setStartTimestamp(command.getStartTimestamp());
											if (i > 0) {
												lastTime = curTime;
											}
											curTime = command.getTimestamp() + command.getStartTimestamp();
											if (lastTime - curTime > 5*60*1000) {
												writeOneLine(cw2, assign, lastTime, REST_INSESSION, student);
											}
											writeOneLine(cw2, assign, curTime, getEventType(ex), student);
										}
										numCommands[8]++;
										list += "E";
									}
									i++;
									if (i+1 < commands.size()) {
										command = commands.get(i+1);
									} else {
										break;
									}
								}
								breakdownList.add(countConsecutiveCommands(list));
								list = "";
							}
						}
					}
				}
				if (!list.equals("")) {
					breakdownList.add(countConsecutiveCommands(list));
				}
				for(int i = 0; i < numCommands.length; i++) {
					retVal.add(i+5, numCommands[i]+"");
					sum[i] += numCommands[i];
					if (i > 1) {
						sum[9] += numCommands[i];
					}
				}
				for(int i = 0; i < breakdownList.size(); i++) {
					retVal.add(i+14, breakdownList.get(i));
				}
				String[] nextLine = retVal.toArray(new String[1]);
				cw.writeNext(nextLine);
				
				System.out.println("written " + assign + " student " + student + "to " + csv.getName());
			}
			List<String> retVal = new ArrayList<>();
			retVal.add("Sum");
			retVal.add("");
			retVal.add("");
			retVal.add("");
			retVal.add("");
			for (int i = 0; i < sum.length; i++) {
				retVal.add(sum[i]+"");
			}
			String[] nextLine = retVal.toArray(new String[1]);
			cw.writeNext(nextLine);
			fw.close();
			cw.close();

			fw2.close();
			cw2.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void createDistributionData(String assign, File folder) {
		File csv = new File(folder,assign+"Distribution.csv");
		FileWriter fw;
		try {
			if (csv.exists()) {
				csv.delete();
			}
			csv.createNewFile();
			fw = new FileWriter(csv);
			CSVWriter cw = new CSVWriter(fw);
			String[] header = getHeader();
			cw.writeNext(header);
			
			String[] sum = new String[header.length];
			long[] restTimeSum = new long[REST.length];
			sum[0] = "Sum";
			for (int i = 1; i < sum.length; i++) {
				sum[i] = "";
			}
			int[] restSum = new int[REST.length];
			for (String student : data.keySet()) {
				System.out.println("Writing " + assign + " student " + student + " to " + csv.getName());
				List<List<EHICommand>> nestedCommands = data.get(student);
				List<String> retVal = new ArrayList<>();
				retVal.add(student);
				long totalTime = totalTimeSpent(nestedCommands);
				if (totalTime == 0) {
					continue;
				}
				long wallClockTime = wallClockTime(nestedCommands);
				retVal.add(convertToHourMinuteSecond(totalTime));
				retVal.add(convertToHourMinuteSecond(wallClockTime));

				long[] restTime = {0,0};
				for (int i = 0; i < REST.length; i++) {
					if (i < REST.length-1) {
						restTime = restTime(nestedCommands, REST[i], REST[i+1]);
					} else {
						restTime = restTime(nestedCommands, REST[i], Long.MAX_VALUE);
					}
					retVal.add(convertToHourMinuteSecond(totalTime - restTime[0]));
					retVal.add(convertToHourMinuteSecond(restTime[0]));
					retVal.add(restTime[1]+"");
					retVal.add(getTime(restTime[1] == 0? 0:1.0*restTime[2]/restTime[1]));
					restSum[i] += restTime[1];
					restTimeSum[i] += restTime[2];
				}
				
				String[] days = daysSpent(nestedCommands);
				if (days != null) {
					for (int i = 0; i < days.length; i++) {
						retVal.add(days[i]);
					}
				}
				
				String[] nextLine = retVal.toArray(new String[1]);
				cw.writeNext(nextLine);
				System.out.println("written " + assign + " student " + student + "to " + csv.getName());
			}
			int sum2 = 0;
			for (int i = 0; i < restSum.length; i++) {
				sum[4*i+5] = restSum[i]+"";
				sum2 += restSum[i];
				sum[4*i+6] = getTime(restSum[i] == 0? 0:1.0*restTimeSum[i]/restSum[i]);
			}
			sum[1] = sum2+"";
			cw.writeNext(sum);
			fw.close();
			cw.close();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}
	
	private void createPrevPauseDistribution(String assign, File folder) {
		File csv = new File(folder,assign+"PauseDistribution.csv");
		FileWriter fw;
		try {
			if (csv.exists()) {
				csv.delete();
			}
			csv.createNewFile();
			fw = new FileWriter(csv);
			CSVWriter cw = new CSVWriter(fw);
			String[] prev = {"Prev"};
			cw.writeNext(prev);
			String[] header = getPauseHeader();
			cw.writeNext(header);

			String[] nextLine = new String[header.length];
			int[] sum = new int[PauseCommand.TYPES.length];
			long[] sumPause = new long[sum.length];
			
			for (String student : data.keySet()) {
				System.out.println("Writing " + assign + " student " + student + " to " + csv.getName());
				List<List<EHICommand>> nestedCommands = data.get(student);
				List<String> retVal = new ArrayList<>();
				int[] numCommmands = new int[sum.length];
				long[] pauseTimes = new long[sum.length];
				long[] max = new long[sum.length];
				long[] min = new long[sum.length];
				double[] mean = new double[sum.length];
				List<List<Long>> pauses = new ArrayList<>();
				for (String s : PauseCommand.TYPES) {
					pauses.add(new ArrayList<>());
				}
				retVal.add(student);
				for (List<EHICommand> commands : nestedCommands) {
					for (EHICommand command : commands) {
						if (command instanceof PauseCommand) {
							for (int i = 0; i < sum.length; i++) {
								if (command.getDataMap().get("prevType").equals(PauseCommand.TYPES[i])) {
									numCommmands[i]++;
									long pause = Long.parseLong(command.getDataMap().get("pause"));
									pauses.get(i).add(pause);
									pauseTimes[i] += pause;
									if (min[i] == 0 || min[i] > pause) {
										min[i] = pause;
									} 
									if (max[i] < pause) {
										max[i] = pause;
									}
									break;
								}
							}
						}
					}
				}
				for (int i = 0; i < sum.length; i++) {
					retVal.add(numCommmands[i]+"");
					if (numCommmands[i] == 0) {
						retVal.add("0");
					} else {
						mean[i] = 1.0*pauseTimes[i]/numCommmands[i];
						retVal.add(mean[i]+"");
					}
					retVal.add(min[i]+"");
					retVal.add(max[i]+"");
					double std = std(pauses.get(i),mean[i]);
					if (std > 10773 && std < 10774) {
						int a = 0;
					}
					retVal.add(std(pauses.get(i),mean[i])+"");
					
					sum[i] += numCommmands[i];
					sumPause[i] += pauseTimes[i];
				}
//				String[] nextLine = retVal.toArray(new String[1]);
				nextLine = retVal.toArray(new String[1]);
				cw.writeNext(nextLine);
				System.out.println("written " + assign + " student " + student + "to " + csv.getName());
			}
//			String[] nextLine = new String[header.length];
			nextLine = new String[header.length];
			nextLine[0] = "Sum";
			for (int i = 0; i < sum.length; i++) {
				nextLine[1+i*5] = sum[i]+"";
				if (sum[i] == 0) {
					nextLine[2+i*5] = 0+"";
				} else {
					nextLine[2+i*5] = sumPause[i]/sum[i]+"";
				}
			}
			cw.writeNext(nextLine);
			
			String[] empty = {};
			cw.writeNext(empty);
			String[] next = {"next"};
			cw.writeNext(next);
			sum = new int[PauseCommand.TYPES.length];
			
			sumPause = new long[sum.length];
			
			for (String student : data.keySet()) {
				System.out.println("Writing " + assign + " student " + student + " to " + csv.getName());
				List<List<EHICommand>> nestedCommands = data.get(student);
				List<String> retVal = new ArrayList<>();
				int[] numCommmands = new int[sum.length];
				long[] pauseTimes = new long[sum.length];
				long[] max = new long[sum.length];
				long[] min = new long[sum.length];
				double[] mean = new double[sum.length];
				List<List<Long>> pauses = new ArrayList<>();
				for (String s : PauseCommand.TYPES) {
					pauses.add(new ArrayList<>());
				}
				retVal.add(student);
				
				for (List<EHICommand> commands : nestedCommands) {
					for (EHICommand command : commands) {
						if (command instanceof PauseCommand) {
							for (int i = 0; i < sum.length; i++) {
								if (command.getDataMap().get("nextType").equals(PauseCommand.TYPES[i])) {
									numCommmands[i]++;
									long pause = Long.parseLong(command.getDataMap().get("pause"));
									pauses.get(i).add(pause);
									pauseTimes[i] += pause;
									if (min[i] == 0 || min[i] > pause) {
										min[i] = pause;
									} 
									if (max[i] < pause) {
										max[i] = pause;
									}
									break;
								}
							}
						}
					}
				}
				for (int i = 0; i < sum.length; i++) {
					retVal.add(numCommmands[i]+"");
					if (numCommmands[i] == 0) {
						retVal.add("0");
					} else {
						mean[i] = 1.0*pauseTimes[i]/numCommmands[i];
						retVal.add(mean[i]+"");
					}
					retVal.add(min[i]+"");
					retVal.add(max[i]+"");
					retVal.add(std(pauses.get(i),mean[i])+"");
					sum[i] += numCommmands[i];
					sumPause[i] += pauseTimes[i];
				}
				nextLine = retVal.toArray(new String[1]);
				cw.writeNext(nextLine);
				System.out.println("written " + assign + " student " + student + "to " + csv.getName());
			}
			nextLine = new String[header.length];
			nextLine[0] = "Sum";
			for (int i = 0; i < sum.length; i++) {
				nextLine[1+i*5] = sum[i]+"";
				if (sum[i] == 0) {
					nextLine[2+i*5] = 0+"";
				} else {
					nextLine[2+i*5] = sumPause[i]/sum[i]+"";
				}
			}
			cw.writeNext(nextLine);
			
			fw.close();
			cw.close();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}
	
	private void createWebStats(String assign, File folder) {
		File csv = new File(folder,assign+"WebStats.csv");
		FileWriter fw;
		File csv2 = new File(folder,assign+"WebSearches.csv");
		FileWriter fw2;
		try {
			if (csv.exists()) {
				csv.delete();
			}
			if (csv2.exists()) {
				csv2.delete();
			}
			csv.createNewFile();
			csv2.createNewFile();
			fw = new FileWriter(csv);
			fw2 = new FileWriter(csv2);
			CSVWriter cw = new CSVWriter(fw);
			CSVWriter cw2 = new CSVWriter(fw2);
			String[] header = {"Title", "URL", "# of Visits", "Provided?"};
//			String[] header2 = {"ID", "Search Word", "Title", "URL", "Sequence", "Last Page of the Search?", "Pasted Text"};
			String[] header2 = {"Search Word"};
			cw.writeNext(header);
			cw2.writeNext(header2);

			Map<String, Integer> urls = new HashMap<>();
			Map<String, String> titles = new HashMap<>();
			Map<String, List<String>> searches = new LinkedHashMap<>();
			Map<String, List<String>> contents = new HashMap<>();
			for (String student : data.keySet()) {
				EHICommand lastSearch = null;
				System.out.println("Writing " + assign + " student " + student + " to " + csv.getName());
				List<List<EHICommand>> nestedCommands = data.get(student);
				for (List<EHICommand> commands : nestedCommands) {
					for (int i = 0; i < commands.size(); i++) {
						EHICommand command = commands.get(i);
						if (command instanceof WebCommand) {
							String url = command.getDataMap().get("URL");
							if (command.getAttributesMap().get("type").equals("Search")) {
								lastSearch = command;
								if (!searches.containsKey(lastSearch.getDataMap().get("keyword"))) {
									searches.put(lastSearch.getDataMap().get("keyword"), new ArrayList<>());
								}
							} 
							if (command.getAttributesMap().get("type").equals("Search Result") || command.getAttributesMap().get("type").equals("Stack Overflow")) {
								String searchWord = "null";
								if (lastSearch != null) {
									searchWord = lastSearch.getDataMap().get("keyword");
								}
								if (!searches.containsKey(searchWord)) {
									searches.put(searchWord, new ArrayList<>());
								}
								searches.get(searchWord).add(url);
//								if (!contents.containsKey(url)) {
//									List<String> list = new ArrayList<>();
//									if (!url.contains("google.com")) {
//										list.add(readWebContent(url));
////										list.add(readWebContent2(url));
//										contents.put(url, list);
//									}
//								}
							}
							if (command.getAttributesMap().get("type").contains("Provided")) {
//								if (!searches.containsKey("Provided")) {
//									searches.put("Provided", new ArrayList<>());
//								}
//								searches.get("Provided").add(url);
//								if (!contents.containsKey(url)) {
//									List<String> list = new ArrayList<>();
//									list.add(readWebContent(url));
////									list.add(readWebContent2(url));
//									contents.put(url, list);
//								}
							}
							urls.merge(url, 1, Integer::sum);
							titles.put(url, command.getDataMap().get("keyword"));
						}
						if (command instanceof PasteCommand) {
							outer:
							for (int j = i-1; j >= 0 && j > i-20; j--) {
								EHICommand command2 = commands.get(j);
								String pastedText = "";
								if (command2 instanceof InsertStringCommand || command2 instanceof Replace) {
									if (command2 instanceof InsertStringCommand) {
										pastedText = command2.getDataMap().get(InsertStringCommand.XML_Data_Tag);
									} else {
										pastedText = command2.getDataMap().get("insertedText");
									}
									if (pastedText.length() > 10) {
										for (String url : contents.keySet()) {
											List<String> list = contents.get(url);
											String pastedText2 = pastedText.replaceAll("\\s", "");
											if (list.get(0).contains(pastedText2)) {
												for (int k = 1; k < list.size(); k++) {
													if (list.get(k).replaceAll("\\s",  "").equals(pastedText2)) {
														break outer;
													}
												}
												list.add(pastedText);
												break outer;
											}
										}
									}
								}
							}
						}
					}
				}
				for (String s : searches.keySet()) {
					List<String> nextLine = new ArrayList<>();
//					nextLine.add(student);
					if (s.contains(" - Google Search")) {
						s = s.substring(0,s.indexOf(" - Google Search"));
					} 
					nextLine.add(s);
					cw2.writeNext(nextLine.toArray(new String[1]));

//					if (searches.get(s).size() == 0) {
//						nextLine.add("No Result");
//						cw2.writeNext(nextLine.toArray(new String[1]));
//						continue;
//					}
//					for (int i = 0; i < searches.get(s).size(); i++) {
//						String url = searches.get(s).get(i);
//						nextLine.add(titles.get(url));
//						nextLine.add(url);
//						nextLine.add(i+1+"");
//						nextLine.add(i == searches.get(s).size()-1 ? "Last" : "");
//						if (contents.containsKey(url)) {
//							List<String> list = contents.get(url);
//							for (int j = 1; j < list.size(); j++) {
//								nextLine.add(list.get(j));
//							}
//						}
//						cw2.writeNext(nextLine.toArray(new String[1]));
//						nextLine = new ArrayList<>();
//						nextLine.add(student);
//						nextLine.add("");
//					}
				}
				searches.clear();
			}
			Map<String, Integer> sortedMap = new LinkedHashMap<>();
			urls.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).forEachOrdered(x -> sortedMap.put(x.getKey(), x.getValue()));
			for (String s : sortedMap.keySet()) {
				String[] nextLine = {titles.get(s), s, sortedMap.get(s)+"", isProvided(s)? "Provided":""};
				cw.writeNext(nextLine);
			}
			fw.close();
			cw.close();
			
			fw2.close();
			cw2.close();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}
	
	private void createNextPauseDistribution(String assign, File folder) {
		File csv = new File(folder,assign+"NextPauseDistribution.csv");
		FileWriter fw;
		try {
			if (csv.exists()) {
				csv.delete();
			}
			csv.createNewFile();
			fw = new FileWriter(csv);
			CSVWriter cw = new CSVWriter(fw);
			String[] header = getPauseHeader();
			cw.writeNext(header);
			
			int[] sum = new int[PauseCommand.TYPES.length];
			long[] sumPause = new long[sum.length];
			for (String student : data.keySet()) {
				System.out.println("Writing " + assign + " student " + student + " to " + csv.getName());
				List<List<EHICommand>> nestedCommands = data.get(student);
				List<String> retVal = new ArrayList<>();
				int[] numCommmands = new int[sum.length];
				long[] pauseTimes = new long[sum.length];
				retVal.add(student);
				for (List<EHICommand> commands : nestedCommands) {
					for (EHICommand command : commands) {
						if (command instanceof PauseCommand) {
							for (int i = 0; i < sum.length; i++) {
								if (command.getDataMap().get("nextType").equals(PauseCommand.TYPES[i])) {
									numCommmands[i]++;
									pauseTimes[i] += Long.parseLong(command.getDataMap().get("pause")); 
									break;
								}
							}
						}
					}
				}
				for (int i = 0; i < sum.length; i++) {
					retVal.add(numCommmands[i]+"");
					if (numCommmands[i] == 0) {
						retVal.add("0");
					} else {
						retVal.add(pauseTimes[i]/numCommmands[i]+"");
					}
					sum[i] += numCommmands[i];
					sumPause[i] += pauseTimes[i];
				}
				String[] nextLine = retVal.toArray(new String[1]);
				cw.writeNext(nextLine);
				System.out.println("written " + assign + " student " + student + "to " + csv.getName());
			}
			String[] nextLine = new String[header.length];
			nextLine[0] = "Sum";
			for (int i = 0; i < sum.length; i++) {
				nextLine[1+i*2] = sum[i]+"";
				if (sum[i] == 0) {
					nextLine[2+i*2] = 0+"";
				} else {
					nextLine[2+i*2] = sumPause[i]/sum[i]+"";
				}
			}
			cw.writeNext(nextLine);
			fw.close();
			cw.close();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}
	
	private String[] getPauseHeader() {
		List<String> retVal = new ArrayList<>();
		retVal.add("ID");
		for (String s : PauseCommand.TYPES) {
			retVal.add(s);
			retVal.add("Avg. Time");
			retVal.add("Shortest Time");
			retVal.add("Longest Time");
			retVal.add("STD");
		}
		return retVal.toArray(new String[1]);
	}
	
	private void writeOneLine(CSVWriter cw, String assign, long time, String type, String pid) {
		String[] nextLine = new String[4]; 
		nextLine[0] = assign + " " + pid;
		Date date = new Date(time);
		DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		nextLine[1] = df.format(date);
		nextLine[2] = type;
		nextLine[3] = pid;
		if (nextLine != null) {
			cw.writeNext(nextLine);
		}
	}
	
	public List<List<EHICommand>> replayLogs(String projectPath, Analyzer analyzer){
		List<List<EHICommand>> nestedCommands = analyzer.convertXMLLogToObjects(projectPath+File.separator+"Rest");
		sortNestedCommands(nestedCommands);
		return nestedCommands;
	}

	private void maybeAddPauseCommand(List<EHICommand> newCommands, EHICommand last, EHICommand cur) {
		long rest = cur.getTimestamp()-last.getTimestamp();
		if (rest >= 1*ONE_SECOND) {
			PauseCommand rCommnad = new PauseCommand(last, cur, rest);
			rCommnad.setStartTimestamp(last.getStartTimestamp());
			rCommnad.setTimestamp(last.getTimestamp()+1);
			newCommands.add(rCommnad);
		} 
		newCommands.add(cur);
	}
	
	private EHICommand maybeAddWebCommandBeforeLogs(Iterator<EHICommand> iterator, long startTimestamp, List<EHICommand> commands) {
		if (iterator == null) {
			return null;
		}
		EHICommand webCommand = null;
		long timestamp = 0;
		while((webCommand = iterator.next()) != null && (timestamp = webCommand.getTimestamp() - startTimestamp) < 0) {
			webCommand.setStartTimestamp(0);
			webCommand.setTimestamp(timestamp);
			commands.add(webCommand);
		}
		return webCommand;
	}

	public String readWebContent(String aURL) {
		String content = "";
		Scanner sc = null;
		try {
			URL url = new URL(aURL);
//			System.out.println("accessing webpage " + aURL);
			URLConnection con = url.openConnection();
			con.setReadTimeout(5000);
			sc = new Scanner(con.getInputStream());
			StringBuffer sb = new StringBuffer();
			while (sc.hasNext()) {
				sb.append(sc.next());
			}
			content = sb.toString();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (sc != null) {
				sc.close();
			}
		}
		if (content.equals("") && !aURL.substring(0,5).equals("https")) {
			aURL = aURL.replaceFirst("http", "https");
			return readWebContent(aURL);
		}
		
//		System.out.println("webpage content " + aURL + content);
//		return content.replaceAll("<[^>]*>", "").replaceAll("\\s", "");
		return content;
	}
	
	private boolean isProvided(String s) {
		for (String url : WebCommand.PROVIDED_URL) {
			if (s.equals(url)) {
				return true;
			}
		}
		return false;
	}
	
//	public String readWebContent2(String url){
//		try {
//			HttpURLConnection httpClient =(HttpURLConnection) new URL(url).openConnection();
//			httpClient.setRequestMethod("GET");
//			httpClient.setRequestProperty("User-Agent", "Mozilla/5.0");
//			int responseCode = httpClient.getResponseCode();
//			try (BufferedReader in = new BufferedReader(
//					new InputStreamReader(httpClient.getInputStream()))) {
//				StringBuilder response = new StringBuilder();
//				String line;
//				while ((line = in.readLine()) != null) {
//					response.append(line);
//				}
//				if (response.toString().equals("") && !url.substring(0,5).equals("https")) {
//					url = url.replaceFirst("http", "https");
//					return readWebContent2(url);
//				}	
//				return response.toString();
//			}
//		} catch (Exception e) {
//			// TODO: handle exception
//			e.printStackTrace();
//		}
//		return "";
//	}
}
