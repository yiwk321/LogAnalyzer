package generators;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import analyzer.extension.replayView.FileUtility;
import au.com.bytecode.opencsv.CSVReader;
import fluorite.commands.CheckStyleCommand;
import fluorite.commands.EHICommand;
import fluorite.commands.LocalCheckCommand;
import fluorite.commands.LocalChecksRawBatchCommand;
import fluorite.commands.PauseCommand;
import logAnalyzer.LogAnalyzerLoggerFactory;
import logAnalyzer.Replayer;
// make this subclass of web generator?
//public class LocalCheckCommandGenerator extends PauseCommandGenerator {
public class LocalChecksRawBatchCommandGenerator extends ExternalCommandGenerator {

	
////	int lastAddedExternalIndex = 0;
//	String[] checkStyleChecks;
//	protected List<String[]> emptyList = new ArrayList();
//	
//	public static File[] listFolders(File aParent) {
//		File[] aFolders = aParent.listFiles(new FileFilter() {
//            public boolean accept(File file) {
//             return file.isDirectory();
//            }});
////		if (aFolders.length == 0) {
////			return null;
////		}
//		return aFolders;
//	}
//	
//	public static File findFirstBFDescendantMatching (File aRoot, String aRegularExpression) {
//		if (aRoot.isFile()) {
//			return null;
//		}
//		File[] aFiles = aRoot.listFiles(new FilenameFilter() {
//                  public boolean accept(File dir, String name) {
//                   return name.matches(aRegularExpression);
//                  }});
//		if (aFiles.length == 0) {
//			File[] aFolders = listFolders(aRoot);
//			if (aFolders.length == 0) {
//				return null;
//			}
//			for (File aFolder:aFolders) {
//				File retVal = findFirstBFDescendantMatching(aFolder, aRegularExpression);
//				if (retVal != null) {
//					return retVal;
//				}
//			}
//			return null;
//		}
//		return aFiles[0];
//	}
//	
//	protected String student;
	File[] localChecksRawFiles;
	List<String[]> localChecksTuples = new LinkedList();
	List<String> localChecksEvents = new LinkedList();
	List<Long> recentTimes = new LinkedList();

	static String assignmentWithNumber;
	static final String ASSIGNMENT = "assignment";
	
	public static String extractAssignmentWithNumber (String aString) {
		Pattern p = Pattern.compile("assignment[ _-]*\\d+");
		Matcher m = p.matcher(aString); 
		while (m.find()) {
			return "assignment" +  extractNumber(m.group());
		}
		return "";
	}
	
	public static int extractNumber (String aString) {
		Pattern p = Pattern.compile("\\d+");
		Matcher m = p.matcher(aString); 
		while (m.find()) {
			return Integer.parseInt(m.group());
		}
		return -1;
	}
	
	public static synchronized void setAssignmentWithNumber(String aStudent) {
		if (assignmentWithNumber == null) {
		String aStudentLowerCase = aStudent.toLowerCase();
		String anAssignmentWithNumber = extractAssignmentWithNumber(aStudentLowerCase);
		assignmentWithNumber = anAssignmentWithNumber; 
		}
	}

	
	
	public LocalChecksRawBatchCommandGenerator(Replayer replayer, CountDownLatch aLatch, String aStudent, Map<String, List<EHICommand>> commandMap) {
		super(replayer, aLatch, commandMap);
		setAssignmentWithNumber(aStudent);
//		checkStyleEvents = readCheckstyleEvents(aStudent);
//		student = aStudent;
		File[]  aLocalChecksRawFiles = null;
		try {
		aLocalChecksRawFiles = FileUtility.getLocalChecksRawsLogFiles(aStudent );
		} catch (Exception e) {
			System.err.println("Could not find local checks raw logs files for " + aStudent);
			e.printStackTrace();
			
		}
		File aTargetLocalChecksRawFile = null;
		
		if (aLocalChecksRawFiles != null && assignmentWithNumber != null) {
			for (File aFile:aLocalChecksRawFiles) {
				if (aFile.getName().toLowerCase().contains(assignmentWithNumber)) {
					aTargetLocalChecksRawFile = aFile;
					break;
				}
			}
		}
		if (aTargetLocalChecksRawFile != null) {
			FileUtility.getRecentEvents(aTargetLocalChecksRawFile, 
					localChecksEvents, localChecksTuples, recentTimes, 0, 1, df);
	
		}
		localChecksRawFiles = aLocalChecksRawFiles;
		
	}
	
//	public static String toCSVString (String[] aStrings) {
//		if (aStrings.length == 0) {
//			return "";
//		}
//		StringBuilder stringBuilder = new StringBuilder();
//		stringBuilder.append(aStrings[0]);
//		for (int anArrayIndex = 1; anArrayIndex < aStrings.length; anArrayIndex++) {
//			stringBuilder.append(',');
//			stringBuilder.append(aStrings[anArrayIndex]);
//		}
//		return stringBuilder.toString();
//	}
//	List<String[]> checkStyleEvents = new ArrayList();
////	public static File findLocalChecksFolder (String aStudent) {
////		File aStudentFile = new File(aStudent);
////		return findFirstBFDescendantMatching(aStudentFile, "LocalChecks");
////	}
//	public static File findLogsFolder (String aStudent) {
//		File aStudentFile = new File(aStudent);
//		return findFirstBFDescendantMatching(aStudentFile, "Logs");
//	}
//	public static File getCheckstyleLog (String aStudent) {
//		File aLogsFolder = findLogsFolder(aStudent);
//		if (aLogsFolder == null) {
//			return null;
//		}
//		File retVal = new File(aLogsFolder, "LocalChecks/CheckStyle_All.csv");
//		if (!retVal.exists())
//			return null;
//		return retVal;
//	}
//	public static List<String[]>  readCheckstyleEvents(String aStudent) {
//		List<String[]> retVal = new ArrayList();
////		String aCheckStyleAllFileName = aStudent + "/Submission attachment(s)/Logs/LocalChecks/CheckStyle_All.csv";
////		File aFile = new File(aCheckStyleAllFileName);
//		File aCheckStyleFile = getCheckstyleLog(aStudent);
////		checkStyleEvents.clear();
//		if (aCheckStyleFile == null || !aCheckStyleFile.exists()) {
//			return retVal;
//		}
//
//		try {
//			
//			
//				CSVReader cr = new CSVReader(new FileReader(aCheckStyleFile));
//				String[] nextLine = null;
//				while ((nextLine = cr.readNext()) != null) {
//					retVal.add(nextLine);
//				}
//				cr.close();
//				return retVal;
//			
//
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//			return retVal;
//		} catch (IOException e) {
//			e.printStackTrace();
//			return retVal;
//		} 
//	}

	
//	public static boolean hasPauseCommand(List<EHICommand> commands) {
//		for (EHICommand aCommand:commands) {
//			if (aCommand instanceof PauseCommand) {
//				return true;
//			}
//		}
//		return false;
//	}

//}
protected boolean hasNextExternalEvent() {
	return lastAddedExternalIndex < localChecksEvents.size();
}
protected String[] previousEvent;
//static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE MMM dd HH:mm:ss z yyyy");
// SimpleDateFormat df = new SimpleDateFormat("EEEE MMM dd HH:mm:ss z yyyy");
SimpleDateFormat df = new SimpleDateFormat("EEEE MMM dd HH:mm:ss z yyyy");

protected void logCommand (EHICommand aCommand) {
	if (aCommand == null) {
		System.err.println("null command");
		return;
	}
//	if (studentName.contains("Dare")) {
//		System.out.println("Logged command " + lastAddedExternalIndex + " " +aCommand);
//		if (lastAddedExternalIndex == 32) {
//			System.out.println("Approaching end:");
//		}
//	}
	try {
	
	LogAnalyzerLoggerFactory.getLogAnalyzerAssignmentMetrics().numLocaCheckRawBatchCommands++;
	} catch (Exception e) {
		System.err.println("Could noot parse date of:" + aCommand);
		e.printStackTrace();
	}
}

//SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

protected long getNextExternalEventTimeStamp() {
	String[] event = localChecksTuples.get(lastAddedExternalIndex);
	previousEvent = event;
	return recentTimes.get(lastAddedExternalIndex);

////	Thu May 27 12:40:29 EDT 2021
//	try {
//		return df.parse(event[1]).getTime();
//	} catch (Exception e) {
//		// TODO Auto-generated catch block
//		e.printStackTrace();
//		return -1;
//	}
}

protected static List emptyList = new ArrayList();
protected List<EHICommand>  createExternalCommands(boolean fromPreviousEvent) {
	String[] anEvent = previousEvent;
	if (!fromPreviousEvent) {
	 anEvent = localChecksTuples.get(lastAddedExternalIndex);
	}
	EHICommand aCommand = new LocalChecksRawBatchCommand(toCSVString(anEvent));
	List<EHICommand> retVal = new ArrayList<>();
	retVal.add(aCommand);
//	System.out.println("Adding aCommand " + aCommand + " for " + student);
	return retVal;
}

public List<EHICommand> addCommands(int aSession, List<EHICommand> commands, long nextStartTime) {
       	
	return super.addCommands(aSession, commands, nextStartTime);
	

}

//public static void main (String[] args) {
//	String aDataString1 = "Thu May 27 16:40:29 EDT 2021";
////	TemporalAccessor accessor = formatter.parse(aDataString1);
//	try {
//		Date date = df.parse(aDataString1);
//		System.out.println(date);
//	} catch (ParseException e) {
//		// TODO Auto-generated catch block
//		e.printStackTrace();
//	}
//	
//	
//}


}
