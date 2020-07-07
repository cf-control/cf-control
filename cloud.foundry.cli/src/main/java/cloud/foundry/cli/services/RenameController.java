package cloud.foundry.cli.services;

import static picocli.CommandLine.*;
import static picocli.CommandLine.usage;

import cloud.foundry.cli.crosscutting.exceptions.UpdateException;
import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.crosscutting.mapping.CfOperationsCreator;
import cloud.foundry.cli.operations.ApplicationsOperations;
import cloud.foundry.cli.operations.ServicesOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import reactor.core.publisher.Mono;

import java.util.concurrent.Callable;

/**
 * This class realizes the functionality that is needed for the rename commands.
 * You can rename applications or services.
 */
@Command(name = "rename", header = "%n@|green Rename an application or a service.|@",
        subcommands = {RenameController.RenameApplicationCommand.class,
        RenameController.RenameServiceCommand.class})
public class RenameController implements Callable<Integer> {

    @Override
    public Integer call() throws Exception {
        usage(this, System.out);
        return 0;
    }

    @Command(name = "application", description = "Rename an application.")
    static class RenameApplicationCommand implements Callable<Integer> {

        private static final Log log = Log.getLog(RenameApplicationCommand.class);

        @Mixin
        LoginCommandOptions loginOptions;

        @Parameters(index = "0", description = "The current name of the app")
        private String currentName;

        @Parameters(index = "1", description = "The new name of the app")
        private String newName;

        @Override
        public Integer call() throws Exception {
            log.info("Renaming application...");
            DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(loginOptions);
            ApplicationsOperations applicationOperations = new ApplicationsOperations(cfOperations);
            Mono<Void> toRename = applicationOperations.rename(newName, currentName);
            try {
                toRename.block();
            } catch (RuntimeException e) {
                throw new UpdateException(e);
            }
            log.info("Renamed the app from", currentName,"to", newName);
            return 0;
        }
    }

    @Command(name = "service", description = "Rename a service.")
    static class RenameServiceCommand implements Callable<Integer> {

        private static final Log log = Log.getLog(RenameController.RenameServiceCommand.class);
        @Mixin
        LoginCommandOptions loginOptions;
        @Parameters(index = "0", description = "The current name of the service")
        private String currentName;
        @Parameters(index = "1", description = "The new name of the service")
        private String newName;

        @Override
        public Integer call() throws Exception {
            log.info("Renaming service...");
            DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(loginOptions);
            ServicesOperations servicesOperations = new ServicesOperations(cfOperations);
            Mono<Void> toRename = servicesOperations.rename(newName, currentName);
            try {
                toRename.block();
            } catch (RuntimeException e) {
                throw new UpdateException(e);
            }
            log.info("Renamed service. Old-name:", currentName,"new-name:", newName);
            return 0;
        }
    }
}
