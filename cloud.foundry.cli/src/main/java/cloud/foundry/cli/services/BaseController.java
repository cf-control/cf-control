package cloud.foundry.cli.services;

import cloud.foundry.cli.crosscutting.exceptions.CreationException;
import cloud.foundry.cli.crosscutting.exceptions.CredentialException;
import cloud.foundry.cli.crosscutting.exceptions.DiffException;
import cloud.foundry.cli.crosscutting.exceptions.RefResolvingException;
import cloud.foundry.cli.crosscutting.exceptions.ApplyException;
import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.crosscutting.mapping.RefResolver;
import org.yaml.snakeyaml.constructor.ConstructorException;
import picocli.CommandLine.Command;
import picocli.CommandLine;

import java.io.IOException;
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
        GetController.class,
        DiffController.class,
        ApplyController.class,
        UpdateController.class})
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
        cli.setExecutionExceptionHandler((ex, commandLine, parseResult) -> {
            // TODO: different handling for different exceptions
            if (ex instanceof IOException) {
                Log.error("I/O error:", ex.getMessage());
            } else if (ex instanceof CreationException) {
                Log.error("Failed to create message:" + ex.getMessage());
            } else if (ex instanceof UnsupportedOperationException) {
                Log.error("Operation not supported/implemented:", ex.getMessage());
            } else if (ex instanceof CredentialException) {
                Log.error("Credentials error:", ex.getMessage());
            } else if (ex instanceof RefResolvingException) {
                Log.error("Failed to resolve " + RefResolver.REF_KEY + "-occurrences:", ex.getMessage());
            } else if (ex instanceof ConstructorException) {
                Log.error("Cannot interpret yaml contents:", ex.getMessage());
            } else if (ex instanceof DiffException) {
                Log.error("Unable to perform the diff:", ex.getMessage());
            } else if (ex instanceof ApplyException) {
                Log.error("An error occurred during the apply:", ex.getMessage());
            } else if (ex instanceof IllegalStateException) {
                // a little bit ugly, but it works
                // the problem is in reactor.core.Exceptions
                // it creates lambda class instances which are hard to test on...
                if (ex.getMessage().toLowerCase().contains("retries exhausted")) {
                    // now, if there is a cause and it's an InvalidTokenException, we can at least provide a hint
                    // that it's likely a password issue
                    if (ex.getCause() != null &&
                            ex.getCause().toString().toLowerCase().contains("invalidtokenexception")) {
                        Log.error("Request to CF API failed: invalid token error (is the password correct?)");
                    } else {
                        Log.exception(ex, "Request to CF API failed:");
                    }
                } else {
                    Log.exception(ex, "Unexpected illegal state error");
                }
            } else {
                Log.exception(ex, "Unexpected error occurred");
            }

            // no need for returning different exit codes for different exceptions, but that's also possible in the
            // future
            return 1;
        }
        );

        int exitCode = cli.execute(args);
        System.exit(exitCode);
    }

}
