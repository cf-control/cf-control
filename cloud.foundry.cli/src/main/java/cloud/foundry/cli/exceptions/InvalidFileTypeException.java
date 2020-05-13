package cloud.foundry.cli.exceptions;

import java.io.IOException;

public class InvalidFileTypeException extends IOException {

    public InvalidFileTypeException(String msg) {
        super(msg);
    }

    public InvalidFileTypeException() {
        super();
    }
}
