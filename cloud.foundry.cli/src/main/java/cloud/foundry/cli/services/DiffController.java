package cloud.foundry.cli.services;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Mixin;

import cloud.foundry.cli.operations.*;
import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.crosscutting.mapping.CfOperationsCreator;
import cloud.foundry.cli.crosscutting.mapping.YamlMapper;
import cloud.foundry.cli.crosscutting.mapping.beans.ConfigBean;
import cloud.foundry.cli.logic.DiffLogic;
import cloud.foundry.cli.logic.GetLogic;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;

import java.io.IOException;
import java.util.concurrent.Callable;

/**
 * This class realizes the functionality that is needed for the diff commands. They provide a comparison of the
 * state of a cloud foundry instance with a provided configuration file.
 */
@Command(name = "diff",
        header = "%n@|green Print the differences between the given yaml file" +
                " and the configuration of your cf instance.|@",
        mixinStandardHelpOptions = true)
public class DiffController implements Callable<Integer> {

    private static final String NO_DIFFERENCES = "There are no differences.";

    private static final Log log = Log.getLog(DiffController.class);

    @Mixin
    private LoginCommandOptions loginOptions;

    @Mixin
    private YamlCommandOptions yamlCommandOptions;

    @Override
    public Integer call() throws IOException {
        ConfigBean desiredConfigBean = YamlMapper.loadBeanFromFile(yamlCommandOptions.getYamlFilePath(),
                ConfigBean.class);
        DefaultCloudFoundryOperations cloudFoundryOperations = CfOperationsCreator.createCfOperations(loginOptions);
        OperationsFactory.setInstance(new DefaultOperationsFactory(cloudFoundryOperations));

        log.debug("Desired config:", desiredConfigBean);
        log.info("Fetching all information for target space...");

        GetLogic getLogic = new GetLogic(OperationsFactory.getInstance());
        ConfigBean currentConfigBean = getLogic.getAll(loginOptions);

        log.debug("Current Config:", currentConfigBean);
        log.info("Diffing ...");

        DiffLogic diffLogic = new DiffLogic();
        String output = diffLogic.createDiffOutput(currentConfigBean, desiredConfigBean);
        log.debug("Diff string of config:", output);

        if (output.isEmpty()) {
            System.out.println(NO_DIFFERENCES);
        } else {
            System.out.println(output);
        }

        return 0;
    }

}

