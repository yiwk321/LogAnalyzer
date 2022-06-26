package generators;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.eclipse.core.internal.utils.FileUtil;

import ch.uzh.ifi.seal.changedistiller.ast.FileUtils;
import fluorite.commands.EHICommand;
import logAnalyzer.Replayer;



public class SourceCodeOnRunGenerator extends LocalCheckCommandGenerator{
	Map<String, List<EHICommand>> commandMap;
	File checkpointsFolder;
	
	public static void deleteFolder (File aFolder) throws IOException {
		Files.walk(aFolder.toPath())
		.sorted(Comparator.reverseOrder())
		.map(Path::toFile)
		.forEach(File::delete);
	}
	
	public SourceCodeOnRunGenerator(Replayer replayer, CountDownLatch aLatch, String aStudent,
			Map<String, List<EHICommand>> aCommandMap, List<String[]> events) {
		super(replayer, aLatch, aStudent, aCommandMap, events);
		commandMap = aCommandMap;
		String[] aFileNames = aCommandMap.keySet().toArray(new String[0]);
		if (aFileNames.length > 0) {
			File aFile = new File (aFileNames[0]);
			
			File aLogsFolder = aFile.getParentFile().getParentFile();
			checkpointsFolder = new File(aLogsFolder, "SourceCheckPoints");
			try { 
			if (checkpointsFolder.exists()) {
			
				 deleteFolder(checkpointsFolder);
				
			
			}
			checkpointsFolder.mkdir();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
//		for (String aFileName:aCommandMap.keySet().toArray()) {
//			File aFile = new File(aFileName);
//			Folder aParentFolder = aFile.getParent();
//		}
		
		// TODO Auto-generated constructor stub
	}
	public List<EHICommand> addCommands(int aSession, List<EHICommand> commands, long nextStartTime) {
		List<EHICommand> aCommandsWithLocalChecks = super.addCommands(aSession, commands, nextStartTime);
		File aSessionFolder = new File(checkpointsFolder, Integer.toString(aSession));
		if (!aSessionFolder.exists()) { // this should always be true
			aSessionFolder.mkdir();
		}
		return aCommandsWithLocalChecks;
	}

}
