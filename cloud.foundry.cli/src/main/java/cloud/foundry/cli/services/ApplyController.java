package cloud.foundry.cli.services;

import cloud.foundry.cli.crosscutting.mapping.CfOperationsCreator;
import cloud.foundry.cli.crosscutting.mapping.YamlMapper;
import cloud.foundry.cli.crosscutting.mapping.beans.ConfigBean;
import cloud.foundry.cli.logic.ApplyLogic;
import cloud.foundry.cli.operations.ApplicationsOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import picocli.CommandLine;

import java.util.concurrent.Callable;

/**
 * This class realizes the functionality that is needed for the apply commands. They provide the service of manipulating
 * the state of a cloud foundry instance such that it matches with a provided configuration file.
 */
@CommandLine.Command(name = "apply",
        header = "%n@|green Apply the configuration from a given yaml file to your cf instance.|@",
        mixinStandardHelpOptions = true,
        subcommands = {ApplyController.ApplyApplicationCommand.class})
public class ApplyController implements Callable<Integer> {

    @Override
    public Integer call() {
        CommandLine.usage(this, System.out);
        return 0;
    }

    //TODO update the description as soon as the command does more than just creating applications
    @CommandLine.Command(name = "applications", description = "Create applications that are present in the given yaml" +
            " file, but not in your cf instance.")
    static class ApplyApplicationCommand implements Callable<Integer> {

        @CommandLine.Mixin
        private LoginCommandOptions loginOptions;

        @CommandLine.Mixin
        private YamlCommandOptions yamlCommandOptions;

        @Override
        public Integer call() throws Exception {
            DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(loginOptions);
            ApplicationsOperations applicationsOperations = new ApplicationsOperations(cfOperations);

            ConfigBean desiredConfig = YamlMapper.loadBean(yamlCommandOptions.getYamlFilePath(), ConfigBean.class);

            ApplyLogic applyLogic = new ApplyLogic(cfOperations);
            applyLogic.applyApplications(desiredConfig);

            return 0;
        }
    }
}
