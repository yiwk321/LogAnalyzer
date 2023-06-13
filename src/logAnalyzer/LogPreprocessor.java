package logAnalyzer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import fluorite.commands.EHICommand;
import fluorite.commands.LocalChecksRawCommand;
import util.misc.Common;

public class LogPreprocessor {

//	private static final String ESCAPED_AMPERSAND = " &amp; ";
	private static final String ESCAPED_AMPERSAND = "AMPERSAND";

	private static final String SPACED_ESCAPED_AMPERSAND = " " + ESCAPED_AMPERSAND + " ";

//	private static final String ESCAPED_AMPERSAND = "Amper1sand";

//	private static final String ESCAPED_LT = " &lt; ";
	private static final String ESCAPED_LT = " &lt; ";

	private static final String ESCAPED_GT = " &gt; ";
	public static final String NULL_MARKER = "0x0";
	public static final String ILLEGAL_MARKER = "0x?";

	private static final String URL_STRING_START = "url=\"";
	private static final String URL_STRING_END = "\"";


	private static final String SEARCH_STRING_START = "searchString=\"";
	private static final String SEARCH_STRING_END = "\" starttimestamp=";
	private static final int EXPECTED_FILE_SIZE = 10000;
	private static final int EXPECTED_LINE_SIZE = 1000;

	private static StringBuilder stringBuilder = new StringBuilder();
	
	private static final char SUBSTITUTE_CHAR = '#';
	private static final String SUBSTITUTE_SEMICOLON = "SEMICOLON";
	
	private static final String SUBSTITUTE_LINE = "SEMICOLON";
	
	private static final String COMMENT_START = "<!--";
	private static final String COMMENT_END = "-->";


	
	public static final String deleteChar(String aString, int aPosition) {
		String aLeftOfColumn = aString.substring(0, aPosition);
		String aRightOfColumn = aString.substring(aPosition + 1, aString.length() );
		return aLeftOfColumn + SUBSTITUTE_CHAR + aRightOfColumn;
	}
	public static final String commentLine(String aString) {
		return COMMENT_START + aString + COMMENT_END;
	}
	
	public static final String preProcessURL(String anOriginal, int aLineNumber, int aColumnNumber, int aCurrentLineNumber) {
		int aURLStart = anOriginal.indexOf(URL_STRING_START);
		int aURLContentStart = aURLStart + URL_STRING_START.length();

		int aURLEnd = anOriginal.indexOf(URL_STRING_END, aURLContentStart);
		
		if (aURLStart < 0 || aURLEnd < 0) {
			return anOriginal;
		}
		String aURLContent = anOriginal.substring(aURLContentStart , aURLEnd);
		String aPossiblyModifiedURL = aURLContent.replace(";", SUBSTITUTE_SEMICOLON);
//		if (aLineNumber == aCurrentLineNumber) {
//			System.err.println("Found target line number");
//		}
		 if (aURLContent.equals(aPossiblyModifiedURL)) {
			
				return anOriginal;

			}		
		String aPrefix = anOriginal.substring(0, aURLContentStart);
		String aSuffix = anOriginal.substring(aURLEnd);
		String aResult = aPrefix + aPossiblyModifiedURL + aSuffix;

		return aResult;
		}
	
	public static final String preProcessSearchString(String anOriginal, int aLineNumber, int aColumnNumber, int aCurrentLineNumber) {
		int aSearchStart = anOriginal.indexOf(SEARCH_STRING_START);
		int aSearchEnd = anOriginal.indexOf(SEARCH_STRING_END);
		
		if (aSearchStart < 0 || aSearchEnd < 0) {
			return anOriginal;
		}
		int aSearchContentStart = aSearchStart + SEARCH_STRING_START.length();
		String aSearchContent = anOriginal.substring(aSearchContentStart , aSearchEnd);
		String aPossiblyModifiedSearch = aSearchContent.replace("\"", "");
		 aPossiblyModifiedSearch = aPossiblyModifiedSearch.replace("<", ESCAPED_LT);
		 aPossiblyModifiedSearch = aPossiblyModifiedSearch.replace(">", ESCAPED_GT);
		 if (aSearchContent.equals(aPossiblyModifiedSearch)) {
//				if (aLineNumber == aCurrentLineNumber) {
//					return deleteChar(anOriginal, aColumnNumber);
//				}
				return anOriginal;

			}		
		String aPrefix = anOriginal.substring(0, aSearchContentStart);
		String aSuffix = anOriginal.substring(aSearchEnd);
		String aResult = aPrefix + aPossiblyModifiedSearch + aSuffix;

		return aResult;
		}
	
	public static void main(String[] args) {
		System.out.println ((int) 0x1);
		System.out.println ((int) 0x2);
		System.out.println ((int) 0x11);
		String aOriginal = "" + 0x0 + '\0';
		
		System.out.println("original" + aOriginal);
//		String anOriginal = "  <Command __id=\"118\" _type=\"WebVisitCommand\" date=\"Thu Aug 18 10:00:20 EDT 2022\" numVisits=\"2\" searchString=\"java checkstyle class is not in a \"dictionary\" - Google Search\" starttimestamp=\"1660830920291\" timestamp=\"300147\" url=\"https://www.google.com/search?q=java+checkstyle+class+is+not+in+a+dictionary &amp; rlz=1C1ONGR_enUS989US989 &amp; oq=java+checkstyle+class+is+not+in+a+dictionary &amp; aqs=chrome..69i57j33i299.5725j0j4 &amp; sourceid=chrome &amp; ie=UTF-8\" />\r\n" ; 
//		String aNew = removeSearchStringQuotes(anOriginal);
//		if (aNew.equals(anOriginal)) {
//			System.out.println("Original");
//		}
	}
	static StringBuilder lineStringBuilder = new StringBuilder(EXPECTED_LINE_SIZE);
	public static boolean nonLatin(char aChar) {
		return Character.UnicodeBlock.of(aChar) != Character.UnicodeBlock.BASIC_LATIN ;

	}
	//Handling org.xml.sax.SAXParseException; lineNumber: 42395; columnNumber: 108; An invalid XML character (Unicode: 0x11) was found in the CDATA section.

	public static int[] getRowAndColumnNumber (String aMessage) {
		int[] aCellNumber = {-1, -1};
		return aCellNumber;
		
	}
//  <exceptionString><![CDATA[Exception in thread "main" java.nio.file.InvalidPathException: Illegal char <"> at index 37: C:\Users\hello\21workspace\524a5Java\"fake.lisp"
	static String markerString = "Illegal char <";

	public static String removeExceptionStringIllegalCharacter(String anOriginal) {
		int aLeftIndex = anOriginal.indexOf(markerString);
		if (aLeftIndex < 0) {
			return anOriginal;
		}
		int aRightIndex = anOriginal.indexOf('>', aLeftIndex);
		if (aRightIndex < 0) {
			return anOriginal;
		}
		return anOriginal.substring(0, aLeftIndex) + markerString +  ILLEGAL_MARKER + ">";
				

	}
	public static String removeIllegalChars(String anOriginal, int aColumnNumber) {
//		if (!anOriginal.contains(",")) {
//			return anOriginal;
//		}
		String aRemoveException = removeExceptionStringIllegalCharacter(anOriginal);
		if (aRemoveException != anOriginal) {
			return aRemoveException;
		}
		lineStringBuilder.setLength(0);
		int aCurrentLineNumber = 0;
		for (int i = 0; i < anOriginal.length(); i++) {

			char aChar = anOriginal.charAt(i);
			
			if (//((anOriginal.contains("<WebVisit") || anOriginal.contains("<text>")) && 
					nonLatin(aChar)) {
//				System.out.println("Found non latin at " + i + " in:" + anOriginal);
				lineStringBuilder.append(ILLEGAL_MARKER);
			} 
			else if (aChar >= 0x0 && aChar  <= 0x1b ) {

//			else if (aChar == 0x0 || aChar == 0x11 || aChar == 0xc || aChar == 0x7 || aChar == 0x1 || aChar == 0x2 || aChar == 0x3 ||aChar == 0x5 || aChar == 0x13 || aChar == 0xb) {
//				System.out.println("Found null in:" + anOriginal);
				lineStringBuilder.append(NULL_MARKER);
			} 
			else if (aColumnNumber == i) {
				lineStringBuilder.append(ILLEGAL_MARKER);
			}
			else {
				lineStringBuilder.append(aChar);


			}
			aCurrentLineNumber++;
		}

		
				
		return lineStringBuilder.toString();
	}

	public static final void escapeWebSearch(File aFile, int aLineNumber, int aColumnNumber) {
		stringBuilder.setLength(0);
		int aCurrentLineNumber = 0;

		try (BufferedReader br = new BufferedReader(new FileReader(aFile))) {
			boolean aChanged = false;
			if (aCurrentLineNumber == aLineNumber) {
				System.out.println ("found target line");
			}

			String anOriginal, aNew = null; // why was this initialization required by Java?
			while ((anOriginal = br.readLine()) != null) {
//				if (aCurrentLineNumber == 4453 || aCurrentLineNumber == 777) {
//					System.err.println("Found offending line");
//				}
				aNew = anOriginal;
//				if (anOriginal.contains("&")) {
//					System.out.println("Found &") ;
//				}
//				aNew = removeIllegalChars(anOriginal);
				if (anOriginal.contains("WebVisitCommand")) {
					if (anOriginal.contains(SPACED_ESCAPED_AMPERSAND)) {
						aNew = aNew.replace(ESCAPED_AMPERSAND, SPACED_ESCAPED_AMPERSAND);

					}					
					else if (!anOriginal.contains(ESCAPED_AMPERSAND)) {
						
						aNew = aNew.replace("&", ESCAPED_AMPERSAND);
					}
					aNew = preProcessSearchString(aNew, aLineNumber, aColumnNumber, aCurrentLineNumber);
					aNew = preProcessURL(aNew, aLineNumber, aColumnNumber, aCurrentLineNumber);
					
				}
				if (anOriginal.equals(aNew)) {
					if (aCurrentLineNumber == aLineNumber) {
						aNew = commentLine(aNew);
					}
				}
				if (!anOriginal.equals(aNew)) {
					aChanged = true;
				}
				
				stringBuilder.append(aNew + "\n");
				aCurrentLineNumber++;

			}
		
			if (aChanged) {
				String toWrite = stringBuilder.toString();
				System.out.println("writing file:" + aFile);
				Common.writeText(aFile, toWrite);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

//		String anOriginal = Common.toText(aFile);
//		if (anOriginal.contains(ESCAPED_AMPERSAND)) {
//			return; // we have already preprocessed
//		}
//		String aNew = anOriginal.replace("&", ESCAPED_AMPERSAND);
//		int anIndex = anOriginal.indexOf("&");
//		if (aNew.equals(anOriginal)) {;
//			return;
//		}
//		Common.writeText(aFile, aNew);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}

	}
	//Handling org.xml.sax.SAXParseException; lineNumber: 42395; columnNumber: 108; An invalid XML character (Unicode: 0x11) was found in the CDATA section.
//   <exceptionString><![CDATA[Exception in thread "main" java.nio.file.InvalidPathException: Illegal char <"> at index 37: C:\Users\hello\21workspace\524a5Java\"fake.lisp"
//I***@25150325951300 {Selecting Thread}(SocketChannelFullMessageRead) EvtSrc(AScatterGatherReadCommand)  [comp533.AHalloweenSimClientNIOSenderImpl@537f60bf]<-(7)java.nio.HeapByteBuffer[pos=26 lim=33 cap=4194304] java.nio.channels.SocketChannel[connected local=/127.0.0.1:51676 remote=localhost/127.0.0.1:9000]
//	Command echoed FROM server: 0x00x00x0ipc
	public static final void removeIllegalChars(File aFile, String aMessage, int aLineNumber, int aColumnNumber) {
		stringBuilder.setLength(0);
		int aCurrentLineNumber = 0;
		try (BufferedReader br = new BufferedReader(new FileReader(aFile))) {
			boolean aChanged = false;

			String anOriginal, aNew = null; // why was this initialization required by Java?
			while ((anOriginal = br.readLine()) != null) {				
				
				aNew = removeIllegalChars(anOriginal, aColumnNumber);
				
				if (anOriginal.equals(aNew)) {
					if (aCurrentLineNumber == aLineNumber) {
						aNew = commentLine(aNew);
					}
				}
				if (!anOriginal.equals(aNew)) {
					aChanged = true;
				}
				
//				if (!anOriginal.equals(aNew)) {
//					aChanged = true;
//				} else if (aCurrentLineNumber == aLineNumber) {
//					aNew = commentLine(aNew);
//				}
				
				stringBuilder.append(aNew + "\n");
				aCurrentLineNumber++;

			}
		
			if (aChanged) {
				String toWrite = stringBuilder.toString();
				System.out.println("writing file:" + aFile);
				Common.writeText(aFile, toWrite);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

//		String anOriginal = Common.toText(aFile);
//		if (anOriginal.contains(ESCAPED_AMPERSAND)) {
//			return; // we have already preprocessed
//		}
//		String aNew = anOriginal.replace("&", ESCAPED_AMPERSAND);
//		int anIndex = anOriginal.indexOf("&");
//		if (aNew.equals(anOriginal)) {;
//			return;
//		}
//		Common.writeText(aFile, aNew);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}

	}
	public static final void commentTargetLine(File aFile, String aMessage, int aLineNumber, int aColumnNumber) {
		stringBuilder.setLength(0);
		int aCurrentLineNumber = 0;
		try (BufferedReader br = new BufferedReader(new FileReader(aFile))) {

			String anOriginal, aNew = null; // why was this initialization required by Java?
			while ((anOriginal = br.readLine()) != null) {	
				aNew = anOriginal;
				
					if (aCurrentLineNumber == aLineNumber) {
						aNew = commentLine(aNew);
					}
				
				
//				if (!anOriginal.equals(aNew)) {
//					aChanged = true;
//				} else if (aCurrentLineNumber == aLineNumber) {
//					aNew = commentLine(aNew);
//				}
				
				stringBuilder.append(aNew + "\n");
				aCurrentLineNumber++;

			}
		
			
				String toWrite = stringBuilder.toString();
				System.out.println("writing file:" + aFile);
				Common.writeText(aFile, toWrite);
			

		} catch (IOException e) {
			e.printStackTrace();
		}

//		String anOriginal = Common.toText(aFile);
//		if (anOriginal.contains(ESCAPED_AMPERSAND)) {
//			return; // we have already preprocessed
//		}
//		String aNew = anOriginal.replace("&", ESCAPED_AMPERSAND);
//		int anIndex = anOriginal.indexOf("&");
//		if (aNew.equals(anOriginal)) {;
//			return;
//		}
//		Common.writeText(aFile, aNew);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}

	}
	public static List<EHICommand>  removeDuplicateLocalCheckCommands(List<EHICommand> aCommands) {
		if (aCommands == null) {
			return null;
		}
		List<EHICommand> aPossiblyModifiedCommands = new ArrayList();
		LocalChecksRawCommand aPrevious = null, aNew = null;
		boolean aChanged = false;

		for(EHICommand aCommand:aCommands) {
			if (aCommand instanceof LocalChecksRawCommand) {
				aNew = (LocalChecksRawCommand) aCommand;
			     if (aPrevious != null) {
					if (aNew.getCSVRow().equals(aPrevious.getCSVRow())) { 
						aChanged = true;
//						System.out.println(" duplicate\n" + aNew.getCSVRow()+ " of \n" + aPrevious.getCSVRow());
						continue;
					}
				}
				aPrevious = aNew;
			}
			aPossiblyModifiedCommands.add(aCommand);
		}
		return aPossiblyModifiedCommands;
			

//		String anOriginal = Common.toText(aFile);
//		if (anOriginal.contains(ESCAPED_AMPERSAND)) {
//			return; // we have already preprocessed
//		}
//		String aNew = anOriginal.replace("&", ESCAPED_AMPERSAND);
//		int anIndex = anOriginal.indexOf("&");
//		if (aNew.equals(anOriginal)) {;
//			return;
//		}
//		Common.writeText(aFile, aNew);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}

		
	}
	

}
