package generators;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import fluorite.commands.DifficultyCommand;
import fluorite.commands.EHICommand;
import fluorite.commands.PauseCommand;
import logAnalyzer.Replayer;

public abstract class CommandGenerator implements Runnable {
	CountDownLatch latch;
	Map<String, List<EHICommand>> commandMap;
	Map<String, Map<String, Map<String, List<EHICommand>>>> commandsMaps;
	Replayer replayer;
	static List<EHICommand> emptyList = new ArrayList();
	
	
	protected List<EHICommand> getRemainingAssignmentCommands(long minMaxTime) {
		return emptyList;
	}
	Date date = new Date();
	public  Date toDate (EHICommand aCommand) {
//		long aStartTimeStamp = EHEventRecorder.getInstance().getStartTimestamp();
//		long aTime = aStartTimeStamp + aCommandTimestamp;
		date.setTime(aCommand.getStartTimestamp() + aCommand.getTimestamp());
		return date;
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
	public static boolean hasCommand(List<EHICommand> commands, Class aCommandClass) {
		for (EHICommand aCommand:commands) {
			if (aCommand.getClass() == aCommandClass) {
				return true;
			}
		}
		return false;
	}

	public static String newFileName (String aFileName, long aTime) {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS");
		Date aDate = new Date(aTime);
		String aNewDateFormat = df.format(aDate);
		int aLogIndex = aFileName.lastIndexOf("Log");
		String aPrefix = aFileName.substring(0, aLogIndex);
		String newFileName = aPrefix + "Log" + aNewDateFormat + ".xml";

		return  newFileName;
		
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
				List<EHICommand> commands = removePauseCommands(commandMap.get(fileName));
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
				String aNewFileName = newFileName(fileName, aStartTime);
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
	
	public List<EHICommand> removePauseCommands(List<EHICommand> commands) {
		List<EHICommand> newCommands = new ArrayList<>();
		for (int i = 0; i < commands.size(); i++) {
			EHICommand command = commands.get(i);
			if (!(command instanceof PauseCommand)) {
				newCommands.add(command);
			}
		}
		return newCommands;
	}
	public static long toTimestamp(EHICommand aCommand) {
		try {
			return aCommand.getStartTimestamp() + aCommand.getTimestamp();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		}
	}
	
	public abstract List<EHICommand> addCommands(int aSessionIndex, List<EHICommand> commands, long nextStartTime);
}
