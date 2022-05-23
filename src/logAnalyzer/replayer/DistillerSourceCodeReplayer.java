package logAnalyzer.replayer;

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

public class DistillerSourceCodeReplayer extends AnAssignmentReplayer {

	public static final String PROJECT = "C:\\Users\\dewan\\Downloads\\Assignment1A\\Assignment 1\\Beier, Isiah(Isiah Beier)\\Submission attachment(s)\\A1";
	public static final String SEPARATOR = File.separator;
//	private Analyzer analyzer;
	List<List<EHICommand>> nestedCommands;
	private String currentFileName;
	private int i, j = 0;
	private int currentSrc = 0;
	private String fileContent;
	static final long DAY = 24 * 3600 * 1000;
	static FileDistiller distiller = ChangeDistiller.createFileDistiller(Language.JAVA);
//	static File f1 = new File("E:\\Test\\524A5\\replaySrc0\\main\\lisp\\BasicOperationManager.java");
//	static File f2 = new File("E:\\Test\\524A5\\replaySrc0\\main\\lisp\\evaluator\\BasicOperationManager.java");

	public static void main(String[] args) {
//		printChangesBetweenCompile();
		printChangesBetweenSave(PROJECT);
//		printChangesByTime();
//		System.out.println(findFileChanges(f1, f2));
	}
	

	public static void printChangesByTime(String aProject) {
//		Analyzer analyzer = new AnAnalyzer();
		DistillerSourceCodeReplayer replayer = new DistillerSourceCodeReplayer();
//		analyzer.addAnalyzerListener(replayer);
		File[] src = replayer.initProject(aProject);
		long startTime = replayer.nestedCommands.get(0).get(1).getStartTimestamp();
		long endTime = startTime + DAY;
		for (int i = 0; i < 10; i++, startTime += DAY, endTime += DAY) {
			System.out.println("DAY " + (i + 1));
			Map<String, List<SourceCodeChange>> changes = replayer.findChanges(startTime, endTime, src);
			if (changes == null)
				return;
			for (String key : changes.keySet()) {
				if (changes.get(key).isEmpty()) {
					continue;
				}
				System.out.println(key);
				for (SourceCodeChange change : changes.get(key)) {
					System.out.println(change.getClass().getSimpleName() + "change:" + change.getChangedEntity() + " "
							+ change.getChangeType());
				}
				System.out.println();
			}
		}
		System.exit(0);
	}

	public static void printChangesBetweenCompile(String aProject) {
//		Analyzer analyzer = new AnAnalyzer();
		DistillerSourceCodeReplayer replayer = new DistillerSourceCodeReplayer();
//		analyzer.addAnalyzerListener(replayer);
		int count = 0;
		for (Map<String, List<SourceCodeChange>> changes : replayer.findChangesBetweenCompile(aProject, 0,
				Long.MAX_VALUE)) {
			if (changes == null)
				return;
			System.out.println("Copmile " + count++ + ":" + System.lineSeparator());
			for (String key : changes.keySet()) {
				if (changes.get(key).isEmpty()) {
					continue;
				}
				System.out.println(key);
				for (SourceCodeChange change : changes.get(key)) {
					System.out.println(change.getClass().getSimpleName() + "change:" + change.getChangedEntity() + " "
							+ change.getChangeType());
				}
				System.out.println();
			}
		}
		System.exit(0);
	}

	public static void printChangesBetweenSave(String aProject) {
//		Analyzer analyzer = new AnAnalyzer();
		DistillerSourceCodeReplayer replayer = new DistillerSourceCodeReplayer();
//		analyzer.addAnalyzerListener(replayer);
		int count = 0;
		for (Map<String, List<SourceCodeChange>> changes : replayer.findChangesBetweenSave(aProject, 0,
				Long.MAX_VALUE)) {
			if (changes == null)
				return;
			System.out.println("Save " + count++ + ":" + System.lineSeparator());
//			System.out.println(changes);
			for (String key : changes.keySet()) {
				if (changes.get(key).isEmpty()) {
					continue;
				}
				System.out.println(key);
				for (SourceCodeChange change : changes.get(key)) {
					System.out.println(change.getChangeType().toString() + ": " + change.getChangedEntity()
							+ System.lineSeparator() + "\tParent = " + change.getParentEntity() + " range = "
							+ change.getChangedEntity().getSourceRange());
//					System.out.println("**********************************************");
//					System.out.println("toString() = " + change.toString());
//					System.out.println("getClass() = " + change.getClass().getSimpleName());
//					System.out.println("getSignificanceLevel() = " + change.getSignificanceLevel());
//					System.out.println("getLabel() = " + change.getLabel());
//					SourceCodeEntity entity = change.getChangedEntity();
//					System.out.println();
//					System.out.println("changedEntity = " + entity);
//					System.out.println("entity.getLabel() = " + entity.getLabel());
//					System.out.println("entity.getModifiers() = " + entity.getModifiers());
//					System.out.println("entity.getUniqueName() = " + entity.getUniqueName());
//					System.out.println("entity.getAssociatedEntities() = " + entity.getAssociatedEntities());
//					System.out.println("entity.getSourceRange() = " + entity.getSourceRange());
//					System.out.println("entity.getType() = " + entity.getType());
//					
//					ChangeType changeType = change.getChangeType();
//					System.out.println();
//					System.out.println("changedType = " + changeType);
//					entity = change.getParentEntity();
//					System.out.println();
//					System.out.println("parentEntity = " + entity);
//					System.out.println("parentEntity.getLabel() = " + entity.getLabel());
//					System.out.println("parentEntity.getModifiers() = " + entity.getModifiers());
//					System.out.println("parentEntity.getUniqueName() = " + entity.getUniqueName());
//					System.out.println("parentEntity.getAssociatedEntities() = " + entity.getAssociatedEntities());
//					System.out.println("parentEntity.getSourceRange() = " + entity.getSourceRange());
//					System.out.println("parentEntity.getType() = " + entity.getType());
//					StructureEntityVersion rootEntity = change.getRootEntity();
//					System.out.println("rootEntity = " + rootEntity);
//					System.out.println("rootEntity.getUniqueName() = " + rootEntity.getUniqueName());
//					System.out.println("rootEntity.getVersion() = " + rootEntity.getVersion());
//					System.out.println("rootEntity.getSourceCodeChanges() = " + rootEntity.getSourceCodeChanges());
//					System.out.println("rootEntity.getType() = " + rootEntity.getType());
//					System.out.println();
//					
//					System.out.println(change.toString());
				}
				System.out.println();
//				return;
			}
		}
		System.exit(0);
	}
	
	
	public void replayTo (String aProject,  long aTime, String aDestination) {
		if (aDestination == null) {
			SimpleDateFormat format = new SimpleDateFormat("yy-MM-dd-HH-mm-ss");
			Date aDate = new Date(aTime);
			aDestination = aProject + File.separator + format.format(aDate);
		}
		initProjectSingleSource(aProject);
		File aDestinationFile = new File (aDestination);
		if (aDestinationFile.exists()) {
			FileUtility.deleteFolder(aDestinationFile);
		} else {
			aDestinationFile.mkdirs();
		}
		replayTo(aDestinationFile, aTime );
		
		
	}
	public List<Map<String, List<SourceCodeChange>>> findChangesBetweenSave(String aProject, long startTime,
			long endTime) {
		File[] src = initProject(aProject);
		replayTo(src[currentSrc], startTime);
		List<Map<String, List<SourceCodeChange>>> changesBetweenSave = new ArrayList<>();
		long currentTime = 0;
		boolean empty = false;
		for (int i = 0; i < nestedCommands.size(); i++) {
			List<EHICommand> commands = nestedCommands.get(i);
			for (int j = 0; j < commands.size(); j++) {
				EHICommand command = commands.get(j);
				if (command instanceof EclipseCommand
						&& ((EclipseCommand) command).getCommandID().equals("org.eclipse.ui.file.save")) {
					try {
						currentTime = command.getStartTimestamp() + command.getTimestamp();
						File src1 = src[currentSrc % 2];
						File src2 = src[(currentSrc + 1) % 2];
						if (!empty) {
							if (src2.exists()) {
								FileUtility.deleteFolder(src2);
							}
							FileUtility.copyDirectory(src1.getPath(), src2.getPath());
						}
//						if (src2.exists()) {
//							FileUtility.deleteFolder(src2);
//						}
//						FileUtility.copyDirectory(src1.getPath(), src2.getPath());
						replayTo(src2, currentTime);
						Map<String, List<SourceCodeChange>> changes = findProjectChanges(src1, src2);
						empty = true;
						for (List<SourceCodeChange> change : changes.values()) {
							if (!change.isEmpty()) {
								empty = false;
								break;
							}
						}
						if (!empty) {
//							System.out.println("is empty");
							changesBetweenSave.add(changes);
							currentSrc++;
						}
//						changesBetweenCompile.add(findProjectChanges(src1, src2));
					} catch (IOException e) {
						e.printStackTrace();
					}
//					currentSrc++;
				}
			}
		}
		for (File srcFolder : src) {
			FileUtility.deleteFolder(srcFolder);
		}
		return changesBetweenSave;
	}

	public List<Map<String, List<SourceCodeChange>>> findChangesBetweenCompile(String project, long startTime,
			long endTime) {
		File[] src = initProject(project);
		replayTo(src[currentSrc], startTime);
		List<Map<String, List<SourceCodeChange>>> changesBetweenCompile = new ArrayList<>();
		long currentTime = 0;
		for (int i = 0; i < nestedCommands.size(); i++) {
			List<EHICommand> commands = nestedCommands.get(i);
			for (int j = 0; j < commands.size(); j++) {
				EHICommand command = commands.get(j);
				if (command instanceof CompilationEventCommand && command.getDataMap().get("success").equals("true")) {
					try {
						currentTime = command.getStartTimestamp() + command.getTimestamp();
						File src1 = src[currentSrc % 2];
						File src2 = src[(currentSrc + 1) % 2];
						if (src2.exists()) {
							FileUtility.deleteFolder(src2);
						}
						FileUtility.copyDirectory(src1.getPath(), src2.getPath());
						replayTo(src[(currentSrc + 1) % 2], currentTime);
						changesBetweenCompile.add(findProjectChanges(src1, src2));
					} catch (IOException e) {
						e.printStackTrace();
					}
					currentSrc++;
				}
			}
		}
		return changesBetweenCompile;
	}
	
	public File[] initProject(String projectPath) {
		File project = new File(projectPath);
		File replaySrc0 = new File(project, "replaySrc0");
		File replaySrc1 = new File(project, "replaySrc1");
		File[] src = { replaySrc0, replaySrc1 };
		if (replaySrc0.exists()) {
			FileUtility.deleteFolder(replaySrc0);
		}
		if (replaySrc1.exists()) {
			FileUtility.deleteFolder(replaySrc1);
		}
		replaySrc0.mkdir();
		replaySrc1.mkdir();
//		nestedCommands = ReplayUtility.replayLogs(projectPath, analyzer);
		nestedCommands = readStudentNestedList(new File(projectPath));
		i = 0;
		j = 0;
		currentFileName = findFirstFilePath();
		return src;
	}
	
	public void initProjectSingleSource(String projectPath) {
		
		nestedCommands = readStudentNestedList(new File(projectPath));
		i = 0;
		j = 0;
		currentFileName = findFirstFilePath();
	}

	public String findFirstFilePath() {
		int[] idx = ReplayUtility.findFirstFile(nestedCommands);
		i = idx[0];
		j = idx[1];
		EHICommand command = nestedCommands.get(i).get(j);
		if (command instanceof FileOpenCommand) {
			j++;
			fileContent = command.getDataMap().get("snapshot");
			return ReplayUtility.getRelativeFilePath(command);
		}
		return "";
	}

	public Map<String, List<SourceCodeChange>> findChanges(long startTime, long endTime, File[] src) {
		if (currentSrc == 0) {
			replayTo(src[0], startTime);
		}
		try {
			File src1 = src[currentSrc % 2];
			File src2 = src[(currentSrc + 1) % 2];
			if (src2.exists()) {
				FileUtility.deleteFolder(src2);
			}
			FileUtility.copyDirectory(src1.getPath(), src2.getPath());
			replayTo(src2, endTime);
			currentSrc++;
			return findProjectChanges(src1, src2);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public Map<String, List<SourceCodeChange>> findProjectChanges(File src1, File src2) {
		Map<String, List<SourceCodeChange>> allChanges = new HashMap<>();
		try {
			List<Path> src1Files = FileUtility.listFiles(src1.toPath());
			List<Path> src2Files = FileUtility.listFiles(src2.toPath());
			HashMap<String, Path> src1Map = new HashMap<String, Path>();
			for (Path path : src1Files) {
				if (!path.toFile().isDirectory())
					src1Map.put(path.getFileName().toString(), path);
			}
			for (Path path : src2Files) {
				if (!path.toString().endsWith(".java")) {
					continue;
				}
				if (!path.toFile().isDirectory()) {
					Path path1 = src1Map.get(path.getFileName().toString());
					if (path1 != null) {
						List<SourceCodeChange> changes = findFileChanges(path1.toFile(), path.toFile());
						if (changes != null) {
							List<SourceCodeChange> oldChanges = allChanges.get(path.getFileName().toString());
//							List<SourceCodeChange> oldChanges = new ArrayList( allChanges.get(path.getFileName().toString()));

							if (oldChanges != null) {
								oldChanges = new ArrayList(oldChanges);
								oldChanges.addAll(changes);
							} else {
								allChanges.put(path.getFileName().toString(), changes);
							}
						}
					} else {
						File temp = new File(src1, "temp.java");
						if (temp.exists())
							temp.delete();
						temp.createNewFile();
						try (BufferedWriter bw = new BufferedWriter(new FileWriter(temp))) {
							String name = path.getFileName().toString();
							bw.write("public class " + name.substring(0, name.lastIndexOf(".java")) + " {}");
						} catch (IOException e) {
							e.printStackTrace();
						}
						List<SourceCodeChange> changes = findFileChanges(temp, path.toFile());
						if (changes != null) {
//							List<SourceCodeChange> oldChanges = allChanges.get(path.getFileName().toString());
							List<SourceCodeChange> oldChanges = allChanges.get(path.getFileName().toString());

							if (oldChanges != null) {
								try {
									oldChanges = new ArrayList(oldChanges);
								oldChanges.addAll(changes);
								} catch (Exception e) {
									e.printStackTrace();
								}
							} else {
								allChanges.put(path.getFileName().toString(), changes);
							}
						}
						temp.delete();
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return allChanges;
	}

	static final String ADD_FUN = "ADDITIONAL_FUNCTIONALITY";

	public static List<SourceCodeChange> findFileChanges(File file1, File file2) {
		try {
			distiller.extractClassifiedSourceCodeChanges(file1, file2);
			List<SourceCodeChange> changes = distiller.getSourceCodeChanges();
			List<String> newMethods = new ArrayList<>();
			StringBuilder content = FileUtility.readFile(file2);
			for (SourceCodeChange change : changes) {
				if (change.getLabel().equals(ADD_FUN)) {
					SourceCodeEntity entity = change.getChangedEntity();
//					String anEntityToString = entity.toString();
//					int aStartMethodIndex = anEntityToString.lastIndexOf('.');

					if (entity.isAbstract()) {
						continue;
					}
					String possiblyIncompleteFullMethod = content.substring(entity.getStartPosition(), entity.getEndPosition());
					int aStartIndex = entity.getStartPosition();
					int aCurlyIndex = possiblyIncompleteFullMethod.indexOf("{");
					if (aCurlyIndex >= 0) {
						aCurlyIndex += entity.getStartPosition();
					}
					else {
						int aMaxOffset = Math.min(20, content.length() - entity.getEndPosition());
						int anOffset = 0;
						for (; anOffset < aMaxOffset && content.charAt(entity.getEndPosition() + anOffset) != '{'
								; anOffset++)
							;
						if (anOffset < aMaxOffset) {
							aCurlyIndex = entity.getEndPosition() + anOffset;
							int aNewLineIndex = possiblyIncompleteFullMethod.lastIndexOf('\n');
							aStartIndex += aNewLineIndex + 1;
						} else {
							continue;
						}
					}

//
//					if (aStartMethodIndex >= 0) {
//						method = anEntityToString.substring(aStartMethodIndex + 1);
//					
//					} else {
//						method = content.substring(entity.getStartPosition(), entity.getEndPosition());
//
//						int anIndex = method.indexOf("{");
//						if (anIndex < 1) {
//							System.out.println("Method without {" + method);
//							continue;
//						} else {
					String methodHeader = content.substring(aStartIndex, aCurlyIndex);
					newMethods.add(methodHeader + "{}");
				}

			}
			if (!newMethods.isEmpty()) {
				content = FileUtility.readFile(file1);
				content.deleteCharAt(content.lastIndexOf("}"));
				for (String method : newMethods) {
					content.append(System.lineSeparator() + method);
				}
				content.append(System.lineSeparator() + "}");
				try (BufferedWriter bw = new BufferedWriter(new FileWriter(file1))) {
					bw.write(content.toString());
				}
				distiller.extractClassifiedSourceCodeChanges(file1, file2);
				List<SourceCodeChange> methodChanges = distiller.getSourceCodeChanges();
				if (!methodChanges.isEmpty()) {
					for (int i = 0, j = 0; i < changes.size(); i++) {
						SourceCodeChange change = changes.get(i);
						if (change.getLabel().equals(ADD_FUN)) {
							while (j < methodChanges.size() && !methodChanges.get(j).getRootEntity().getUniqueName()
									.equals(change.getChangedEntity().getUniqueName())) {
								j++;
							}
							if (methodChanges.size() < j) {
								methodChanges.add(j, change);
								j++;
							} else {
								methodChanges.add(change);
							}
						}
					}
					return methodChanges;
				}
			}
			return changes;
		} catch (Exception e) {
			System.err.println("Warning: error while change distilling. " + e.getMessage());
			e.printStackTrace();
		}
		return null;
	}

	FileEditor editor;

	public void replayTo(File src, long endTime) {
		if (fileContent == null) {
			editor = FileEditor.getEditor(src, currentFileName);
		} else {
			editor = FileEditor.getEditor(src, currentFileName, fileContent);
			fileContent = null;
		}
		for (; i < nestedCommands.size(); i++) {
			List<EHICommand> commands = nestedCommands.get(i);
			for (; j < commands.size(); j++) {
				EHICommand command = commands.get(j);
//				if (command.getTimestamp() == 5015283L) {
//					System.out.println ("found offending time stamp");
//				}
				long timestamp = command.getStartTimestamp() + command.getTimestamp();
				if (timestamp > endTime) {
					editor.writeToDisk();
//					System.out.println("Write to disk:" + editor.getContent());
					return;
				}
				if (command instanceof FileOpenCommand && !ReplayUtility.isNull(command.getDataMap().get("filePath"))) {
					if (editor != null) {
						editor.writeToDisk();// previous file
//						System.out.println("Write to disk:" + editor.getContent());

					}
					currentFileName = ReplayUtility.getRelativeFilePath(command);
//					if (currentFileName.contains("line/Line")) {
//						System.out.println("Found offending file");
//					}
					String snapshot = ReplayUtility.getSnapshot(command);
					if (!ReplayUtility.isNull(snapshot) && !snapshot.startsWith("null")) {
						editor = FileEditor.getEditor(src, currentFileName, snapshot);
					} else {
						File aCurrentFile = new File(src, currentFileName);
//						if (!editor.getFileName().equals(aCurrentFile.getAbsolutePath())) {
							if (aCurrentFile.exists()) {
								snapshot = FileUtility.readFile(aCurrentFile).toString();
								editor = FileEditor.getEditor(src, currentFileName, snapshot);

							} else {
								editor = FileEditor.getEditor(src, currentFileName);

							}
//						}

						
								 
//						editor = FileEditor.getEditor(src, currentFileName);
					}
				}
				if (command instanceof Insert) {
					int offset = Integer.parseInt(command.getAttributesMap().get("offset"));
					String text = command.getDataMap().get("text");
					editor.insert(offset, text);
				}
				if (command instanceof Delete) {
					int offset = Integer.parseInt(command.getAttributesMap().get("offset"));
					String text = command.getDataMap().get("text");
					editor.delete(offset, text);
				}
				if (command instanceof Replace) {
					int offset = Integer.parseInt(command.getAttributesMap().get("offset"));
					editor.replace(offset, command.getDataMap().get("deletedText"),
							command.getDataMap().get("insertedText"));
				}
				if (command instanceof MoveCaretCommand) {
					editor.moveCursor(Integer.parseInt(command.getAttributesMap().get("caretOffset")));
				}
			}
			j = 0;
		}
		editor.writeToDisk();// current file
//		System.out.println("Write to disk:" + editor.getContent());

	}

//	public DistillerReplayerPD(Analyzer anAnalyzer) {
//		super(anAnalyzer);
//		analyzer = anAnalyzer;
//	}
}
