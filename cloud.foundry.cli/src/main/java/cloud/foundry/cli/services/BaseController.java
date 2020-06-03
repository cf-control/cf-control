package cloud.foundry.cli.services;

import picocli.CommandLine.Command;
import picocli.CommandLine;

import java.util.concurrent.Callable;

/**
 * This class works as the entry point for the command line application.
 * Based on this entrypoint you can call subcommands depending on your use case.
 */
@Command(name = "cf-control", header = "%n@|green Welcome to cf-control|@",
        description = "This program helps in configuring your cf instance.",
        mixinStandardHelpOptions = true,
        version = "1.0",
        subcommands = {
        CreateController.class,
        GetController.class})
public class BaseController implements Callable<Integer> {

    @Override
    public Integer call() {
        // this code is executed if the user just runs the app
        CommandLine.usage(this, System.out);
        return 0;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new BaseController()).execute(args);
        System.exit(exitCode);
    }

}
