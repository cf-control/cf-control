package cloud.foundry.cli.crosscutting.exceptions;

public class CredentialException extends Exception {

    public CredentialException(String msg) {
        super(msg);
    }

    public CredentialException() {
        super();
    }
}
