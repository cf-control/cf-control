package cloud.foundry.cli.services;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Mixin;

import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.crosscutting.mapping.CfOperationsCreator;
import cloud.foundry.cli.crosscutting.mapping.YamlMapper;
import cloud.foundry.cli.crosscutting.mapping.beans.ConfigBean;
import cloud.foundry.cli.logic.ApplyLogic;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;

import java.io.IOException;
import java.util.concurrent.Callable;

/**
 * This class realizes the functionality that is needed for the apply commands.
 * They provide the service of manipulating the state of a cloud foundry
 * instance such that it matches with a provided configuration file.
 */
@Command(name = "apply",
        header = "%n@|green Apply the configuration from a given yaml file to your cf instance.|@",
        mixinStandardHelpOptions = true)

public class ApplyController implements Callable<Integer> {

    private static final Log log = Log.getLog(ApplyController.class);

    @Mixin
    private LoginCommandOptions loginOptions;

    @Mixin
    private YamlCommandOptions yamlCommandOptions;

    @Override
    public Integer call() throws IOException {
        log.info("Interpreting YAML file");
        ConfigBean desiredConfigBean = YamlMapper.loadBeanFromFile(yamlCommandOptions.getYamlFilePath(),
                ConfigBean.class);
        log.verbose("Interpreting YAML file completed");


        DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(loginOptions);
        ApplyLogic applyLogic = new ApplyLogic(cfOperations);

        // create space if it does not exist
        String desiredSpaceName = desiredConfigBean.getTarget().getSpace();
        if (desiredSpaceName != null && !loginOptions.getSpace().equals(desiredSpaceName)) {
            loginOptions.setSpace(desiredSpaceName);
            applyLogic.applySpace(desiredSpaceName);

            cfOperations = CfOperationsCreator.createCfOperations(loginOptions);
            applyLogic = new ApplyLogic(cfOperations);
        }

        applyLogic.applyAll(desiredConfigBean, loginOptions);

        return 0;
    }

}
