package cloud.foundry.cli.crosscutting.mapping;

import picocli.CommandLine;
import picocli.CommandLine.ParseResult;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import cloud.foundry.cli.crosscutting.logging.Log;

/**
 * This utility class creates certain options with default values, which were not passed when the command was called.
 * There are currently default values for <code>api</code>, <code>organization</code> and <code>space</code>.
 */
public class CfArgumentsCreator {

    /**
     * Name of the property file, which contains default values
     */
    private static final String CF_CONTROL_PROPERTIES = "cf_control.properties";

    /**
     * The method determines the commandline arguments.
     * The following specification applies:
     * when the command is called and one of the option <code>space</code> or / and <code>api</code>
     * or / and <code>organization</code> is missing,
     * the method extends the CommandLine with the corresponding option and
     * sets a default value for this, which has been configured in a property file.
     * <p>
     * For example, it converts the command
     * "command -s spaceValue -a apiValue"
     * into
     * "command -s <space name> -a <API endpoint> -o <default organization name>"
     * by appending the -o with its default value.
     *
     * @param cli  CommandLine interpreter
     * @param args Commandline arguments
     * @return Commandline arguments
     */
    public static String[] determineCommandLine(CommandLine cli, String[] args) {
        List<String> optionNames = Arrays.asList("-a", "-o", "-s");
        List<String> missingOptions = new ArrayList<>();

        ParseResult parseResult = cli.parseArgs(args);
        while (parseResult.hasSubcommand()) {
            parseResult = parseResult.subcommand();
        }

        for (String name : optionNames) {
            if (!parseResult.hasMatchedOption(name)) {
                missingOptions.add(name);
            }
        }

        Log.verbose("User has not passed values for arguments ", missingOptions, ", using default values");

        return extendCommandLine(missingOptions, new LinkedList<>(Arrays.asList(args)));
    }

    /**
     * Extends the commandLine with the missing options by reading the associated default value from a property file.
     *
     * @param missingOptions missing Options like -s, -a, -o
     * @param args           Commandline arguments
     * @return Commandline arguments
     */
    private static String[] extendCommandLine(List<String> missingOptions, LinkedList<String> args) {
        try {
            ClassLoader classLoader = CfArgumentsCreator.class.getClassLoader();
            InputStream input = classLoader.getResourceAsStream(CF_CONTROL_PROPERTIES);

            Properties prop = new Properties();
            prop.load(input);

            missingOptions.forEach(key -> {
                args.add(key);
                args.add(prop.getProperty(key));

                Log.info("Extended CommandLine Argument with the Option: " + key +
                        " and value " + "'" + prop.getProperty(key) + "'");
            });
        } catch (IOException ex) {
            Log.error(ex.getMessage());
        }

        return args.toArray(new String[0]);
    }

}
