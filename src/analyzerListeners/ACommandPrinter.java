package analyzerListeners;

import java.util.List;
import java.util.Map;

import fluorite.commands.EHICommand;

public class ACommandPrinter implements CommandPrinter {

	@Override
	public void newAssignment(String anAssignment, Map<String, List<List<EHICommand>>> anAssignmentData) {
		System.out.println("New Assignment:" + anAssignment);
	}

	@Override
	public void newStudent(String aStudent, List<List<EHICommand>> aNestedCommandList, long aTotalTimeSpent,
			long aWallClockTime, long[] aRestTimes) {
		System.out.println("New Student:" + aStudent);

		
	}

	@Override
	public void newSession(int aSessionNumber) {
		System.out.println("New Session:" + aSessionNumber);
		
	}

	@Override
	public void newCommandInSession(int aStartCommandIndex, long aCommandTime, EHICommand aStartCommand,
			String aStartCommandTypeChar, String anEventTypeString, boolean anInSession, String aRestType, String aText,
			int anEndCommandIndex, EHICommand anEndCommand) {
			System.out.println("Start Command" + aStartCommand + "\n + end command " + anEndCommand);
	}

   

}
