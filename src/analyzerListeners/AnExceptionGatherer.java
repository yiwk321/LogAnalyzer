package analyzerListeners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fluorite.commands.EHExceptionCommand;
import fluorite.commands.EHICommand;
import fluorite.commands.ExceptionCommand;

public class AnExceptionGatherer extends AnExceptionPrinter implements ExceptionGatherer {
	Map<String, Map<String, List<List<StackTraceData>>>> stackTraces = new HashMap();
	Map<String, List<List<StackTraceData>>> currentAssignmentMap;
	List<List<StackTraceData>> currentStudentSessions;
	List<StackTraceData> currentStudentSession;
	String currentAssignment, currentStudent;
	int currentSession;

	@Override
	public Map<String, Map<String, List<List<StackTraceData>>>> getStackTraces() {
		return stackTraces;
	}
	
	@Override
	public void newAssignment(String anAssignment, Map<String, List<List<EHICommand>>> anAssignmentData) {
		System.out.println("New Assignment:" + anAssignment);
		currentAssignmentMap = new HashMap();
		stackTraces.put(anAssignment, currentAssignmentMap);
	}

	@Override
	public void newStudent(String aStudent, List<List<EHICommand>> aNestedCommandList, long aTotalTimeSpent,
			long aWallClockTime, long[] aRestTimes) {
		System.out.println("New Student:" + aStudent);
		currentStudentSessions = new ArrayList<>();
		currentAssignmentMap.put(aStudent, currentStudentSessions);
	}

	@Override
	public void newSession(int aSessionNumber) {
		System.out.println("New Session:" + aSessionNumber);
		currentStudentSession = new ArrayList<>();
		currentStudentSessions.add(currentStudentSession);
	}
	
	@Override
	public void newCommandInSession(int aStartCommandIndex, long aCommandTime, EHICommand aStartCommand,
			String aStartCommandTypeChar, String anEventTypeString, boolean anInSession, String aRestType, String aText,
			int anEndCommandIndex, EHICommand anEndCommand) {
		if (!aStartCommandTypeChar.equals("E")) {
			return;
		}
			super.newCommandInSession(aStartCommandIndex, aCommandTime, aStartCommand, aStartCommandTypeChar, anEventTypeString, anInSession, aRestType, aText, anEndCommandIndex, anEndCommand);
		
		if (aStartCommand instanceof ExceptionCommand) {
			ExceptionCommand anExceptionCommand = (ExceptionCommand) aStartCommand;
			
			System.out.println (anExceptionCommand.getOutputText());
			currentStudentSession.add(new StackTraceData(null, null));
		}
		if (aStartCommand instanceof EHExceptionCommand) {
			EHExceptionCommand anExceptionCommand = (EHExceptionCommand) aStartCommand;
			
			System.out.println (anExceptionCommand.getOutputText());
		}

	}

}
