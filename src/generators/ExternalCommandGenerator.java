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
public abstract class ExternalCommandGenerator extends PauseCommandGenerator {
//	List<String[]> studentLC;
//	String student;
	int lastAddedExternalIndex = 0;
	long lastAddedTimeStamp;
//	SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

	public ExternalCommandGenerator(Replayer replayer, CountDownLatch aLatch,  Map<String, List<EHICommand>> commandMap) {
		super(replayer, aLatch, commandMap);
//		studentLC = events;
//		student = aStudent;
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
protected abstract boolean hasNextExternalEvent() ;
protected abstract long getNextExternalEventTimeStamp() ;
protected List<EHICommand>  createExternalCommands() {
	return createExternalCommands(false);
}
protected void logCommand (EHICommand aCommand) {
	
}

protected abstract List<EHICommand>  createExternalCommands(boolean fromPreviousEvent) ;

public List<EHICommand> addCommands(int aSession, List<EHICommand> commands, long nextStartTime) {
//	if (student.contains("Earlene")) {
//		System.out.println("found student");
//	}
	long aStartTimeStamp = commands.get(0).getTimestamp2();
	Date aStartDate = new Date(aStartTimeStamp);
//	System.out.println("Adding commands for start time:" + aStartDate);
	boolean aHasPauseCommand = hasPauseCommand(commands);
//	if (lastAddedExternalIndex < studentLC.size()) {
	Date aDate = new Date(aStartTimeStamp);
//	System.out.println("Next start time:"+ aDate );
	if (hasNextExternalEvent()) {

		List<EHICommand> newCommands = new ArrayList<>();
		EHICommand last = null;
		EHICommand cur = null;
//		String[] event = null;
		long nextExternalEventTimestamp = 0;
		// ignore events before the initial command and initialize time stamp of next external command
		while (true) {
//			event = studentLC.get(lastAddedExternalIndex);
			try {
//				timestamp = df.parse(event[1]).getTime();
				nextExternalEventTimestamp = getNextExternalEventTimeStamp();

				// these are not referenced
//				Date aLastEventDate = new Date(timestamp);
//				Date aDate2 = new Date(aStartTimeStamp);
				
				if (nextExternalEventTimestamp > aStartTimeStamp) {
					break;
				}
				 aDate = new Date(nextExternalEventTimestamp);
				 
//				 System.out.println("Ignoring command at timestamp:" + aDate + "as it is greater than start date:" + aStartDate + "(" + this + ")");
			} catch (Exception e) {
				e.printStackTrace();
			}
			lastAddedExternalIndex++;
//			if (lastAddedExternalIndex >= studentLC.size()) {
			if (!hasNextExternalEvent()) { // all external events occurred too early

				break;
			}
		}
	
		
//		for (EHICommand command : commands) {
  		for (int aCommandIndex = 0; aCommandIndex < commands.size(); aCommandIndex++) {
			
			EHICommand command = commands.get(aCommandIndex);
			if (cur == null) { // first command
				cur = command;
				newCommands.add(command); // add the first command
			} else {
				last = cur;
				cur = command;
				if (aCommandIndex == commands.size()-1 && nextExternalEventTimestamp < nextStartTime) {
					// we are at the end of the internal command list
//					LocalCheckCommand command2 = new LocalCheckCommand(event[2]);
					//use the commands associated with nextExternalEventTimestamp
					List<EHICommand> externalCommandList = createExternalCommands(true); // why here?
					// get all the other external commands before the nextStartTime
					while (nextExternalEventTimestamp < nextStartTime && 
							hasNextExternalEvent()
//							lastAddedExternalIndex < studentLC.size()
							) {
					//	command2 = new LocalCheckCommand(event[2]); 
						// why are we losing the original list
						// we are not losing as we have not incremented index
						externalCommandList = createExternalCommands(); // same as passing false
						
						if (last.getStartTimestamp() == 0) { // first command, no last
//							command2.setStartTimestamp(cur.getStartTimestamp());								
//							command2.setTimestamp(timestamp-cur.getStartTimestamp());
							for (EHICommand anExternalCommand:externalCommandList) {
							anExternalCommand.setStartTimestamp(cur.getStartTimestamp());								
							anExternalCommand.setTimestamp(nextExternalEventTimestamp-cur.getStartTimestamp());
							}

							
						} else { // not first command, so there is last
							for (EHICommand anExternalCommand:externalCommandList) {

							anExternalCommand.setStartTimestamp(last.getStartTimestamp());
							anExternalCommand.setTimestamp(nextExternalEventTimestamp-last.getStartTimestamp());
							}
						}
						if (externalCommandList.size() > 1) {
							System.err.println("Size of external commmads:" + externalCommandList.size() );
						}
						for (EHICommand anExternalCommand:externalCommandList) {

						newCommands.add(anExternalCommand); 
						logCommand(anExternalCommand);
//						System.out.println ("Adding command:" + System.identityHashCode(anExternalCommand));

						}
						lastAddedExternalIndex++; // why not inside loop
//						if (lastAddedExternalIndex >= studentLC.size()) {
						if (!hasNextExternalEvent()) {
							break;
						}
//						event = studentLC.get(lastAddedExternalIndex);
//						try {
//							timestamp = df.parse(event[1]).getTime();
//						} catch (ParseException e) {
//							e.printStackTrace();
//						}
						nextExternalEventTimestamp = getNextExternalEventTimeStamp();
					}
				} else if (nextExternalEventTimestamp >= last.getStartTimestamp()+last.getTimestamp() && 
						nextExternalEventTimestamp < cur.getStartTimestamp() + cur.getTimestamp()) {
//					LocalCheckCommand command2 = new LocalCheckCommand(event[2]);
					List<EHICommand> anExternalCommandList = createExternalCommands(true);

					while (nextExternalEventTimestamp < cur.getStartTimestamp() + cur.getTimestamp() && 
//							lastAddedExternalIndex < studentLC.size()
							hasNextExternalEvent()
							) {
//						command2 = new LocalCheckCommand(event[2]);
						anExternalCommandList = createExternalCommands();
						if (last.getStartTimestamp() == 0) {
							for (EHICommand anExternalCommand:anExternalCommandList) {

							anExternalCommand.setStartTimestamp(cur.getStartTimestamp());
							anExternalCommand.setTimestamp(nextExternalEventTimestamp-cur.getStartTimestamp());
							}
						} else {
							for (EHICommand anExtenalCommand:anExternalCommandList) {

							anExtenalCommand.setStartTimestamp(last.getStartTimestamp());
							anExtenalCommand.setTimestamp(nextExternalEventTimestamp-last.getStartTimestamp());
							}
						}
						for (EHICommand anExternalCommand:anExternalCommandList) {

						newCommands.add(anExternalCommand);
						logCommand(anExternalCommand);
//						System.out.println ("Adding command:" + System.identityHashCode(anExternalCommand));
						}
						lastAddedExternalIndex++;
//						if (lastAddedExternalIndex >= studentLC.size()) {
						if (!hasNextExternalEvent()) {

							break;
						}
//						event = studentLC.get(lastAddedExternalIndex);
//						try {
//							timestamp = df.parse(event[1]).getTime();
//						} catch (ParseException e) {
//							e.printStackTrace();
//						}
						nextExternalEventTimestamp = getNextExternalEventTimeStamp();
					}
					if (!aHasPauseCommand) {
						for (EHICommand command2:anExternalCommandList) {
					maybeAddPauseCommand(newCommands, command2, cur);
						}
					}
				} else { // need to advance current command to find next sandwiched command
					newCommands.add(command);
				}
			}
		}
  		return super.addCommands(aSession, newCommands, nextStartTime);
	} else {
		return super.addCommands(aSession, commands, nextStartTime);
	}
}
public static String toCSVString (String[] aStrings) {
	if (aStrings.length == 0) {
		return "";
	}
	StringBuilder stringBuilder = new StringBuilder();
	stringBuilder.append(aStrings[0]);
	for (int anArrayIndex = 1; anArrayIndex < aStrings.length; anArrayIndex++) {
		stringBuilder.append(',');
		stringBuilder.append(aStrings[anArrayIndex]);
	}
	return stringBuilder.toString();
}
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
