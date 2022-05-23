package drivers;

import logAnalyzer.replayer.DistillerSourceCodeReplayer;

public class SourceCodeReplayerDriverPD {
	public static final String PROJECT = "C:\\Users\\dewan\\Downloads\\Assignment1A\\Assignment 1\\Beier, Isiah(Isiah Beier)\\Submission attachment(s)\\A1";
	public static final long START_TIME = 1622092284233L;
	public static final long OFFSET = 5193161L;
//	5015283L
	public static final long END_TIME = START_TIME + OFFSET;
	public static void main(String[] args) {
		DistillerSourceCodeReplayer aReplayer = new DistillerSourceCodeReplayer();
		aReplayer.replayTo(PROJECT, END_TIME, null); 
//		printChangesBetweenCompile();
//		printChangesByTime();
//		System.out.println(findFileChanges(f1, f2));
	}
}
