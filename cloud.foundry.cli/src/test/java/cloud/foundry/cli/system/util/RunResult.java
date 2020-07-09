package cloud.foundry.cli.system.util;


/**
 * Represents the result of a system test run simulation. Holds exit code as well as contents of stdout/stderr streams.
 */
public class RunResult {
    private final int exitCode;
    private final StreamContents streamContents;

    /**
     * Default constructor.
     * @param exitCode exit code
     * @param streamContents stream contents
     */
    public RunResult(int exitCode, StreamContents streamContents) {
        this.exitCode = exitCode;
        this.streamContents = streamContents;
    }

    /**
     * Get exit code.
     * @return exit code
     */
    public int getExitCode() {
        return exitCode;
    }

    /**
     * Get stream contents.
     * @return stream contents
     */
    public StreamContents getStreamContents() {
        return streamContents;
    }
}
