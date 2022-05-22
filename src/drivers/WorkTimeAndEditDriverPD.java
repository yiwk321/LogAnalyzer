package drivers;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

import analyzer.logAnalyzer.AnIntervalReplayer;

public class WorkTimeAndEditDriverPD {
//	public static final File STUDENT_FOLDER = new File("E:\\Test\\Assignment 4\\Zheng, Chongyi(harryzcy)");
	public static final File STUDENT_FOLDER = new File("C:\\Users\\dewan\\Downloads\\Assignment1A\\Assignment 1\\Beier, Isiah(Isiah Beier)");

	//Path to student folder
//	public static final File STUDENT_FOLDER = new File("C:\\Users\\Zhizhou\\eclipse2021-workspace\\A1");
	//Parse time stamps to long if they are strings
	public static final DateFormat DF = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");
	//For context based work time, a pause is considered a rest if pauseTime > MULTIPLIER * context-based-threshold
	public static final double MULTIPLIER = 1;
	//DEFAULT_THRESHOLD in minutes
	//For fixed work time, a pause is considered a rest if pauseTime > DEFAULT_THRESHOLD
	//For context based work time, a threshold = DEFAULT_THRESHOLD if it is undefined for that type of command
	public static final int DEFAULT_THRESHOLD = 5;
//	public static final long START_TIME = 0;
//	public static final long END_TIME = Long.MAX_VALUE;
	//Print traces messages 
	public static final boolean TRACE = false;
	public static final long START_TIME = 1622092284233L;
	public static final long OFFSET = 5193161L;
	public static final long END_TIME = START_TIME + OFFSET;
	
	public static void main(String[] args) {
		if (!STUDENT_FOLDER.exists()) {
			System.err.println("Error: Student Folder does not exist");
			return;
		}
		AnIntervalReplayer replayer = new AnIntervalReplayer(MULTIPLIER, DEFAULT_THRESHOLD, TRACE);
		//getStartTime(File studentFolder) returns the wall time of first FileOpenCommand in the project. -1 when failed
//		long startTime = replayer.getStartTime(STUDENT_FOLDER);
		long startTime = START_TIME;
		//getEndTime(File studentFolder) returns the wall time of last edit, -1 when failed
//		long endTime = replayer.getEndTime(STUDENT_FOLDER);
		long endTime = END_TIME;
		System.out.println("Start Time: " + parseWallTime(startTime));
		System.out.println("End Time: " + parseWallTime(endTime));
		
		/* 
		 * getWorkTime(File studentFolder, long startTime, long endTime)
		 * returns a long[] workTimes where 
		 * workTimes[0] = context based work time, -1 if failed
		 * workTimes[1] = fixed work time, -1 if failed
		 */
		long[] workTimes = replayer.getWorkTime(STUDENT_FOLDER, startTime, endTime);
		System.out.println("Context Based Work Time: " + format(workTimes[0]));
		System.out.println("Fixed Work Time (" + DEFAULT_THRESHOLD + "min): " + format(workTimes[1]));
		int[] edits = replayer.getEdits(STUDENT_FOLDER, startTime, endTime);
		System.out.println("Insert: " + edits[0]);
		System.out.println("Delete: " + edits[1]);
		/* 
		 * getDistanceAndProcedures(File studentFolder, long startTime, long endTime)
		 * returns a Map<String, List<String>> offsetAndProcedureMap where 
		 * the keys are file names edited and 
		 * the 1st value of the list is the maximum distance of the edits in the file and
		 * the following values are the procedures in the range
		 */
		Map<String, List<String>> distanceAndProcedureMap = replayer.getDistanceAndProcedures(STUDENT_FOLDER, startTime, endTime);
		for (Entry<String, List<String>> entry : distanceAndProcedureMap.entrySet()) {
			System.out.println("Class: " + entry.getKey());
			List<String> value = entry.getValue();
			System.out.println("Offset Range: " + value.get(0));
			System.out.println("Procedure in the range: ");
			for (int i = 1; i < value.size(); i++) {
				System.out.print(value.get(i)+" ");
			}
			System.out.println();
		}
		System.exit(0);
	}
	
	public static long parseTime(String date, Scanner scanner) {
		try {
			return DF.parse(date).getTime();
		} catch (ParseException e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	public static String parseWallTime(long time) {
		return new Date(time).toString();
	}
	
	protected static String format(long time){
		String sign = "";
		if (time < 0) {
			sign += "-";
			time = -1 * time;
		}
		long hour = time / 3600000;
		long minute = time % 3600000 / 60000;
		long second = time % 60000 / 1000;
		return sign + String.format("%d:%02d:%02d", hour, minute, second);
	}
}
