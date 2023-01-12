package logAnalyzer.analyzers;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import fluorite.commands.EHICommand;

public abstract class LogAnalyzer<T> {
	Map<String, T> map = new HashMap<>();
	
	public abstract void read(Entry<String, List<List<EHICommand>>> entry);
	
	public File getOutput(File course) {
		File output = new File(course.getParent(), "\\stats\\" + course.getName() + " " + getSurfix() + ".csv");
		if (output.exists()) {
			output.delete();
		}
		return output;
	}
	
	public abstract String getSurfix();
	
	public abstract void write(File course, Map<String, List<Float>> gradeMap);
	public <T2> List<T2> getListFromMap(Map<String, List<T2>> map, String onyen) {
		if (map.containsKey(onyen)) {
			return map.get(onyen);
		}
		List<T2> list = new ArrayList<>();
		map.put(onyen, list);
		return list;
	}
	
	public long getWorkTime(List<List<EHICommand>> nestedCommands) {
		return totalTimeSpent(nestedCommands) - restTime(nestedCommands, 5 * 60 * 1000L, Long.MAX_VALUE)[0];
	}
	
	protected long totalTimeSpent(List<List<EHICommand>> nestedCommands){
		long projectTime = 0;
		try {
			for(int k = 0; k < nestedCommands.size(); k++) {
				List<EHICommand> commands = nestedCommands.get(k);
				if (commands.size() == 0) {
					continue;
				}
				int j = 0;
				for(; j < commands.size(); j++) {
					if (commands.get(j).getStartTimestamp() > 0 && commands.get(j).getTimestamp() > 1) {
						break;
					}
				}
				if (j >= commands.size()) {
					continue;
				}
				long timestamp1 = commands.get(j).getTimestamp() + commands.get(j).getStartTimestamp();
				EHICommand command2 = commands.get(commands.size()-1);
				long timestamp2 = command2.getStartTimestamp() + command2.getTimestamp();
				projectTime += timestamp2 - timestamp1;
			}
		} catch (Exception e) {
			return 0;
		}
		if (projectTime > 10000000L * 1000L) {
			System.out.println(projectTime);
		}
		return projectTime;
	}
	
	protected long[] restTime(List<List<EHICommand>> nestedCommands, long time, long time2) {
		long[] restTime = {0,0,0};
		for (int i = 0; i < nestedCommands.size(); i++) {
			List<EHICommand> commands = nestedCommands.get(i);
			EHICommand last = null;
			EHICommand cur = null;
			int k = 0;
			for(; k < commands.size(); k++) {
				if (commands.get(k).getStartTimestamp() > 0 && commands.get(k).getTimestamp() > 1) {
					break;
				}
			}
			for(; k < commands.size(); k++) {
				if (cur != null) {
					last = cur;
				}
				cur = commands.get(k);
				if (last != null && last.getTimestamp() > 0) {
					long diff = cur.getStartTimestamp() + cur.getTimestamp() - last.getTimestamp() - last.getStartTimestamp();
					if (diff > time) {
						restTime[0] += diff;
						if (diff < time2) {
							restTime[1]++;
							restTime[2] += diff;
						}
					}
				}
			}
		}
		return restTime;
	}

	public void addGrades(List<String> nextLine, List<Float> grades) {
		if (grades == null) {
			for (int i = 0; i < 3; i++) {
				nextLine.add("0");
			}
		} else {
			for (float grade : grades) {
				nextLine.add(grade+"");
			}
		}
	}
	
	public int getThreshold() {
//		int sum = 0;
//		T value = map.entrySet().iterator().next().getValue();
//		if (value instanceof List<?> && ((List) value).get(0) instanceof Integer) {
//			for (Entry<String, List<Integer>> entry : ((HashMap<String, List<Integer>>)map).entrySet()) {
//				sum += entry.getValue().stream().reduce(0, Integer::sum);
//			}
//			return sum / map.size() / 5;
//		} else {
			return 0;
//		}
	}
}
