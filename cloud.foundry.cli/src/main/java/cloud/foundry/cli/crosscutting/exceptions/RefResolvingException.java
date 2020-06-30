package cloud.foundry.cli.crosscutting.exceptions;

/**
 * Thrown to indicate that an error occurred during ref resolving.
 */
public class RefResolvingException extends RuntimeException {

    public RefResolvingException(String message) {
        super(message);
    }

    public RefResolvingException(String message, Throwable throwable) {
        super(message, throwable);
    }

}
