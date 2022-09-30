package generators;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

import analyzer.extension.replayView.FileUtility;
import fluorite.commands.CheckStyleCommand;
import fluorite.commands.EHICommand;
import fluorite.commands.PiazzaPostCommand;
import logAnalyzer.Replayer;

public class PiazzaCommandGenerator extends ExternalCommandGenerator {

	String student;
//	String studentKey;
//	List<JSONObject> studentPosts;
//	List<JSONObject> officeHourRequests;
	List<JSONObject> piazzaPosts;
	Pattern studentNamePattern = Pattern.compile(".*\\\\(.*), (.*)\\((.*)\\)");
	
	public PiazzaCommandGenerator(Replayer replayer, CountDownLatch aLatch, String aStudent, 
								  Map<String, List<EHICommand>> commandMap, JSONObject piazzaPosts) {
		super(replayer, aLatch, commandMap);
		student = aStudent;
		this.piazzaPosts = new ArrayList<>();
//		String piazzaPostsString = FileUtility.readFile(piazzaPostsFile).toString();
//		System.out.println(piazzaPostsString);
//		JSONObject piazzaPosts = new JSONObject(piazzaPostsString);
//		studentKey = findKeyMatchingStudent(aStudent, piazzaPosts.keySet());
		findPiazzaPosts(aStudent, piazzaPosts);
		try {
			this.piazzaPosts.sort((o1, o2)->{
				return Long.compare(o1.getLong("time"), o2.getLong("time")); 
			});
		} catch (IllegalArgumentException e) {
			// TODO: handle exception
			e.printStackTrace();
			Comparators.verifyTransitivity((o1, o2)->{
				return (int)(o1.getLong("time") - o2.getLong("time")); 
			}, this.piazzaPosts);
			System.out.println("");
		}
	}
	
//	private String findKeyMatchingStudent(String student, Set<String> keyset) {
//		Matcher matcher = studentNamePattern.matcher(student);
//		if (!matcher.matches()) {
//			return null;
//		}
//
//		String onyen = matcher.group(3);
//		String firstName = matcher.group(2);
//		String lastName = matcher.group(1);
//		List<String> firstNameMatches = new ArrayList<>(); 
//		List<String> lastNameMatches = new ArrayList<>();
//		boolean firstNameMatched = false;
//		boolean lastNameMatched = false;
//		
//		for (String key : keyset) {
//			if (key.contains(onyen+"@")) {
//				return key;
//			}
//			firstNameMatched = key.contains(firstName);
//			lastNameMatched = key.contains(lastName);
//			if (firstNameMatched && lastNameMatched) {
//				return key;
//			}
//			if (firstNameMatched) {
//				firstNameMatches.add(key);
//			}
//			if (lastNameMatched) {
//				lastNameMatches.add(key);
//			}
//		}
//		
//		if (firstNameMatches.size() == 1 && lastNameMatches.size() == 0) {
//			return firstNameMatches.get(0);
//		}
//		if (firstNameMatches.size() == 0 && lastNameMatches.size() == 1) {
//			return lastNameMatches.get(0);
//		}
//		return null;
//	}

	private void findPiazzaPosts(String student, JSONObject piazzaPostsJson) {
//		officeHourRequests = findOfficeHourRequests(student, piazzaPosts);
//		studentPosts = findStudentPosts(student, piazzaPosts);
//		studentPosts = new ArrayList<>();
//		officeHourRequests = new ArrayList<>();
//		piazzaPosts = new ArrayList<>();
		Matcher matcher = studentNamePattern.matcher(student);
		if (!matcher.matches()) {
			return;
		}
		String onyen = matcher.group(3);
//		String firstName = matcher.group(2);
//		String lastName = matcher.group(1);
		String author = onyen+ "(" + onyen + "@live.unc.edu)";
		if (!piazzaPostsJson.has(author)) {
			return;
		}
		JSONArray posts = piazzaPostsJson.getJSONArray(author);
		for (Object post : posts) {
			if (!(post instanceof JSONObject)) {
				continue;
			}
			JSONObject postJson = (JSONObject)post;
//			if (postJson.getBoolean("is_office_hour_request")) {
//				officeHourRequests.add(postJson);
//			} else {
//				studentPosts.add(postJson);
//			}
			piazzaPosts.add(postJson);
		}
	}
	
//	private List<JSONObject> findOfficeHourRequests(String student, JSONObject piazzaPosts) {
//		List<JSONObject> officeHourRequests = new ArrayList<>();
//		JSONArray instructorPosts = piazzaPosts.getJSONArray("Prasun Dewan(dewan@cs.unc.edu)");
//		JSONObject officeHourPost = null;
//		for (Object post : instructorPosts) {
//			if (!(post instanceof JSONObject)) {
//				continue;
//			}
//			JSONObject postJson = (JSONObject) post;
//			if (postJson.getString("subject").equals("Office Hours")) {
//				officeHourPost = postJson;
//				break;
//			}
//		}
//		if (officeHourPost == null) {
//			return officeHourRequests;
//		}
//		JSONArray officeHourAllInstructors = officeHourPost.getJSONArray("children");
//		for (Object instructorOfficeHour : officeHourAllInstructors) {
//			if (!(instructorOfficeHour instanceof JSONObject)) {
//				continue;
//			}
//			JSONArray requests = ((JSONObject) instructorOfficeHour).getJSONArray("children");
//			for (Object request : requests) {
//				if (!(request instanceof JSONObject)) {
//					continue;
//				}
//				JSONObject requstJson = (JSONObject) request;
//				if (requstJson.getString("author").equals(student)) {
//					officeHourRequests.add(requstJson);
//				}
//			}
//		}
//		return officeHourRequests;
//	}
//	
//	private List<JSONObject> findStudentPosts(String student, JSONObject piazzaPosts) {
//		List<JSONObject> studentPosts = new ArrayList<>();
//		JSONArray posts = piazzaPosts.getJSONArray(student);
//		for (Object post : posts) {
//			if (!(post instanceof JSONObject)) {
//				continue;
//			}
//			studentPosts.add((JSONObject)post);
//		}
//		return studentPosts;
//	}
	@Override
	protected boolean hasNextExternalEvent() {
		// TODO Auto-generated method stub
		return lastAddedExternalIndex < piazzaPosts.size();
	}

	protected JSONObject previousEvent;
	
	@Override
	protected long getNextExternalEventTimeStamp() {
		// TODO Auto-generated method stub
		previousEvent = piazzaPosts.get(lastAddedExternalIndex);
		return previousEvent.getLong("time");
	}

	
	@Override
	protected List<EHICommand> createExternalCommands(boolean fromPreviousEvent) {
		// TODO Auto-generated method stub
		if (!fromPreviousEvent) {
			previousEvent = piazzaPosts.get(lastAddedExternalIndex);
		}
		EHICommand aCommand = new PiazzaPostCommand(previousEvent);
		List<EHICommand> retVal = new ArrayList<>();
		retVal.add(aCommand);
		System.out.println("Adding aCommand " + aCommand + " for " + student);
		return retVal;
	}

}

final class Comparators
{
    /**
     * Verify that a comparator is transitive.
     *
     * @param <T>        the type being compared
     * @param comparator the comparator to test
     * @param elements   the elements to test against
     * @throws AssertionError if the comparator is not transitive
     */
    public static <T> void verifyTransitivity(Comparator<T> comparator, Collection<T> elements)
    {
        for (T first: elements)
        {
            for (T second: elements)
            {
                int result1 = comparator.compare(first, second);
                int result2 = comparator.compare(second, first);
                if (result1 != -result2)
                {
                    // Uncomment the following line to step through the failed case
                    //comparator.compare(first, second);
                    throw new AssertionError("compare(" + first + ", " + second + ") == " + result1 +
                        " but swapping the parameters returns " + result2);
                }
            }
        }
        for (T first: elements)
        {
            for (T second: elements)
            {
                int firstGreaterThanSecond = comparator.compare(first, second);
                if (firstGreaterThanSecond <= 0)
                    continue;
                for (T third: elements)
                {
                    int secondGreaterThanThird = comparator.compare(second, third);
                    if (secondGreaterThanThird <= 0)
                        continue;
                    int firstGreaterThanThird = comparator.compare(first, third);
                    if (firstGreaterThanThird <= 0)
                    {
                        // Uncomment the following line to step through the failed case
                        //comparator.compare(first, third);
                        throw new AssertionError("compare(" + first + ", " + second + ") > 0, " +
                            "compare(" + second + ", " + third + ") > 0, but compare(" + first + ", " + third + ") == " +
                            firstGreaterThanThird);
                    }
                }
            }
        }
    }

    /**
     * Prevent construction.
     */
    private Comparators()
    {
    }
}
