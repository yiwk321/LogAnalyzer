package analyzerListeners;

public class CommandPrinterFactory {
static CommandPrinter commandPrinter;

public static CommandPrinter getCommandPrinter() {
	if (commandPrinter == null) {
		commandPrinter = new ACommandPrinter();
	}
	return commandPrinter;
}

public static void setCommandPrinter(CommandPrinter commandPrinter) {
	CommandPrinterFactory.commandPrinter = commandPrinter;
}

}
