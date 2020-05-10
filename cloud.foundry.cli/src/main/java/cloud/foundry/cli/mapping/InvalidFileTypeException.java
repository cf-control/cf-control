package cloud.foundry.cli.mapping;

import java.io.IOException;

public class InvalidFileTypeException extends IOException {

    public InvalidFileTypeException(String msg) {
        super(msg);
    }

    public InvalidFileTypeException() {
        super();
    }
}
