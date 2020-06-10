package cloud.foundry.cli.crosscutting.exceptions;

/**
 * Indicates that some error has occurred, while using the diff logic methods.
 */
public class DiffException extends Exception {

    public DiffException(String message) {
        super(message);
    }

    public DiffException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
