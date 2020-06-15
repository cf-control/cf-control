package cloud.foundry.cli.crosscutting.exceptions;

public class ApplyException extends RuntimeException  {

    public ApplyException(Throwable e) {
        super(e);
    }
}
