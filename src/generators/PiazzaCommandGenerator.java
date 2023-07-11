package generators;

import java.text.ParseException;
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
import fluorite.commands.LocalCheckCommand;
import fluorite.commands.PiazzaPostCommand;
import fluorite.commands.ZoomSessionStartCommand;
import fluorite.util.EHUtilities;
import logAnalyzer.AnAssignmentReplayer;
import logAnalyzer.LogAnalyzerLoggerFactory;
import logAnalyzer.Replayer;

public class PiazzaCommandGenerator extends ExternalCommandGenerator {

	String student;
	String studentName;
	List<JSONObject> piazzaPosts;
	Pattern studentNamePattern = Pattern.compile(".*\\\\(.*), (.*)\\((.*)\\)");
	
	public PiazzaCommandGenerator(Replayer replayer, CountDownLatch aLatch, String aStudent, 
								  Map<String, List<EHICommand>> commandMap, JSONObject piazzaPosts) {
		super(replayer, aLatch, commandMap);
		student = aStudent;
		studentName = toParenthesizedStudentName(student);
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
	
	public static String toParenthesizedStudentName(String aLongName) {
		int aLeftParenIndex = aLongName.indexOf("(");
		if (aLeftParenIndex < 0) {
			return aLongName;
		}
		return aLongName.substring(aLeftParenIndex, aLongName.length());
	}
	
	//"subject": "<p>Hi Professor, thank you for pointing out the mistake. I tried the link above and got another error. I wonder if this happened because I tried to install the plugin in China. Thank you for your help!</p>\n<p><img src=\"/redirect/s3?bucket=uploads&amp;prefix=paste%2Fjqpqwx9l2l36ng%2F2d01eb7ec7e86e20443de7cf2f5510f51f2ed8ffe939c3e64f8e7433bbb5728a%2FScreen_Shot_2021-01-23_at_3.31.38_PM.png\" alt=\"\" /></p>\n<p><img src=\"/redirect/s3?bucket=uploads&amp;prefix=paste%2Fjqpqwx9l2l36ng%2F93d55fe0e7c4c6cd289a308962bd41d728ce7dd253912ff2f8c723d854bb6c19%2FScreen_Shot_2021-01-23_at_3.40.19_PM.png\" width=\"744\" height=\"670\" alt=\"\" /></p>",

	static String SUBJECT = "\"subject\":";
	public static String toSubject (String aPost) {
		int aSubjectIndex = aPost.indexOf(SUBJECT);
		if (aSubjectIndex < 0) {
			return aPost;
		}
		int anEndIndex = aPost.indexOf(',', aSubjectIndex);
		if (anEndIndex < 0) {
			anEndIndex = aPost.length();
		}
		String retVal = aPost.substring(aSubjectIndex, anEndIndex);
		return retVal;
	
	}
	
	public static Set<String> findAllPiazzaPosters(JSONObject piazzaPostsJson) {
		Set<String> retVal = new HashSet();
		for (String aKey: piazzaPostsJson.keySet()) {
			if (aKey.contains("nstructor") || aKey.contains("nonymous")) {
				continue;
			}
			String aName = AnAssignmentReplayer.normalizeName(aKey);
			if (aName != null) {
				retVal.add(aName);
			} else {
				System.out.println("Illegal name " + aKey);
			}

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
//		String author = onyen;
		String author = ChainedCommandGenerator.capitalizeNames(onyen);

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
//		return previousEvent.getLong("time");
		return toTimestamp(previousEvent);
	}
	
	public String extractSummary(PiazzaPostCommand aCommand) {
		Map<String, String> aDataMap = aCommand.getDataMap();
		String aPost = aDataMap.get(aCommand.PIAZZA_POST);
		return toSubject(aPost);
		
	}
	protected void logCommand (EHICommand aCommand) {	
		LogAnalyzerLoggerFactory.logMessage(studentName + "-->" + 
				toDate(aCommand)+ 
				"Piazza Post " + 
				extractSummary((PiazzaPostCommand) aCommand) + 
				"\n");
		LogAnalyzerLoggerFactory.getLogAnalyzerAssignmentMetrics().numPiazzaPosts++;
	}
	public static long toTimestamp(JSONObject event) {
		try {
			return event.getLong("time");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		}
	}
	@Override
	protected List<EHICommand> getRemainingAssignmentCommands(long minEndTime) {
		List<EHICommand> retVal = new ArrayList();
		for (int anIndex = lastAddedExternalIndex;  anIndex < piazzaPosts.size(); anIndex++) {
			JSONObject anEvent = piazzaPosts.get(anIndex);
			long aTimeStamp = toTimestamp(anEvent);
			if (aTimeStamp > minEndTime) {
				break; // maybe check the previous folder
			}
			EHICommand aCommand = new PiazzaPostCommand(anEvent);
			aCommand.setTimestamp(aTimeStamp);
			retVal.add(aCommand);		
		}
		return retVal;
	}
	
	@Override
	protected List<EHICommand> createExternalCommands(boolean fromPreviousEvent) {
		// TODO Auto-generated method stub
		if (!fromPreviousEvent) {
			previousEvent = piazzaPosts.get(lastAddedExternalIndex);
		}
		PiazzaPostCommand aCommand = new PiazzaPostCommand(previousEvent);
		List<EHICommand> retVal = new ArrayList<>();
//		String aPost = aCommand.getDataMap().get("piazza_post");
//		
//		LogAnalyzerLoggerFactory.logMessage(
//				student + "-->" + EHUtilities.toDate(aCommand) + ": Piazza " + toSubject(aPost) + "\n");
//		LogAnalyzerLoggerFactory.getLogAnalyzerAssignmentMetrics().numPiazzaPosts++;
		
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
