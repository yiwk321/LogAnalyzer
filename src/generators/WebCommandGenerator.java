package generators;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import fluorite.commands.EHICommand;
import fluorite.commands.WebCommand;
import logAnalyzer.Replayer;

public class WebCommandGenerator extends PauseCommandGenerator{
	EHICommand webCommand = null;
	List<EHICommand> webCommands = null;
	Iterator<EHICommand> webCommandIterator = null;
	
	public WebCommandGenerator(Replayer replayer, CountDownLatch aLatch, Map<String, List<EHICommand>> commandMap, File studentFolder) {
		super(replayer, aLatch, commandMap);
		File browser = new File(studentFolder, "Browser");
		if (browser.exists() && browser.listFiles().length != 0) {
			webCommands = readWebCommands(browser.listFiles()[0]);
			if (webCommands != null) {
				webCommandIterator = webCommands.iterator();
			}
		}
	}
	
	public void addCommands(List<EHICommand> commands, List<EHICommand> newCommands) {
		EHICommand last = null;
		EHICommand cur = null;
		if (webCommand == null) {
//			webCommand = maybeAddWebCommandBeforeLogs(webCommandIterator, startTimestamp, newCommands);
			webCommand = webCommandIterator.next();
		}
		long timestamp = 0;
		if (webCommand != null) {
			timestamp = webCommand.getTimestamp() - commands.get(commands.size()-1).getStartTimestamp();
		}
		for (EHICommand command : commands) {
//			if (command.getTimestamp() + logStartTimestamp < startTimestamp || command.getTimestamp() + logStartTimestamp > endTimestamp) {
//				continue;
//			}
			if (cur == null) {
				cur = command;
				newCommands.add(command);
			} else {
				last = cur;
				cur = command;
				while (webCommand != null && timestamp >= last.getTimestamp() && timestamp <= cur.getTimestamp()) {
					webCommand.setStartTimestamp(command.getStartTimestamp());
					webCommand.setTimestamp(timestamp);
					maybeAddPauseCommand(newCommands, last,	webCommand);
					if (webCommandIterator.hasNext()) {
						last = webCommand;
						webCommand = webCommandIterator.next();
						timestamp = webCommand.getTimestamp() - command.getStartTimestamp();
					} else {
						webCommand = null;
						break;
					}
				}
				maybeAddPauseCommand(newCommands, last, cur);
			}
		}
	}
	
//	private EHICommand maybeAddWebCommandBeforeLogs(Iterator<EHICommand> iterator, long startTimestamp, List<EHICommand> commands) {
//		if (iterator == null) {
//			return null;
//		}
//		EHICommand webCommand = null;
//		long timestamp = 0;
//		while((webCommand = iterator.next()) != null && (timestamp = webCommand.getTimestamp() - startTimestamp) < 0) {
//			webCommand.setStartTimestamp(0);
//			webCommand.setTimestamp(timestamp);
//			commands.add(webCommand);
//		}
//		return webCommand;
//	}
	
	protected List<EHICommand> readWebCommands(File file){
		if (!file.exists()) {
			return null;
		}
		List<EHICommand> retVal = new ArrayList<>();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(file));
			String nextLine;
			SimpleDateFormat format = new SimpleDateFormat("M/d/yyyy hh:mm:ss");
			Date date;
			String keyword;
			String url;
			WebCommand webCommand;
			while ((nextLine = br.readLine()) != null) {
				try {
					String[] tokens = nextLine.split("\t");
					if (tokens.length >= 3) {
						date = format.parse(tokens[0]);
						keyword = tokens[1];
						url = tokens[2];
						if (keyword.contains("google.com/url?") || keyword.equals(url)) {
							continue;
						}
						webCommand = new WebCommand(keyword, url);
						webCommand.setTimestamp(date.getTime());
						retVal.add(0, webCommand);
					} else {
						System.out.println("Failed to parse WebCommand");
					}
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return retVal;
	}
}
