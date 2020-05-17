package cloud.foundry.cli.crosscutting.exceptions;

public class CreationException extends Exception {

        public CreationException(String errorMessage) {
            super(errorMessage);
        }
}
