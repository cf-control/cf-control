package cloud.foundry.cli.system;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;


/**
 * Captures writes to System.out/System.err and stores them in internal buffers.
 * To use, call installNewStreams() before you run some kind of main(...), and use restoreOldStreams() to reset
 * everything to the default state expected in the JVM.
 */
public class StreamManager {
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();

    /**
     * Install custom streams to replace System.out/System.err targets. Writes are then captured by this class.
     */
    public void installNewStreams() {
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    /**
     * Restore System.out/System.err streams after you installed custom streams.
     */
    public void restoreOldStreams() {
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
        System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err)));
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
