package generators;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

import fluorite.commands.EHICommand;
import fluorite.commands.PiazzaPostCommand;
import logAnalyzer.AnAssignmentReplayer;
import logAnalyzer.Replayer;

public class PiazzaCommandGenerator extends ExternalCommandGenerator {

	String student;
	List<JSONObject> piazzaPosts;
	Pattern studentNamePattern = Pattern.compile(".*\\\\(.*), (.*)\\((.*)\\)");
	
	public PiazzaCommandGenerator(Replayer replayer, CountDownLatch aLatch, String aStudent, 
								  Map<String, List<EHICommand>> commandMap, JSONObject piazzaPosts) {
		super(replayer, aLatch, commandMap);
		student = aStudent;
		this.piazzaPosts = new ArrayList<>();
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
	
	public static Set<String> findAllPiazzaPosters(JSONObject piazzaPostsJson) {
		Set<String> retVal = new HashSet();
		for (String aKey: piazzaPostsJson.keySet()) {
			if (aKey.contains("nstructor") || aKey.contains("nonymous")) {
				continue;
			}
			retVal.add(AnAssignmentReplayer.normalizeName(aKey));
		}
		return retVal;
	}
	
	private void findPiazzaPosts(String student, JSONObject piazzaPostsJson) {
		Matcher matcher = studentNamePattern.matcher(student);
		if (!matcher.matches()) {
			return;
		}
		String onyen = matcher.group(3);
//		if (student.contains("Neville")) {
//			System.out.println("found author");
//		}
//		String author = onyen+ "(" + onyen + "@live.unc.edu)";
		String author = onyen;

		if (!piazzaPostsJson.has(author)) {
			return;
		}
		JSONArray posts = piazzaPostsJson.getJSONArray(author);
		for (Object post : posts) {
			if (!(post instanceof JSONObject)) {
				continue;
			}
			JSONObject postJson = (JSONObject)post;
			if ((!postJson.has("is_diary") || !postJson.getBoolean("is_diary")) 
			 && (!postJson.has("root_is_diary") || !postJson.getBoolean("root_is_diary"))) {
				piazzaPosts.add(postJson);
			}
		}
	}

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
//		System.out.println("Adding aCommand " + aCommand + " for " + student);
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
