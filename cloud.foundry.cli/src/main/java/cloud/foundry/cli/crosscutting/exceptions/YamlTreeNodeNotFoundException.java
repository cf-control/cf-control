package cloud.foundry.cli.crosscutting.exceptions;

public class YamlTreeNodeNotFoundException extends RuntimeException {

    public YamlTreeNodeNotFoundException(String message) {
        super(message);
    }
}
