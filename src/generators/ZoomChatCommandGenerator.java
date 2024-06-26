package generators;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;

import analyzer.extension.replayView.FileUtility;
import fluorite.commands.EHICommand;
import fluorite.commands.PiazzaPostCommand;
import fluorite.commands.ZoomChatCommand;
import fluorite.commands.ZoomSessionEndCommand;
import fluorite.commands.ZoomSessionStartCommand;
import fluorite.util.EHUtilities;
import logAnalyzer.AnAssignmentReplayer;
import logAnalyzer.LogAnalyzerLoggerFactory;
import logAnalyzer.Replayer;

public class ZoomChatCommandGenerator extends ExternalCommandGenerator {

	String student;
	String studentName;
	List<ZoomChatCommand> zoomChats;
	Pattern studentNamePattern = Pattern.compile(".*\\\\(.*), (.*)\\((.*)\\)");
//	Pattern speakerPattern = Pattern.compile("(\\d\\d:\\d\\d:\\d\\d) From  (.*)  to  Everyone:");
	Pattern fullNamePattern = Pattern.compile("(\\S+)\\s+(\\(.*\\))?\\s?(.*)");
	Pattern firstOrLastNamePattern = Pattern.compile("\\s*(\\S+).*");
	static Pattern vttPattern = Pattern.compile("(?m)^(\\d{2}:\\d{2}:\\d{2}\\.\\d+) +--> +(\\d{2}:\\d{2}:\\d{2}\\.\\d+).*[\\r\\n]+\\s*(?s)((?:(?!\\r?\\n\\r?\\n).)*)");
	static Pattern chatPattern = Pattern.compile("(.*): (.*)");

	Pattern txtPattern = Pattern.compile("(?m)^(\\d{2}:\\d{2}:\\d{2})\\s*(?s)((?:(?!(\\d{2}:\\d{2}:\\d{2})).)*)");
	SimpleDateFormat df2 = new SimpleDateFormat("zzzyyyyMMdd-HHmmss");
	SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss zzz");
	HashMap<String, List<Long>> sessionTimeMap;
	List<Long> sessionTimes;
	final int TXT_GROUP_IDX = 2;
	static final int VTT_GROUP_IDX = 3;
	int session = 0;
	
	public ZoomChatCommandGenerator(Replayer replayer, CountDownLatch aLatch, String aStudent, 
								  Map<String, List<EHICommand>> commandMap, File zoomChatsFolder, HashMap<String, List<Long>> sessionTimeMap) {
		super(replayer, aLatch, commandMap);
		student = aStudent;
		studentName = PiazzaCommandGenerator.toParenthesizedStudentName(student);

		zoomChats = new ArrayList<>();
		sessionTimes = new ArrayList<>();
		this.sessionTimeMap = sessionTimeMap;
		
		findZoomChats(aStudent, zoomChatsFolder);
		if (zoomChats.size() > 0) {
			sessionTimeMap.put(student, sessionTimes);
//			if (aStudent.contains("Marks")) {
//				System.out.println("found Marks");
//				for (ZoomChatCommand aCommand:zoomChats) {
//					System.out.println("Chat command:" + aCommand.persist());
//				}
//			}
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
	
	public static Set<String> findAllZoomChatters (File zoomChatFolder) {
		Set<String> retVal= new HashSet();
		if (zoomChatFolder == null) {
			System.err.println("Null zoom chat foler");
			return retVal;
		}
		for (File chat : zoomChatFolder.listFiles()) {			
			findAllZoomChattersOneFile(retVal, chat);
		}
     		return retVal;
	}
	
	public static void findAllZoomChattersOneFile (Set<String> retVal, File chat) {
		String chatString = FileUtility.readFile(chat).toString();
		String chatName = chat.getName();
		Pattern pattern = vttPattern;
		int groupIdx = VTT_GROUP_IDX;		
		if (chatName.endsWith(".txt") && !chatName.equals("total OH transcript.txt")) {
			
			if (chatString.endsWith(System.lineSeparator())) {
				chatString += System.lineSeparator();
			}
		}
		Matcher matcher = pattern.matcher(chatString);
		Matcher chatMatcher = null; 		
		while (matcher.find()) {
			chatMatcher = chatPattern.matcher(matcher.group(groupIdx));
			if (chatMatcher.matches()) {
				String speaker = chatMatcher.group(1);
//				if (speaker.contains("nstructor") || speaker.contains("nonymous")) {
//					continue;
//				}
				String aNormalizedName = AnAssignmentReplayer.normalizeName(speaker);
				if (aNormalizedName != null) {
					retVal.add(aNormalizedName);
				} else {
					System.err.println("Illegal name:" + speaker);
				}
//				retVal.add(aNormalizedName);				
			}
		}	
	}
	
	

	private void findZoomChats(String student, File zoomChatFolder) {
		Matcher matcher = studentNamePattern.matcher(student);
//		if (student.contains("Walker")) {
//			System.out.println("found student find zoom chats");
//		}
		if (!matcher.matches()) {
			return;
		}
		String name = matcher.group(3);
		for (File chat : zoomChatFolder.listFiles()) {
			Date sessionStartTime = new Date();
			try {
				String sessionStartTimeString = chat.getName().substring(0, 18);
//				sessionStartTime = df2.parse(chat.getName().substring(0, 18));
				sessionStartTime = df2.parse(sessionStartTimeString);

			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			findZoomChatsOneFile(name, chat, sessionStartTime);
		}
		if (zoomChats.size() > 0) {
		System.out.println("found zoom chats for:"+ student);
		
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
//				String speaker = chatMatcher.group(1);
				String speaker = chatMatcher.group(1).toLowerCase();
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
//	public static long toTimestamp(EHICommand aCommand) {
//		try {
//			return aCommand.getStartTimestamp() + aCommand.getTimestamp();
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//			return -1;
//		}
//	}
	@Override
	protected List<EHICommand> getRemainingAssignmentCommands(long minEndTime) {
		List<EHICommand> retVal = new ArrayList();
		for (int anIndex = lastAddedExternalIndex;  anIndex < zoomChats.size(); anIndex++) {
			EHICommand aCommand = zoomChats.get(anIndex);
			long aTimeStamp = toTimestamp(aCommand);
			if (aTimeStamp > minEndTime) {
				break; // maybe check the previous folder
			}
			retVal.add(aCommand);		
		}
		return retVal;
	}

	@Override
	protected boolean hasNextExternalEvent() {
//		if (zoomChats.size() > 0) {
//			System.out.println("has zoom chat");
//		}
		// TODO Auto-generated method stub
		return lastAddedExternalIndex < zoomChats.size();
	}

	protected ZoomChatCommand previousEvent;
	
	@Override
	protected long getNextExternalEventTimeStamp() {
		// TODO Auto-generated method stub
		previousEvent = zoomChats.get(lastAddedExternalIndex);
//		return previousEvent.getStartTimestamp() + previousEvent.getTimestamp();
		long retVal = previousEvent.getStartTimestamp() + previousEvent.getTimestamp();
//		if (student.contains("Marks")) {
//			Date date = new Date(retVal);
//			System.out.println ("Next event:" + date);
//		}
		return retVal;
	}
	public List<EHICommand> addCommands(int aSession, List<EHICommand> commands, long nextStartTime) {
//		if (student.contains("Genaro")) {
//			System.out.println("Found student");
//		}
//		if (nextStartTime == Long.MAX_VALUE) {
////			System.out.println(" add external commands");
//			if (zoomChats.size() > 0) {
//				System.out.println(" zoom chats > 0 for synthesized student:" + student );
//
//			}
//		}
		return super.addCommands(aSession, commands, nextStartTime);
	}
	
	
	protected void logCommand (EHICommand aCommand) {
		String aCommandType;
		if ((aCommand instanceof ZoomSessionStartCommand)) {
			aCommandType = "Zoom session start";
		} else if (((aCommand instanceof ZoomSessionEndCommand))) {
			aCommandType = "Zoom session end";
		} else {
			return;
		}
		
		LogAnalyzerLoggerFactory.logMessage(studentName + "-->" + 
				toDate(aCommand)+ 
				aCommandType +
				"\n");
		LogAnalyzerLoggerFactory.getLogAnalyzerAssignmentMetrics().numZoomSessions++;
	}

	
	@Override
	protected List<EHICommand> createExternalCommands(boolean fromPreviousEvent) {
		// TODO Auto-generated method stub
		if (!fromPreviousEvent) {
			previousEvent = zoomChats.get(lastAddedExternalIndex);
		}
		EHICommand aCommand = previousEvent;
		List<EHICommand> retVal = new ArrayList<>();
//		if (aCommand instanceof ZoomSessionStartCommand) {
//			LogAnalyzerLoggerFactory.logMessage(
//					student + "-->" + EHUtilities.toDate(aCommand) + "\n");
//			LogAnalyzerLoggerFactory.getLogAnalyzerAssignmentMetrics().numZoomSessions++;
////			System.out.println(student + "-->" + aCommand);
//		}
		retVal.add(aCommand);
//		System.out.println("Adding aCommand " + aCommand + " for " + student);
		return retVal;
	}
}
