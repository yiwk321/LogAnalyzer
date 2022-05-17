package analyzerListeners;

import java.util.List;
import java.util.Map;

import fluorite.commands.EHExceptionCommand;
import fluorite.commands.EHICommand;
import fluorite.commands.ExceptionCommand;

public class AnExceptionPrinter extends ACommandPrinter implements ExceptionPrinter {




//	@Override
//	public void newCommandInSession(int aCommandIndex, long aCommandTime, EHICommand aCommand, String aCommandTypeChar,
//			String anEventTypeString, boolean anInSession, String aRestType, String aText) {
//		if (aCommandTypeChar.equals("E")) {
//			super.newCommandInSession(aCommandIndex, aCommandTime, aCommand, aCommandTypeChar, anEventTypeString, anInSession, aRestType, aText);
//			
//			
////			if (aCommand instanceof ExceptionCommand) {
////				ExceptionCommand anExceptionCommand = (ExceptionCommand) aCommand;
////				
////				anExceptionCommand.getDataMap().get("Date")
////			}
//
//		}

//	      }
	@Override
	public void newCommandInSession(int aStartCommandIndex, long aCommandTime, EHICommand aStartCommand,
			String aStartCommandTypeChar, String anEventTypeString, boolean anInSession, String aRestType, String aText,
			int anEndCommandIndex, EHICommand anEndCommand) {
		if (!aStartCommandTypeChar.equals("E")) {
			return;
		}
			super.newCommandInSession(aStartCommandIndex, aCommandTime, aStartCommand, aStartCommandTypeChar, anEventTypeString, anInSession, aRestType, aText, anEndCommandIndex, anEndCommand);
		
		if (aStartCommand instanceof ExceptionCommand) {
			ExceptionCommand anExceptionCommand = (ExceptionCommand) aStartCommand;
			
			System.out.println (anExceptionCommand.getOutputText());
		}
		if (aStartCommand instanceof EHExceptionCommand) {
			EHExceptionCommand anExceptionCommand = (EHExceptionCommand) aStartCommand;
			
			System.out.println (anExceptionCommand.getOutputText());
		}

	}

}
