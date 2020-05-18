package cloud.foundry.cli.services;

import picocli.CommandLine;

public class CreateControllerCommandOptions {

    @CommandLine.Option(names = {"-y", "--yaml"}, required = true)
    String yamlFilePath;

    public String getYamlFilePath() {
        return yamlFilePath;
    }
}
