package cloud.foundry.cli.logic.apply;

import cloud.foundry.cli.crosscutting.exceptions.ApplyException;
import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.logic.diff.change.CfChange;
import cloud.foundry.cli.logic.diff.change.container.CfContainerChange;
import cloud.foundry.cli.logic.diff.change.map.CfMapChange;
import cloud.foundry.cli.logic.diff.change.object.CfNewObject;
import cloud.foundry.cli.logic.diff.change.object.CfObjectValueChanged;
import cloud.foundry.cli.logic.diff.change.object.CfRemovedObject;
import cloud.foundry.cli.operations.ApplicationsOperations;
import cloud.foundry.cli.operations.ServicesOperations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.LinkedList;
import java.util.List;

/**
 * This class is responsible to build the requests in the context of services according to the CfChanges.
 * The class does create the request tasks by implementing the {@link CfChangeVisitor} interface.
 */
public class ServiceRequestsPlaner implements CfChangeVisitor {

    private static final Log log = Log.getLog(ServiceRequestsPlaner.class);

    private final ServicesOperations servicesOperations;
    private final String serviceName;
    private final List<Mono<Void>> requests;

    private ServiceRequestsPlaner(ServicesOperations servicesOperations, String serviceName) {
        this.servicesOperations = servicesOperations;
        this.serviceName = serviceName;
        this.requests = new LinkedList<>();
    }

    /**
     * Creates the request for CfNewObject
     * @param newObject the CfNewObject to be visited
     */
    @Override
    public void visitNewObject(CfNewObject newObject) {

    }

    /**
     * Creates the requests for CfObjectValueChanged
     * @param objectValueChanged the CfObjectValueChanged to be visited
     */
    @Override
    public void visitObjectValueChanged(CfObjectValueChanged objectValueChanged) {

    }

    /**
     * Creates the requests for CfRemovedObject
     * @param removedObject the CfRemovedObject to be visited
     */
    @Override
    public void visitRemovedObject(CfRemovedObject removedObject) {

    }

    /**
     * Creates the requests CfContainerChange
     * @param containerChange the CfContainerChange to be visited
     */
    @Override
    public void visitContainerChange(CfContainerChange containerChange) {

    }

    /**
     * Creates the requests for CfMapChange
     * @param mapChange the CfMapChange to be visited
     */
    @Override
    public void visitMapChange(CfMapChange mapChange) {

    }

    /**
     * Creates the requests for one service.
     * @param servicesOperations the ServicesOperations object used for
     * @param serviceName the name of the service
     * @param serviceChanges a list with all the Changes found during diff for that specific application
     * @throws ApplyException if an error during the apply logic occurs. May contain another exception inside
     * with more details
     * @return Flux of all requests that are required to apply the changes
     */
    public static Flux<Void> create(ServicesOperations servicesOperations, String serviceName,
                                    List<CfChange> serviceChanges) {
        ServiceRequestsPlaner serviceRequestsPlaner = new ServiceRequestsPlaner(servicesOperations,
                serviceName);
        for (CfChange applicationChange : serviceChanges) {
            applicationChange.accept(serviceRequestsPlaner);
        }

        return Flux.merge(serviceRequestsPlaner.requests);
    }

}
