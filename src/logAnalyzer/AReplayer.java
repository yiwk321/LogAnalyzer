package logAnalyzer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import analyzer.Analyzer;
import analyzer.MainConsoleUI;
import analyzer.extension.ADifficultyPredictionAndStatusPrinter;
import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import dayton.ellwanger.helpbutton.exceptionMatcher.ExceptionMatcher;
import dayton.ellwanger.helpbutton.exceptionMatcher.JavaExceptionMatcher;
import dayton.ellwanger.helpbutton.exceptionMatcher.PrologExceptionMatcher;
import dayton.ellwanger.helpbutton.exceptionMatcher.SMLExceptionMatcher;
import fluorite.commands.ConsoleInput;
import fluorite.commands.ConsoleOutput;
import fluorite.commands.ConsoleOutputCommand;
import fluorite.commands.CopyCommand;
import fluorite.commands.Delete;
import fluorite.commands.EHExceptionCommand;
import fluorite.commands.EHICommand;
import fluorite.commands.EclipseCommand;
import fluorite.commands.ExceptionCommand;
import fluorite.commands.FileOpenCommand;
import fluorite.commands.GetHelpCommand;
import fluorite.commands.Insert;
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
import fluorite.util.EHUtilities;

public class AReplayer extends ADifficultyPredictionAndStatusPrinter{
	protected static final String LOCALCHECK_EVENTS = "C:\\Users\\Zhizhou\\Desktop\\events_524_f19";
	protected static final String REST_INSESSION = "Rest(In Session)";
	protected static final String REST_ENDSESSION = "Rest(End Session)";
	protected static final String REST_LOSEFOCUS = "Rest(Lose Focus)";
	protected static final String ECLIPSE_LOST_FOCUS = "ECLIPSE_LOST_FOCUS";
	protected static final String ECLIPSE_CLOSED = "ECLIPSE_CLOSED";
	protected static final String XML_START1 = "<Events startTimestamp=\"";
	protected static final String XML_START2 = "\" logVersion=\"";
	protected static final String XML_VERSION = "1.0.0.202008151525";
	protected static final String XML_START3 = "\">\r\n";
	protected static final String XML_FILE_ENDING = "\r\n</Events>"; 
	protected static final long ONE_SECOND = 1000;
	protected static final long ONE_MIN = 60*1000;
	protected static final long TEN_MIN = 10*ONE_MIN;
	protected static final long FIVE_MIN = 5*ONE_MIN;
	protected static final long HALF_MIN = ONE_MIN/2;
	protected static final long TWO_MIN = 2*ONE_MIN;
	protected static final long DAY = 24*60*ONE_MIN;
	protected static final long[] REST = {ONE_SECOND, 2*ONE_SECOND, 5*ONE_SECOND, 10*ONE_SECOND, 15*ONE_SECOND, HALF_MIN, ONE_MIN, TWO_MIN, FIVE_MIN, TEN_MIN, 2*TEN_MIN, 3*TEN_MIN, 9*FIVE_MIN, 6*TEN_MIN};
	protected int threadCount = 0;
	protected CountDownLatch latch;
	protected Analyzer analyzer;
	private static final int THREAD_LIM = 3;
	private int count = 0;
	private int currentExceptions = 0; 
	private int totalExceptions = 0;
	private List<List<List<String>>> metrics = null;
	private Map<String, Map<String, List<List<EHICommand>>>> data;
	private ExceptionMatcher[] ems = {JavaExceptionMatcher.getInstance(), PrologExceptionMatcher.getInstance(), SMLExceptionMatcher.getInstance()};

	public AReplayer(Analyzer anAnalyzer) {
		super(anAnalyzer);
		analyzer = anAnalyzer;
		data = new HashMap<>();
	}

	public void analyzeFolder(String classFolderPath) {
		File folder = new File(classFolderPath);
		if (!folder.exists()) {
			System.out.println("Class Folder does not exist");
			System.exit(0);
		}
		File[] assigns = folder.listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				return pathname.isDirectory();
			}
		});
		for (int i = 0; i < assigns.length; i++) {
			File assignFolder = assigns[i];
			data.put(assignFolder.getName(), new HashMap<>());
			File[] students = assignFolder.listFiles(new FileFilter() {
				public boolean accept(File pathname) {
					return pathname.isDirectory();
				}
			});
			for (int j = 0; j < students.length; j++) {
				File studentFolder  = students[j];
				File submissionFolder = new File(studentFolder,"Submission attachment(s)");
				if (!submissionFolder.exists()) {
					continue;
				}
				for (File projectFolder : submissionFolder.listFiles(new FileFilter() {
					public boolean accept(File pathname) {
						return pathname.isDirectory();
					}
				})) {
					System.out.println("Reading " + assignFolder.getName() + " student " + studentFolder.getName());
					data.get(assignFolder.getName()).put(studentFolder.getName(), replayLogs(projectFolder.getPath(), analyzer));
				}
			}
		}
		for (String assign : data.keySet()) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					//					createAssignData(assign, folder);
					//					createDistributionData(assign, folder);
//					createPrevPauseDistribution(assign, folder);
					createEvents(assign, folder);
					count++;
					if (count == data.size()) {
						System.out.println("finished");
						System.exit(0);
					}
				}
			}).start();
		}
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
			String[] header = {"Student", "Total Time Spent", "Active Time", "Rest Time", "Wall Clock Time", "Insert", "Delete", "Replace", "Copy", "Paste", "Run", "Exception", "Exception Breakdown"};
			cw.writeNext(header);

			if (csv2.exists()) {
				csv2.delete();
			}
			csv2.createNewFile();
			fw2 = new FileWriter(csv2);
			CSVWriter cw2 = new CSVWriter(fw2);
			String[] header2 = {"case_id", "timestamp", "activity", "user"};
			cw2.writeNext(header2);

			for (String student : data.get(assign).keySet()) {
				System.out.println("Writing " + assign + " student " + student + "to " + csv.getName());
				List<List<EHICommand>> nestedCommands = data.get(assign).get(student);
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

			}
			List<String> retVal = new ArrayList<>();
			retVal.add("Sum");
			retVal.add("");
			retVal.add("");
			retVal.add("");
			retVal.add("");
			for (int i = 1; i < sum.length; i++) {
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

	private void createEvents(String assign, File folder) {
		File csv = new File(folder,assign+"Event.csv");
		FileWriter fw;
		try {
			if (csv.exists()) {
				csv.delete();
			}
			csv.createNewFile();
			fw = new FileWriter(csv);
			CSVWriter cw = new CSVWriter(fw);
			String[] header = {"case_id", "timestamp", "activity", "user"};
			cw.writeNext(header);
			for (String student : data.get(assign).keySet()) {
				System.out.println("Writing " + assign + " student " + student + "to " + csv.getName());
				List<List<EHICommand>> nestedCommands = data.get(assign).get(student);
				List<List<Command>> breakdown = fixBreakdown(nestedCommands);
				for (List<Command> list : breakdown) {
					for (Command command : list) {
						writeOneLine(cw, assign,  command.getTime(), command.getType(), student);
					}
				}
			}
			fw.close();
			cw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public List<List<Command>> fixBreakdown(List<List<EHICommand>> nestedCommands){
		List<List<Command>> exceptionToFixList = new ArrayList<>();
		List<Command> commandList = null;
		boolean fixing = false;
		int fixOffset = -1;
		int range = 50;
		for(int i = 0; i < nestedCommands.size(); i++) {
			List<EHICommand> commands = nestedCommands.get(i);
			for(int j = 0; j < commands.size(); j++) {
				EHICommand command = commands.get(j);
				if (command instanceof ExceptionCommand) {
					if (commandList == null) {
						commandList = new ArrayList<>();
					}
					commandList.add(new Command("Exception", command.getStartTimestamp()+command.getTimestamp()));
					fixing = true;
					fixOffset = -1;
				} 
				if (command instanceof Insert || command instanceof Replace || command instanceof Delete) {
					int offset = Integer.parseInt(command.getAttributesMap().get("offset"));
					if (fixing) {
						if (fixOffset == -1) {
							fixOffset = offset;
							commandList.add(new Command("Fixing", command.getStartTimestamp()+command.getTimestamp()));
						}
						if (Math.abs(offset - fixOffset) <= range) {
							fixOffset = offset;
						} else {
							fixing = false;
							commandList.add(new Command("Editing", command.getStartTimestamp()+command.getTimestamp()));
						}
					}
				}
				if (command instanceof PauseCommand && Long.parseLong(command.getDataMap().get("pause")) > 5*60*1000) {
					commandList.add(new Command("Break", command.getStartTimestamp()+command.getTimestamp()));
				}
				if (command instanceof RunCommand) {
					if (command.getAttributesMap().get("type").equals("Debug")) {
						commandList.add(new Command("Debug", command.getStartTimestamp()+command.getTimestamp()));
					} else {
						commandList.add(new Command("Run", command.getStartTimestamp()+command.getTimestamp()));
					}
				}
				if (command instanceof WebCommand) {
					commandList.add(new Command("Website", command.getStartTimestamp()+command.getTimestamp()));
				}
				if (command instanceof ConsoleOutput) {
					if (commandList != null) {
						commandList.add(new Command("Fixed", command.getTimestamp()+command.getStartTimestamp()));
						exceptionToFixList.add(commandList);
						commandList = null;
					}
				}
			}
		}
		return exceptionToFixList;
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
		return null;
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
			sum[0] = "Sum";
			for (int i = 1; i < sum.length; i++) {
				sum[i] = "";
			}
			int[] restSum = new int[REST.length];
			long[] restTimeSum = new long[REST.length];
			for (String student : data.get(assign).keySet()) {
				System.out.println("Writing " + assign + " student " + student + " to " + csv.getName());
				List<List<EHICommand>> nestedCommands = data.get(assign).get(student);
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

	public void createPauseCommandLogs(String classFolderPath) {
		File folder = new File(classFolderPath);
		if (!folder.exists()) {
			System.out.println("Class Folder does not exist");
			System.exit(0);
		}
		File[] assigns = folder.listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				return pathname.isDirectory();
			}
		});
		int numThread = 0;
		for (int i = 0; i < assigns.length; i++) {
			File assignFolder = assigns[i];
			File[] students = assignFolder.listFiles(new FileFilter() {
				public boolean accept(File pathname) {
					return pathname.isDirectory();
				}
			});
			numThread += students.length;
		}
		latch = new CountDownLatch(numThread);
		for (int i = 0; i < assigns.length; i++) {
			File assignFolder = assigns[i];
			File[] students = assignFolder.listFiles(new FileFilter() {
				public boolean accept(File pathname) {
					return pathname.isDirectory();
				}
			});
			for (int j = 0; j < students.length; j++) {
				File studentFolder  = students[j];
				File submissionFolder = new File(studentFolder,"Submission attachment(s)");
				if (!submissionFolder.exists()) {
					latch.countDown();
					continue;
				}
				File projectFolder = getProjectFolder(submissionFolder);
				//				for (File projectFolder : submissionFolder.listFiles(new FileFilter() {
				//					public boolean accept(File pathname) {
				//						return pathname.isDirectory() && !pathname.getName().contains("MACOS");
				//					}
				//				})) {
				System.out.println("Reading " + assignFolder.getName() + " student " + studentFolder.getName());
				File rest = new File(projectFolder.getPath(), "Logs"+File.separator+"Eclipse" + File.separator+"Rest");
				if (rest.exists()) {
					synchronized (this) {
						threadCount--;
					}
					latch.countDown();
					continue;
				}
				File[] logs = new File(projectFolder.getPath(), "Logs"+File.separator+"Eclipse").listFiles(new FileFilter() {
					public boolean accept(File pathname) {
						return pathname.getName().startsWith("Log") && pathname.getName().endsWith(".xml");
					}
				});
				if (logs == null) {
					synchronized (this) {
						threadCount--;
					}
					latch.countDown();
					continue;
				}
				Thread thread = new Thread(new Runnable() {
					public void run() {
						try {
							for (File file : logs) {
								List<EHICommand> commands = readOneLogFile(file.getPath(), analyzer);
								if (commands.size() < 2) {
									continue;
								}
								List<EHICommand> newCommands = new ArrayList<>();
								EHICommand last = null;
								EHICommand cur = null;
								for (EHICommand command : commands) {
									if (cur == null) {
										cur = command;
										newCommands.add(command);
									} else {
										last = cur;
										cur = command;
										maybeAddPauseCommand(newCommands, last, cur);
									}
								}
								String logContent = XML_START1 + getLogFileCreationTime(file) + XML_START2 + XML_VERSION + XML_START3;
								for (EHICommand c : newCommands) {
									logContent += c.persist();
								}
								logContent += XML_FILE_ENDING;
								try {
									File newLog = new File(file.getParent()+File.separator+"Rest"+File.separator+file.getName());
									if (newLog.exists()) {
										newLog.delete();
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
							// TODO: handle exception
							e.printStackTrace();
						}finally {
							// TODO: handle finally clause
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
		}
		//		}
		try {
			latch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			System.exit(0);
		}
	}

	private void maybeAddPauseCommand(List<EHICommand> newCommands, EHICommand last, EHICommand cur) {
		long rest = cur.getTimestamp()-last.getTimestamp();
		if (rest >= 1*ONE_SECOND) {
			PauseCommand command = new PauseCommand(last, cur, rest);
			command.setStartTimestamp(last.getStartTimestamp());
			command.setTimestamp(last.getTimestamp()+1);
			newCommands.add(command);
		} 
		newCommands.add(cur);
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

			for (String student : data.get(assign).keySet()) {
				System.out.println("Writing " + assign + " student " + student + " to " + csv.getName());
				List<List<EHICommand>> nestedCommands = data.get(assign).get(student);
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
				List<List<EHICommand>> nestedCommands = data.get(assign).get(student);
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
					if (mean[i] == 0) {
						int a = 0;
					}
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

	public double std(List<Long> pauses, double mean) {
		double sum = 0;
		for (Long l : pauses) {
			sum += Math.pow((1.0 * l) - mean,2);
		}
		return Math.sqrt(sum/pauses.size());
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

	public List<EHICommand> readOneLogFile(String path, Analyzer analyzer){
		EHLogReader reader = new EHLogReader();
		File log = new File(path);
		if (!log.exists()) {
			System.out.println("log does not exist:" + path);
			return new ArrayList<EHICommand>();
		}

		if (!path.endsWith(".xml")) {
			System.out.println("log is not in xml format:" + path);
			return new ArrayList<EHICommand>();
		}

		System.out.println("Reading " + path);
		try {
			List<EHICommand> commands = reader.readAll(path);
			sortCommands(commands, 0, commands.size()-1);
			return commands;
		} catch (Exception e) {
			System.out.println("Could not read file" + path + e);
		}
		return new ArrayList<EHICommand>();
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

	public String countConsecutiveCommands(String list) {
		char lastChar = ' ';
		char curChar = ' ';
		int count = 1;
		String retVal = "";
		for (int i = 0; i < list.length(); i++) {
			if (i != 0) {
				lastChar = curChar;
			}
			curChar = list.charAt(i);
			if (curChar == lastChar){
				count++;
			} else if (i != 0){
				retVal += lastChar + "" + count;
				count = 1;
			}
		}
		retVal += curChar + "" + count;
		count = 1;
		return retVal;
	}

	public List<List<EHICommand>> replayLogs(String projectPath, Analyzer analyzer){
		refineLogFiles(projectPath);
		//		List<List<EHICommand>> nestedCommands = analyzer.convertXMLLogToObjects(projectPath+File.separator+"Logs"+File.separator+"Eclipse"+File.separator+"Rest");
		List<List<EHICommand>> nestedCommands = analyzer.convertXMLLogToObjects(projectPath+File.separator+"Logs"+File.separator+"Eclipse");
		sortNestedCommands(nestedCommands);
		return nestedCommands;
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

	public List<List<List<String>>> createMetrics(String projectPath) {
		File metricFolder = new File(projectPath+File.separator+"Logs"+File.separator+"Metrics");
		List<String> metricFiles = MainConsoleUI.getFilesForFolder(metricFolder);
		metrics = new ArrayList<>();
		String largestFileName = "";
		String secondLargestFileName = "";
		for (int i = 0; i < metricFiles.size(); i++) {
			String aFileName = metricFiles.get(i);
			if (aFileName.compareTo(largestFileName) > 0) {
				secondLargestFileName = largestFileName;
				largestFileName = aFileName;
			} else if (aFileName.compareTo(secondLargestFileName) > 0) {
				secondLargestFileName = aFileName;
			}
		}
		for (int i = 0; i < metricFiles.size(); i++) {
			List<List<String>> metric = new ArrayList<>();
			metrics.add(metric);
			BufferedReader r;
			try {
				r = new BufferedReader(new FileReader(new File(metricFolder.getPath()+File.separator+metricFiles.get(i))));
				String line=r.readLine();
				while(true){
					line = r.readLine();
					if (line == null) {
						break;
					}
					try {
						Integer.parseInt(line.substring(0, line.indexOf(",")));
					} catch (NumberFormatException e) {
						continue;
					}
					metric.add(Arrays.asList(line.split(","))); 
				}
				r.close();
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}
		return metrics;
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

	public void refineLogFiles(String projectPath){
		String logPath = projectPath + File.separator + "Logs" + File.separator + "Eclipse";
		try {
			File logDirectory = new File(logPath);
			if (!logDirectory.exists()) {
				return;
			}
			for (File file : logDirectory.listFiles(new FileFilter() {
				public boolean accept(File pathname) {
					return pathname.getPath().contains(".lck");
				}
			})) {
				File xmlFile = new File(file.getPath().substring(0,file.getPath().lastIndexOf(".")));
				if (xmlFile.exists()) {
					BufferedReader reader = new BufferedReader(new FileReader(xmlFile));
					String lastLine = null;
					String currentLine = null;
					while((currentLine = reader.readLine()) != null) {
						lastLine = currentLine;
					}
					if (lastLine != null && !lastLine.endsWith("</Events>")) {
						BufferedWriter writer = new BufferedWriter(new FileWriter(xmlFile, true));
						writer.write(XML_FILE_ENDING);
						writer.close();
					}	
					reader.close();
				}
				file.delete();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} 
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
		for (int i = 0; i < nestedCommands.size(); i++) {
			List<EHICommand> commands = nestedCommands.get(i);
			EHICommand last = null;
			EHICommand cur = null;
			int k = 0;
			for(; k < commands.size(); k++) {
				if (commands.get(k).getStartTimestamp() > 0 || commands.get(k).getTimestamp() > 0) {
					break;
				}
			}
			for(; k < commands.size(); k++) {
				if (cur != null) {
					last = cur;
				}
				cur = commands.get(k);
				if (last != null) {
					long diff = cur.getStartTimestamp() + cur.getTimestamp() - last.getTimestamp() - last.getStartTimestamp();
					if (diff > time) {
						restTime[0] += diff;
						if (diff < time2) {
							restTime[1]++;
							restTime[2] += diff;
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

	public int getCurrentExceptions() {
		return currentExceptions;
	}

	public int getTotalExceptions() {
		return totalExceptions;
	}

	protected List<EHICommand> readWebCommands(File file){
		if (!file.exists()) {
			return null;
		}
		List<EHICommand> retVal = new ArrayList<>();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(file));
			String nextLine;
			SimpleDateFormat format = new SimpleDateFormat("M/d/yyyy hh:mm:ss");
			Date date;
			String keyword;
			String url;
			WebCommand webCommand;
			while ((nextLine = br.readLine()) != null) {
				try {
					String[] tokens = nextLine.split("\t");
					if (tokens.length >= 3) {
						date = format.parse(tokens[0]);
						keyword = tokens[1];
						url = tokens[2];
						if (keyword.contains("google.com/url?") || keyword.equals(url)) {
							continue;
						}
						webCommand = new WebCommand(keyword, url);
						webCommand.setTimestamp(date.getTime());
						retVal.add(0, webCommand);
					} else {
						System.out.println("Failed to parse WebCommand");
					}
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return retVal;
	}

	public List<List<List<String>>> getMetrics() {
		List<List<List<String>>> retval = new ArrayList<>();
		long startTime = 0;
		long endTime = Long.MAX_VALUE;
		if (startTime > endTime) {
			long temp = startTime;
			startTime = endTime;
			endTime = temp;
		} 
		for(int k = 0, m = 0; k < metrics.size(); k++) {
			List<List<String>> metric = metrics.get(k);
			if (metric.size() == 0) {
				continue;
			}
			if ((Long.parseLong(metric.get(metric.size()-1).get(4)) < startTime) || (Long.parseLong(metric.get(0).get(4)) > endTime)) {
				continue;
			}
			retval.add(new ArrayList<>());
			for(int l = 0; l < metrics.get(k).size(); l++) {
				if ((Long.parseLong(metric.get(l).get(4)) >= startTime) && (Long.parseLong(metric.get(l).get(4)) <= endTime)) {
					retval.get(m).add(metric.get(l));
				}
			}
			m++;
		}
		return retval;
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

	protected Map<String, List<String[]>> readLocalCheckEvents(String assign) {
		Map<String, List<String[]>> localCheckEvents = new HashMap<>();
		for (File file : new File(LOCALCHECK_EVENTS).listFiles()) {
			if (!assign.substring(assign.indexOf(" ")+1).equals(file.getName().substring("assignment".length(),file.getName().indexOf("_")))) {
				continue;
			}
			try {
				CSVReader cr = new CSVReader(new FileReader(file));
				String[] nextLine = cr.readNext();
				List<String[]> studentLC = null;
				while ((nextLine = cr.readNext()) != null) {
					if (!localCheckEvents.containsKey(nextLine[3])) {
						studentLC = new ArrayList<>();
						localCheckEvents.put(nextLine[3], studentLC);
					}
					studentLC.add(nextLine);
				}
				cr.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				return null;
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			} 
		}
		return localCheckEvents;
	}

	public void createLocalCheckCommands(String classFolderPath) {
		SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		File folder = new File(classFolderPath);
		if (!folder.exists()) {
			System.out.println("Class Folder does not exist");
			System.exit(0);
		}
		File[] assigns = folder.listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				return pathname.isDirectory();
			}
		});
		int numThread = 0;
		for (int i = 0; i < assigns.length; i++) {
			File assignFolder = assigns[i];
			File[] students = assignFolder.listFiles(new FileFilter() {
				public boolean accept(File pathname) {
					return pathname.isDirectory();
				}
			});
			numThread += students.length;
		}
		latch = new CountDownLatch(numThread);
		for (int i = 0; i < assigns.length; i++) {
			File assignFolder = assigns[i];
			Map<String, List<String[]>> localCheckEvents = readLocalCheckEvents(assignFolder.getName());
			File[] students = assignFolder.listFiles(new FileFilter() {
				public boolean accept(File pathname) {
					return pathname.isDirectory();
				}
			});
			for (int j = 0; j < students.length; j++) {
				File studentFolder  = students[j];
				File submissionFolder = new File(studentFolder,"Submission attachment(s)");
				if (!submissionFolder.exists()) {
					latch.countDown();
					continue;
				}
				//				for (File projectFolder : submissionFolder.listFiles(new FileFilter() {
				//					public boolean accept(File pathname) {
				//						return pathname.isDirectory() && !pathname.getName().contains("MACOS");
				//					}
				//				})) {
				File projectFolder = getProjectFolder(submissionFolder);
				System.out.println("Reading " + assignFolder.getName() + " student " + studentFolder.getName());
				File[] logs = new File(projectFolder.getPath(), "Logs"+File.separator+"Eclipse"+File.separator+"Rest").listFiles(new FileFilter() {
					public boolean accept(File pathname) {
						return pathname.getName().startsWith("Log") && pathname.getName().endsWith(".xml");
					}
				});
				if (logs == null || !localCheckEvents.containsKey(studentFolder.getName())) {
					synchronized (this) {
						threadCount--;
					}
					latch.countDown();
					continue;
				}
				Thread thread = new Thread(new Runnable() {
					public void run() {

						try {
							List<String[]> studentLC = localCheckEvents.get(studentFolder.getName());
							for (File file : logs) {
								List<EHICommand> commands = readOneLogFile(file.getPath(), analyzer);
								if (commands.size() < 2) {
									continue;
								}
								List<EHICommand> newCommands = new ArrayList<>();
								EHICommand last = null;
								EHICommand cur = null;
								int i = 0;
								String[] event = studentLC.get(i);
								long timestamp = df.parse(event[1]).getTime();
								for (EHICommand command : commands) {
									if (cur == null) {
										cur = command;
										newCommands.add(command);
									} else {
										last = cur;
										cur = command;
										if (timestamp >= last.getStartTimestamp()+last.getTimestamp() && timestamp < cur.getStartTimestamp() + cur.getTimestamp()) {
											List<String> events = new ArrayList<>();
											while (timestamp < cur.getStartTimestamp() + cur.getTimestamp() && i+1 < studentLC.size()) {
												events.add(event[2]);
												i++;
												event = studentLC.get(i);
												timestamp = df.parse(event[1]).getTime();
											}
											LocalCheckCommand command2 = new LocalCheckCommand(event[2]);
											command2.setStartTimestamp(last.getStartTimestamp());
											command2.setTimestamp(timestamp-last.getStartTimestamp());
											newCommands.add(command2);
										}
									}
								}
								String logContent = XML_START1 + getLogFileCreationTime(file) + XML_START2 + XML_VERSION + XML_START3;
								for (EHICommand c : newCommands) {
									logContent += c.persist();
								}
								logContent += XML_FILE_ENDING;
								try {
									File newLog = new File(file.getParent()+File.separator+"LocalCheck"+File.separator+file.getName());
									if (newLog.exists()) {
										newLog.delete();
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
						}finally {
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
		}
		//		}
		try {
			latch.await();
			System.out.println(latch.getCount());
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			System.exit(0);
		}
	}

	public File getProjectFolder(File folder) {
		for (File file : folder.listFiles((file)->{
			return file.isDirectory();
		})) {
			if (file.getName().equals("src")) {
				return folder;
			} 
		}
		for (File file : folder.listFiles((file)->{
			return file.isDirectory();
		})) {
			return getProjectFolder(file);
		}
		return folder;
	}
}