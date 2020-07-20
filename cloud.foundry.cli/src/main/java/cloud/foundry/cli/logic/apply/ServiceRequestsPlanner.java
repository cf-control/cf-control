package cloud.foundry.cli.logic.apply;

import static com.google.common.base.Preconditions.checkNotNull;

import cloud.foundry.cli.crosscutting.exceptions.ApplyException;
import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.crosscutting.mapping.beans.ServiceBean;
import cloud.foundry.cli.logic.diff.change.CfChange;
import cloud.foundry.cli.logic.diff.change.container.CfContainerChange;
import cloud.foundry.cli.logic.diff.change.object.CfNewObject;
import cloud.foundry.cli.logic.diff.change.object.CfObjectValueChanged;
import cloud.foundry.cli.logic.diff.change.object.CfRemovedObject;
import cloud.foundry.cli.operations.ServicesOperations;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;


/**
 * This class is responsible to build the requests in the context of services according to the CfChanges. 
 **/
public class ServiceRequestsPlanner {

    private static final Log log = Log.getLog(ServiceRequestsPlanner.class);

    private final ServicesOperations servicesOperations;
    private  String serviceName;

    public ServiceRequestsPlanner(ServicesOperations servicesOperations) {
        this.servicesOperations = servicesOperations;
    }

    /**
     * Creates the requests for one service.
     *
     * @param serviceName        the name of the service
     * @param serviceChanges     a list with all the Changes found during diff for
     *                           that specific application
     * @throws ApplyException       if an error during the apply logic occurs. May
     *                              contain another exception inside with more details.
     * @throws NullPointerException when any of the arguments is null
     * @return Flux of all requests that are required to apply the changes
     */
    public Flux<Void> createApplyRequests(String serviceName, List<CfChange> serviceChanges) {
        checkNotNull(serviceName);
        checkNotNull(serviceChanges);

        try {
            this.serviceName = serviceName;
            return doCreateApplyRequests(serviceChanges);
        } catch (Exception exception) {
            throw new ApplyException(exception);
        }
    }

    private Flux<Void> doCreateApplyRequests(List<CfChange> changes) {

        if (hasNewObject(changes)) {
            log.debug("Requesting creation of service", serviceName);

            ServiceBean bean = (ServiceBean) getChange(changes, change -> change instanceof CfNewObject)
                .get()
                .getAffectedObject();

            return Flux.merge(this.servicesOperations.create(this.serviceName, bean));
        } else if (hasRemovedObject(changes)) {
            log.debug("Requesting removal of service", serviceName);

            return Flux.merge(this.servicesOperations.remove(serviceName));
        } else if (hasContainerChange(changes) || hasObjectValueChanged(changes)) {
            log.debug("Requesting update of service", serviceName);
            
            ServiceBean bean = (ServiceBean) getChange(changes, 
                 change -> change instanceof CfContainerChange || change instanceof CfObjectValueChanged)
                .get()
                .getAffectedObject();

            return Flux.merge(this.servicesOperations.update(serviceName, bean));
        } else {
           
            throw new ApplyException("No request can be added for service: " + serviceName);
        }
    }

    private boolean hasRemovedObject(List<CfChange> changes) {
        return changes.stream().anyMatch(change -> change instanceof CfRemovedObject);
    }

    private boolean hasNewObject(List<CfChange> changes) {
        return changes.stream().anyMatch(change -> change instanceof CfNewObject);
    }

    private boolean hasContainerChange(List<CfChange> changes) {
        return changes.stream().anyMatch(change -> change instanceof CfContainerChange);
    }

    private boolean hasObjectValueChanged(List<CfChange> changes) {
        return changes.stream().anyMatch(change -> change instanceof CfObjectValueChanged);
    }

    private Optional<CfChange> getChange(List<CfChange> changes, Predicate<CfChange> predicate) {
        return changes
            .stream()
            .filter(predicate)
            .findFirst();
    }
}
