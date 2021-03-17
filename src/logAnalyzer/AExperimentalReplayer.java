package logAnalyzer;

import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import analyzer.Analyzer;
import au.com.bytecode.opencsv.CSVReader;
import fluorite.commands.EHICommand;
import generators.WebCommandGenerator;

public class AExperimentalReplayer extends AReplayer{
	private Map<String, List<List<EHICommand>>> data;
	private static final int THREAD_LIM = 6;
	private static final String TIMESTAMP = "C:\\Users\\Zhizhou\\Desktop\\timestamp for each participant.csv";
	private Map<String, Long[]> timestamps = new HashMap<>();
	
	public AExperimentalReplayer(Analyzer anAnalyzer) {
		super(anAnalyzer);
		data = new HashMap<>();
		System.setProperty("http.agent", "Chrome");
	}
	
	public void readTimestamp() {
		try {
			SimpleDateFormat df = new SimpleDateFormat("HH:mm");
			File timestampFile = new File(TIMESTAMP);
			CSVReader cr = new CSVReader(new FileReader(timestampFile));
			cr.readNext();
			String[] token = null;
			while ((token = cr.readNext()) != null) {
				long startTime = df.parse(token[4]).getTime();
				if (startTime < 15*3600*1000) {
					startTime += 12*3600*1000;
				}
				long endTime = df.parse(token[5]).getTime();
				if (endTime < 15*3600*1000) {
					endTime += 12*3600*1000;
				}
				Long[] times = {startTime, endTime};
				timestamps.put(token[0], times);
			}
			cr.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void createWebCommandLogs(String classFolderPath, String surfix) {
		File folder = new File(classFolderPath);
		if (!folder.exists()) {
			System.out.println("Class Folder does not exist");
			System.exit(0);
		}
		File[] students = folder.listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				return pathname.isDirectory();
			}
		});
		latch = new CountDownLatch(students.length);
		for (int j = 0; j < students.length; j++) {
			File studentFolder  = students[j];
			File logFolder = new File(studentFolder,"Eclipse");
			if (!logFolder.exists()) {
				latch.countDown();
				continue;
			}
			System.out.println("Reading " + studentFolder.getName());
			File[] logs = new File(logFolder.getPath()).listFiles(new FileFilter() {
				public boolean accept(File pathname) {
					return pathname.getName().startsWith("Log") && pathname.getName().endsWith(".xml");
				}
			});
			Thread thread = new Thread(new WebCommandGenerator(analyzer, latch, logs, threadCount, studentFolder, surfix));
			while(true) {
				if (threadCount > THREAD_LIM) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				
				if (threadCount <= THREAD_LIM) {
					synchronized (this) {
						threadCount++;
						thread.start();
						break;
					}
				}
			}
		}
		try {
			latch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			System.exit(0);
		}
	}

	public void analyzeFolder(String classFolderPath) {
		File folder = new File(classFolderPath);
		if (!folder.exists()) {
			System.out.println("Class Folder does not exist");
			System.exit(0);
		}
		File[] participants = folder.listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				return pathname.isDirectory();
			}
		});
		for (int i = 0; i < participants.length; i++) {
			File participantFolder = new File(participants[i],"Eclipse");
			if (!participantFolder.exists()) {
				continue;
			}
			System.out.println("Reading student " + participantFolder.getName());
			data.put(participants[i].getName(), replayLogs(participantFolder.getPath(), analyzer));
		}
//		createAssignData("Experiment", folder, data);
//		createDistributionData("Experiment", folder, data);
		createPauseDistribution("Experiment", folder, data);
//		createWebStats("Experiment", folder, data);
		System.exit(0);
	}
}
