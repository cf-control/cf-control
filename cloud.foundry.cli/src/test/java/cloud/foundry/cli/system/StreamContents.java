package cloud.foundry.cli.system;


/**
 * Helper class. Holds contents of both stdout and stderr. Used mainly to wrap results of a system test run simulation.
 */
public class StreamContents {
    private final String stdoutContent;
    private final String stderrContent;

    /**
     * Default constructor.
     * @param stdoutContent content of stdout stream (aka System.out in Java)
     * @param stderrContent content of stderr stream (aka System.err in Java)
     */
    public StreamContents(String stdoutContent, String stderrContent) {
        this.stdoutContent = stdoutContent;
        this.stderrContent = stderrContent;
    }

    /**
     * Get stdout contents.
     * @return stdout contents
     */
    public String getStdoutContent() {
        return stdoutContent;
    }

    /**
     * Get stderr contents.
     * @return stderr contents
     */
    public String getStderrContent() {
        return stderrContent;
    }
}
