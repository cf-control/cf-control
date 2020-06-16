package cloud.foundry.cli.crosscutting.exceptions;

/**
 * Signals that a update operation has failed.
 */
public class UpdateException extends Exception {

    public UpdateException(Throwable throwable) {
        super(throwable);
    }
}