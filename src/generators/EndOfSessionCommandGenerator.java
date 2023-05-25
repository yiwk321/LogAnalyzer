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
		boolean hasSessionCommand = false;
		for (EHICommand command : commands) {
			
				newCommands.add(command);
				if (command instanceof EndOfSessionCommand) {
					hasSessionCommand = true;
				}
				
			
		}
		if (hasSessionCommand) {
			return newCommands;
		}
		EndOfSessionCommand aCommand = new EndOfSessionCommand(aSession);
		aCommand.setStartTimestamp(ChainedCommandGenerator.getStartTimestamp(commands));
		long aMaxTime = ChainedCommandGenerator.getMaxTime(commands);
		aCommand.setTimestamp(aMaxTime - aCommand.getStartTimestamp());
		newCommands.add(aCommand);
		return newCommands;
	}
}
