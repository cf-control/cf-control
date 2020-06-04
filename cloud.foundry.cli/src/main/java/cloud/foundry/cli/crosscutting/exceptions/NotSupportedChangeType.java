package cloud.foundry.cli.crosscutting.exceptions;

public class NotSupportedChangeType extends Exception {

    public NotSupportedChangeType(String msg) {
        super(msg);
    }

    public NotSupportedChangeType(Throwable throwable) {
        super(throwable);
    }
}
