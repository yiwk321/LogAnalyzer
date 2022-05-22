package drivers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.eclipse.swt.internal.win32.INITCOMMONCONTROLSEX;

import analyzer.AnAnalyzer;
import analyzer.Analyzer;
import analyzer.extension.ADifficultyPredictionAndStatusPrinter;
import analyzer.extension.replayView.FileEditor;
import analyzer.extension.replayView.FileUtility;
import analyzer.extension.replayView.ReplayUtility;
import ch.uzh.ifi.seal.changedistiller.ChangeDistiller;
import ch.uzh.ifi.seal.changedistiller.ChangeDistiller.Language;
import ch.uzh.ifi.seal.changedistiller.distilling.FileDistiller;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeEntity;
import fluorite.commands.CompilationEventCommand;
import fluorite.commands.Delete;
import fluorite.commands.EHICommand;
import fluorite.commands.EclipseCommand;
import fluorite.commands.FileOpenCommand;
import fluorite.commands.Insert;
import fluorite.commands.MoveCaretCommand;
import fluorite.commands.Replace;
import logAnalyzer.AnAssignmentReplayer;
import logAnalyzer.Replayer;
import logAnalyzer.replayer.DistillerSourceCodeReplayer;

public class DistillerReplayerDriverPD extends AnAssignmentReplayer {

	public static final String PROJECT = "C:\\Users\\dewan\\Downloads\\Assignment1A\\Assignment 1\\Beier, Isiah(Isiah Beier)\\Submission attachment(s)\\A1";
//	public static final String SEPARATOR = File.separator;
////	private Analyzer analyzer;
//	List<List<EHICommand>> nestedCommands;
//	private String currentFileName;
//	private int i, j = 0;
//	private int currentSrc = 0;
//	private String fileContent;
//	static final long DAY = 24 * 3600 * 1000;
//	static FileDistiller distiller = ChangeDistiller.createFileDistiller(Language.JAVA);
////	static File f1 = new File("E:\\Test\\524A5\\replaySrc0\\main\\lisp\\BasicOperationManager.java");
////	static File f2 = new File("E:\\Test\\524A5\\replaySrc0\\main\\lisp\\evaluator\\BasicOperationManager.java");

	public static void main(String[] args) {
//		printChangesBetweenCompile();
		new DistillerSourceCodeReplayer().printChangesBetweenSave(PROJECT);
//		printChangesByTime();
//		System.out.println(findFileChanges(f1, f2));
	}
	

	

//	public DistillerReplayerPD(Analyzer anAnalyzer) {
//		super(anAnalyzer);
//		analyzer = anAnalyzer;
//	}
}
