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
// make this subclass of web generator?
//public class LocalCheckCommandGenerator extends PauseCommandGenerator {
public class LocalCheckCommandGenerator extends ExternalCommandGenerator {

	List<String[]> studentLC;
	String student;
//	int lastAddedExternalIndex = 0;
	SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

	public LocalCheckCommandGenerator(Replayer replayer, CountDownLatch aLatch, String aStudent, Map<String, List<EHICommand>> commandMap, List<String[]> events) {
		super(replayer, aLatch, commandMap);
		studentLC = events;
		student = aStudent;
	}
//	public static boolean hasPauseCommand(List<EHICommand> commands) {
//		for (EHICommand aCommand:commands) {
//			if (aCommand instanceof PauseCommand) {
//				return true;
//			}
//		}
//		return false;
//	}

//}
protected boolean hasNextExternalEvent() {
	return lastAddedExternalIndex < studentLC.size();
}
protected String[] previousEvent;
protected long getNextExternalEventTimeStamp() {
	String[] event = studentLC.get(lastAddedExternalIndex);
	previousEvent = event;
	try {
		return df.parse(event[1]).getTime();
	} catch (ParseException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
		return -1;
	}
}

protected List<EHICommand>  createExternalCommands(boolean fromPreviousEvent) {
	String[] anEvent = previousEvent;
	if (!fromPreviousEvent) {
	 anEvent = studentLC.get(lastAddedExternalIndex);
	}
	EHICommand aCommand = new LocalCheckCommand(anEvent[2]);
	List<EHICommand> retVal = new ArrayList();
	retVal.add(aCommand);
	return retVal;
}


//public List<EHICommand> addCommands(int aSession, List<EHICommand> commands, long nextStartTime) {
////	if (student.contains("Beier")) {
////		System.out.println("found student");
////	}
//	long aStartTimeStamp = commands.get(0).getTimestamp2();
//	boolean aHasPauseCommand = hasPauseCommand(commands);
////	if (lastAddedExternalIndex < studentLC.size()) {
//	if (hasNextExternalEvent()) {
//
//		List<EHICommand> newCommands = new ArrayList<>();
//		EHICommand last = null;
//		EHICommand cur = null;
////		String[] event = null;
//		long timestamp = 0;
//		// ignore events before the initial command
//		while (true) {
////			event = studentLC.get(lastAddedExternalIndex);
//			try {
////				timestamp = df.parse(event[1]).getTime();
//				timestamp = getNextExternalEventTimeStamp();
//
//				Date aLastEventDate = new Date(timestamp);
//				Date aDate2 = new Date(aStartTimeStamp);
//				
//				if (timestamp > aStartTimeStamp) {
//					break;
//				}
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//			lastAddedExternalIndex++;
//			if (lastAddedExternalIndex >= studentLC.size()) {
//				break;
//			}
//		}
//	
//		
////		for (EHICommand command : commands) {
//		for (int aCommandIndex = 0; aCommandIndex < commands.size(); aCommandIndex++) {
//			
//			EHICommand command = commands.get(aCommandIndex);
//			if (cur == null) {
//				cur = command;
//				newCommands.add(command);
//			} else {
//				last = cur;
//				cur = command;
//				if (aCommandIndex == commands.size()-1 && timestamp < nextStartTime) {
////					LocalCheckCommand command2 = new LocalCheckCommand(event[2]);
//					List<EHICommand> command2List = createExternalCommands(); // why here?
//
//					while (timestamp < nextStartTime && lastAddedExternalIndex < studentLC.size()) {
//					//	command2 = new LocalCheckCommand(event[2]); 
//						command2List = createExternalCommands(); 
//						
//						if (last.getStartTimestamp() == 0) {
////							command2.setStartTimestamp(cur.getStartTimestamp());								
////							command2.setTimestamp(timestamp-cur.getStartTimestamp());
//							for (EHICommand command2:command2List) {
//							command2.setStartTimestamp(cur.getStartTimestamp());								
//							command2.setTimestamp(timestamp-cur.getStartTimestamp());
//							}
//
//							
//						} else {
//							for (EHICommand command2:command2List) {
//
//							command2.setStartTimestamp(last.getStartTimestamp());
//							command2.setTimestamp(timestamp-last.getStartTimestamp());
//							}
//						}
//						for (EHICommand command2:command2List) {
//
//						newCommands.add(command2);
//						}
//						lastAddedExternalIndex++;
//						if (lastAddedExternalIndex >= studentLC.size()) {
//							break;
//						}
////						event = studentLC.get(lastAddedExternalIndex);
////						try {
////							timestamp = df.parse(event[1]).getTime();
////						} catch (ParseException e) {
////							e.printStackTrace();
////						}
//						timestamp = getNextExternalEventTimeStamp();
//					}
//				} else if (timestamp >= last.getStartTimestamp()+last.getTimestamp() && timestamp < cur.getStartTimestamp() + cur.getTimestamp()) {
////					LocalCheckCommand command2 = new LocalCheckCommand(event[2]);
//					List<EHICommand> command2List = createExternalCommands();
//
//					while (timestamp < cur.getStartTimestamp() + cur.getTimestamp() && 
////							lastAddedExternalIndex < studentLC.size()
//							hasNextExternalEvent()
//							) {
////						command2 = new LocalCheckCommand(event[2]);
//						command2List = createExternalCommands();
//						if (last.getStartTimestamp() == 0) {
//							for (EHICommand command2:command2List) {
//
//							command2.setStartTimestamp(cur.getStartTimestamp());
//							command2.setTimestamp(timestamp-cur.getStartTimestamp());
//							}
//						} else {
//							for (EHICommand command2:command2List) {
//
//							command2.setStartTimestamp(last.getStartTimestamp());
//							command2.setTimestamp(timestamp-last.getStartTimestamp());
//							}
//						}
//						for (EHICommand command2:command2List) {
//
//						newCommands.add(command2);
//						}
//						lastAddedExternalIndex++;
////						if (lastAddedExternalIndex >= studentLC.size()) {
//						if (hasNextExternalEvent()) {
//
//							break;
//						}
////						event = studentLC.get(lastAddedExternalIndex);
////						try {
////							timestamp = df.parse(event[1]).getTime();
////						} catch (ParseException e) {
////							e.printStackTrace();
////						}
//						timestamp = getNextExternalEventTimeStamp();
//					}
//					if (!aHasPauseCommand) {
//						for (EHICommand command2:command2List) {
//					maybeAddPauseCommand(newCommands, command2, cur);
//						}
//					}
//				} else {
//					newCommands.add(command);
//				}
//			}
//		}
//		return super.addCommands(aSession, newCommands, nextStartTime);
//	} else {
//		return super.addCommands(aSession, commands, nextStartTime);
//	}
//}
//public List<EHICommand> addCommandsNonModular(int aSession, List<EHICommand> commands, long nextStartTime) {
////if (student.contains("Beier")) {
////System.out.println("found student");
////}
//long aStartTimeStamp = commands.get(0).getTimestamp2();
//boolean aHasPauseCommand = hasPauseCommand(commands);
//if (lastAddedStudentLCIndex < studentLC.size()) {
//List<EHICommand> newCommands = new ArrayList<>();
//EHICommand last = null;
//EHICommand cur = null;
//String[] event = null;
//long timestamp = 0;
//// ignore events before the initial command
//while (true) {
//	event = studentLC.get(lastAddedStudentLCIndex);
//	try {
//		timestamp = df.parse(event[1]).getTime();
//		Date aDate1 = new Date(timestamp);
//		Date aDate2 = new Date(aStartTimeStamp);
//		
//		if (timestamp > aStartTimeStamp) {
//			break;
//		}
//	} catch (ParseException e) {
//		e.printStackTrace();
//	}
//	lastAddedStudentLCIndex++;
//	if (lastAddedStudentLCIndex >= studentLC.size()) {
//		break;
//	}
//}
//
//
////for (EHICommand command : commands) {
//for (int j = 0; j < commands.size(); j++) {
//	
//	EHICommand command = commands.get(j);
//	if (cur == null) {
//		cur = command;
//		newCommands.add(command);
//	} else {
//		last = cur;
//		cur = command;
//		if (j == commands.size()-1 && timestamp < nextStartTime) {
//			LocalCheckCommand command2 = new LocalCheckCommand(event[2]);
//			while (timestamp < nextStartTime && lastAddedStudentLCIndex < studentLC.size()) {
//				command2 = new LocalCheckCommand(event[2]);
//				
//				if (last.getStartTimestamp() == 0) {
//					command2.setStartTimestamp(cur.getStartTimestamp());								
//					command2.setTimestamp(timestamp-cur.getStartTimestamp());
//				} else {
//					command2.setStartTimestamp(last.getStartTimestamp());
//					command2.setTimestamp(timestamp-last.getStartTimestamp());
//				}
//				newCommands.add(command2);
//				lastAddedStudentLCIndex++;
//				if (lastAddedStudentLCIndex >= studentLC.size()) {
//					break;
//				}
//				event = studentLC.get(lastAddedStudentLCIndex);
//				try {
//					timestamp = df.parse(event[1]).getTime();
//				} catch (ParseException e) {
//					e.printStackTrace();
//				}
//			}
//		} else if (timestamp >= last.getStartTimestamp()+last.getTimestamp() && timestamp < cur.getStartTimestamp() + cur.getTimestamp()) {
//			LocalCheckCommand command2 = new LocalCheckCommand(event[2]);
//			while (timestamp < cur.getStartTimestamp() + cur.getTimestamp() && lastAddedStudentLCIndex < studentLC.size()) {
//				command2 = new LocalCheckCommand(event[2]);
//				if (last.getStartTimestamp() == 0) {
//					command2.setStartTimestamp(cur.getStartTimestamp());
//					command2.setTimestamp(timestamp-cur.getStartTimestamp());
//				} else {
//					command2.setStartTimestamp(last.getStartTimestamp());
//					command2.setTimestamp(timestamp-last.getStartTimestamp());
//				}
//				newCommands.add(command2);
//				lastAddedStudentLCIndex++;
//				if (lastAddedStudentLCIndex >= studentLC.size()) {
//					break;
//				}
//				event = studentLC.get(lastAddedStudentLCIndex);
//				try {
//					timestamp = df.parse(event[1]).getTime();
//				} catch (ParseException e) {
//					e.printStackTrace();
//				}
//			}
//			if (!aHasPauseCommand)
//			maybeAddPauseCommand(newCommands, command2, cur);
//		} else {
//			newCommands.add(command);
//		}
//	}
//}
//return super.addCommands(aSession, newCommands, nextStartTime);
//} else {
//return super.addCommands(aSession, commands, nextStartTime);
//}
//}
}
