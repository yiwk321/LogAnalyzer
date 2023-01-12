package generators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import fluorite.commands.EHICommand;
import fluorite.commands.EndOfSessionCommand;
import fluorite.commands.LocalCheckCommand;
import logAnalyzer.Replayer;

public class LocalCheckCommandClassifier extends CommandGenerator{
//	Map<String, List<Integer>> testToLocalCheckCommandIndices;
//	List<EHICommand> commands;

	public LocalCheckCommandClassifier(String aStudent, Replayer aReplayer, CountDownLatch aLatch, Map<String, List<EHICommand>> aMap) {
		latch = aLatch;
		commandMap = aMap;
		replayer = aReplayer;
//		replayer.
	}
	
	public static void addIndex(Map<String, List<Integer>> aMap, String aString, int anIndex) {
		List<Integer> aList = aMap.get(aString);
		if (aList == null) {
			aList = new ArrayList();
			aMap.put(aString, aList);
		}
		aList.add(anIndex);
	}
	public static Map<String, List<Integer>> getTestToLocalCheckCommandIndices ( List<EHICommand> aCommands) {
		Map<String, List<Integer>> retVal = new HashMap();
		for (int index = 0; index < aCommands.size(); index++) {
			EHICommand aCommand = aCommands.get(index);
			if (!(aCommand instanceof LocalCheckCommand)) {
				continue;
			}
			LocalCheckCommand aLocalCheckCommand = (LocalCheckCommand) aCommand;
			String aTest =aLocalCheckCommand.getTestcase();
			addIndex(retVal, aTest, index);			
		}
		return retVal;
	}
	
	public static boolean isDifficultyPhase (
			
			List<EHICommand> aCommands, int aStartInden, int aStopIndex ) {
		// TO-DO
		
		return false;
	}
	
	
	public List<EHICommand> addCommands(int aSession, List<EHICommand> aCommands, long nextStartTime) {
		 
		List<EHICommand> newCommands = new ArrayList<>();
		EHICommand last = null;
		EHICommand cur = null;
		for (EHICommand command : aCommands) {
			
				newCommands.add(command);
			
		}
		EndOfSessionCommand aCommand = new EndOfSessionCommand(aSession);
		newCommands.add(aCommand);
		return newCommands;
	}
}
