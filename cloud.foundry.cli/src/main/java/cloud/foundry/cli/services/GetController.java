package cloud.foundry.cli.services;

import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.crosscutting.mapping.beans.ConfigBean;
import cloud.foundry.cli.crosscutting.mapping.CfOperationsCreator;
import cloud.foundry.cli.crosscutting.mapping.YamlMapper;
import cloud.foundry.cli.logic.GetLogic;
import cloud.foundry.cli.operations.ApplicationsOperations;
import cloud.foundry.cli.operations.ServicesOperations;
import cloud.foundry.cli.operations.SpaceDevelopersOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

import java.util.concurrent.Callable;

/**
 * This class realizes the functionality that is needed for the get commands.
 * They provide various information about a cloud foundry instance.
 */
@Command(name = "get", header = "%n@|green Get the current configuration of your cf instance.|@",
        mixinStandardHelpOptions = true)
public class GetController implements Callable<Integer> {

    private static final Log log = Log.getLog(GetController.class);

    @Mixin
    private static RequiredLoginCommandOptions requiredLoginCommandOptions;

    @Override
    public Integer call() {
        DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(
                null,
                requiredLoginCommandOptions);
        GetLogic getLogic = new GetLogic();

        SpaceDevelopersOperations spaceDevelopersOperations = new SpaceDevelopersOperations(cfOperations);
        ServicesOperations servicesOperations = new ServicesOperations(cfOperations);
        ApplicationsOperations applicationsOperations = new ApplicationsOperations(cfOperations);

        log.info("Fetching all information for target space");
        ConfigBean allInformation = getLogic.getAll(spaceDevelopersOperations, servicesOperations,
                applicationsOperations, requiredLoginCommandOptions);
        log.verbose("Fetching all information for target space completed");

        System.out.println(YamlMapper.dump(allInformation));
        return 0;
    }

}

