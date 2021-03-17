package logAnalyzer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import analyzer.AnAnalyzer;
import analyzer.Analyzer;

public class Driver {
	private static String classFolderPath = "E:\\testdata\\Fall2020";
	private static String experimentalClassFolderPath = "C:\\Users\\Zhizhou\\OneDrive\\UNC CH\\Junior 1st Sem\\hermes\\git\\Hermes\\Hermes\\data\\ExperimentalData";
	private static Analyzer analyzer;
	private static AReplayer replayer;
	private static int timeout = 5000;
	private static String url = "http://stackoverflow.com/questions/4186835/how-to-add-multiple-components-to-a-jframe";
	
	public static void main(String[] args) {
		analyzer = new AnAnalyzer();
		replayer = new AReplayer(analyzer);
		analyzer.addAnalyzerListener(replayer);	
		replayer.createExtraCommand(classFolderPath, "", AReplayer.LOCALCHECK);
//		replayer.createLocalCheckCommands(classFolderPath);
//		replayer.analyzeFolder(classFolderPath);
//		replayer = new AExperimentalReplayer(analyzer);
//		((AExperimentalReplayer)replayer).readTimestamp();
//		String s1 = ((AExperimentalReplayer)replayer).readWebContent(url);
//		String s2 = ((AExperimentalReplayer)replayer).readWebContent2(url);
//		System.out.println(s1.equals(s2));
//		analyzer.addAnalyzerListener(replayer);	
//		replayer.createPauseCommandLogs(experimentalClassFolderPath);
//		replayer.analyzeFolder(experimentalClassFolderPath);
//		String s1 = ((AExperimentalReplayer)replayer).readWebContent(url);
//		String s2 = "";
//		System.out.println();
//		try {
//			s2 = get(url);
//		} catch (Exception e) {
//			// TODO: handle exception
//			e.printStackTrace();
//		}
//		System.out.println(s1.equals(s2));
//		Date d = new Date(1379108460000L);
//		System.out.println(d);
	}
	
//	public static String get(String urlString) throws Exception{
//         HttpURLConnection httpClient =(HttpURLConnection) new URL(urlString).openConnection();
//        httpClient.setRequestMethod("GET");
//        httpClient.setRequestProperty("User-Agent", "Mozilla/5.0");
//        int responseCode = httpClient.getResponseCode();
//        System.out.println("Response Code : " + responseCode);
//        try (BufferedReader in = new BufferedReader(
//                new InputStreamReader(httpClient.getInputStream()))) {
//            StringBuilder response = new StringBuilder();
//            String line;
//            while ((line = in.readLine()) != null) {
//                response.append(line);
//            }
//            System.out.println(response.toString());
//            return response.toString();
//        }
//	}
}

















