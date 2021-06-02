package logAnalyzer;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Scanner;

public class IntervalDriver {
	public static final File STUDENT_FOLDER = new File("C:\\Users\\Zhizhou\\eclipse2021-workspace\\A1");
	//Parse string to long if the date if a string
	public static final DateFormat DF = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");
	//For context based work time, a pause is considered a rest if pauseTime > MULTIPLIER * context-based-threshold
	public static final double MULTIPLIER = 1;
	//DEFAULT_THRESHOLD in minutes
	//For fixed work time, a pause is considered a rest if pauseTime > DEFAULT_THRESHOLD
	//For context based work time, a threshold = DEFAULT_THRESHOLD if it is undefined for that type of command
	public static final int DEFAULT_THRESHOLD = 5;
	public static final long START_TIME = 0;
	public static final long END_TIME = Long.MAX_VALUE;
	public static final boolean CONSOLE_OUTPUT = false;
	
	public static void main(String[] args) {
		if (!STUDENT_FOLDER.exists()) {
			System.err.println("Error: Student Folder does not exist");
			return;
		}
		AnIntervalReplayer replayer = new AnIntervalReplayer(MULTIPLIER, DEFAULT_THRESHOLD);
		/* getWorkTime(File studentFolder, long startTime, long endTime)
		 * returns a long[] workTimes where 
		 * workTimes[0] is the context based work time
		 * workTimes[1] is the fixed work time 
		 * returns {-1, -1} if failed
		 */
		long[] workTimes = replayer.getWorkTime(STUDENT_FOLDER, START_TIME, END_TIME);
		System.out.println("Context Based Work Time: " + replayer.format(workTimes[0]));
		System.out.println("Fixed Work Time (" + DEFAULT_THRESHOLD + "min): " + replayer.format(workTimes[1]));
	}
	
	public static long parseTime(String date, Scanner scanner) {
		try {
			return DF.parse(date).getTime();
		} catch (ParseException e) {
			e.printStackTrace();
			return -1;
		}
	}
}
