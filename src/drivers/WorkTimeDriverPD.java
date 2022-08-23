package drivers;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Scanner;

import logAnalyzer.AnIntervalReplayerWithEdits;

public class WorkTimeDriverPD {
//	public static final File STUDENT_FOLDER = new File("E:\\Test\\Assignment 4\\Zheng, Chongyi(harryzcy)");
	public static final File STUDENT_FOLDER = new File("C:\\Users\\dewan\\Downloads\\Assignment1A\\Assignment 1\\Beier, Isiah(Isiah Beier)");

	//Parse time stamps to long if they are strings
	public static final DateFormat DF = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");
	//For context based work time, a pause is considered a rest if pauseTime > MULTIPLIER * context-based-threshold
	public static final double MULTIPLIER = 1;
	//DEFAULT_THRESHOLD in minutes
	//For fixed work time, a pause is considered a rest if pauseTime > DEFAULT_THRESHOLD
	//For context based work time, a threshold = DEFAULT_THRESHOLD if it is undefined for that type of command
	public static final int DEFAULT_THRESHOLD = 5;
	public static final long START_TIME = 1621567798869L;
	public static final long OFFSET = 5228370L;
	public static final long END_TIME = START_TIME + OFFSET;
	public static final boolean CONSOLE_OUTPUT = false;
	
	public static void main(String[] args) {
		if (!STUDENT_FOLDER.exists()) {
			System.err.println("Error: Student Folder does not exist");
			return;
		}
		AnIntervalReplayerWithEdits replayer = new AnIntervalReplayerWithEdits(MULTIPLIER, DEFAULT_THRESHOLD, false);
		/* 
		 * getWorkTime(File studentFolder, long startTime, long endTime)
		 * returns a long[] workTimes where 
		 * workTimes[0] = context based work time, -1 if failed
		 * workTimes[1] = fixed work time, -1 if failed
		 */
		long[] workTimes = replayer.getWorkTime(STUDENT_FOLDER, START_TIME, END_TIME);
		System.out.println("Context Based Work Time: " + format(workTimes[0]));
		System.out.println("Fixed Work Time (" + DEFAULT_THRESHOLD + "min): " + format(workTimes[1]));
		int[] edits = replayer.getEdits(STUDENT_FOLDER, START_TIME, START_TIME);
		System.out.println("Insert: " + edits[0]);
		System.out.println("Delete: " + edits[1]);
	}
	
	public static long parseTime(String date, Scanner scanner) {
		try {
			return DF.parse(date).getTime();
		} catch (ParseException e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	protected static String format(long time){
		boolean negative = false;
		if (time < 0) {
			negative = true;
			time = -1 * time;
		}
		long hour = time / 3600000;
		long minute = time % 3600000 / 60000;
		long second = time % 60000 / 1000;
		return negative?"-":"" + String.format("%d:%02d:%02d", hour, minute, second);
	}
}
