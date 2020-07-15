package cloud.foundry.cli.crosscutting.mapping;

import static java.util.Arrays.asList;

import cloud.foundry.cli.services.DumpController;
import picocli.CommandLine;
import picocli.CommandLine.ParseResult;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.yaml.snakeyaml.constructor.ConstructorException;
import org.yaml.snakeyaml.parser.ParserException;
import org.yaml.snakeyaml.scanner.ScannerException;

import cloud.foundry.cli.crosscutting.exceptions.RefResolvingException;
import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.crosscutting.mapping.beans.ConfigBean;
import cloud.foundry.cli.crosscutting.mapping.beans.TargetBean;

/**
 * This utility class creates certain options with default values, which were
 * not passed when the command was called. There are currently default values
 * for <code>api</code>, <code>organization</code> and <code>space</code>.
 */
public class CfArgumentsCreator {

    private static final Log log = Log.getLog(CfArgumentsCreator.class);

    /**
     * Name of the property file, which contains default values
     */
    private static final String CF_CONTROL_PROPERTIES = "cf-control.properties";

    /**
     * This method reads in arguments passed on the command line. The tool uses
     * default values unless the user overwrites them by passing parameters on the
     * CLI. For <code>get</code> command, the default values are defined in a
     * properties file. Otherwise for the <code>diff</code>/<code>apply</code>
     * command, the default values can be fetched from the taget section of the
     * given YAML file.
     * 
     * Example: A call "get -s <space name> -a <API endpoint>" would be extended to
     * "get -s <space name> -a <API endpoint> -o <default organization name>", where
     * the default organization name is taken from the properties file.
     * 
     * A call "diff -s <space name> -a <API endpoint> -y <Path to YAML file>" would
     * be extended to "diff -s <space name> -a <API endpoint> -y <Path to YAML File>
     * -o <default organization name>", where the default organization name is taken
     * from the taget section of the given YAML file.
     * 
     *
     * @param cli  CommandLine interpreter
     * @param args Commandline arguments
     * @return Commandline arguments
     * @throws picocli.CommandLine.ParameterException if the arguments are invalid
     */
    public static String[] determineCommandLine(CommandLine cli, String[] args, CommandLine.ParseResult subcommand) {
        // exclude commands which don't use the
        // cloud.foundry.cli.services.LoginCommandOptions mixin
        // if the subcommand is null, we're in the base controller (or its help options)
        // TODO: find out whether it's possible to recognize the LoginCommandOptions
        // mixin with reflections only
        if (subcommand == null || subcommand.commandSpec().userObject() instanceof DumpController) {
            return args;
        }

        List<String> optionNames = asList("-a", "-o", "-s");
        List<String> missingOptions = new ArrayList<>();

        // to receive the options of the command, you have to go down until the last
        // 'subcommand'
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
        // In apply space we don't have a yaml file. Should behave in the same way as the get command
        //TODO: Remove this if, after cretaing apply all and removing apply subcommands
        if (asList(args).contains("get") || asList(args).contains("space")) {
            // get missing values from config-properties File and extend to get command
            return extendForGetCommand(missingOptions, new LinkedList<>(asList(args)));
        } else {
            // get missing values from the given YAML File and extend to diff/apply command
            return extendForDiffAndApplyCommand(missingOptions, new LinkedList<>(asList(args)));
        }
    }

    /**
     * Extends the commandLine with the missing options by reading the associated
     * default value from a property file.
     *
     * @param missingOptions missing Options like -s, -a, -o
     * @param args           Commandline arguments
     * @return Commandline arguments
     */
    private static String[] extendForGetCommand(List<String> missingOptions, LinkedList<String> args) {
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

    /**
     * Extends the commandLine with the missing options by reading the given yaml
     * file.
     *
     * @param missingOptions missing Options like -s, -a, -o
     * @param args           Commandline arguments
     * @return Commandline arguments
     */
    private static String[] extendForDiffAndApplyCommand(List<String> missingOptions, LinkedList<String> args) {

        String yamlPath = args.get(args.indexOf("-y") + 1);
        ConfigBean configBean =  new ConfigBean();

        try {
            configBean = YamlMapper.loadBeanFromFile(yamlPath, ConfigBean.class);
        } catch (IOException | ParserException | ScannerException | RefResolvingException | ConstructorException ex) {
            log.error(ex.getMessage());
            System.exit(1);
        }

        TargetBean targetBean = configBean.getTarget();
        missingOptions.forEach(key -> {
            args.add(key);
            String value = "";
            switch (key) {
            case "-a":
                value = targetBean.getEndpoint();
                break;
            case "-o":
                value = targetBean.getOrg();
                break;
            case "-s":
                value = targetBean.getSpace();
                break;
            default:
                break;
            }

            args.add(value);
            log.info("Extended CommandLine Argument with the Option: " + key +
                " and value " + "'" + value + "'");
        });

        return args.toArray(new String[0]);
    }

}
