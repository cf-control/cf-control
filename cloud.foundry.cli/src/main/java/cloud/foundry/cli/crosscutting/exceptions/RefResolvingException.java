package cloud.foundry.cli.crosscutting.exceptions;

/**
 * TODO documentation
 */
public class RefResolvingException extends RuntimeException {

    public RefResolvingException(String message) {
        super(message);
    }

    public RefResolvingException(Throwable throwable) {
        super(throwable);
    }

}
