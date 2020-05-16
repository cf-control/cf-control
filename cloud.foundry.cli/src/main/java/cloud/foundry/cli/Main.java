package cloud.foundry.cli;

import cloud.foundry.cli.services.GetController;
import picocli.CommandLine;

import java.io.PrintWriter;

/**
 * This class serves as the entry point for the application. Its responsibility is to run the particular command that
 * the user has entered.
 */
public class Main {

    public static void main(String[] args) {
        //TODO delegate the arguments to the correct command class

        CommandLine commandLine = new CommandLine(new GetController());
        commandLine.setErr(new PrintWriter(System.err));
        commandLine.execute(args);
    }
}
