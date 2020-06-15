package cloud.foundry.cli.crosscutting.exceptions;

/**
 * Signals that something during apply has failed.
 */
public class ApplyException extends RuntimeException  {

    public ApplyException(Throwable e) {
        super(e);
    }
}
