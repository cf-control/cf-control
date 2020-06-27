package cloud.foundry.cli.system;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class wrapping arguments that shall be passed to the main() function during a system test run simulation.
 * Uses builder pattern.
 */
public class ArgumentsBuilder {
    // stores all arguments entered by the user.
    private final List<String> arguments;

    /**
     * Default constructor. Initializes builder. Use build() to fetch the final arguments array.
     */
    public ArgumentsBuilder() {
        arguments = new ArrayList<>();
    }

    /**
     * Append an argument.
     * @param argument argument to append
     */
    public ArgumentsBuilder addArgument(String argument) {
        arguments.add(argument);
        return this;
    }

    /**
     * Convenience method to append a named option.
     * @param optionName name of option (e.g., -o, -u, -a)
     * @param optionValue option value
     */
    public ArgumentsBuilder addOption(String optionName, String optionValue) {
        arguments.add(optionName);
        arguments.add(optionValue);
        return this;
    }

    /**
     * @return an array containing the entered arguments in the respective order
     */
    public String[] build() {
        // to simulate a main() run, we need a regular String array
        String[] argumentsArray = new String[arguments.size()];
        arguments.toArray(argumentsArray);

        return argumentsArray;
    }
}
