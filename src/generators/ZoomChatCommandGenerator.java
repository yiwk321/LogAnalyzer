package generators;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import analyzer.extension.replayView.FileUtility;
import fluorite.commands.EHICommand;
import fluorite.commands.ZoomChatCommand;
import fluorite.commands.ZoomSessionEndCommand;
import fluorite.commands.ZoomSessionStartCommand;
import logAnalyzer.Replayer;

public class ZoomChatCommandGenerator extends ExternalCommandGenerator {

	String student;
	List<ZoomChatCommand> zoomChats;
	Pattern studentNamePattern = Pattern.compile(".*\\\\(.*), (.*)\\((.*)\\)");
//	Pattern speakerPattern = Pattern.compile("(\\d\\d:\\d\\d:\\d\\d) From  (.*)  to  Everyone:");
	Pattern fullNamePattern = Pattern.compile("(\\S+)\\s+(\\(.*\\))?\\s?(.*)");
	Pattern firstOrLastNamePattern = Pattern.compile("\\s*(\\S+).*");
	Pattern vttPattern = Pattern.compile("(?m)^(\\d{2}:\\d{2}:\\d{2}\\.\\d+) +--> +(\\d{2}:\\d{2}:\\d{2}\\.\\d+).*[\\r\\n]+\\s*(?s)((?:(?!\\r?\\n\\r?\\n).)*)");
	Pattern chatPattern = Pattern.compile("(.*): (.*)");
	Pattern txtPattern = Pattern.compile("(?m)^(\\d{2}:\\d{2}:\\d{2})\\s*(?s)((?:(?!(\\d{2}:\\d{2}:\\d{2})).)*)");
	SimpleDateFormat df2 = new SimpleDateFormat("zzzyyyyMMdd-HHmmss");
	SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss zzz");
	HashMap<String, List<Long>> sessionTimeMap;
	List<Long> sessionTimes;
	final int TXT_GROUP_IDX = 2;
	final int VTT_GROUP_IDX = 3;
	int session = 0;
	
	public ZoomChatCommandGenerator(Replayer replayer, CountDownLatch aLatch, String aStudent, 
								  Map<String, List<EHICommand>> commandMap, File zoomChatsFolder, HashMap<String, List<Long>> sessionTimeMap) {
		super(replayer, aLatch, commandMap);
		student = aStudent;
		zoomChats = new ArrayList<>();
		sessionTimes = new ArrayList<>();
		this.sessionTimeMap = sessionTimeMap;
		findZoomChats(aStudent, zoomChatsFolder);
		if (zoomChats.size() > 0) {
			sessionTimeMap.put(student, sessionTimes);
		}
		File[] files = new File(student).listFiles((file)->{
			return !file.isDirectory() && file.getName().endsWith("ZoomSession");
		});
		for (File file2 : files) {
			file2.delete();
		}
		File file = new File(student, session + "ZoomSession");
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void findZoomChats(String student, File zoomChatFolder) {
		Matcher matcher = studentNamePattern.matcher(student);
		if (!matcher.matches()) {
			return;
		}
		String name = matcher.group(3);
		for (File chat : zoomChatFolder.listFiles()) {
			Date sessionStartTime = new Date();
			try {
				sessionStartTime = df2.parse(chat.getName().substring(0, 18));
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			findZoomChatsOneFile(name, chat, sessionStartTime);
		}
	}
	
	private void findZoomChatsOneFile(String name, File chat, Date sessionStartTime) {
		String chatString = FileUtility.readFile(chat).toString();
		String chatName = chat.getName();
		Pattern pattern = vttPattern;
		int groupIdx = VTT_GROUP_IDX;
		
		if (chat.getName().endsWith(".txt") && !chatName.equals("total OH transcript.txt")) {
			pattern = txtPattern;
			groupIdx = TXT_GROUP_IDX;
			if (chatString.endsWith(System.lineSeparator())) {
				chatString += System.lineSeparator();
			}
		}

		Matcher matcher = pattern.matcher(chatString);
		Matcher chatMatcher = null; 
		ZoomChatCommand zoomChatCommand = null;
		ZoomChatCommand first = null;
		ZoomChatCommand last = null;
		String currentChat = "";
		boolean hasSession = false;
		while (matcher.find()) {
			chatMatcher = chatPattern.matcher(matcher.group(groupIdx));
			if (chatMatcher.matches()) {
				String speaker = chatMatcher.group(1);
				if (speaker.equals(name)) {
					if (!hasSession) {
						session++;
						hasSession = true;
						ZoomSessionStartCommand zoomSessionStartCommand = new ZoomSessionStartCommand(df.format(sessionStartTime), matcher.group(1));
						zoomSessionStartCommand.setTimestamp(getTimestamp(matcher.group(1), pattern));
						zoomSessionStartCommand.setStartTimestamp(sessionStartTime.getTime());
						zoomChats.add(zoomSessionStartCommand);
					}
					if (zoomChatCommand == null) {
						currentChat = chatMatcher.group(2) + " ";
						zoomChatCommand = new ZoomChatCommand(speaker, currentChat, 
								df.format(sessionStartTime), matcher.group(1));
						zoomChatCommand.setStartTimestamp(sessionStartTime.getTime());
//						long timestamp = 0;
//						String startTimeString = matcher.group(1);
//						timestamp += Long.parseLong(startTimeString.substring(0, 2)) * 3600 * 1000;
//						timestamp += Long.parseLong(startTimeString.substring(3, 5)) * 60 * 1000;
//						timestamp += Long.parseLong(startTimeString.substring(6, 8)) * 1000;
//						if (pattern == vttPattern) {
//							timestamp += Long.parseLong(startTimeString.substring(9, 12));
//						}
						zoomChatCommand.setTimestamp(getTimestamp(matcher.group(1), pattern));
						zoomChats.add(zoomChatCommand);

						if (last != null && (zoomChatCommand.getTimestamp() - last.getTimestamp()) > 5 * 60000) {
							if (first != last) {
								if (last.getTimestamp() - first.getTimestamp() > 30000) {
									sessionTimes.add(last.getTimestamp() - first.getTimestamp());
									ZoomSessionEndCommand zoomSessionEndCommand = new ZoomSessionEndCommand(df.format(sessionStartTime), last.getDataMap().get("speak_time"));
									zoomSessionEndCommand.setTimestamp(last.getTimestamp());
									zoomSessionEndCommand.setStartTimestamp(last.getStartTimestamp());
									zoomChats.add(zoomSessionEndCommand);
									
									ZoomSessionStartCommand zoomSessionStartCommand = new ZoomSessionStartCommand(df.format(sessionStartTime), matcher.group(1));
									zoomSessionStartCommand.setTimestamp(getTimestamp(matcher.group(1), pattern));
									zoomSessionStartCommand.setStartTimestamp(last.getStartTimestamp());
									zoomChats.add(zoomSessionStartCommand);
								}
							}
							
							first = zoomChatCommand;
						}
						if (first == null) {
							first = zoomChatCommand;
						}
						last = zoomChatCommand;
						
					} else {
						currentChat += chatMatcher.group(2) + " ";
						zoomChatCommand.setChat(currentChat);
					}
				} else {
					zoomChatCommand = null;
					currentChat = "";
				}
			}
		}
		if (first != null && first != last) {
//			if (last.getTimestamp() - first.getTimestamp() > 30000) {
				sessionTimes.add(last.getTimestamp() - first.getTimestamp());
				ZoomSessionEndCommand zoomSessionEndCommand = new ZoomSessionEndCommand(df.format(sessionStartTime), last.getDataMap().get("speak_time"));
				zoomSessionEndCommand.setTimestamp(last.getTimestamp());
				zoomSessionEndCommand.setStartTimestamp(last.getStartTimestamp());
				zoomChats.add(zoomSessionEndCommand);
//			}
		}
	}
	
	protected long getTimestamp(String startTimeString, Pattern pattern) {
		long timestamp = 0;
		timestamp += Long.parseLong(startTimeString.substring(0, 2)) * 3600 * 1000;
		timestamp += Long.parseLong(startTimeString.substring(3, 5)) * 60 * 1000;
		timestamp += Long.parseLong(startTimeString.substring(6, 8)) * 1000;
		if (pattern == vttPattern) {
			timestamp += Long.parseLong(startTimeString.substring(9, 12));
		}
		return timestamp;
	}


	@Override
	protected boolean hasNextExternalEvent() {
		// TODO Auto-generated method stub
		return lastAddedExternalIndex < zoomChats.size();
	}

	protected ZoomChatCommand previousEvent;
	
	@Override
	protected long getNextExternalEventTimeStamp() {
		// TODO Auto-generated method stub
		previousEvent = zoomChats.get(lastAddedExternalIndex);
		return previousEvent.getStartTimestamp() + previousEvent.getTimestamp();
	}
	
	@Override
	protected List<EHICommand> createExternalCommands(boolean fromPreviousEvent) {
		// TODO Auto-generated method stub
		if (!fromPreviousEvent) {
			previousEvent = zoomChats.get(lastAddedExternalIndex);
		}
		EHICommand aCommand = previousEvent;
		List<EHICommand> retVal = new ArrayList<>();
		retVal.add(aCommand);
//		System.out.println("Adding aCommand " + aCommand + " for " + student);
		return retVal;
	}
}
