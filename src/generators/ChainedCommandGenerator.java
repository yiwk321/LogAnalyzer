package generators;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;

import org.json.JSONObject;

import fluorite.commands.DifficultyCommand;
import fluorite.commands.EHICommand;
import fluorite.commands.PauseCommand;
import logAnalyzer.AReplayer;
import logAnalyzer.Replayer;

public class ChainedCommandGenerator extends CommandGenerator {
	protected List<CommandGenerator> commandGenerators = new ArrayList();
	protected  Replayer replayer;
	CountDownLatch latch;
	String student;
	Map<String, List<EHICommand>> commandMap;
	boolean appendAllRemainingCommands;
	protected boolean foundTracedStudent = false;
//	private Map<String, String> logToWrite;
	
	public static String capitalizeNames (String aLowerCaseName) {
		String[] aNames = aLowerCaseName.split(" ");
		StringBuilder retVal = new StringBuilder();
		
		for (String aName:aNames) {
			String anUpperName = Character.toUpperCase(aName.charAt(0)) + aName.substring(1);
			retVal.append(anUpperName);
			retVal.append(" ");
		}
		retVal.deleteCharAt(retVal.length() - 1);
		return retVal.toString();
	}

	public ChainedCommandGenerator(Replayer aReplayer, CountDownLatch aLatch, 
			String aStudent, Map<String, List<EHICommand>> aStudentLog, 
			List<String[]> localCheckEvents, JSONObject piazzaPosts, File zoomChatsFolder, HashMap<String, List<Long>> map, boolean appendAllRemainingCommands) {
		replayer = aReplayer;
		student = aStudent;
		System.out.println("Student:" + aStudent);
//		if (aStudent.contains("Genaro")) {
//			System.out.println("Found Genaro");
//		}
//		boolean isSynthesized = aStudent.contains("Synthesized");
//		if (isSynthesized) {
//			System.out.println("Processing sythesized assignment");
//		}
		
//		if (student.contains("Mark")) {
//			foundTracedStudent = true;
//		}
		latch = aLatch;
		commandMap = aStudentLog;
		this.appendAllRemainingCommands = appendAllRemainingCommands;
//		logToWrite = new TreeMap<>();
		if (localCheckEvents == null) {
			System.err.println("No localcheck logs for" + aStudent + ", not adding to command generator");
		} else {
			System.out.println("Found localcheck logs for" + aStudent + ", not adding to command generator");
			
			commandGenerators.add(new DifficultyPredictionCommandGenerator(replayer, latch, aStudentLog));

			commandGenerators.add(new EndOfSessionCommandGenerator(replayer, latch, aStudentLog));
//			commandGenerators.add(new PauseCommandGenerator(this, null, aStudentLog));
			commandGenerators.add(new LocalCheckCommandGenerator(replayer, latch, aStudent, aStudentLog, localCheckEvents));
			commandGenerators.add(new CheckstyleCommandGenerator(replayer, latch, aStudent, aStudentLog));
//			commandGenerators.add(new LocalCheckCommandGenerator(this, latch, student, studentLog, localCheckEvents));
			commandGenerators.add(new LocalChecksRawBatchCommandGenerator(replayer, latch, aStudent, aStudentLog));

			if (piazzaPosts != null) {
				commandGenerators.add(new PiazzaCommandGenerator(replayer, latch, aStudent, aStudentLog, piazzaPosts));
			}
			if (zoomChatsFolder != null && zoomChatsFolder.exists()) {
				commandGenerators.add(new ZoomChatCommandGenerator(replayer, latch, aStudent, aStudentLog, zoomChatsFolder, map));
			}
		}
	}
	
	public static long getMaxTime(List<EHICommand> aCommands) {
		for (int anIndex = aCommands.size() - 1; anIndex >= 0; anIndex --) {
			EHICommand aCommand = aCommands.get(anIndex);
			long aTimestamp = toTimestamp(aCommand);
			if (aTimestamp != 0) {
				return aTimestamp;
			}
		}
		return 0;
		
	}
	
	public static long getStartTimestamp(List<EHICommand> aCommands) {
		for (EHICommand aCommand:aCommands) {
			long aStartTimestamp = aCommand.getStartTimestamp() ;
			if (aStartTimestamp != 0) {
				return aStartTimestamp;
			}			
		}
		return 0;
	}

	public void addRemainingCommands(List<EHICommand> commands) {
		long aStartTimestamp = getStartTimestamp(commands);

		if (aStartTimestamp == 0) {
			System.err.println("time stamp is 0 of first command");
		}
		long aMinEndTime = getMaxTime(commands);
		List<EHICommand> aNextRemainingCommands;
		List<EHICommand> anAllRemaningCommands = new ArrayList();
		for (CommandGenerator aCommandGenerator:commandGenerators) {
			aNextRemainingCommands = aCommandGenerator.getRemainingAssignmentCommands(aMinEndTime);
			aMinEndTime = Math.max(aMinEndTime, getMaxTime(aNextRemainingCommands));	
			anAllRemaningCommands.addAll(aNextRemainingCommands);
		}
//		AReplayer.sortCommands(anAllRemaningCommands);
		for (EHICommand aCommand:anAllRemaningCommands) {
			long aFullTimeStamp = toTimestamp(aCommand);
			aCommand.setStartTimestamp(aStartTimestamp);
			long aRelativeTimestamp = aFullTimeStamp - aStartTimestamp;
			if (aRelativeTimestamp < 0) {
				System.err.println("Start time stamp " + aStartTimestamp);
				System.err.println("full time stamp " + aFullTimeStamp);

			}
			aCommand.setTimestamp(aFullTimeStamp - aStartTimestamp);
		}
		commands.addAll(anAllRemaningCommands);
		AReplayer.sortCommands(commands);

		
		
	}
	@Override
	public List<EHICommand> addCommands(int aSessionIndex, List<EHICommand> commands, long nextStartTime) {
//		if (student.contains("Dare") ||student.contains("Earlene")) {
//			System.out.println("Found tracked student");
//			if (aSessionIndex == 7) {
//				System.out.println("Found tracked session");
//			}
//		}

		for (CommandGenerator aCommandGenerator:commandGenerators) {
			commands = aCommandGenerator.addCommands(aSessionIndex, commands, nextStartTime);
		}
		
//		long aStartTimestamp = getStartTimestamp(commands);
//
//		if (aStartTimestamp == 0) {
//			System.err.println("time stamp is 0 of first command");
//		}
//		long aMinEndTime = getMaxTime(commands);
//		List<EHICommand> aNextRemainingCommands;
//		List<EHICommand> anAllRemaningCommands = new ArrayList();
//		for (CommandGenerator aCommandGenerator:commandGenerators) {
//			aNextRemainingCommands = aCommandGenerator.getRemainingAssignmentCommands(aMinEndTime);
//			aMinEndTime = Math.max(aMinEndTime, getMaxTime(aNextRemainingCommands));	
//			anAllRemaningCommands.addAll(aNextRemainingCommands);
//		}
//		AReplayer.sortCommands(anAllRemaningCommands);
//		for (EHICommand aCommand:anAllRemaningCommands) {
//			long aFullTimeStamp = toTimestamp(aCommand);
//			aCommand.setStartTimestamp(aStartTimestamp);
//			aCommand.setTimestamp(aFullTimeStamp - aStartTimestamp);
//		}
		
		return commands;
	}
	
	public void run() {
		try {
			if (latch != null) {
				System.out.println(Thread.currentThread().getName() + " started");
			}
			if (foundTracedStudent) {
				System.out.println("found traced student");
			}
//			for (String fileName : commandMap.keySet()) {
			String[] keyset = commandMap.keySet().toArray(new String[0]);
			long lastCommandTime = -1;
			for (int j = 0; j < commandMap.size(); j++) {
				String fileName = keyset[j];
				System.out.println("Processing file:" + fileName);
//				List<EHICommand> commands = removePauseCommands(commandMap.get(fileName));
				List<EHICommand> commands = commandMap.get(fileName);
				File file = new File(fileName);
				if (commands.size() < 2) {
					continue;
				}
				long startTimestamp = getLogFileCreationTime(file);
				if (commands.get(commands.size()-1).getStartTimestamp() == 0) {
					System.out.println("Resetting start time stamp to:" + startTimestamp);

					for (EHICommand command : commands) {
						command.setStartTimestamp(startTimestamp);
					}
				}
				List<EHICommand> newCommands = null;
				if (j == commandMap.size()-1) {
					if (lastCommandTime == -1) {
						List<EHICommand> nextCommands = commandMap.get(keyset[j]);
						EHICommand command = nextCommands.get(nextCommands.size()-1);
						lastCommandTime = command.getStartTimestamp() + command.getTimestamp();
					}
					newCommands = addCommands(j, commands, 
							appendAllRemainingCommands ? 
									Long.MAX_VALUE : 
										lastCommandTime + 600000);
					if (j == commandMap.size()-1) {
//						if (student.contains("Dare")) {
//							System.out.println("Found traced student");
//						}
						addRemainingCommands(newCommands);
					}
				} else {
					List<EHICommand> nextCommands = commandMap.get(keyset[j+1]);
					long nextStartTime = -1;
					for(int k = 0; k < nextCommands.size(); k++) {
						if (nextCommands.get(k).getStartTimestamp() > 0 || nextCommands.get(k).getTimestamp() > 0) {
							nextStartTime = nextCommands.get(k).getStartTimestamp();
							break;
						}
					}
					newCommands = addCommands(j, commands, nextStartTime);
				}
				StringBuffer buf = new StringBuffer();
				long aStartTime = newCommands.get(0).getTimestamp2();
//				buf.append(Replayer.XML_START1 + getLogFileCreationTime(file) + Replayer.XML_START2 + Replayer.XML_VERSION + Replayer.XML_START3);
				buf.append(Replayer.XML_START1 + aStartTime + Replayer.XML_START2 + Replayer.XML_VERSION + Replayer.XML_START3);

				int i = 0;
				// reason for this?
				while(i < newCommands.size() && (newCommands.get(i) instanceof DifficultyCommand || newCommands.get(i) instanceof PauseCommand)) {
					i++;
				}
				for (; i < newCommands.size();i++) {
					try {
						buf.append(newCommands.get(i).persist());
					}catch (Exception e) {
						i--;
					}
				}
				buf.append(Replayer.XML_FILE_ENDING);
//				replayer.updateLogMap(fileName, buf.toString());
				String aNewFileName = CommandGenerator.newFileName(fileName, aStartTime);
				replayer.updateLogMap(aNewFileName, buf.toString());

			}
		} catch (Exception e) {
			e.printStackTrace();
		}finally {
			if (latch != null) {
				latch.countDown();
				System.out.println(Thread.currentThread().getName() + " finished, " + latch.getCount() + " threads remaining");
			}
		}
	} 
	

}
