package cloud.foundry.cli.crosscutting.exceptions;

/**
 * Indicates that some error has occurred, while trying to receive some credential information.
 */
public class CredentialException extends Exception {

    public CredentialException(String msg) {
        super(msg);
    }

    public CredentialException() {
        super();
    }
}
