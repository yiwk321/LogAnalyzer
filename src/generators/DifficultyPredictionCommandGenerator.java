package generators;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import analyzer.DiscreteChunksAnalyzer;
import difficultyPrediction.ADifficultyPredictionPluginEventProcessor;
import difficultyPrediction.DifficultyPredictionPluginEventProcessor;
import difficultyPrediction.DifficultyPredictionSettings;
import difficultyPrediction.DifficultyRobot;
import difficultyPrediction.Mediator;
import difficultyPrediction.PredictionParametersSetterSelector;
import difficultyPrediction.eventAggregation.EventAggregator;
import difficultyPrediction.predictionManagement.PredictionManagerStrategy;
import difficultyPrediction.statusManager.StatusListener;
import fluorite.commands.DifficultyCommand;
import fluorite.commands.EHICommand;
import fluorite.commands.PauseCommand;
import fluorite.commands.PredictionCommand;
import fluorite.commands.PredictionType;
import logAnalyzer.Replayer;

public class DifficultyPredictionCommandGenerator extends CommandGenerator implements StatusListener{
	DifficultyPredictionPluginEventProcessor difficultyEventProcessor;
	Mediator mediator;
	EventAggregator eventAggregator;

	// for each student a new instance is created
	public DifficultyPredictionCommandGenerator(Replayer aReplayer, CountDownLatch aLatch, Map<String, List<EHICommand>> aMap) {
		DifficultyPredictionSettings.setReplayMode(true);
		latch = aLatch;
		commandMap = aMap;
		replayer = aReplayer;
//		difficultyEventProcessor = new ADifficultyPredictionPluginEventProcessor();
//		// do not create difficuty thread
//		difficultyEventProcessor.commandProcessingStarted();
//		 mediator = difficultyEventProcessor
//					.getDifficultyPredictionRunnable().getMediator();
		 mediator = DifficultyRobot.getInstance();
		 mediator.addStatusListener(this);
		 eventAggregator = mediator.getEventAggregator();
		 eventAggregator
			.setEventAggregationStrategy(new DiscreteChunksAnalyzer(""
					+ PredictionParametersSetterSelector.getSingleton()
							.getSegmentLength()));


	}

//	public void maybeAddPauseCommand(List<EHICommand> newCommands, EHICommand last, EHICommand cur) {
//		long rest = cur.getTimestamp()-last.getTimestamp();
//		if (rest >= 1*Replayer.ONE_SECOND) {
//			PauseCommand command = new PauseCommand(last, cur, rest);
//			command.setStartTimestamp(last.getStartTimestamp());
//			command.setTimestamp(last.getTimestamp()+1);
//			newCommands.add(command);
//		} 
//		newCommands.add(cur);
//	}
	public static boolean hasPredictionCommand(List<EHICommand> commands) {
		for (EHICommand aCommand:commands) {
			if (aCommand instanceof PredictionCommand 
//					|| aCommand instanceof DifficultyCommand 
					) {
				return true;
			}
		}
		return false;
	}
	List<EHICommand> newCommands;
	public List<EHICommand> addCommands(int aSession, List<EHICommand> commands, long nextStartTime) {
		if (hasPredictionCommand(commands)) {
			return commands;
		}
		newCommands = new ArrayList<>();
		EHICommand last = null;
		EHICommand cur = null;
	
		for (EHICommand newCommand : commands) {
			if (newCommand instanceof PredictionCommand) {
				int i = 5;
			}
			// short circuit event processing thread
			newCommands.add(newCommand);
			if (!newCommand.getCommandType().equals("PredictionCommand")
					&& !newCommand.getCommandType().equals(
							"DifficultyStatusCommand")
					&& !(newCommand instanceof PredictionCommand)
					&& !(newCommand instanceof DifficultyCommand)
					&& !(newCommand instanceof PauseCommand)	
					) {
				mediator.processEvent(newCommand);
			}
				
//			difficultyEventProcessor.newCommand(command);
//			if (cur == null) {
//				cur = command;
//				newCommands.add(command);
//			} else {
//				last = cur;
//				cur = command;
//				maybeAddPauseCommand(newCommands, last, cur);
//			}
		}
		return newCommands;
	}

	@Override
	public void modelBuilt(boolean arg0, Exception arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void newAggregatedStatus(String predictionValue) {
		PredictionType predictionType;
		 if(predictionValue.equals(PredictionManagerStrategy.PROGRESS_PREDICTION))
        {
       	 predictionType = PredictionType.MakingProgress;
        }
//        if(predictionValue.equals("TIE"))
//        {
//       	 predictionType = PredictionType.Indeterminate;
//        }
        else if(predictionValue.equals(PredictionManagerStrategy.DIFFICULTY_PREDICTION))

        {
       	 predictionType = PredictionType.HavingDifficulty;
        }
        else {
       	 predictionType = PredictionType.Indeterminate;
        }
        PredictionCommand predictionCommand = new PredictionCommand(predictionType);
        newCommands.add(predictionCommand);
		
	}

	@Override
	public void newAggregatedStatus(int arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void newManualStatus(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void newManualStatus(DifficultyCommand arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void newReplayedStatus(int arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void newStatus(String predictionValue) {
		// ugh deuplicating code in DifficultyRobot
//		PredictionType predictionType;
//		 if(predictionValue.equals(PredictionManagerStrategy.PROGRESS_PREDICTION))
//         {
//        	 predictionType = PredictionType.MakingProgress;
//         }
////         if(predictionValue.equals("TIE"))
////         {
////        	 predictionType = PredictionType.Indeterminate;
////         }
//         else if(predictionValue.equals(PredictionManagerStrategy.DIFFICULTY_PREDICTION))
//
//         {
//        	 predictionType = PredictionType.HavingDifficulty;
//         }
//         else {
//        	 predictionType = PredictionType.Indeterminate;
//         }
//         PredictionCommand predictionCommand = new PredictionCommand(predictionType);
//         newCommands.add(predictionCommand);
		
	}

	@Override
	public void newStatus(int arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void predictionError(Exception arg0) {
		// TODO Auto-generated method stub
		
	}
}
