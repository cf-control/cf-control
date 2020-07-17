package cloud.foundry.cli.logic;

import static com.google.common.base.Preconditions.checkNotNull;

import cloud.foundry.cli.crosscutting.exceptions.UpdateException;
import cloud.foundry.cli.operations.ApplicationsOperations;
import cloud.foundry.cli.operations.OperationsFactory;
import cloud.foundry.cli.operations.ServicesOperations;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;

/**
 * Handles the operations to rename applications or services from a cloud
 * foundry instance.
 */
public class RenameLogic {

    private final ApplicationsOperations applicationsOperations;
    private final ServicesOperations servicesOperations;

    /**
     * Creates a new instance that will use the provided operationsFactory internally.
     *
     * @param operationsFactory the factory that should be used to create the operations objects
     * @throws NullPointerException if the argument is null
     */
    public RenameLogic(@Nonnull OperationsFactory operationsFactory) {
        checkNotNull(operationsFactory);

        this.applicationsOperations = operationsFactory.createApplicationsOperations();
        this.servicesOperations = operationsFactory.createServiceOperations();
    }

    /**
     * Rename an existing application.

     * @param newName The new name of the application
     * @param currentName The current name of the application
     * @throws UpdateException if an error occurs during the nameChange procedure
     * @throws NullPointerException when newName, currentName or applicationsOperations is null
     */
    public void renameApplication(String newName, String currentName) {
        checkArgumentsNotNull(newName, currentName);

        Mono<Void> toRename = applicationsOperations.rename(newName, currentName);
        try {
            toRename.block();
        } catch (RuntimeException e) {
            throw new UpdateException(e);
        }
    }


    /**
     * Rename an existing service.
     * @param newName The new name of the service
     * @param currentName The current name of the service
     * @throws UpdateException if an error occurs during the nameChange procedure
     * @throws NullPointerException when newName, currentName or servicesOperations is null
     */
    public void renameService(String newName, String currentName) {
        checkArgumentsNotNull(newName, currentName);

        Mono<Void> toRename = servicesOperations.rename(newName, currentName);
        try {
            toRename.block();
        } catch (RuntimeException e) {
            throw new UpdateException(e);
        }
    }


    private void checkArgumentsNotNull(Object newName, Object currentName) {
        checkNotNull(newName);
        checkNotNull(currentName);
    }

}
