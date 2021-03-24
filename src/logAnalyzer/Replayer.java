package logAnalyzer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import dayton.ellwanger.helpbutton.exceptionMatcher.ExceptionMatcher;
import dayton.ellwanger.helpbutton.exceptionMatcher.JavaExceptionMatcher;
import dayton.ellwanger.helpbutton.exceptionMatcher.PrologExceptionMatcher;
import dayton.ellwanger.helpbutton.exceptionMatcher.SMLExceptionMatcher;
import fluorite.commands.ConsoleInput;
import fluorite.commands.ConsoleOutputCommand;
import fluorite.commands.CopyCommand;
import fluorite.commands.Delete;
import fluorite.commands.EHExceptionCommand;
import fluorite.commands.EHICommand;
import fluorite.commands.ExceptionCommand;
import fluorite.commands.GetHelpCommand;
import fluorite.commands.InsertStringCommand;
import fluorite.commands.LocalCheckCommand;
import fluorite.commands.PasteCommand;
import fluorite.commands.Replace;
import fluorite.commands.RequestHelpCommand;
import fluorite.commands.PauseCommand;
import fluorite.commands.RunCommand;
import fluorite.commands.ShellCommand;
import fluorite.commands.WebCommand;
import fluorite.util.EHLogReader;
import generators.CommandGenerator;
import generators.LocalCheckCommandGenerator;
import generators.PauseCommandGenerator;

public abstract class Replayer{
	public static final int PAUSE = 0;
	public static final int LOCALCHECK = 1;
	public static final String LOCALCHECK_EVENTS = "C:\\Users\\Zhizhou\\git\\Hermes_Log_Parser\\NewVersionTest\\";
	public static final String REST_INSESSION = "Rest(In Session)";
	public static final String REST_ENDSESSION = "Rest(End Session)";
	public static final String REST_LOSEFOCUS = "Rest(Lose Focus)";
	public static final String ECLIPSE_LOST_FOCUS = "ECLIPSE_LOST_FOCUS";
	public static final String ECLIPSE_CLOSED = "ECLIPSE_CLOSED";
	public static final String XML_START1 = "<Events startTimestamp=\"";
	public static final String XML_START2 = "\" logVersion=\"";
	public static final String XML_VERSION = "1.0.0.202008151525";
	public static final String XML_START3 = "\">\r\n";
	public static final String XML_FILE_ENDING = "\r\n</Events>"; 
	public static final long ONE_SECOND = 1000;
	public static final long ONE_MIN = 60*1000;
	public static final long TEN_MIN = 10*ONE_MIN;
	public static final long FIVE_MIN = 5*ONE_MIN;
	public static final long HALF_MIN = ONE_MIN/2;
	public static final long TWO_MIN = 2*ONE_MIN;
	public static final long DAY = 24*60*ONE_MIN;
	public static final long[] REST = {ONE_SECOND, 2*ONE_SECOND, 5*ONE_SECOND, 10*ONE_SECOND, 15*ONE_SECOND, HALF_MIN, ONE_MIN, TWO_MIN, FIVE_MIN, TEN_MIN, 2*TEN_MIN, 3*TEN_MIN, 9*FIVE_MIN, 6*TEN_MIN};
	private ExceptionMatcher[] ems = {JavaExceptionMatcher.getInstance(), PrologExceptionMatcher.getInstance(), SMLExceptionMatcher.getInstance()};
	private Map<String, String> logToWrite;
	private EHLogReader reader;
	protected File root;
	
	public Replayer() {
		logToWrite = new TreeMap<>();
		reader = new EHLogReader();
	}
	
	public abstract void readLogs(String path);
	
	public Map<String, List<EHICommand>> readStudent(File student) {
		System.out.println("Reading student " + student);
		if (!student.exists()) {
			System.out.println("Folder " + student + " does not exist");
			return null;
		}
		File logFolder = null;
		File submission = new File(student,"Submission attachment(s)");
		if (submission.exists()) {
			logFolder = new File(getProjectFolder(submission), "Logs"+File.separator+"Eclipse");
		} else {
			logFolder = new File(student, "Eclipse");
		}
		if (!logFolder.exists()) {
			System.out.println("No logs found for student " + student.getName());
			return null;
		}
		File[] logFiles = logFolder.listFiles(File::isDirectory);
		if (logFiles != null && logFiles.length > 0) {
			logFiles = logFiles[0].listFiles((file)->{return file.getName().startsWith("Log") && file.getName().endsWith(".xml");});
		} else {
			logFiles = logFolder.listFiles((file)->{return file.getName().startsWith("Log") && file.getName().endsWith(".xml");});
		}
		if (logFiles == null) {
			System.out.println("No logs found for student " + student.getName());
			return null;
		}
		Map<String, List<EHICommand>> logs = new TreeMap<>();
		for (File logFile : logFiles) {
			List<EHICommand> ret = readOneLogFile(logFile);
			if (ret != null) {
				logs.put(logFile.getPath(), ret);
			}
		}
		return logs;
	}
	
	public List<EHICommand> readOneLogFile(File log){
		String path = log.getPath();
		System.out.println("Reading file " + path);
		if (!log.exists()) {
			System.out.println("log does not exist:" + path);
			return null;
		}
		if (!path.endsWith(".xml")) {
			System.out.println("log is not in xml format:" + path);
			return null;
		}
		try {
			List<EHICommand> commands = reader.readAll(path);
			sortCommands(commands, 0, commands.size()-1);
			return commands;
		} catch (Exception e) {
			System.out.println("Could not read file" + path + e);
		}
		return null;
	}
	
	protected Map<String, List<String[]>> readLocalCheckEvents(String assign) {
		Map<String, List<String[]>> localCheckEvents = new HashMap<>();
		Pattern pattern = Pattern.compile("(\\d)+");
		Matcher logMatcher = null;
		Matcher eventsMatcher = null;
		try {
			File folder = new File(LOCALCHECK_EVENTS);
			File[] files = folder.listFiles((f)->{return f.getName().endsWith(".csv");});
			for (File file : files) {
				logMatcher = pattern.matcher(assign.substring(assign.lastIndexOf(File.separator)));
				eventsMatcher = pattern.matcher(file.getName());
				if (!(logMatcher.find() && eventsMatcher.find() && logMatcher.group(0).equals(eventsMatcher.group(0)))) {
					continue;
				}
				CSVReader cr = new CSVReader(new FileReader(file));
				String[] nextLine = cr.readNext();
				List<String[]> studentLC = null;
				while ((nextLine = cr.readNext()) != null) {
					if (!localCheckEvents.containsKey(assign+File.separator+nextLine[3]+","+nextLine[4])) {
						studentLC = new ArrayList<>();
						localCheckEvents.put(assign+File.separator+nextLine[3]+","+nextLine[4], studentLC);
					}
					studentLC.add(nextLine);
				}
				cr.close();
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} 
		return localCheckEvents;
	}
	
	public String readWebContent(String aURL) {
		String content = "";
		Scanner sc = null;
		try {
			URL url = new URL(aURL);
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
		
		return content;
	}
	
	public void createExtraCommand(String surfix, int mode) {
		CountDownLatch latch = new CountDownLatch(countStudents());
		createExtraCommand(latch, surfix, mode);
		try {
			latch.await();
			writeLogs(surfix);
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			System.out.println("Done!");
		}
	}
	
	public abstract void createExtraCommand(CountDownLatch latch, String surfix, int mode);

	public void createExtraCommandStudent(CountDownLatch latch, Map<String, List<EHICommand>> studentLog, String surfix, int mode, List<String[]> localCheckEvents) {
		CommandGenerator cg;
		if (mode == LOCALCHECK && localCheckEvents != null) {
			cg = new LocalCheckCommandGenerator(this, latch, studentLog, localCheckEvents);
		} else {
			cg = new PauseCommandGenerator(this, latch, studentLog);
		}
		new Thread(cg).start();
	}
	
	public abstract int countStudents();
	
	public abstract int countAssignments();
	
	public synchronized void updateLogMap(String path, String logContent) {
		logToWrite.put(path, logContent);
	}
	
	private void writeLogs(String surfix) {
		for (String fileName : logToWrite.keySet()) {
			try {
				File file = new File(fileName);
				if (file.getParentFile().getName().contains("Eclipse")) {
					file = new File(file.getParent()+File.separator+surfix+File.separator+file.getName());
				}
				System.out.println("Writing to file " + file.getPath());
				if (file.exists()) {
					file.delete();
				}
				file.getParentFile().mkdirs();
				file.createNewFile();
				BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
				writer.write(logToWrite.get(fileName));
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void analyze() {
		CountDownLatch latch = new CountDownLatch(countAssignments());
		analyze(latch);
		try {
			latch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			System.out.println("Done!");
		}
	}
	
	public abstract void analyze(CountDownLatch latch);
	
	public void createAssignData(String assign, Map<String, List<List<EHICommand>>> data) {
		File csv = new File(assign+".csv");
		FileWriter fw;
		File csv2 = new File(assign+"Event.csv");
		FileWriter fw2;
		int[] sum = new int[11];
		try {
			if (csv.exists()) {
				csv.delete();
			}
			csv.createNewFile();
			fw = new FileWriter(csv);
			CSVWriter cw = new CSVWriter(fw);
			String[] header = {"Student", "Total Time Spent", "Active Time", 
							   "Rest Time", "Wall Clock Time", "Pause", "Web", 
							   "Insert", "Delete", "Replace", "Copy", "Paste", 
							   "Run", "Exception", "LocalChecks", "Exception Breakdown"};
			cw.writeNext(header);
			
			if (csv2.exists()) {
				csv2.delete();
			}
			csv2.createNewFile();
			fw2 = new FileWriter(csv2);
			CSVWriter cw2 = new CSVWriter(fw2);
			List<String[]> output = new ArrayList<>();
			String[] header2 = {"case_id", "timestamp", "activity", "user"};
			cw2.writeNext(header2);
			assign = assign.substring(assign.lastIndexOf(File.separator)+1);
			for (String student : data.keySet()) {
				System.out.println("Generating AssignData for student " + student);
				List<List<EHICommand>> nestedCommands = data.get(student);
				student = student.substring(student.lastIndexOf(File.separator)+1);
				
				List<String> retVal = new ArrayList<>();
				retVal.add(student);
				long totalTime = totalTimeSpent(nestedCommands);
				if (totalTime == 0) {
					continue;
				}
				long[] restTime = restTime(nestedCommands, FIVE_MIN, Long.MAX_VALUE);
				long wallClockTime = wallClockTime(nestedCommands);
				retVal.add(convertToHourMinuteSecond(totalTime));
				retVal.add(convertToHourMinuteSecond(totalTime - restTime[0]));
				retVal.add(convertToHourMinuteSecond(restTime[0]));
				retVal.add(convertToHourMinuteSecond(wallClockTime));
				int[] numCommands = new int[10];
				List<String> breakdownList = new ArrayList<>();
				StringBuffer list = new StringBuffer();
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
						if (lastTime - curTime > FIVE_MIN) {
							addOneLine(output, assign, lastTime, REST_INSESSION, student);
						}
						if (command instanceof ShellCommand && ((ShellCommand)command).getAttributesMap().get("type").equals(ECLIPSE_LOST_FOCUS)) {
							addOneLine(output, assign, curTime, REST_LOSEFOCUS, student);
						} else if (command instanceof ShellCommand && ((ShellCommand)command).getAttributesMap().get("type").equals(ECLIPSE_CLOSED)) {
							addOneLine(output, assign, curTime, REST_ENDSESSION, student);
						} else if (command instanceof InsertStringCommand || command instanceof CopyCommand ||
								command instanceof Delete ||
								command instanceof Replace || command instanceof PasteCommand || command instanceof ExceptionCommand ||
								command instanceof RunCommand || command instanceof ConsoleOutputCommand || command instanceof ConsoleInput ||
								command instanceof RequestHelpCommand || command instanceof GetHelpCommand || command instanceof LocalCheckCommand) {
							addOneLine(output, assign, curTime, getEventType(command), student);
						}
						if (command instanceof PauseCommand) {
							numCommands[0]++;
							list.append("A");
						}
						if (command instanceof WebCommand) {
							numCommands[1]++;
							list.append("W");
						}
						if (command instanceof InsertStringCommand) {
							numCommands[2]++;
							list.append("I");
						}
						if (command instanceof Delete) {
							numCommands[3]++;
							list.append("D");
						}
						if (command instanceof Replace) {
							numCommands[4]++;
							list.append("R");
						}
						if (command instanceof CopyCommand) {
							numCommands[5]++;
							list.append("C");
						}
						if (command instanceof PasteCommand) {
							numCommands[6]++;
							list.append("P");
						}
						if (command instanceof RunCommand) {
							numCommands[7]++;
							list.append("U");
						}
						if (command instanceof LocalCheckCommand) {
							numCommands[9]++;
							list.append("L");
						}
						if (command instanceof ExceptionCommand || command instanceof EHExceptionCommand) {
							if(isException(command)) {
								if (command instanceof EHExceptionCommand) {
									ExceptionCommand ex = new ExceptionCommand(command.getDataMap().get("exceptionString"),"");
									ex.setTimestamp(command.getTimestamp());
									ex.setStartTimestamp(command.getStartTimestamp());
								}
								numCommands[8]++;
								list.append("E");
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
										(command instanceof ShellCommand && ((ShellCommand)command).getAttributesMap().get("type").equals("ECLIPSE_CLOSED")) ||
										command instanceof LocalCheckCommand) {
										if (i > 0) {
											lastTime = curTime;
										}
										curTime = command.getTimestamp() + command.getStartTimestamp();
										if (lastTime - curTime > FIVE_MIN) {
											addOneLine(output, assign, lastTime, REST_INSESSION, student);
										}
										if (command instanceof ShellCommand && ((ShellCommand)command).getAttributesMap().get("type").equals(ECLIPSE_LOST_FOCUS)) {
											addOneLine(output, assign, curTime, REST_LOSEFOCUS, student);
										} else if (command instanceof ShellCommand && ((ShellCommand)command).getAttributesMap().get("type").equals(ECLIPSE_CLOSED)) {
											addOneLine(output, assign, curTime, REST_ENDSESSION, student);
										} else {
											addOneLine(output, assign, curTime, getEventType(command), student);
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
											if (lastTime - curTime > FIVE_MIN) {
												addOneLine(output, assign, lastTime, REST_INSESSION, student);
											}
											addOneLine(output, assign, curTime, getEventType(ex), student);
										}
										numCommands[8]++;
										list.append("E");
									}
									i++;
									if (i+1 < commands.size()) {
										command = commands.get(i+1);
									} else {
										break;
									}
								}
								breakdownList.add(countConsecutiveCommands(list));
								list = new StringBuffer();
							}
						}
					}
				}
				if (list.length() != 0) {
					breakdownList.add(countConsecutiveCommands(list));
				}
				for(int i = 0; i < numCommands.length; i++) {
					retVal.add(i+5, numCommands[i]+"");
					sum[i] += numCommands[i];
					sum[numCommands.length] += numCommands[i];
				}
				for(int i = 0; i < breakdownList.size(); i++) {
					retVal.add(i+5+numCommands.length, breakdownList.get(i));
				}
				String[] nextLine = retVal.toArray(new String[1]);
				cw.writeNext(nextLine);
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
			cw2.writeAll(output);
			fw.close();
			cw.close();
			
			fw2.close();
			cw2.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void createDistributionData(String assign, Map<String, List<List<EHICommand>>> data) {
		File csv = new File(assign+"Distribution.csv");
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
			sum[0] = "Sum";
			int[] restSum = new int[REST.length];
			long[] restTimeSum = new long[REST.length];
			for (int i = 1; i < sum.length; i++) {
				sum[i] = "";
			}
			assign = assign.substring(assign.lastIndexOf(File.separator)+1);
			for (String student : data.keySet()) {
				System.out.println("Generating DistributionData for student " + student);
				List<List<EHICommand>> nestedCommands = data.get(student);
				student = student.substring(student.lastIndexOf(File.separator)+1);
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
	
	public void createPauseDistribution(String assign, Map<String, List<List<EHICommand>>> data) {
		File csv = new File(assign+"PauseDistribution.csv");
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
			assign = assign.substring(assign.lastIndexOf(File.separator)+1);
			for (String student : data.keySet()) {
				List<List<EHICommand>> nestedCommands = data.get(student);
				if (nestedCommands.size() == 0) continue;
				student = student.substring(student.lastIndexOf(File.separator)+1);
				List<String> retVal = new ArrayList<>();
				int[] numCommmands = new int[sum.length];
				long[] pauseTimes = new long[sum.length];
				long[] max = new long[sum.length];
				long[] min = new long[sum.length];
				double[] mean = new double[sum.length];
				List<List<Long>> pauses = new ArrayList<>();
				for (int i = 0; i < PauseCommand.TYPES.length; i++) {
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
					retVal.add(std(pauses.get(i),mean[i])+"");
					
					sum[i] += numCommmands[i];
					sumPause[i] += pauseTimes[i];
				}
				nextLine = retVal.toArray(new String[1]);
				cw.writeNext(nextLine);
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
			
			String[] empty = {};
			cw.writeNext(empty);
			String[] next = {"next"};
			cw.writeNext(next);
			sum = new int[PauseCommand.TYPES.length];
			
			sumPause = new long[sum.length];
			
			for (String student : data.keySet()) {
				List<List<EHICommand>> nestedCommands = data.get(student);
				if (nestedCommands.size() == 0) continue;
				student = student.substring(student.lastIndexOf(File.separator)+1);
				List<String> retVal = new ArrayList<>();
				int[] numCommmands = new int[sum.length];
				long[] pauseTimes = new long[sum.length];
				long[] max = new long[sum.length];
				long[] min = new long[sum.length];
				double[] mean = new double[sum.length];
				List<List<Long>> pauses = new ArrayList<>();
				for (int i = 0; i < PauseCommand.TYPES.length; i++) {
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
	
	public void createWebStats(String assign, File folder, Map<String, List<List<EHICommand>>> data) {
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
				System.out.println("Generating WebStats for student " + student);
				EHICommand lastSearch = null;
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
	
	public double std(List<Long> pauses, double mean) {
		double sum = 0;
		for (Long l : pauses) {
			sum += Math.pow((1.0 * l) - mean,2);
		}
		return Math.sqrt(sum/pauses.size());
	}
	
	public void addOneLine(List<String[]> output, String assign, long time, String type, String pid) {
		String[] nextLine = new String[4]; 
		nextLine[0] = assign + " " + pid;
		Date date = new Date(time);
		DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		nextLine[1] = df.format(date);
		nextLine[2] = type;
		nextLine[3] = pid;
		if (nextLine != null) {
			output.add(nextLine);
		}
	}
	
	public String countConsecutiveCommands(StringBuffer list) {
		char lastChar = ' ';
		char curChar = ' ';
		int count = 1;
		StringBuffer retVal = new StringBuffer();
		for (int i = 0; i < list.length(); i++) {
			if (i != 0) {
				lastChar = curChar;
			}
			curChar = list.charAt(i);
			if (curChar == lastChar){
				count++;
			} else if (i != 0){
				retVal.append(lastChar + "" + count);
				count = 1;
			}
		}
		retVal.append(curChar + " "+ count);
		return retVal.toString();
	}
	
	protected void sortNestedCommands(List<List<EHICommand>> nestedCommands){
		for (int i = 0; i < nestedCommands.size(); i++) {
			List<EHICommand> commands = nestedCommands.get(i);
			if (commands == null || commands.size() < 2) {
				nestedCommands.remove(i);
				i--;
			} else if (commands.size() > 2) {
				sortCommands(commands, 0, commands.size()-1);
			}
		}
	}
	
	private void sortCommands(List<EHICommand> commands, int start, int end){
		for(int i = 0; i < commands.size(); i++) {
			if (commands.get(i) == null) {
				commands.remove(i);
				i--;
			}
		}
		EHICommand command = null;
		long cur = 0;
		for(int i = 2; i < commands.size(); i++) {
			command = commands.get(i);
			cur = command.getStartTimestamp()+command.getTimestamp();
			int j = i-1;
			while (j > 1){
				if (commands.get(j).getStartTimestamp() + commands.get(j).getTimestamp() > cur) {
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

	protected long getLogFileCreationTime(File file) {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS");
		try {
			return df.parse(file.getName().substring(3, 27)).getTime();
		} catch (ParseException e) {
			e.printStackTrace();
			return 0;
		}
	}
	
	public String[] getPauseHeader() {
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
	
	protected String getEventType(EHICommand command) {
		if (command instanceof InsertStringCommand || 
			command instanceof CopyCommand ||
			command instanceof Delete ||
			command instanceof Replace || command instanceof PasteCommand) {
			return "Edit";
		}
		if (command instanceof RunCommand || command instanceof ConsoleOutputCommand || command instanceof ConsoleInput || command instanceof EHExceptionCommand) {
			return "IO";
		}
		if (command instanceof ExceptionCommand) {
			return "Exception";
		}
		if (command instanceof RequestHelpCommand || command instanceof GetHelpCommand) {
			return "Request";
		}
		if (command instanceof LocalCheckCommand) {
			return "LocalCheck";
		}
		return null;
	}
	
	public File getProjectFolder(File folder) {
		for (File file : folder.listFiles(File::isDirectory)) {
			if (file.getName().equals("src")) {
				return folder;
			} 
		}
		for (File file : folder.listFiles(File::isDirectory)) {
			return getProjectFolder(file);
		}
		return folder;
	}
	
	protected String[] getHeader() {
		String[] header = new String[4*REST.length+5];
		header[0] = "Student";
		header[1] = "Total Time Spent";
		header[2] = "Wall Clock Time";
		for (int i = 0; i < REST.length; i++) {
			String t = getTime(REST[i]);
			header[3+i*4] = "Active Time(" + t + ")";
			header[4+i*4] = "Rest Time(" + t + ")";
			header[5+i*4] = "# of Rests(" + t + ")";
			header[6+i*4] = "Avg. Rest Time(" + t + ")";
		}
		header[header.length-2] = "# of Days";
		header[header.length-1] = "Time Spent Each Day";
		return header;
	}
	
	protected String getTime(long t) {
		long ret = t / ONE_SECOND;
		if (ret < 60) {
			return ret + "s";
		}
		ret /= 60;
		if (ret < 60) {
			return ret + "m";
		}
		double ret2 = ret / 60.0;
		return ret2 + "h";
	}
	
	protected String getTime(double t) {
		double ret = t / ONE_SECOND;
		DecimalFormat df = new DecimalFormat("#.###");
		if (ret < 60) {
			return df.format(ret);
		}
		ret /= 60;
		if (ret < 60) {
			return df.format(ret);
		}
		ret /= 60.0;
		return df.format(ret);
	}

	protected long totalTimeSpent(List<List<EHICommand>> nestedCommands){
		long projectTime = 0;
		for(int k = 0; k < nestedCommands.size(); k++) {
			List<EHICommand> commands = nestedCommands.get(k);
			if (commands.size() == 0) {
				continue;
			}
			int j = 0;
			for(; j < commands.size(); j++) {
				if (commands.get(j).getStartTimestamp() > 0 || commands.get(j).getTimestamp() > 0) {
					break;
				}
			}
			long timestamp1 = commands.get(j).getTimestamp() + commands.get(j).getStartTimestamp();
			EHICommand command2 = commands.get(commands.size()-1);
			long timestamp2 = command2.getStartTimestamp() + command2.getTimestamp();
			projectTime += timestamp2 - timestamp1;
		}
		return projectTime;
	}
	
	protected long[] restTime(List<List<EHICommand>> nestedCommands, long time, long time2) {
		long[] restTime = {0,0,0};
		for (List<EHICommand> commands : nestedCommands) {
			for (EHICommand command : commands) {
				if (command instanceof PauseCommand) {
					long pause = Long.parseLong(command.getDataMap().get("pause"));
					if (pause > time) {
						restTime[0] += pause;
						if (pause < time2) {
							restTime[1]++;
							restTime[2] += pause;
						}
					}
				}
			}
		}
		return restTime;
	}
	
	protected long wallClockTime(List<List<EHICommand>> nestedCommands) {
		long wallClockTime = 0;
		EHICommand c1 = null;
		EHICommand c2 = null;
		if (nestedCommands.size() == 0) {
			return 0;
		}
		for (int j = 0; j < nestedCommands.get(0).size(); j++) {
			c1 = nestedCommands.get(0).get(j);
			if (c1.getStartTimestamp() > 0 || c1.getTimestamp() > 0) {
				break;
			}
		}
		if (c1 == null) {
			return 0;
		}
		c2 = nestedCommands.get(nestedCommands.size()-1).get(nestedCommands.get(nestedCommands.size()-1).size()-1);
		wallClockTime = c2.getStartTimestamp()+c2.getTimestamp()-c1.getStartTimestamp()-c1.getTimestamp();
		return wallClockTime;
	}
	
	protected String[] daysSpent(List<List<EHICommand>> nestedCommands) {
		List<String> retVal = new ArrayList<>();
		int days = 1;
		long startTime = 0;
		long endTime = 0;
		List<EHICommand> commands = nestedCommands.get(0);
		EHICommand command = null;
		List<List<EHICommand>> nestedCommands2 = new ArrayList<>();
		List<EHICommand> commands2 = null;
		for (int i = 1; i < commands.size(); i++) {
			command = commands.get(i);
			if (command.getStartTimestamp() != 0) {
				break;
			}
		}
		startTime = command.getStartTimestamp();
		startTime -= startTime % DAY;
		commands = nestedCommands.get(nestedCommands.size()-1);
		endTime = command.getStartTimestamp() + command.getTimestamp();
		endTime = endTime - endTime % DAY + DAY;
		long timeStamp = 0;
		for (int i = 0; i < nestedCommands.size(); i++) {
			commands = nestedCommands.get(i);
			commands2 = new ArrayList<>();
			nestedCommands2.add(commands2);
			for (int j = 0; j < commands.size(); j++) {
				command = commands.get(j);
				timeStamp = command.getStartTimestamp()+command.getTimestamp();
				if (timeStamp == 0) {
					continue;
				}
				if (timeStamp > startTime && timeStamp < (startTime+DAY)) {
					commands2.add(command);
				} else if (timeStamp >= (startTime+DAY)){
					days++;
					startTime = timeStamp - timeStamp % DAY;
					if (nestedCommands2.size() > 0 && nestedCommands2.get(0).size() > 0) {
						retVal.add(convertToHourMinuteSecond(totalTimeSpent(nestedCommands2)));
					}
					nestedCommands2 = new ArrayList<>();
					commands2 = new ArrayList<>();
					nestedCommands2.add(commands2);
					commands2.add(command);
				}
			}
		}
		if (nestedCommands2.size() > 0 && nestedCommands2.get(0).size() > 0) {
			retVal.add(convertToHourMinuteSecond(totalTimeSpent(nestedCommands2)));
		}
		retVal.add(0,days+"");
		return retVal.toArray(new String[1]);
	}

	protected String convertToHourMinuteSecond(long timeSpent){
		int hour = (int) (timeSpent / 3600000);
		int minute = (int) (timeSpent % 3600000 / 60000);
		int second = (int) (timeSpent % 60000 / 1000);
		return hour + ":" + minute + ":" + second;
	}

	protected boolean isException(EHICommand command) {
		String output = "";
		String language = "";
		if (command instanceof EHExceptionCommand) {
			output = command.getDataMap().get("outputString");
		} else {
			output = command.getDataMap().get("exceptionString");
			language = command.getDataMap().get("language");
		}
		for (ExceptionMatcher em : ems) {
			if ((language.equals("") || language.equals(em.getLanguage())) && em.isException(output)) {
				return true;
			}
		}
		return false;
	}
	
	public boolean isProvided(String s) {
		for (String url : WebCommand.PROVIDED_URL) {
			if (s.equals(url)) {
				return true;
			}
		}
		return false;
	}

	public abstract void delete(String path);
	
	public void deleteStudent(File student) {
		System.out.println("Deleting student " + student);
		if (!student.exists()) {
			System.out.println("Folder " + student + " does not exist");
			return;
		}
		File logFolder = null;
		File submission = new File(student,"Submission attachment(s)");
		if (submission.exists()) {
			logFolder = new File(getProjectFolder(submission), "Logs"+File.separator+"Eclipse");
		} else {
			logFolder = new File(student, "Eclipse");
		}
		if (!logFolder.exists()) {
			System.out.println("No logs found for student " + student.getName());
			return;
		}
		File[] generatedLogs = logFolder.listFiles(File::isDirectory);
		if (generatedLogs != null && generatedLogs.length > 0) {
			for (File file : generatedLogs) {
				deleteFolder(file);
			}
		}
	}
	
	public void deleteFolder(File folder) {
		System.out.println("Deleting folder " + folder.getName());
		for (File file : folder.listFiles()) {
			if (file.isDirectory()) {
				deleteFolder(file);
			} else {
				System.out.println("Deleting file " + file.getName());
				file.delete();
			}
		}
		folder.delete();
	}
}
