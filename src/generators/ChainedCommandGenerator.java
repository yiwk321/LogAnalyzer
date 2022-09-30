package generators;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;

import org.json.JSONObject;

import fluorite.commands.DifficultyCommand;
import fluorite.commands.EHICommand;
import fluorite.commands.PauseCommand;
import logAnalyzer.Replayer;

public class ChainedCommandGenerator extends CommandGenerator {
	protected List<CommandGenerator> commandGenerators = new ArrayList();
	protected  Replayer replayer;
	CountDownLatch latch;
	String student;
	Map<String, List<EHICommand>> commandMap;
//	private Map<String, String> logToWrite;

	public ChainedCommandGenerator(Replayer aReplayer, CountDownLatch aLatch, 
			String aStudent, Map<String, List<EHICommand>> aStudentLog, 
			List<String[]> localCheckEvents, JSONObject piazzaPosts, File zoomChatsFolder) {
		replayer = aReplayer;
		student = aStudent;
		latch = aLatch;
		commandMap = aStudentLog;
//		logToWrite = new TreeMap<>();

//		commandGenerators.add(new PauseCommandGenerator(this, null, aStudentLog));
		commandGenerators.add(new LocalCheckCommandGenerator(replayer, latch, aStudent, aStudentLog, localCheckEvents));
		commandGenerators.add(new CheckstyleCommandGenerator(replayer, latch, aStudent, aStudentLog));
//		commandGenerators.add(new LocalCheckCommandGenerator(this, latch, student, studentLog, localCheckEvents));
		if (piazzaPosts != null) {
			commandGenerators.add(new PiazzaCommandGenerator(replayer, latch, aStudent, aStudentLog, piazzaPosts));
		}
		if (zoomChatsFolder != null && zoomChatsFolder.exists()) {
			commandGenerators.add(new ZoomChatCommandGenerator(replayer, latch, aStudent, aStudentLog, zoomChatsFolder));
		}
	}

	@Override
	public List<EHICommand> addCommands(int aSessionIndex, List<EHICommand> commands, long nextStartTime) {
		for (CommandGenerator aCommandGenerator:commandGenerators) {
			commands = aCommandGenerator.addCommands(aSessionIndex, commands, nextStartTime);
		}
		return commands;
	}
	
	public void run() {
		try {
			if (latch != null) {
				System.out.println(Thread.currentThread().getName() + " started");
			}
//			for (String fileName : commandMap.keySet()) {
			String[] keyset = commandMap.keySet().toArray(new String[0]);
			for (int j = 0; j < commandMap.size(); j++) {
				String fileName = keyset[j];
//				List<EHICommand> commands = removePauseCommands(commandMap.get(fileName));
				List<EHICommand> commands = commandMap.get(fileName);
				File file = new File(fileName);
				if (commands.size() < 2) {
					continue;
				}
				long startTimestamp = getLogFileCreationTime(file);
				if (commands.get(commands.size()-1).getStartTimestamp() == 0) {
					for (EHICommand command : commands) {
						command.setStartTimestamp(startTimestamp);
					}
				}
				List<EHICommand> newCommands = null;
				if (j == commandMap.size()-1) {
					newCommands = addCommands(j, commands, Long.MAX_VALUE);
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
