package cloud.foundry.cli.crosscutting.logging;

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
    private static final String DEBUG_ENV_VAR_NAME = "DEBUG";

    /**
     * The name of the CF-Control logger.
     */
    public static final String LOGGER_NAME = "cfctl";

    static {
        logger = java.util.logging.Logger.getLogger(LOGGER_NAME);

        // by default, we don't want to log debug messages
        // however, the user can opt-in to them by setting the environment variable $DEBUG
        if (System.getenv(DEBUG_ENV_VAR_NAME) != null) {
            logger.setLevel(Level.FINE);
        } else {
            logger.setLevel(Level.INFO);
        }
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
        logger.setLevel(level);
    }

    /**
     * Log a debug message. These are typically not displayed outside a development environment.
     * @param arg0 mandatory log argument
     * @param args optional additional arguments
     */
    public static void debug(Object arg0, Object... args) {
        Log.log(Level.FINE, arg0, args);
    }

    /**
     * Log an info message. This is the normal log level, used to inform users about the current state of the execution
     * and providing other feedback.
     * @param arg0 mandatory log argument
     * @param args optional additional arguments
     */
    public static void info(Object arg0,Object... args) {
        Log.log(Level.INFO, arg0, args);
    }

    /**
     * Log a warning. Used for instance in situations where an error occurred which could be dealt with automatically.
     * @param arg0 mandatory log argument
     * @param args optional additional arguments
     */
    public static void warn(Object arg0, Object... args) {
        Log.log(Level.WARNING, arg0, args);
    }

    /**
     * Log an error. This should be used in situations where the execution cannot be continued safely and has to be
     * aborted.
     * The caller is responsible for exiting the program properly and appropriately.
     * @param arg0 mandatory log argument
     * @param args optional additional arguments
     */
    public static void error(Object arg0, Object... args) {
        Log.log(Level.SEVERE, arg0, args);
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
        logger.log(Level.SEVERE, message, thrown);
    }
}
