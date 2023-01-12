package logAnalyzer.analyzers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONObject;

import analyzer.extension.replayView.FileUtility;
import au.com.bytecode.opencsv.CSVWriter;
import fluorite.commands.EHICommand;

public class PiazzaAndZoomAnalyzer extends LogAnalyzer<List<Integer>> {
	Map<String, List<Integer>> piazzaPostMap = new HashMap<>();
	Map<String, List<Integer>> zoomChatMap = new HashMap<>();
	List<JSONObject> piazzaPosts;
	Map<String, Integer> zoomSessionMap = new HashMap<>();
	Map<String, Integer> zoomTimeMap = new HashMap<>();
	Map<String, List<Long>> workTimeMap = new HashMap<>();
	
	public void read(File course) {
		JSONObject piazzaPostsJson = FileUtility.readJSON(getPiazza(course));
		File[] studentFolders = getA1(course).listFiles((file) -> {
			return file.isDirectory() && !file.getName().equals("ZoomChatsAnon");
		});
		for (File studentFolder : studentFolders) {
			String folderName = studentFolder.getName();
			String onyen = folderName.substring(folderName.indexOf("(") + 1, folderName.length() - 1);

			List<Integer> list = getListFromMap(zoomChatMap, onyen);
			File[] zoomSession = studentFolder.listFiles((file) -> {
				return !file.isDirectory() && file.getName().contains("ZoomSession");
			});
			File zoom = null;
			if (zoomSession.length == 1) {
				zoom = zoomSession[0];
				zoomSessionMap.put(onyen,
						Integer.parseInt(zoom.getName().substring(0, zoom.getName().indexOf("Zoom"))));
			} else {
				zoomSessionMap.put(onyen, 0);
			}

			int numOH = 0;
			int numDiary = 0;
			int numQuestion = 0;
			int numPosts = 0;
			for (JSONObject post : findPiazzaPosts(onyen, piazzaPostsJson)) {
				numPosts++;
				if (post.getString("root_subject").equals("Protocol for Synchronous Interaction")
						|| post.getBoolean("is_office_hour_request")) {
					numOH++;
				}
				if (post.getBoolean("root_is_diary")) {
					numDiary++;
				}
				if (post.getString("type").equals("question")) {
					numQuestion++;
				}
			}
			list = getListFromMap(piazzaPostMap, onyen);
			list.add(numPosts - numOH - numDiary);
			list.add(numDiary);
			list.add(numQuestion);
		}
		File zoomtime = getZoomTimes(course);
		if (zoomtime == null) {
			return;
		}
		String[] zoomSessionTimesString = FileUtility.readFile(zoomtime).toString().split("\\R");
		String onyen = null;
		for (int i = 0; i < zoomSessionTimesString.length; i++) {
			String s = zoomSessionTimesString[i];
			if (s.length() > 15) {
				onyen = s.substring(s.lastIndexOf("(") + 1, s.lastIndexOf(")"));
			} else if (s.startsWith("Total: ")) {
				zoomTimeMap.put(onyen, Integer.parseInt(s.substring(7)));
			}
		}
	}
	
	private List<JSONObject> findPiazzaPosts(String onyen, JSONObject piazzaPostsJson) {
		List<JSONObject> list = new ArrayList<>();
		String author = onyen + "(" + onyen + "@live.unc.edu)";
		if (!piazzaPostsJson.has(author)) {
			return list;
		}
		JSONArray posts = piazzaPostsJson.getJSONArray(author);
		for (Object post : posts) {
			if (!(post instanceof JSONObject)) {
				continue;
			}
			JSONObject postJson = (JSONObject) post;
			list.add(postJson);
		}
		return list;
	}
	
	@Override
	public String getSurfix() {
		// TODO Auto-generated method stub
		return "Stats";
	}
	@Override
	public void write(File course, Map<String, List<Float>> gradeMap) {
		String[] headers = { "#piazza posts", "#diary", "#question", "# OH", "total OH time", "total OH time(s)",
				"total work time", "total work time(s)" };
		System.out.println(course.getPath());
		File grades = getGrades(course);
		if (grades == null) {
			return;
		}
		String[] lines = FileUtility.readFile(grades).toString().split("\\R");
		String line1 = lines[0];
		String[] firstLine1 = line1.substring(0, line1.indexOf("\"")).split(",");
		String[] firstLine2 = line1.substring(line1.indexOf("\"")).split("\",\"");
		List<String> nextLine = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			nextLine.add(firstLine1[i]);
		}
		for (int i = 0; i < headers.length; i++) {
			nextLine.add(headers[i]);
		}
		for (int i = 0; i < firstLine2.length; i++) {
			nextLine.add(firstLine2[i]);
		}
		nextLine.add("Total assign grade");
		nextLine.add("Total quiz grade");
		nextLine.add("Total exam grade");

		try (CSVWriter cw = new CSVWriter(new FileWriter(getOutput(course)))) {
			cw.writeNext(nextLine.toArray(headers));

			for (int i = 1; i < lines.length; i++) {
				nextLine.clear();
				String[] line = lines[i].split(",");
				String onyen = line[2];
				for (int j = 0; j < 3; j++) {
					nextLine.add(line[j]);
				}
				List<Integer> piazzaPosts = getListFromMap(piazzaPostMap, onyen);
				if (piazzaPosts.size() == 0) {
					for (int j = 0; j < 3; j++) {
						nextLine.add("0");
					}
				} else {
					for (int j = 0; j < 3; j++) {
						nextLine.add(piazzaPosts.get(j) + "");
					}
				}

				Integer zommSessions = zoomSessionMap.get(onyen);
				nextLine.add(zommSessions == null ? "0" : zommSessions.toString());
				nextLine.add(
						convertToHourMinuteSecond(zoomTimeMap.get(onyen) == null ? 0 : zoomTimeMap.get(onyen) * 1000L));
				nextLine.add(zoomTimeMap.get(onyen) == null ? "0" : zoomTimeMap.get(onyen) + "");
				List<Long> times = getListFromMap(workTimeMap, onyen);
				long totalTime = 0;
				for (long time : times) {
					totalTime += time;
				}
				nextLine.add(convertToHourMinuteSecond(totalTime));
				nextLine.add((int) (totalTime / 1000) + "");
				float assignSum = 0;
				float quizSum = 0;
				float examSum = 0;
				for (int j = 0; j < firstLine2.length; j++) {
					String name = firstLine2[j];
					String grade = line[j + 3];
					if (grade.isEmpty()) {
						grade = "0";
					}
					float gradef = Float.parseFloat(grade);
					if (name.contains("Assignment")) {
						assignSum += gradef;
					} else if (name.startsWith("Midterm") || name.startsWith("Final")) {
						examSum += gradef;
					} else {
						quizSum += gradef;
					}
					nextLine.add(grade);
				}
				nextLine.add(assignSum + "");
				nextLine.add(quizSum + "");
				nextLine.add(examSum + "");
				cw.writeNext(nextLine.toArray(headers));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	public File getA1(File course) {
		File a1 = new File(course, "Assignment 1");
		if (a1.exists()) {
			return a1;
		}
		return null;
	}

	public File getPiazza(File course) {
		File[] files = course.listFiles((file) -> {
			return file.getName().endsWith(".json");
		});
		if (files.length >= 1) {
			return files[0];
		}
		return null;
	}

	public File getGrades(File course) {
		File grades = new File(course, "gradebook_exportAnon.csv");
		if (grades.exists()) {
			return grades;
		}
		return null;
	}

	public File getZoomTimes(File course) {
		File zoomTime = new File(course, "ZoomSessionTimesSeconds.txt");
		if (zoomTime.exists()) {
			return zoomTime;
		}
		return null;
	}
	
	@Override
	public void read(Entry<String, List<List<EHICommand>>> entry) {
		// TODO Auto-generated method stub
		
	}
	
	protected String convertToHourMinuteSecond(long timeSpent) {
		int hour = (int) (timeSpent / 3600000);
		int minute = (int) (timeSpent % 3600000 / 60000);
		int second = (int) (timeSpent % 60000 / 1000);
		return hour + ":" + (minute < 10 ? "0" + minute : minute) + ":" + (second < 10 ? "0" + second : second);
	}

//	public File getOutput(File course) {
//		File output = new File(course.getParent(), "\\stats\\" + course.getName() + " Stats.csv");
//		if (output.exists()) {
//			output.delete();
//		}
//		return output;
//	}
}
