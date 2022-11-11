package drivers;

import logAnalyzer.AContextBasedReplayer;
import logAnalyzer.ASemesterReplayer;
import logAnalyzer.AnAssignmentReplayer;
import logAnalyzer.AnExperimentReplayer;
import logAnalyzer.ReplayerListener;

public class ListenableFolderAnalyzer extends Driver{
	public static String[] folders;
//	public static String prefix = "Assignment ";
//	public static int[] assignments = {
//										0
////										1,
////										2,
////										3,
////										4,
////										5,
////										6
//										};
	
	public static void analyzeFolder(
			String aFolder, 
			boolean isGenerate,
			boolean isDelete,
			ReplayerListener[] aReplayerListeners) {
		folders = new String[] {aFolder};
		boolean generate = isGenerate;
//		boolean generate = false;

		for (String folder: folders) {
//			replayer = new AContextBasedReplayer(multiplier, defaultPauseTime);
			replayer = new AnAssignmentReplayer();
			for (ReplayerListener aListener:aReplayerListeners) {
				replayer.addReplayerListener(aListener);
			}

			path = folder;
			isRead = false;
			if (generate || isDelete) {
				delete();
				
				
			}
			if (generate) {
				try {
				generate(false); 
				} catch (Exception e) {
					e.printStackTrace();
				}
				
			}
			analyze();
		}
//			replayer = new AnExperimentReplayer(multiplier, defaultPauseTime);
//		}
//		for (String folder : folders) {
//			for (int assign : assignments) {
//				path = folder + File.separator + prefix + assign;
//				path = folderPath[0];
//				isRead = false;
//				if (generate) {
//					delete();
//					generate();
//				}
//				analyze();
//			}
//		}
//		System.exit(0);
	}
}