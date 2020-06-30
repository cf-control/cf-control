package cloud.foundry.cli.logic.apply;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import cloud.foundry.cli.crosscutting.exceptions.ApplyException;
import cloud.foundry.cli.crosscutting.exceptions.UpdateException;
import cloud.foundry.cli.crosscutting.exceptions.CreationException;
import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.crosscutting.mapping.beans.ServiceBean;
import cloud.foundry.cli.logic.diff.change.CfChange;
import cloud.foundry.cli.logic.diff.change.object.CfNewObject;
import cloud.foundry.cli.logic.diff.change.object.CfRemovedObject;
import cloud.foundry.cli.operations.ServicesOperations;
import reactor.core.publisher.Flux;

import java.util.List;


/**
 * This class is responsible to build the requests in the context of services according to the CfChanges.
 * The class does create the request tasks by implementing the {@link CfChangeVisitor} interface.
 */
public class ServiceRequestsPlaner extends RequestsPlaner {

    private static final Log log = Log.getLog(ServiceRequestsPlaner.class);

    private final ServicesOperations servicesOperations;
    private final String serviceName;

    private ServiceRequestsPlaner(ServicesOperations servicesOperations, String serviceName) {
        this.servicesOperations = servicesOperations;
        this.serviceName = serviceName;
    }

    /**
     * Creates the request for CfNewObject
     * @param newObject the CfNewObject to be visited
     */
    @Override
    public void visitNewObject(CfNewObject newObject) {
        checkArgument(this.getRequests().size() == 0,
                "There may not be any other requests for that service when adding a create request.");

        Object affectedObject = newObject.getAffectedObject();
        if (affectedObject instanceof ServiceBean) {
            try {
                addCreateServiceRequest((ServiceBean) affectedObject);
            } catch (CreationException | IllegalArgumentException | NullPointerException | SecurityException e) {
                throw new ApplyException(e);
            }
        }
        else {
            throw new IllegalArgumentException("Only changes of services are permitted.");
        }
    }

    private void addCreateServiceRequest(ServiceBean affectedObject) {
        this.getRequests().add(this.servicesOperations.create(this.serviceName, affectedObject));
    }

    /**
     * Creates the requests for CfRemovedObject
     * @param removedObject the CfRemovedObject to be visited
     */
    @Override
    public void visitRemovedObject(CfRemovedObject removedObject) {
        checkArgument(this.getRequests().size() == 0,
                "There may not be any other requests for that service when adding a remove request.");

        if (!(removedObject.getAffectedObject() instanceof ServiceBean)) {
            throw new IllegalArgumentException("Only changes of services are permitted.");
        }

        try {
            log.info("Adding remove request for service " + serviceName);
            this.addRequest(servicesOperations.remove(serviceName));
        } catch (UpdateException | NullPointerException e) {
            throw new ApplyException(e);
        }
    }

    /**
     * Creates the requests for one service.
     * @param servicesOperations the ServicesOperations object used for
     * @param serviceName the name of the service
     * @param serviceChanges a list with all the Changes found during diff for that specific application
     * @throws ApplyException if an error during the apply logic occurs. May contain another exception inside
     * with more details
     * @throws NullPointerException when any of the arguments is null
     * @return Flux of all requests that are required to apply the changes
     */
    public static Flux<Void> createApplyRequests(ServicesOperations servicesOperations,
                                                 String serviceName,
                                                 List<CfChange> serviceChanges) {
        checkNotNull(servicesOperations);
        checkNotNull(serviceName);
        checkNotNull(serviceChanges);

        ServiceRequestsPlaner serviceRequestsPlaner = new ServiceRequestsPlaner(servicesOperations,
                serviceName);
        for (CfChange applicationChange : serviceChanges) {
            applicationChange.accept(serviceRequestsPlaner);
        }

        return Flux.merge(serviceRequestsPlaner.getRequests());
    }

}
