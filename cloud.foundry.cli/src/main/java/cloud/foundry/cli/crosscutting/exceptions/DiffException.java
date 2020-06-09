package cloud.foundry.cli.crosscutting.exceptions;

public class DiffException extends Throwable {

    public DiffException(String message) {
        super(message);
    }

    public DiffException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
