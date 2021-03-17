package generators;

import java.io.File;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import analyzer.Analyzer;
import fluorite.commands.EHICommand;
import fluorite.commands.PauseCommand;
import logAnalyzer.AReplayer;

public class PauseCommandGenerator extends CommandGenerator{
	Analyzer analyzer;
	CountDownLatch latch;
	File[] logs;
	Integer threadCount;
	
	public PauseCommandGenerator(Analyzer anAnalyzer, CountDownLatch aLatch, File[] logs, Integer aThreadCount, String aSurfix) {
		analyzer = anAnalyzer;
		latch = aLatch;
		this.logs = logs;
		threadCount = aThreadCount;
		surfix = aSurfix;
	}

	public void maybeAddPauseCommand(List<EHICommand> newCommands, EHICommand last, EHICommand cur) {
		long rest = cur.getTimestamp()-last.getTimestamp();
		if (rest >= 1*AReplayer.ONE_SECOND) {
			String range = "";
			if (rest < 2*AReplayer.ONE_SECOND) {
				range = "1s-2s";
			} else if (rest < 5*AReplayer.ONE_SECOND) {
				range = "2s-5s";
			} else if (rest < 10*AReplayer.ONE_SECOND) {
				range = "5s-10s";
			} else if (rest < 20*AReplayer.ONE_SECOND) {
				range = "10s-20s";
			} else if (rest < 30*AReplayer.ONE_SECOND) {
				range = "20s-30s";
			} else if (rest < AReplayer.ONE_MIN) {
				range = "30s-1m";
			} else if (rest < AReplayer.TWO_MIN) {
				range = "1m-2m";
			} else if (rest < AReplayer.FIVE_MIN) {
				range = "2m-5m";
			} else if (rest < AReplayer.TEN_MIN) {
				range = "5m-10m";
			} else if (rest < 3*AReplayer.TEN_MIN) {
				range = "10m-30m";
			} else if (rest < 6*AReplayer.TEN_MIN) {
				range = "30m-60m";
			} else {
				range = ">1h";
			}
			PauseCommand command = new PauseCommand(last, cur, rest, range);
			command.setStartTimestamp(last.getStartTimestamp());
			command.setTimestamp(last.getTimestamp()+1);
			newCommands.add(command);
		} 
		newCommands.add(cur);
	}

	public void addCommands(List<EHICommand> commands, List<EHICommand> newCommands) {
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
	}
}
