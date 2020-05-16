package cloud.foundry.cli;

import picocli.CommandLine;

/**
 * This class serves as the entry point for the application. Its responsibility is to run the particular command that
 * the user has entered.
 */
public class Main {

    public static void main(String[] args) {
        //TODO delegate the arguments to the correct command class

        CommandLine.run(null, System.err, args);
    }
}
