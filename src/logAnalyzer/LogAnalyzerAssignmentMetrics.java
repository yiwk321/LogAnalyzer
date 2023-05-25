package logAnalyzer;

public class LogAnalyzerAssignmentMetrics {
	public static int numLocalCheckSessions;
	public static int numLocaCheckCommands;
	public static int numLocaCheckRawBatchCommands;
	public static int numPiazzaPosts;
	public static int numCheckstyleCommands;

	public static int numZoomSessions;
	public String toString() {
		return 
				"Number of commands:" + numLocalCheckSessions + "\n" +
				"Number of localcheck sessions:" + numLocalCheckSessions + "\n" +
				"Number of localcheck commands:" + numLocaCheckCommands + "\n" +
				"Number of localcheck batch raw commands:" + numLocaCheckRawBatchCommands + "\n" +

				"Number of Piazza posts:" + numPiazzaPosts + "\n" +
				"Number of Checkstyle commands:" + numCheckstyleCommands + "\n" +
				"Number of Zoom Sessions:" + numZoomSessions + "\n";
	
	}

}
