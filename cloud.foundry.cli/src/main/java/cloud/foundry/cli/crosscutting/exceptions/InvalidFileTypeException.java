package cloud.foundry.cli.crosscutting.exceptions;

import java.io.IOException;

/**
 * Indicates that a file has not the expected file extension.
 */
public class InvalidFileTypeException extends IOException {

    public InvalidFileTypeException(String msg) {
        super(msg);
    }

    public InvalidFileTypeException() {
        super();
    }
}