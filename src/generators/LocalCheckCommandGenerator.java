package generators;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import fluorite.commands.EHICommand;
import fluorite.commands.LocalCheckCommand;
import logAnalyzer.Replayer;

public class LocalCheckCommandGenerator extends PauseCommandGenerator {
	List<String[]> studentLC;
	int i = 0;
	SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

	public LocalCheckCommandGenerator(Replayer replayer, CountDownLatch aLatch, Map<String, List<EHICommand>> commandMap, List<String[]> events) {
		super(replayer, aLatch, commandMap);
		studentLC = events;
	}
	
	public void addCommands(List<EHICommand> commands, List<EHICommand> newCommands) {
		if (i < studentLC.size()) {
			EHICommand last = null;
			EHICommand cur = null;
			String[] event = null;
			long timestamp = 0;
			event = studentLC.get(i);
			try {
				timestamp = df.parse(event[1]).getTime();
			} catch (ParseException e) {
				e.printStackTrace();
			}
			for (EHICommand command : commands) {
				if (cur == null) {
					cur = command;
					newCommands.add(command);
				} else {
					last = cur;
					cur = command;
					if (timestamp >= last.getStartTimestamp()+last.getTimestamp() && timestamp < cur.getStartTimestamp() + cur.getTimestamp()) {
						while (timestamp < cur.getStartTimestamp() + cur.getTimestamp() && i < studentLC.size()) {
							LocalCheckCommand command2 = new LocalCheckCommand(event[2]);
							command2.setStartTimestamp(last.getStartTimestamp());
							command2.setTimestamp(timestamp-last.getStartTimestamp());
							newCommands.add(command2);
							i++;
							if (i >= studentLC.size()) {
								break;
							}
							event = studentLC.get(i);
							try {
								timestamp = df.parse(event[1]).getTime();
							} catch (ParseException e) {
								e.printStackTrace();
							}
						}
					}
					newCommands.add(command);
				}
			}
			commands = newCommands;
			newCommands = new ArrayList<>();
		}
		super.addCommands(commands, newCommands);
	}
}
