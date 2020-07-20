package cloud.foundry.cli.crosscutting.exceptions;

import org.yaml.snakeyaml.constructor.ConstructorException;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.error.MarkedYAMLException;

/**
 * This exception class wraps instances of {@link MarkedYAMLException} to provide more helpful error messages.
 */
public class YamlParsingException extends RuntimeException {

    private static final String DUMP_HINT = "Use the dump command to view the resolved configuration!";

    /**
     * Constructor for wrapping arbitrary instances of {@link MarkedYAMLException}.
     * @param markedYamlException the exception that occurred during parsing a configuration
     * @param configName a name or the path or url to the configuration file whose contents were tried to parse
     */
    public YamlParsingException(MarkedYAMLException markedYamlException, String configName) {
        super(constructErrorMessage(markedYamlException, configName), markedYamlException);
    }

    /**
     * Special constructor for wrapping a {@link ConstructorException}. It is assumed that the passed exception occurred
     * due to an interpretation error of a ref-resolved configuration.
     * An explicit hint regarding the dump command is added to the error message to make it easier for the user to
     * locate the error in the configuration.
     *
     * @param constructorException the exception that occurred during the interpretation of a ref-resolved config
     */
    public YamlParsingException(ConstructorException constructorException) {
        super(constructErrorMessage(constructorException, "the resolved configuration") +
                        System.lineSeparator() + System.lineSeparator() + DUMP_HINT,
                constructorException);
    }

    private static String constructErrorMessage(MarkedYAMLException constructorException, String configName) {
        String locationMessage = constructErrorLocationMessage(constructorException.getProblemMark(), configName);
        locationMessage += System.lineSeparator();

        String contextMessage = "";
        if (constructorException.getContext() != null) {
            contextMessage = constructorException.getContext();
            contextMessage += System.lineSeparator();
        }

        String problemMessage = constructorException.getProblem();

        return locationMessage + contextMessage + problemMessage;
    }

    private static String constructErrorLocationMessage(Mark mark, String configName) {
        return "In line " + (mark.getLine() + 1) + ", column " + (mark.getColumn() + 1) + " of " + configName + ":";
    }
}
