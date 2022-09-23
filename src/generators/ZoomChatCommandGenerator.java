package generators;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import analyzer.extension.replayView.FileUtility;
import fluorite.commands.EHICommand;
import fluorite.commands.ZoomChatCommand;
import logAnalyzer.Replayer;

public class ZoomChatCommandGenerator extends ExternalCommandGenerator {

	String student;
	List<ZoomChatCommand> zoomChats;
	Pattern studentNamePattern = Pattern.compile(".*\\\\(.*), (.*)\\((.*)\\)");
	Pattern speakerPattern = Pattern.compile("(\\d\\d:\\d\\d:\\d\\d) From  (.*)  to  Everyone:");
	SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss zzz");
	
	public ZoomChatCommandGenerator(Replayer replayer, CountDownLatch aLatch, String aStudent, 
								  Map<String, List<EHICommand>> commandMap, File zoomChatsFolder) {
		super(replayer, aLatch, commandMap);
		student = aStudent;
		zoomChats = new ArrayList<>();
		findZoomChats(aStudent, zoomChatsFolder);
	}

	private void findZoomChats(String student, File zoomChatFolder) {
		Matcher matcher = studentNamePattern.matcher(student);
		if (!matcher.matches()) {
			return;
		}
		String name = matcher.group(3);
//		speakerPattern = Pattern.compile(speakerPattern.pattern().replace("(.*)", name));
		
		File[] chatFolders = zoomChatFolder.listFiles(
				(file)->{return file.isDirectory();});
		for (File chatFolder : chatFolders) {
			String sessionStartTime = chatFolder.getName();
			sessionStartTime = sessionStartTime.substring(0, 19);
			File[] chats = chatFolder.listFiles(
					(parent, fileName)->{return fileName.endsWith(".txt");});
			for (File chat : chats) {
				findZoomChatsOneFile(name, chat, sessionStartTime);
			}
		}
	}
	
	private void findZoomChatsOneFile(String name, File chat, String sessionStartTime) {
		List<String> lines = new ArrayList<>();
		FileUtility.readLines(chat, lines);
		ZoomChatCommand command = null;
		StringBuilder sb = new StringBuilder();
		for (String line : lines) {
			Matcher matcher = speakerPattern.matcher(line);
			if (matcher.matches()) {
				String speaker = matcher.group(2);
				if (command != null) {
					command.setChat(sb.toString());
					sb = new StringBuilder();
				}
				if (speaker.equals(name)) {
					command = new ZoomChatCommand(name, sessionStartTime, matcher.group(1));
					zoomChats.add(command);
				} else {
					command = null;
				}
			} else if (command != null) {
				sb.append(line + System.lineSeparator());
			}
		}
		if (command != null) {
			command.setChat(sb.toString());
		}
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
		String date = previousEvent.getDataMap().get("session_start_time").substring(0, 11);
		String time = previousEvent.getDataMap().get("speak_time");
		
		try {
			return df.parse(date + time + " EST").getTime();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return -1;
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
		System.out.println("Adding aCommand " + aCommand + " for " + student);
		return retVal;
	}

}
