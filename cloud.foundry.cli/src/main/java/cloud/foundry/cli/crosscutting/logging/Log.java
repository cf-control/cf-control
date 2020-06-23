package cloud.foundry.cli.crosscutting.logging;

import java.util.*;
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

    // base package logger
    // Java's logging is hierarchical, i.e., it should be sufficient to e.g., add custom handlers to this one, or set
    // its log level
    private static final Logger baseLogger;

    // we store all loggers we've created so far by their name
    private static final Map<String, Log> logInstances;

    // every logger instance maintains its own Logger object, and forwards its own logs to this instance
    private final Logger logger;

    static {
        // enforce English log output format (especially important for log levels)
        System.setProperty("user.language", "en");
        Locale.setDefault(new Locale("en", "EN"));

        // this code will have to be touched the day someone changes the location of this class in relation to the
        // root package
        // note: an ArrayList doesn't support remove by index
        List<String> packageParts = new LinkedList<>(Arrays.asList(Log.class.getPackage().getName().split("\\.")));
        // delete the two parent packages
        for (int i = 0; i < 2; ++i) {
            packageParts.remove(packageParts.size() - 1);
        }
        String rootPackageName = String.join(".", packageParts);
        baseLogger = Logger.getLogger(rootPackageName);

        logInstances = new HashMap<>();

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

    private Log(String name) {
        logger = Logger.getLogger(name);

        // we want to enable really fine logs on this logger by default
        // log level filtering is done in the baseLogger
        logger.setLevel(DEBUG_LEVEL);
    }

    /**
     * Factory method. Returns log object for a given name. New log objects will be created on demand.
     */
    public static Log getLog(String name) {
        final String baseLoggerName = baseLogger.getName();

        // if we permitted such loggers, the handler registration etc. wouldn't work
        // they all expect a constant prefix
        if (!name.startsWith(baseLoggerName)) {
            throw new RuntimeException("logger name does not start with global prefix " + baseLoggerName);
        }

        Log instance = logInstances.get(name);

        // in case we have no instance stored yet...
        if (instance == null) {
            // ... we create a new one...
            instance = new Log(name);

            // ... and make sure its log level is set to the right value
            instance.logger.setLevel(baseLogger.getLevel());

            // then, we store it in the "cache"
            logInstances.put(name, instance);
        }

        return instance;
    }

    /**
     * Factory method. Returns log object for a given class. New log objects will be created on demand.
     */
    public static Log getLog(Class<?> cls) {
        final String logName = cls.getPackage().getName() + "." + cls.getName();
        return getLog(logName);
    }

    /**
     * Add a handler to the internal logger.
     * @param handler handler to add
     */
    public static void addHandler(Handler handler) {
        baseLogger.addHandler(handler);
    }

    /**
     * Remove a handler from the internal logger.
     * @param handler handler to remove
     */
    public static void removeHandler(Handler handler) {
        baseLogger.removeHandler(handler);
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
    private void log(Level level, Object arg0, Object... args) {
        String message = buildString(arg0, args);
        logger.log(level, message);
    }

    /**
     * Configure level of log messages to be shown to the user, e.g., by printing them to the console.
     * @param level messages of this or any higher level will be shown by the logger
     */
    public static void setLogLevel(Level level) {
        // configure own logger
        baseLogger.setLevel(level);

        // configure child loggers' loglevel
        // otherwise, they won't emit the log records correctly
        for (String logName : logInstances.keySet()) {
            Logger logger = logInstances.get(logName).logger;
            logger.setLevel(level);
        }

        // configure handlers of base logger to show messages of this log level
        Arrays.stream(baseLogger.getHandlers()).forEach(h -> h.setLevel(level));

        // our "nearest parent" _should_ be the root logger
        // we need to configure its handlers to output the messages < INFO as well
        // note: do not configure its log level -- this will cause all other loggers' level too, and generate a lot of
        // noise on the CLI
        Arrays.stream(baseLogger.getParent().getHandlers()).forEach(h -> h.setLevel(level));
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
    public void debug(Object arg0, Object... args) {
        this.log(DEBUG_LEVEL, arg0, args);
    }

    /**
     * Log a verbose info message.
     * @param arg0 mandatory log argument
     * @param args optional additional arguments
     */
    public void verbose(Object arg0, Object... args) {
        this.log(VERBOSE_LEVEL, arg0, args);
    }

    /**
     * Log an info message. This is the normal log level, used to inform users about the current state of the execution
     * and providing other feedback.
     * @param arg0 mandatory log argument
     * @param args optional additional arguments
     */
    public void info(Object arg0,Object... args) {
        this.log(INFO_LEVEL, arg0, args);
    }

    /**
     * Log a warning. Used for instance in situations where an error occurred which could be dealt with automatically.
     * @param arg0 mandatory log argument
     * @param args optional additional arguments
     */
    public void warning(Object arg0, Object... args) {
        this.log(WARNING_LEVEL, arg0, args);
    }

    /**
     * Log an error. This should be used in situations where the execution cannot be continued safely and has to be
     * aborted.
     * The caller is responsible for exiting the program properly and appropriately.
     * @param arg0 mandatory log argument
     * @param args optional additional arguments
     */
    public void error(Object arg0, Object... args) {
        this.log(ERROR_LEVEL, arg0, args);
    }

    /**
     * Log an exception. Should be used for situations where it is unexpected and the program flow cannot be recovered
     * from any more.
     * The caller is responsible for exiting the program properly and appropriately.
     * @param thrown exception to log
     * @param arg0 mandatory log argument
     * @param args optional additional arguments
     */
    public void exception(Throwable thrown, Object arg0, Object... args) {
        String message = buildString(arg0, args);
        logger.log(ERROR_LEVEL, message, thrown);
    }
}
