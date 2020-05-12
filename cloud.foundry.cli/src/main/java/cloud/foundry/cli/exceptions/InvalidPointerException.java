package cloud.foundry.cli.exceptions;

public class InvalidPointerException extends RuntimeException {

    private String pointer;

    public InvalidPointerException(String msg, String pointer) {
        super(msg);
        this.pointer = pointer;
    }

}
