package cloud.foundry.cli.crosscutting.exceptions;

/**
 * Thrown to indicate that a node in a yaml tree could not be found.
 */
public class YamlTreeNodeNotFoundException extends RuntimeException {

    public YamlTreeNodeNotFoundException(String message) {
        super(message);
    }
}
