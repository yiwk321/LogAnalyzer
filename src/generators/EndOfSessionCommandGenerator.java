package generators;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import fluorite.commands.EHICommand;
import fluorite.commands.EndOfSessionCommand;
import logAnalyzer.Replayer;

public class EndOfSessionCommandGenerator extends CommandGenerator{
	public EndOfSessionCommandGenerator(Replayer aReplayer, CountDownLatch aLatch, Map<String, List<EHICommand>> aMap) {
		latch = aLatch;
		commandMap = aMap;
		replayer = aReplayer;
	}

	
	
	public List<EHICommand> addCommands(int aSession, List<EHICommand> commands, long nextStartTime) {
		 
		List<EHICommand> newCommands = new ArrayList<>();
		EHICommand last = null;
		EHICommand cur = null;
		for (EHICommand command : commands) {
			
				newCommands.add(command);
			
		}
		EndOfSessionCommand aCommand = new EndOfSessionCommand(aSession);
		newCommands.add(aCommand);
		return newCommands;
	}
}
