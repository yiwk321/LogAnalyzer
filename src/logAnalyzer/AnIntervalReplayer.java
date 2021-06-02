package logAnalyzer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import fluorite.commands.EHICommand;

public class AnIntervalReplayer extends AContextBasedReplayer{
	public AnIntervalReplayer(double multiplier, int defaultPauseTime) {
		super(multiplier, defaultPauseTime);
	}
	
	public long[] getWorkTime(File student, long start, long end) {
		initPauseMap();
		Map<String, List<EHICommand>> commandMap = readStudent(student);
		long[] retVal = new long[2];
		if (commandMap == null) {
			System.err.println("Error: Cannot read student log");
			retVal[0] = -1;
			retVal[1] = -1;
			return retVal;
		}
		List<List<EHICommand>> nestedCommands = new ArrayList<>();
		out:
		for (String logFile : commandMap.keySet()) {
			List<EHICommand> commands = commandMap.get(logFile);
			int startIndex = 0;
			int lastIndex = commands.size()-1;
			EHICommand last = commands.get(lastIndex);
			long lastTimestamp = last.getStartTimestamp() + last.getTimestamp();
			if (lastTimestamp < start) {
				continue;
			}
			for (; startIndex < commands.size(); startIndex++) {
				EHICommand command = commands.get(startIndex);
				long timestamp = command.getStartTimestamp() + command.getTimestamp();
				if (end > 0 && timestamp > end) {
					break out;
				} else if (timestamp > start) {
					break;
				}
			}
			for (; lastIndex >= 0; lastIndex--) {
				EHICommand command = commands.get(lastIndex);
				long timestamp = command.getStartTimestamp() + command.getTimestamp();
				if (end == 0 || timestamp < end) {
					break;
				}
			}
			nestedCommands.add(commands.subList(startIndex, lastIndex+1));
		}
		long totalTime = totalTimeSpent(nestedCommands);
		long[] restTime = restTime(nestedCommands, defaultPauseTime);
		retVal[0] = totalTime - restTime[0];
		retVal[1] = totalTime - restTime[1];
		return retVal;
	}
}
