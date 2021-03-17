package generators;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import analyzer.Analyzer;
import fluorite.commands.EHICommand;
import fluorite.commands.LocalCheckCommand;

public class LocalCheckCommandGenerator extends PauseCommandGenerator {
	List<String[]> studentLC;
	
	public LocalCheckCommandGenerator(Analyzer anAnalyzer, CountDownLatch aLatch, File[] logs, Integer aThreadCount, List<String[]> events, String aSurfix) {
		super(anAnalyzer, aLatch, logs, aThreadCount, aSurfix);
		studentLC = events;
	}
	
	public void addCommands(List<EHICommand> commands, List<EHICommand> newCommands) {
		SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		EHICommand last = null;
		EHICommand cur = null;
		int i = 0;
		String[] event = studentLC.get(i);
		long timestamp = 0;
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
					while (timestamp < cur.getStartTimestamp() + cur.getTimestamp() && i+1 < studentLC.size()) {
						LocalCheckCommand command2 = new LocalCheckCommand(event[2]);
						command2.setStartTimestamp(last.getStartTimestamp());
						command2.setTimestamp(timestamp-last.getStartTimestamp());
						newCommands.add(command2);
//						events.add(event[2]);
						i++;
						event = studentLC.get(i);
						try {
							timestamp = df.parse(event[1]).getTime();
						} catch (ParseException e) {
							e.printStackTrace();
						}
					}
//					LocalCheckCommand command2 = new LocalCheckCommand(events);
//					command2.setStartTimestamp(last.getStartTimestamp());
//					command2.setTimestamp(timestamp-last.getStartTimestamp());
//					newCommands.add(command2);
				}
			}
		}
		commands.clear();
		super.addCommands(newCommands, commands);
	}
}
