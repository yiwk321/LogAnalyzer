package analyzerListeners;

import java.util.List;
import java.util.Map;

import logAnalyzer.ReplayerListener;

public interface ExceptionGatherer extends ReplayerListener{

	Map<String, Map<String, List<List<StackTraceData>>>> getStackTraces();

}
