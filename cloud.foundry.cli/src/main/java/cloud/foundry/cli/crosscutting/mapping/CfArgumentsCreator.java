package cloud.foundry.cli.crosscutting.mapping;

import cloud.foundry.cli.services.DumpController;
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

    private static final Log log = Log.getLog(CfArgumentsCreator.class);

    /**
     * Name of the property file, which contains default values
     */
    private static final String CF_CONTROL_PROPERTIES = "cf-control.properties";

    /**
     * This method reads in arguments passed on the command line.
     * The tool uses default values unless the user overwrites them by passing parameters on the CLI.
     * The default values are defined in a properties file which is compiled into the application.
     *
     * Example:
     * A call "command -s <space name> -a <API endpoint>" would be extended to
     * "command -s <space name> -a <API endpoint> -o <default organization name>",
     * where the default organization name is taken from the properties file.
     *
     * @param cli  CommandLine interpreter
     * @param args Commandline arguments
     * @return Commandline arguments
     * @throws picocli.CommandLine.ParameterException if the arguments are invalid
     */
    public static String[] determineCommandLine(CommandLine cli, String[] args, CommandLine.ParseResult subcommand) {
        // exclude commands which don't use the cloud.foundry.cli.services.LoginCommandOptions mixin
        // TODO: find out whether it's possible to recognize the LoginCommandOptions mixin with reflections only
        if (subcommand != null && subcommand.commandSpec().userObject() instanceof DumpController) {
            return args;
        }


        List<String> optionNames = Arrays.asList("-a", "-o", "-s");
        List<String> missingOptions = new ArrayList<>();

        // to receive the options of the command, you have to go down until the last 'subcommand'
        ParseResult parseResult = cli.parseArgs(args);
        while (parseResult.hasSubcommand()) {
            parseResult = parseResult.subcommand();
        }

        for (String name : optionNames) {
            if (!parseResult.hasMatchedOption(name)) {
                missingOptions.add(name);
            }
        }

        log.verbose("User has not passed values for arguments ", missingOptions, ", using default values");

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

                log.info("Extended CommandLine Argument with the Option: " + key +
                        " and value " + "'" + prop.getProperty(key) + "'");
            });
        } catch (IOException ex) {
            log.error(ex.getMessage());
            System.exit(1);
        }

        return args.toArray(new String[0]);
    }

}
