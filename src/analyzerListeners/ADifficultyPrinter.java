package analyzerListeners;

import java.util.List;
import java.util.Map;

import fluorite.commands.DifficultyCommand;
import fluorite.commands.EHExceptionCommand;
import fluorite.commands.EHICommand;
import fluorite.commands.ExceptionCommand;
import fluorite.commands.PredictionCommand;

public class ADifficultyPrinter extends ACommandPrinter implements DifficultyPrinter {




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
		if (aStartCommandIndex == 0) {
			return;
		}
//		super.newCommandInSession(aStartCommandIndex, aCommandTime, aStartCommand, aStartCommandTypeChar, anEventTypeString, anInSession, aRestType, aText, anEndCommandIndex, anEndCommand);

		if (!(aStartCommand instanceof DifficultyCommand || aStartCommand instanceof PredictionCommand))
			return;
		super.newCommandInSession(aStartCommandIndex, aCommandTime, aStartCommand, aStartCommandTypeChar, anEventTypeString, anInSession, aRestType, aText, anEndCommandIndex, anEndCommand);
		if (aStartCommand instanceof PredictionCommand) {
			PredictionCommand aPredictionCommand = (PredictionCommand) aStartCommand;
			System.out.println ("Prediction:" + aPredictionCommand.getPredictionType());
			return;
		}
		if (aStartCommand instanceof DifficultyCommand) {
			DifficultyCommand aDifficultyCommand = (DifficultyCommand) aStartCommand;
			System.out.println ("Difficulty Status:" + aDifficultyCommand.getStatus());
			return;
		}
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
