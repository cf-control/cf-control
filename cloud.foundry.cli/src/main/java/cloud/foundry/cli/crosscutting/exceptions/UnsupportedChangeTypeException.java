package cloud.foundry.cli.crosscutting.exceptions;

public class UnsupportedChangeTypeException extends Exception {

    public UnsupportedChangeTypeException(String msg) {
        super(msg);
    }

    public UnsupportedChangeTypeException(Throwable throwable) {
        super(throwable);
    }
}
