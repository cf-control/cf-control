package cloud.foundry.cli.services;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Mixin;

import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.crosscutting.mapping.RefResolver;
import cloud.foundry.cli.crosscutting.mapping.YamlMapper;
import cloud.foundry.cli.crosscutting.mapping.beans.ConfigBean;

import java.util.concurrent.Callable;

/**
 * This class realizes the functionality that is needed for the dump command.
 */
@Command(name = "dump", header = "%n@|green Read a configuration file, resolve all " + RefResolver.REF_KEY + "s and " +
        "print the result to the console. Helps users to understand how the tool resolves " + RefResolver.REF_KEY +
        " and what the resulting config is it would apply.|@",
        mixinStandardHelpOptions = true)
public class DumpController implements Callable<Integer> {

    private Log log = Log.getLog(DumpController.class);

    // FIXME this field is actually not necessary, but needed due to the argument extension mechanism in the class
    // CfArgumentsCreator... its contents are simply ignored by the dump command
    @Mixin
    private static LoginCommandOptions loginOptions;

    @Mixin
    private YamlCommandOptions yamlCommandOptions;

    /**
     * Implementation of the dump command functionality. It reads the provided configuration file, resolves
     * ref-occurrences, prints the resolved configuration and tries to interpret it as a {@link ConfigBean}.
     */
    @Override
    public Integer call() throws Exception {
        String yamlFilePath = yamlCommandOptions.getYamlFilePath();

        log.info("Reading and processing YAML the configuration file...");
        String resolvedConfig = YamlMapper.resolveYamlFile(yamlFilePath);

        // print the config before interpreting it, so that in cases of interpretation errors, the user still gets his
        // resolved config printed out on the console
        System.out.println(resolvedConfig);

        YamlMapper.interpretBean(resolvedConfig, ConfigBean.class);
        return 0;
    }
}
