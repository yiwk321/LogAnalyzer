package generators;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import fluorite.commands.EHICommand;
import fluorite.commands.LocalCheckCommand;
import fluorite.commands.PauseCommand;
import logAnalyzer.Replayer;

public class LocalCheckCommandGenerator extends PauseCommandGenerator {
	List<String[]> studentLC;
	String student;
	int i = 0;
	SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

	public LocalCheckCommandGenerator(Replayer replayer, CountDownLatch aLatch, String aStudent, Map<String, List<EHICommand>> commandMap, List<String[]> events) {
		super(replayer, aLatch, commandMap);
		studentLC = events;
		student = aStudent;
	}
	public static boolean hasPauseCommand(List<EHICommand> commands) {
		for (EHICommand aCommand:commands) {
			if (aCommand instanceof PauseCommand) {
				return true;
			}
		}
		return false;
	}
	public List<EHICommand> addCommands(List<EHICommand> commands, long nextStartTime) {
//		if (student.contains("Beier")) {
//			System.out.println("found student");
//		}
		long aStartTimeStamp = commands.get(0).getTimestamp2();
		boolean aHasPauseCommand = hasPauseCommand(commands);
		if (i < studentLC.size()) {
			List<EHICommand> newCommands = new ArrayList<>();
			EHICommand last = null;
			EHICommand cur = null;
			String[] event = null;
			long timestamp = 0;
			while (true) {
				event = studentLC.get(i);
				try {
					timestamp = df.parse(event[1]).getTime();
					Date aDate1 = new Date(timestamp);
					Date aDate2 = new Date(aStartTimeStamp);
					
					if (timestamp > aStartTimeStamp) {
						break;
					}
				} catch (ParseException e) {
					e.printStackTrace();
				}
				i++;
				if (i >= studentLC.size()) {
					break;
				}
			}
		
			
//			for (EHICommand command : commands) {
			for (int j = 0; j < commands.size(); j++) {
				
				EHICommand command = commands.get(j);
				if (cur == null) {
					cur = command;
					newCommands.add(command);
				} else {
					last = cur;
					cur = command;
					if (j == commands.size()-1 && timestamp < nextStartTime) {
						LocalCheckCommand command2 = new LocalCheckCommand(event[2]);
						while (timestamp < nextStartTime && i < studentLC.size()) {
							command2 = new LocalCheckCommand(event[2]);
							
							if (last.getStartTimestamp() == 0) {
								command2.setStartTimestamp(cur.getStartTimestamp());								
								command2.setTimestamp(timestamp-cur.getStartTimestamp());
							} else {
								command2.setStartTimestamp(last.getStartTimestamp());
								command2.setTimestamp(timestamp-last.getStartTimestamp());
							}
							newCommands.add(command2);
							i++;
							if (i >= studentLC.size()) {
								break;
							}
							event = studentLC.get(i);
							try {
								timestamp = df.parse(event[1]).getTime();
							} catch (ParseException e) {
								e.printStackTrace();
							}
						}
					} else if (timestamp >= last.getStartTimestamp()+last.getTimestamp() && timestamp < cur.getStartTimestamp() + cur.getTimestamp()) {
						LocalCheckCommand command2 = new LocalCheckCommand(event[2]);
						while (timestamp < cur.getStartTimestamp() + cur.getTimestamp() && i < studentLC.size()) {
							command2 = new LocalCheckCommand(event[2]);
							if (last.getStartTimestamp() == 0) {
								command2.setStartTimestamp(cur.getStartTimestamp());
								command2.setTimestamp(timestamp-cur.getStartTimestamp());
							} else {
								command2.setStartTimestamp(last.getStartTimestamp());
								command2.setTimestamp(timestamp-last.getStartTimestamp());
							}
							newCommands.add(command2);
							i++;
							if (i >= studentLC.size()) {
								break;
							}
							event = studentLC.get(i);
							try {
								timestamp = df.parse(event[1]).getTime();
							} catch (ParseException e) {
								e.printStackTrace();
							}
						}
						if (!aHasPauseCommand)
						maybeAddPauseCommand(newCommands, command2, cur);
					} else {
						newCommands.add(command);
					}
				}
			}
			return super.addCommands(newCommands, nextStartTime);
		} else {
			return super.addCommands(commands, nextStartTime);
		}
	}
}
