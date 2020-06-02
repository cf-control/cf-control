package cloud.foundry.cli.crosscutting.exceptions;

/**
 * Indicates that some data is missed or invalid, to perform some operation.
 */
public class InvalidOperationException extends Exception {

    public InvalidOperationException(String msg) {
        super(msg);
    }

    public InvalidOperationException() {
        super();
    }
}
