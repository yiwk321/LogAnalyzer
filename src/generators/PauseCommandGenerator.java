package generators;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import fluorite.commands.EHICommand;
import fluorite.commands.PauseCommand;
import logAnalyzer.Replayer;

public class PauseCommandGenerator extends CommandGenerator{
	public PauseCommandGenerator(Replayer aReplayer, CountDownLatch aLatch, Map<String, List<EHICommand>> aMap) {
		latch = aLatch;
		commandMap = aMap;
		replayer = aReplayer;
	}

	public void maybeAddPauseCommand(List<EHICommand> newCommands, EHICommand last, EHICommand cur) {
		long rest = cur.getTimestamp()-last.getTimestamp();
		if (rest >= 1*Replayer.ONE_SECOND) {
			PauseCommand command = new PauseCommand(last, cur, rest);
			command.setStartTimestamp(last.getStartTimestamp());
			command.setTimestamp(last.getTimestamp()+1);
			newCommands.add(command);
		} 
		newCommands.add(cur);
	}
	public static boolean hasPauseCommand(List<EHICommand> commands) {
		for (EHICommand aCommand:commands) {
			if (aCommand instanceof PauseCommand) {
				return true;
			}
		}
		return false;
	}

	public List<EHICommand> addCommands(int aSession, List<EHICommand> commands, long nextStartTime) {
		if (hasPauseCommand(commands)) {
			return commands;
		}
		List<EHICommand> newCommands = new ArrayList<>();
		EHICommand last = null;
		EHICommand cur = null;
		for (EHICommand command : commands) {
			if (cur == null) {
				cur = command;
				newCommands.add(command);
			} else {
				last = cur;
				cur = command;
				maybeAddPauseCommand(newCommands, last, cur);
			}
		}
		return newCommands;
	}
}
