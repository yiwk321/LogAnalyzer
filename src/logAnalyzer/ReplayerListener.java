package logAnalyzer;

import java.util.List;
import java.util.Map;

import fluorite.commands.EHICommand;

public interface ReplayerListener {
	void newAssignment(	
			String anAssignment,
			Map<String, List<List<EHICommand>>> anAssignmentData);  
	
	void newStudent ( 
			String aStudent,
			List<List<EHICommand>> aNestedCommandList,
			long aTotalTimeSpent,
			long aWallClockTime,
			long[] aRestTimes);
	
	void newSession (
			int aSessionNumber);
			
			
	void newCommandInSession (	
			int aStartCommandIndex,
			long aCommandTime,
			EHICommand aStartCommand,
			String aStartCommandTypeChar,
			String anEventTypeString,
			boolean anInSession,
			String aRestType,
			String aText,
			int anEndCommandIndex,
			EHICommand anEndCommand);
			
}
