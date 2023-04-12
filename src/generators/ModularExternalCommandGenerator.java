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
public abstract class ModularExternalCommandGenerator extends PauseCommandGenerator {

	int lastAddedExternalIndex = 0;
//	SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

	public ModularExternalCommandGenerator(Replayer replayer, CountDownLatch aLatch,
			Map<String, List<EHICommand>> commandMap) {
		super(replayer, aLatch, commandMap);

	}

	protected abstract boolean hasNextExternalEvent();

	protected abstract long getNextExternalEventTimeStamp();

	protected List<EHICommand> createExternalCommands() {
		return createExternalCommands(false);
	}

	protected abstract List<EHICommand> createExternalCommands(boolean fromPreviousEvent);

	private List<EHICommand> newCommands = new ArrayList<>();
	private EHICommand last = null;
	private EHICommand currentExistingCommand = null;
//String[] event = null;
	private long nextExternalEventTimestamp = 0;
	List<EHICommand> earlyExternalCommands = new ArrayList();

	protected void processLeadingExternalCommands() {
		// ignore events before the initial command and initialize time stamp of next
		// external command
		while (true) {
//				event = studentLC.get(lastAddedExternalIndex);
			try {
//					timestamp = df.parse(event[1]).getTime();
				nextExternalEventTimestamp = getNextExternalEventTimeStamp();

				// these are not referenced
//					Date aLastEventDate = new Date(timestamp);
//					Date aDate2 = new Date(startTimeStamp);

				if (nextExternalEventTimestamp > startTimeStamp) {
					break; // end of early commands
				}
				Date aDate = new Date(nextExternalEventTimestamp);
				System.out.println("Ignoring command at timestamp:" + aDate);
			} catch (Exception e) {
				e.printStackTrace();
			}
			List<EHICommand> anExternalCommands = createExternalCommands();
			earlyExternalCommands.addAll(anExternalCommands);
			lastAddedExternalIndex++;
//				if (lastAddedExternalIndex >= studentLC.size()) {
			if (!hasNextExternalEvent()) { // all external events occurred too early

				break;
			}
		}
	}

	protected void processSandwichedExternalCommands(boolean aHasPauseCommand) {
//		LocalCheckCommand command2 = new LocalCheckCommand(event[2]);
		// we have incremented index, so get the previous event using true
		List<EHICommand> anExternalCommandList = createExternalCommands(true);

		while (nextExternalEventTimestamp < currentExistingCommand.getStartTimestamp()
				+ currentExistingCommand.getTimestamp() &&
//				lastAddedExternalIndex < studentLC.size()
				hasNextExternalEvent()) {
//			command2 = new LocalCheckCommand(event[2]);
			anExternalCommandList = createExternalCommands();
			if (last.getStartTimestamp() == 0) { // why would there be such a command
				for (EHICommand anExternalCommand : anExternalCommandList) {

					anExternalCommand.setStartTimestamp(currentExistingCommand.getStartTimestamp());
					anExternalCommand
							.setTimestamp(nextExternalEventTimestamp - currentExistingCommand.getStartTimestamp());
				}
			} else {
				for (EHICommand anExtenalCommand : anExternalCommandList) {

					anExtenalCommand.setStartTimestamp(last.getStartTimestamp());
					anExtenalCommand.setTimestamp(nextExternalEventTimestamp - last.getStartTimestamp());
					// will the start time stamp not be the same for last and current? One will be
					// zero
				}
			}
			for (EHICommand anExternalCommand : anExternalCommandList) {

				newCommands.add(anExternalCommand);
//			System.out.println ("Adding command:" + System.identityHashCode(anExternalCommand));
			}
			lastAddedExternalIndex++;
//			if (lastAddedExternalIndex >= studentLC.size()) {
			if (!hasNextExternalEvent()) {

				break;
			}

			nextExternalEventTimestamp = getNextExternalEventTimeStamp();
		}
		if (aHasPauseCommand) { // this means we will not call the pause command generator so add pause

//		if (!aHasPauseCommand) {
			for (EHICommand anExternalCommand : anExternalCommandList) {
				maybeAddPauseCommand(newCommands, anExternalCommand, currentExistingCommand);
			}
		}
	}

	protected void processTrailingExternalCommands(long aNextSessionStartTime) {
		// we are at the end of the existing command list
		// use the commands associated with nextExternalEventTimestamp
		List<EHICommand> externalCommandList = createExternalCommands(true); // why here?
		// get all the other external commands before the nextStartTime
		while (nextExternalEventTimestamp < aNextSessionStartTime && hasNextExternalEvent()
//			lastAddedExternalIndex < studentLC.size()
		) {
			// are we losing the original list
			// we are not losing as we have not incremented index
			externalCommandList = createExternalCommands(); // same as passing false

			if (last.getStartTimestamp() == 0) { // first command, no last
//			
				for (EHICommand anExternalCommand : externalCommandList) {
					anExternalCommand.setStartTimestamp(currentExistingCommand.getStartTimestamp());
					anExternalCommand
							.setTimestamp(nextExternalEventTimestamp - currentExistingCommand.getStartTimestamp());
				}

			} else { // not first command, so there is last
				for (EHICommand anExternalCommand : externalCommandList) {

					anExternalCommand.setStartTimestamp(last.getStartTimestamp());
					anExternalCommand.setTimestamp(nextExternalEventTimestamp - last.getStartTimestamp());
				}
			}
			if (externalCommandList.size() > 1) {
				System.err.println("Size of external commmads:" + externalCommandList.size());
			}
			for (EHICommand anExternalCommand : externalCommandList) {

				newCommands.add(anExternalCommand);
//		System.out.println ("Adding command:" + System.identityHashCode(anExternalCommand));

			}
			lastAddedExternalIndex++; // why not inside loop
//		if (lastAddedExternalIndex >= studentLC.size()) {
			if (!hasNextExternalEvent()) {
				break;
			}

			nextExternalEventTimestamp = getNextExternalEventTimeStamp();
		}
		
	}

	private long startTimeStamp;
	private Date startDate;

	public List<EHICommand> addCommands(int aSession, List<EHICommand> commands, long nextStartTime) {

////	long startTimeStamp = commands.get(0).getTimestamp2();
//		startTimeStamp = commands.get(0).getTimestamp2();
//
//		boolean aHasPauseCommand = hasPauseCommand(commands);
////	Date startDate = new Date(startTimeStamp);
//		startDate = new Date(startTimeStamp);

//	System.out.println("Next start time:"+ startDate );
//		if (hasNextExternalEvent()) {
		if (!hasNextExternalEvent()) {
			return super.addCommands(aSession, commands, nextStartTime);

		} else {
			startTimeStamp = commands.get(0).getTimestamp2();

			boolean aHasPauseCommand = hasPauseCommand(commands);
//		Date startDate = new Date(startTimeStamp);
			startDate = new Date(startTimeStamp);

			// ignore for now events before the initial command and initialize time stamp of
			// next
			// external command

			processLeadingExternalCommands();

			for (int aCommandIndex = 0; aCommandIndex < commands.size(); aCommandIndex++) {

				EHICommand existingCommand = commands.get(aCommandIndex);
				if (currentExistingCommand == null) { // first command
					currentExistingCommand = existingCommand;
					newCommands.add(existingCommand); // add the first command
				} else {
					last = currentExistingCommand;
					currentExistingCommand = existingCommand;
					if (aCommandIndex == commands.size() - 1 && nextExternalEventTimestamp < nextStartTime) {
//						// we are at the end of the existing command list
						processTrailingExternalCommands(nextStartTime);
//						
					} else if (nextExternalEventTimestamp >= last.getStartTimestamp() + last.getTimestamp()
							&& nextExternalEventTimestamp < currentExistingCommand.getStartTimestamp()
									+ currentExistingCommand.getTimestamp()) {

						processSandwichedExternalCommands(aHasPauseCommand);

					} else { // advance current command to find next sandwiched external command
						newCommands.add(existingCommand);
					}
				}
			}
			return super.addCommands(aSession, newCommands, nextStartTime);
		} 
//		else { // no external event
//			return super.addCommands(aSession, commands, nextStartTime);
//		}
	}

	public static String toCSVString(String[] aStrings) {
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
//long startTimeStamp = commands.get(0).getTimestamp2();
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
//		Date aDate2 = new Date(startTimeStamp);
//		
//		if (timestamp > startTimeStamp) {
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
