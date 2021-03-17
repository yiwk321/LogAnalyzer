package generators;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import analyzer.Analyzer;
import fluorite.commands.EHICommand;
import fluorite.util.EHLogReader;
import logAnalyzer.AReplayer;

public abstract class CommandGenerator implements Runnable {
	Analyzer analyzer;
	CountDownLatch latch;
	File[] logs;
	Integer threadCount;
	String surfix;
	
	protected long getLogFileCreationTime(File file) {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS");
		try {
			return df.parse(file.getName().substring(3, 27)).getTime();
		} catch (ParseException e) {
			e.printStackTrace();
			return 0;
		}
	}

	public List<EHICommand> readOneLogFile(String path, Analyzer analyzer){
		EHLogReader reader = new EHLogReader();
		File log = new File(path);
		if (!log.exists()) {
			System.out.println("log does not exist:" + path);
			return new ArrayList<EHICommand>();
		}

		if (!path.endsWith(".xml")) {
			System.out.println("log is not in xml format:" + path);
			return new ArrayList<EHICommand>();
		}

		System.out.println("Reading " + path);
		try {
			List<EHICommand> commands = reader.readAll(path);
			sortCommands(commands, 0, commands.size()-1);
			return commands;
		} catch (Exception e) {
			System.out.println("Could not read file" + path + e);
		}
		return new ArrayList<EHICommand>();
	}

	private void sortCommands(List<EHICommand> commands, int start, int end){
		for(int i = 0; i < commands.size(); i++) {
			if (commands.get(i) == null) {
				commands.remove(i);
				i--;
			}
		}
		EHICommand command = null;
		long cur = 0;
		for(int i = 2; i < commands.size(); i++) {
			command = commands.get(i);
			cur = command.getStartTimestamp()+command.getTimestamp();
			int j = i-1;
			while (j > 1){
				if (commands.get(j).getStartTimestamp() + commands.get(j).getTimestamp() > cur) {
					j--;
				} else {
					break;
				}
			}
			if (j < i-1) {
				commands.remove(i);
				commands.add(j+1, command);
			}
		}
	}
	
	public void run() {
		try {
			for (File file : logs) {
				List<EHICommand> commands = readOneLogFile(file.getPath(), analyzer);
				if (commands.size() < 2) {
					continue;
				}
				List<EHICommand> newCommands = new ArrayList<>();
				if (commands.get(commands.size()-1).getStartTimestamp() == 0) {
					long startTimestamp = getLogFileCreationTime(file);
					for (EHICommand command : commands) {
						command.setStartTimestamp(startTimestamp);
					}
				}
				addCommands(commands, newCommands);
				String logContent = AReplayer.XML_START1 + getLogFileCreationTime(file) + AReplayer.XML_START2 + AReplayer.XML_VERSION + AReplayer.XML_START3;
				for (EHICommand c : newCommands) {
					logContent += c.persist();
				}
				logContent += AReplayer.XML_FILE_ENDING;
				try {
					File newLog = new File(file.getParent()+File.separator+surfix+File.separator+file.getName());
					if (newLog.exists()) {
						newLog.delete();
					}
					newLog.getParentFile().mkdirs();
					newLog.createNewFile();
					BufferedWriter writer = new BufferedWriter(new FileWriter(newLog, true));
					System.out.println("Writing to file " + newLog.getPath());
					writer.write(logContent);
					writer.close();
					System.out.println("Finished writing to file " + newLog.getPath());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}finally {
			synchronized (this) {
				threadCount--;
			}
			latch.countDown();
		}
	}
	
	public abstract void addCommands(List<EHICommand> commands, List<EHICommand> newCommands);
}
