package cloud.foundry.cli.services;

import cloud.foundry.cli.crosscutting.exceptions.CreationException;
import cloud.foundry.cli.crosscutting.exceptions.MissingCredentialsException;
import cloud.foundry.cli.crosscutting.exceptions.GetException;
import cloud.foundry.cli.crosscutting.exceptions.DiffException;
import cloud.foundry.cli.crosscutting.exceptions.RefResolvingException;
import cloud.foundry.cli.crosscutting.exceptions.UpdateException;
import cloud.foundry.cli.crosscutting.exceptions.ApplyException;
import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.crosscutting.mapping.RefResolver;
import cloud.foundry.cli.crosscutting.mapping.CfArgumentsCreator;
import org.yaml.snakeyaml.constructor.ConstructorException;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.concurrent.Callable;
import java.util.logging.FileHandler;

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

    private static final Log log = Log.getLog(BaseController.class);

    private static class LoggingOptions {
        @Option(names = {"-q", "--quiet"}, description = "Reduce log verbosity and print errors only.")
        private boolean quiet;

        @Option(names = {"-v", "--verbose"}, description = "Enable verbose logging.")
        private boolean verbose;

        @Option(names = {"-d", "--debug"}, description = "Enable debug logging.")
        private boolean debug;

        public boolean isVerbose() {
            return verbose;
        }

        public boolean isDebug() {
            return debug;
        }

        public boolean isQuiet() {
            return quiet;
        }
    }

    @ArgGroup(exclusive = true, multiplicity = "0..1")
    LoggingOptions loggingOptions;

    @Option(names = {"--log-file"}, description = "Write logs to file.")
    private String logFile;

    @Override
    public Integer call() {
        // this code is executed if the user just runs the app
        CommandLine.usage(this, System.out);
        return 0;
    }

    public static void main(String[] args) {
        // now, this is a little annoying, but it seems picocli doesn't provide any other option
        // in order to be able to handle the global logging options, we need to access the values in the base
        // controller
        // unfortunately, these are populated only once we call parseArgs or execute
        // therefore, we have to parse the arguments twice: once with a parseArgs() call to be able to handle the
        // logging options, then again in the execute() call which runs the subcommands etc.
        BaseController controller = new BaseController();

        CommandLine cli = new CommandLine(controller);
        try {
            args = CfArgumentsCreator.determineCommandLine(cli, args);
        } catch (CommandLine.ParameterException parameterException) {
            // the arguments are invalid
            log.error(parameterException.getMessage());
            System.exit(1);
        }

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
                log.error("I/O error:", ex.getMessage());
            } else if (ex instanceof CreationException) {
                log.error("Failed to create message:" + ex.getMessage());
            } else if (ex instanceof UpdateException) {
                log.error("Failed to update message:" + ex.getMessage());
            } else if (ex instanceof UnsupportedOperationException) {
                log.error("Operation not supported/implemented:", ex.getMessage());
            } else if (ex instanceof MissingCredentialsException) {
                log.error("Missing credentials were detected:", ex.getMessage());
            } else if (ex instanceof RefResolvingException) {
                log.error("Failed to resolve " + RefResolver.REF_KEY + "-occurrences:", ex.getMessage());
            } else if (ex instanceof ConstructorException) {
                log.error("Cannot interpret yaml contents:", ex.getMessage());
            } else if (ex instanceof DiffException) {
                log.error("Unable to perform the diff:", ex.getMessage());
            } else if (ex instanceof ApplyException) {
                log.error("An error occurred while processing the apply command:", ex.getMessage());
            } else if (ex instanceof GetException) {
                Throwable getExceptionCause = ex.getCause();

                // the cases that are hard to identify are all wrapped in a GetException
                if (getExceptionCause.getCause() instanceof UnknownHostException) {
                    // wrapped unknown host exceptions stand for unreachable hosts
                    log.error("Unable to connect to the CF API host:", getExceptionCause.getMessage());
                } else if (getExceptionCause instanceof IllegalArgumentException) {
                    // illegal argument exceptions seem to denote invalid organizations and spaces
                    log.error("Wrong arguments provided:", getExceptionCause.getMessage());
                } else if (getExceptionCause instanceof IllegalStateException &&
                        getExceptionCause.getMessage().toLowerCase().contains("retries exhausted") &&
                        getExceptionCause.getCause() != null &&
                        getExceptionCause.getCause().toString().toLowerCase().contains("invalidtokenexception")) {
                    // a little bit ugly, but it works
                    // the problem is in reactor.core.Exceptions
                    // it creates lambda class instances which are hard to test on...
                    // by these checks we can make sure, that the exception was caused by invalid credentials
                    log.error("Request to CF API failed: Invalid username or password " +
                                "(your account might be locked due to too many login attempts with a wrong password)");
                } else {
                    log.error("An unexpected error occurred while processing the get command:", ex.getMessage());
                }

            } else {
                log.exception(ex, "Unexpected error occurred");
            }

            // no need for returning different exit codes for different exceptions, but that's also possible in the
            // future
            return 1;
        });

        // parse args now to be able to configure logging before we continue running the rest of the CLI
        // a nice catch of this approach: we can properly handle all sorts of argument parsing errors nicely
        try {
            cli.parseArgs(args);
        } catch (Exception e) {
            // TODO: consider printing this directly to stderr
            // (we don't necessarily need to use the logger while parsing the args)
            log.error(e.getMessage());
            System.exit(1);
        }

        // will be registered as a handler in the log in case the user enables it
        FileHandler fileHandler = null;

        // seems like picocli doesn't populate the logging options unless either of them is passed
        if (controller.loggingOptions != null) {
            // in case a log file path has been passed, all we have to do is add a file handler for this path
            if (controller.logFile != null) {
                try {
                    fileHandler = new FileHandler(controller.logFile);
                } catch (IOException e) {
                    log.error("Could not open log file", controller.logFile);
                    System.exit(1);
                }
            }

            // now, we can access the logging options in the base controller
            // note: we always enable the most verbose level the user specifies
            // for that reason we can't use an if-else, but must use a chain of plain if clauses
            if (controller.loggingOptions.isQuiet()) {
                // wouldn't make sense to log that we enabled the quiet mode, right?
                // the whole idea is to reduce the amount of log messages
                Log.setQuietLogLevel();
            }
            if (controller.loggingOptions.isVerbose()) {
                Log.setVerboseLogLevel();
                log.verbose("enabling verbose logging");
            }
            if (controller.loggingOptions.isDebug()) {
                Log.setDebugLogLevel();
                log.debug("enabling debug logging");
            }
        }

        // register in the log if available
        if (fileHandler != null) {
            Log.addHandler(fileHandler);
        }

        // okay, logging is configured, now let's run the rest of the CLI
        int exitCode = cli.execute(args);

        // make sure to close the file handler to make sure it writes valid XML including all closing tags
        if (fileHandler != null) {
            Log.removeHandler(fileHandler);
            fileHandler.close();
        }

        System.exit(exitCode);
    }

}
