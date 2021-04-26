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
	
	public List<EHICommand> addCommands(List<EHICommand> commands, long nextStartTime) {
		if (i < studentLC.size()) {
			List<EHICommand> newCommands = new ArrayList<>();
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
//			for (EHICommand command : commands) {
			for (int j = 0; j < commands.size(); j++) {
				EHICommand command = commands.get(j);
				if (cur == null) {
					cur = command;
					newCommands.add(command);
				} else {
					last = cur;
					cur = command;
					if (j == commands.size()-1 && timestamp < nextStartTime) {
						LocalCheckCommand command2 = new LocalCheckCommand(event[2]);
						while (timestamp < nextStartTime && i < studentLC.size()) {
							command2 = new LocalCheckCommand(event[2]);
							if (last.getStartTimestamp() == 0) {
								command2.setStartTimestamp(cur.getStartTimestamp());
								command2.setTimestamp(timestamp-cur.getStartTimestamp());
							} else {
								command2.setStartTimestamp(last.getStartTimestamp());
								command2.setTimestamp(timestamp-last.getStartTimestamp());
							}
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
					} else if (timestamp >= last.getStartTimestamp()+last.getTimestamp() && timestamp < cur.getStartTimestamp() + cur.getTimestamp()) {
						LocalCheckCommand command2 = new LocalCheckCommand(event[2]);
						while (timestamp < cur.getStartTimestamp() + cur.getTimestamp() && i < studentLC.size()) {
							command2 = new LocalCheckCommand(event[2]);
							if (last.getStartTimestamp() == 0) {
								command2.setStartTimestamp(cur.getStartTimestamp());
								command2.setTimestamp(timestamp-cur.getStartTimestamp());
							} else {
								command2.setStartTimestamp(last.getStartTimestamp());
								command2.setTimestamp(timestamp-last.getStartTimestamp());
							}
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
//						maybeAddPauseCommand(newCommands, command2, cur);
					} else {
						newCommands.add(command);
					}
				}
			}
			return super.addCommands(newCommands, nextStartTime);
		} else {
			return super.addCommands(commands, nextStartTime);
		}
	}
}
