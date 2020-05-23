package cloud.foundry.cli.services;

import picocli.CommandLine;

/**
 * Options that are required for create commands.
 */
public class CreateControllerCommandOptions {

    @CommandLine.Option(names = {"-y", "--yaml"}, required = true, description = "The path to the yaml file.")
    String yamlFilePath;

    public String getYamlFilePath() {
        return yamlFilePath;
    }
}
