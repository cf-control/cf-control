package cloud.foundry.cli.crosscutting.exceptions;

/**
 * Signals that a create operation has failed.
 */
public class CreationException extends RuntimeException {

        public CreationException(String errorMessage) {
            super(errorMessage);
        }

        public CreationException(Throwable throwable) {
            super(throwable);
        }
}
