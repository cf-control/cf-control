package cloud.foundry.cli.logic;

import cloud.foundry.cli.crosscutting.exceptions.UpdateException;
import cloud.foundry.cli.operations.ApplicationsOperations;
import cloud.foundry.cli.operations.ServicesOperations;
import reactor.core.publisher.Mono;

/**
 * Handles the operations to rename applications or services from a cloud
 * foundry instance.
 */
public class RenameLogic {

    /**
     * Rename an existing application.
     * @param applicationsOperations ApplicationsOperations
     * @param newName The new name of the application
     * @param currentName The current name of the application
     * @throws UpdateException if an error occurs during the nameChange procedure
     */
    public void renameApplication(ApplicationsOperations applicationsOperations, String newName, String currentName){
        Mono<Void> toRename = applicationsOperations.rename(newName, currentName);
        try {
            toRename.block();
        } catch (RuntimeException e) {
            throw new UpdateException(e);
        }
    }

    /**
     * Rename an existing service.
     * @param servicesOperations ServicesOperations
     * @param newName The new name of the service
     * @param currentName The current name of the service
     * @throws UpdateException if an error occurs during the nameChange procedure
     */
    public void renameService(ServicesOperations servicesOperations, String newName, String currentName){
        Mono<Void> toRename = servicesOperations.rename(newName, currentName);
        try {
            toRename.block();
        } catch (RuntimeException e) {
            throw new UpdateException(e);
        }
    }


}
