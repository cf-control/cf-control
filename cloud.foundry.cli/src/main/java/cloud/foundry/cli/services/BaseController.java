package cloud.foundry.cli.services;

import cloud.foundry.cli.crosscutting.logging.Log;
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
        CommandLine cli = new CommandLine(new BaseController());

        // picocli has a nice hidden feature: one can register a special exception handler and thus deal with
        // exceptions occurring during the execution of a Callable, Runnable etc.
        // when using Callables instead of Runnables, one can just forward _all_ exceptions to this exception handler
        // this then allows for handling all exceptions centrally, which helps eliminate duplicate code and provide a
        // consistent behavior, improving the overall UX on the commandline
        // see https://picocli.info/apidocs/picocli/CommandLine.html#getExecutionExceptionHandler--
        // and https://picocli.info/apidocs/picocli/CommandLine.html#execute-java.lang.String...-
        CommandLine.IExecutionExceptionHandler errorHandler = (ex, commandLine, parseResult) -> {
            // TODO: different handling for different exceptions
            Log.exception(ex, "Unexpected error");
            return 1;
        };
        cli.setExecutionExceptionHandler(errorHandler);

        int exitCode = cli.execute(args);
        System.exit(exitCode);
    }

}
