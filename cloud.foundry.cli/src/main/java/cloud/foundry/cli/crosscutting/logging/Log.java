package cloud.foundry.cli.crosscutting.logging;

import java.util.Arrays;
import java.util.Locale;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The central logging module, providing a standardized interface for all logging activities in the project.
 * By using this module instead of using custom logging facilities (like System.{out,err}.print*, for instance), a
 * consistent style is ensured and log messages can be filtered properly.
 * The interface is similar to Python's logging module, which provides a simple-to-use yet powerful API.
 */
public class Log {
    // we can internally use the Java logging facilities
    private static final Logger logger;

    // the name of the environment variable that needs to be set to turn on the debug messages
    private static final String QUIET_ENV_VAR_NAME = "QUIET";
    private static final String VERBOSE_ENV_VAR_NAME = "VERBOSE";
    private static final String DEBUG_ENV_VAR_NAME = "DEBUG";

    public static final Level ERROR_LEVEL = Level.SEVERE;
    public static final Level WARNING_LEVEL = Level.WARNING;
    public static final Level INFO_LEVEL = Level.INFO;
    public static final Level VERBOSE_LEVEL = Level.FINE;
    public static final Level DEBUG_LEVEL = Level.FINER;

    // by default, we only want to log messages of levels info and greater
    public static final Level DEFAULT_LEVEL = INFO_LEVEL;
    // in quiet mode, we only want to log errors
    public static final Level QUIET_LEVEL = ERROR_LEVEL;

    /**
     * The name of the CF-Control logger.
     */
    public static final String LOGGER_NAME = "cfctl";

    static {
        // enforce English log output format (especially important for log levels)
        System.setProperty("user.language", "en");
        Locale.setDefault(new Locale("en", "EN"));

        // change global stdout log format
        // desired output format: <date> <time> <logger name> [<loglevel>]: <message>
        System.setProperty(
                "java.util.logging.SimpleFormatter.format",
                "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %3$s [%4$-5s] %5$s%6$s%n"
        );

        logger = java.util.logging.Logger.getLogger(LOGGER_NAME);

        // set default log level
        setDefaultLogLevel();

        // by default, we don't want to log verbose messages
        // however, the user can opt-in to them by setting the environment variable $VERBOSE
        if (System.getenv(QUIET_ENV_VAR_NAME) != null) {
            setQuietLogLevel();
        }

        // by default, we don't want to log verbose messages
        // however, the user can opt-in to them by setting the environment variable $VERBOSE
        if (System.getenv(VERBOSE_ENV_VAR_NAME) != null) {
            setVerboseLogLevel();
        }

        // by default, we don't want to log debug messages
        // however, the user can opt-in to them by setting the environment variable $DEBUG
        if (System.getenv(DEBUG_ENV_VAR_NAME) != null) {
            setDebugLogLevel();
        }
    }

    /**
     * Add a handler to the internal logger.
     * @param handler handler to add
     */
    public static void addHandler(Handler handler) {
        logger.addHandler(handler);
    }

    /**
     * Remove a handler from the internal logger.
     * @param handler handler to remove
     */
    public static void removeHandler(Handler handler) {
        logger.removeHandler(handler);
    }

    /**
     * Internal helper building one string from multiple objects using a StringBuilder.
     *
     * @param arg0 mandatory log argument
     * @param args optional additional arguments
     */
    private static String buildString(Object arg0, Object... args) {
        StringBuilder message = new StringBuilder();

        /*
         requiring an initial arg to be passed and then allowing an array of args has two advantages:
         - first of all, we ensure that we receive at least one argument
         - second, we can easily ensure there's no unnecessary trailing whitespace, as we can append it before every
           additional argument, as demonstrated in the loop below
         */

        message.append(arg0);

        for (Object arg : args) {
            message.append(" ").append(arg);
        }

        return message.toString();
    }

    /**
     * Log a message by a given loglevel. The message can consist of one or more objects.
     *
     * @param level loglevel to use
     * @param arg0 mandatory log argument
     * @param args optional additional arguments
     */
    private static void log(Level level, Object arg0, Object... args) {
        String message = buildString(arg0, args);
        logger.log(level, message);
    }

    /**
     * Configure level of log messages to be shown to the user, e.g., by printing them to the console.
     * @param level messages of this or any higher level will be shown by the logger
     */
    public static void setLogLevel(Level level) {
        // configure own logger
        logger.setLevel(level);
        Arrays.stream(logger.getHandlers()).forEach(h -> h.setLevel(level));

        // our "nearest parent" _should_ be the root logger
        // we need to configure its handlers to output the messages < INFO as well
        // note: do not configure its log level -- this will cause all other loggers' level too, and generate a lot of
        // noise on the CLI
        Arrays.stream(logger.getParent().getHandlers()).forEach(h -> h.setLevel(level));
    }

    /**
     * Configure logger to default verbosity.
     */
    public static void setDefaultLogLevel() {
        setLogLevel(DEFAULT_LEVEL);
    }

    /**
     * Configure logger to display verbose messages, too.
     */
    public static void setVerboseLogLevel() {
        setLogLevel(VERBOSE_LEVEL);
    }

    /**
     * Configure logger to display verbose messages, too.
     */
    public static void setQuietLogLevel() {
        setLogLevel(QUIET_LEVEL);
    }

    /**
     * Configure logger to display all messages, including debug and verbose messages.
     */
    public static void setDebugLogLevel() {
        setLogLevel(DEBUG_LEVEL);
    }

    /**
     * Log a debug message. These are typically not displayed outside a development environment.
     * @param arg0 mandatory log argument
     * @param args optional additional arguments
     */
    public static void debug(Object arg0, Object... args) {
        Log.log(DEBUG_LEVEL, arg0, args);
    }

    /**
     * Log a verbose info message.
     * @param arg0 mandatory log argument
     * @param args optional additional arguments
     */
    public static void verbose(Object arg0, Object... args) {
        Log.log(VERBOSE_LEVEL, arg0, args);
    }

    /**
     * Log an info message. This is the normal log level, used to inform users about the current state of the execution
     * and providing other feedback.
     * @param arg0 mandatory log argument
     * @param args optional additional arguments
     */
    public static void info(Object arg0,Object... args) {
        Log.log(INFO_LEVEL, arg0, args);
    }

    /**
     * Log a warning. Used for instance in situations where an error occurred which could be dealt with automatically.
     * @param arg0 mandatory log argument
     * @param args optional additional arguments
     */
    public static void warning(Object arg0, Object... args) {
        Log.log(WARNING_LEVEL, arg0, args);
    }

    /**
     * Log an error. This should be used in situations where the execution cannot be continued safely and has to be
     * aborted.
     * The caller is responsible for exiting the program properly and appropriately.
     * @param arg0 mandatory log argument
     * @param args optional additional arguments
     */
    public static void error(Object arg0, Object... args) {
        Log.log(ERROR_LEVEL, arg0, args);
    }

    /**
     * Log an exception. Should be used for situations where it is unexpected and the program flow cannot be recovered
     * from any more.
     * The caller is responsible for exiting the program properly and appropriately.
     * @param thrown exception to log
     * @param arg0 mandatory log argument
     * @param args optional additional arguments
     */
    public static void exception(Throwable thrown, Object arg0, Object... args) {
        String message = buildString(arg0, args);
        logger.log(ERROR_LEVEL, message, thrown);
    }
}
