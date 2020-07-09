package cloud.foundry.cli.system.util;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;


/**
 * Captures writes to System.out/System.err and stores them in internal buffers.
 * To use, call installNewStreams() before you run some kind of main(...), and use restoreOldStreams() to reset
 * everything to the default state expected in the JVM.
 */
public class StreamManager {
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();

    // we cache the root logger's handlers while the new streams are set up
    // plase see installNewStreams for a description
    private Handler[] cachedRootHandlers = null;

    private Logger getRootLogger() {
        Logger rootLogger = Logger.getGlobal();

        // find true root logger
        while (rootLogger.getParent() != null) {
            rootLogger = rootLogger.getParent();
        }

        return rootLogger;
    }

    /**
     * Install custom streams to replace System.out/System.err targets. Writes are then captured by this class.
     */
    public void installNewStreams() {
        PrintStream outStream = new PrintStream(outContent);
        System.setOut(outStream);
        assert System.out == outStream;

        PrintStream errStream = new PrintStream(errContent);
        System.setErr(errStream);
        assert System.err == errStream;

        /*
        For many hours, we've tried to understand why we cannot capture log messages with the above pattern. This is
        actually the second time that we try to redirect stderr/stdout into buffers; the approach failed in the log
        tests before.
        This time, we were able to understand why redirecting the streams doesn't make the logger write into the
        buffers. Java's standard logging library doesn't use System.err but writes directly to the corresponding
        stream.
        Now, sporadically, it appeared that the log messages actually showed up in the buffers. However, we were
        unable to find out the condition which decides when it works and when it doesn't.
        The approach we now use is to re-setup the console handler after overwriting System.err. It appears that this
        is where the console handler gets the stream from.
        We found we can just store the old handlers of the root logger (this is where the console handler is
        registered), remove them all, and set up a new ConsoleHandler. We don't have to configure it seemingly, it
        looks like Java just uses the "new" System.err.
        Removing the old loggers also has another advantage: log messages are not printed any more during the tests.
         */
        Logger rootLogger = getRootLogger();

        cachedRootHandlers = Logger.getGlobal().getParent().getHandlers();

        for (Handler handler : cachedRootHandlers) {
            rootLogger.removeHandler(handler);
        }

        rootLogger.addHandler(new ConsoleHandler());
    }

    /**
     * Restore System.out/System.err streams after you installed custom streams.
     */
    public void restoreOldStreams() {
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
        System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err)));

        // for a description, see installNewStreams
        Logger rootLogger = getRootLogger();

        for (Handler handler : rootLogger.getHandlers()) {
            rootLogger.removeHandler(handler);
        }

        for (Handler handler : cachedRootHandlers) {
            rootLogger.addHandler(handler);
        }

        cachedRootHandlers = null;
    }

    /**
     * @return captured stdout contents
     */
    public String getStdoutContent() {
        return outContent.toString();
    }

    /**
     * @return captured stderr contents
     */
    public String getStderrContent() {
        return errContent.toString();
    }

    /**
     * @return both captured stdout and stderr contents
     */
    public StreamContents getContents() {
        StreamContents contents = new StreamContents(getStdoutContent(), getStderrContent());
        return contents;
    }
}
