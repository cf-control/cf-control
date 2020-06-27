package cloud.foundry.cli.system;

/**
 * Thrown by custom security manager whenever System.exit(...) is called.
 */
public class SystemExitException extends SecurityException {
    private final int exitCode;

    public SystemExitException(int exitCode) {
        this.exitCode = exitCode;
    }

    public int getExitCode() {
        return exitCode;
    }
}
