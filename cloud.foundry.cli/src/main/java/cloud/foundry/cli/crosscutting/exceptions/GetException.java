package cloud.foundry.cli.crosscutting.exceptions;

/**
 * Indicates that some error has occurred, while using the get logic methods.
 */
public class GetException extends RuntimeException {

    public GetException(Throwable throwable) {
        super(throwable);
    }
}
