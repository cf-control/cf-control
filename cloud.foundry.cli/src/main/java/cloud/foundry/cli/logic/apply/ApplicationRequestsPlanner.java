package cloud.foundry.cli.logic.apply;

import static com.google.common.base.Preconditions.checkNotNull;

import cloud.foundry.cli.crosscutting.exceptions.ApplyException;
import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationBean;
import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationManifestBean;
import cloud.foundry.cli.crosscutting.mapping.validation.ObjectPropertyValidation;
import cloud.foundry.cli.logic.diff.change.CfChange;
import cloud.foundry.cli.logic.diff.change.ChangeType;
import cloud.foundry.cli.logic.diff.change.container.CfContainerChange;
import cloud.foundry.cli.logic.diff.change.container.CfContainerValueChanged;
import cloud.foundry.cli.logic.diff.change.map.CfMapChange;
import cloud.foundry.cli.logic.diff.change.map.CfMapValueChanged;
import cloud.foundry.cli.logic.diff.change.object.CfNewObject;
import cloud.foundry.cli.logic.diff.change.object.CfRemovedObject;
import cloud.foundry.cli.operations.ApplicationsOperations;
import org.cloudfoundry.operations.applications.ApplicationHealthCheck;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.Predicate;

/**
 * This class is responsible to build the requests in the context of
 * applications according to the CfChanges.
 */
public class ApplicationRequestsPlanner {

    private static final Log log = Log.getLog(ApplicationRequestsPlanner.class);

    private static final String META_FIELD_NAME = "meta";
    private static final String PATH_FIELD_NAME = "path";
    private static final String BUILDPACK_FIELD_NAME = "buildpack";
    private static final String COMMAND_FIELD_NAME = "command";
    private static final String STACK_FIELD_NAME = "stack";
    private static final String HEALTH_CHECK_TYPE_FIELD_NAME = "healthCheckType";
    private static final String HEALTH_CHECK_HTTP_ENDPOINT_FIELD_NAME = "healthCheckHttpEndpoint";
    private static final String MEMORY_FIELD_NAME = "memory";
    private static final String DISK_FIELD_NAME = "disk";
    private static final String SERVICES_FIELD_NAME = "services";
    private static final String ENVIRONMENT_VARIABLES_FIELD_NAME = "environmentVariables";
    private static final String ROUTES_FIELD_NAME = "routes";
    private static final String INSTANCES_FIELD_NAME = "instances";


    // stores field-names related to applications that require an app restart when
    // their values in the configuration are changed
    private static final Set<String> FIELDS_REQUIRE_RESTART = new HashSet<String>() {{
        add(META_FIELD_NAME);
        add(PATH_FIELD_NAME);
        add(BUILDPACK_FIELD_NAME);
        add(COMMAND_FIELD_NAME);
        add(STACK_FIELD_NAME);
        add(HEALTH_CHECK_TYPE_FIELD_NAME);
        add(HEALTH_CHECK_HTTP_ENDPOINT_FIELD_NAME);
        add(MEMORY_FIELD_NAME);
        add(DISK_FIELD_NAME);
    }};

    // assertion checks to make sure fields actually exist
    static {
        ObjectPropertyValidation.checkFieldExists(ApplicationBean.class, META_FIELD_NAME, String.class);
        ObjectPropertyValidation.checkFieldExists(ApplicationBean.class, PATH_FIELD_NAME, String.class);
        ObjectPropertyValidation.checkFieldExists(ApplicationManifestBean.class, BUILDPACK_FIELD_NAME, String.class);
        ObjectPropertyValidation.checkFieldExists(ApplicationManifestBean.class, COMMAND_FIELD_NAME, String.class);
        ObjectPropertyValidation.checkFieldExists(ApplicationManifestBean.class, STACK_FIELD_NAME, String.class);
        ObjectPropertyValidation.checkFieldExists(ApplicationManifestBean.class,
                HEALTH_CHECK_TYPE_FIELD_NAME,
                ApplicationHealthCheck.class);
        ObjectPropertyValidation.checkFieldExists(ApplicationManifestBean.class,
                HEALTH_CHECK_HTTP_ENDPOINT_FIELD_NAME,
                String.class);
        ObjectPropertyValidation.checkFieldExists(ApplicationManifestBean.class, MEMORY_FIELD_NAME, Integer.class);
        ObjectPropertyValidation.checkFieldExists(ApplicationManifestBean.class, DISK_FIELD_NAME, Integer.class);
        ObjectPropertyValidation.checkListExists(ApplicationManifestBean.class, SERVICES_FIELD_NAME, String.class);
        ObjectPropertyValidation.checkListExists(ApplicationManifestBean.class, ROUTES_FIELD_NAME, String.class);
        ObjectPropertyValidation.checkFieldExists(ApplicationManifestBean.class, INSTANCES_FIELD_NAME, Integer.class);
        ObjectPropertyValidation.checkMapExists(ApplicationManifestBean.class,
                ENVIRONMENT_VARIABLES_FIELD_NAME,
                String.class,
                Object.class);
    }

    private final ApplicationsOperations appOperations;
    private String applicationName;

    /**
     *
     * @param appOperations the ApplicationOperations object used for
     */
    public ApplicationRequestsPlanner(ApplicationsOperations appOperations) {
        this.appOperations = appOperations;
    }

    /**
     * Creates the remove/create requests for one application.
     *
     * @param applicationName    the name of the application
     * @param applicationChanges a list with all the Changes found during diff for
     *                           that specific application
     * @throws NullPointerException if any of the arguments are null
     * @throws ApplyException if during the planing process a non recoverable error occurs
     * @return flux of all requests that are required to apply the changes
     */
    public Flux<Void> createApplyRequests(@Nonnull String applicationName,
        @Nonnull List<CfChange> applicationChanges) {
        checkNotNull(applicationName);
        checkNotNull(applicationChanges);

        try {
            this.applicationName = applicationName;
            return this.doCreateApplyRequests(applicationChanges);
        } catch (Exception exception) {
            throw new ApplyException(exception);
        }
    }

    private Flux<Void> doCreateApplyRequests(List<CfChange> changes) {
        List<Publisher<Void>> requests = new LinkedList<>();

        if (hasNewObject(changes)) {
            log.debug("Requesting creation of app", applicationName);

            ApplicationBean bean = (ApplicationBean) getChange(changes, change -> change instanceof CfNewObject)
                    .get()
                    .getAffectedObject();

            return Flux.merge(this.appOperations.create(applicationName, bean));
        } else if (hasRemovedObject(changes)) {
            log.debug("Requesting removal of app", applicationName);

            return Flux.merge(this.appOperations.remove(applicationName));
        } else if (hasFieldsThatRequireRestart(changes)) {
            log.debug("Requesting redeployment/update of app", applicationName);

            for (CfChange change : changes) {
                logChange(change);
            }

            ApplicationBean bean = (ApplicationBean) changes.get(0).getAffectedObject();
            return Flux.concat(appOperations.update(applicationName, bean));
        } else if (changes.size() > 0) {
            log.debug("Requesting rolling update of app " + applicationName);
            requests.add(getScaleInstancesRequest(changes));
            requests.add(getChangedEnvironmentVariablesRequests(changes));
            requests.add(getChangedServicesRequests(changes));
            requests.add(getChangedRoutesRequests(changes));
        }

        return Flux.merge(requests);
    }

    private boolean hasFieldsThatRequireRestart(List<CfChange> changes) {
        return changes.stream().anyMatch(change -> FIELDS_REQUIRE_RESTART.contains(change.getPropertyName()));
    }

    private boolean hasRemovedObject(List<CfChange> changes) {
        return changes.stream().anyMatch(change -> change instanceof CfRemovedObject);
    }

    private boolean hasNewObject(List<CfChange> changes) {
        return changes.stream().anyMatch(change -> change instanceof CfNewObject);
    }

    private Flux<Void> getChangedServicesRequests(List<CfChange> changes) {
        Optional<CfChange> optionalServicesChange = getChange(changes,
                change -> change.getPropertyName().equals(SERVICES_FIELD_NAME));
        List<Mono<Void>> requests = new LinkedList<>();

        if (optionalServicesChange.isPresent()) {

            CfContainerChange servicesChange = (CfContainerChange) optionalServicesChange.get();
            logChange(servicesChange);

            for (CfContainerValueChanged valueChanged : servicesChange.getValueChangesBy(ChangeType.ADDED)) {
                log.debug("Requesting binding of service", valueChanged.getValue(), "to application", applicationName);
                requests.add(this.appOperations.bindToService(applicationName, valueChanged.getValue()));
            }

            for (CfContainerValueChanged valueChanged : servicesChange.getValueChangesBy(ChangeType.REMOVED)) {
                log.debug(
                        "Requesting unbinding of service", valueChanged.getValue(),
                        "from application", applicationName
                );
                requests.add(this.appOperations.unbindFromService(applicationName, valueChanged.getValue()));
            }

        }
        return Flux.merge(requests);
    }

    private Flux<Void> getChangedEnvironmentVariablesRequests(List<CfChange> changes) {
        Optional<CfChange> optionalEnvVarsChange = getChange(changes,
                change -> change.getPropertyName().equals(ENVIRONMENT_VARIABLES_FIELD_NAME));

        if (optionalEnvVarsChange.isPresent()) {
            List<Mono<Void>> requests = new LinkedList<>();
            CfMapChange enVarsChange = (CfMapChange) optionalEnvVarsChange.get();
            logChange(enVarsChange);

            for (CfMapValueChanged valueChanged : enVarsChange.getChangedValues()) {
                switch (valueChanged.getChangeType()) {
                    case ADDED:
                        log.debug("Requesting addition of environment variable",
                                valueChanged.getKey(),
                                "with value",
                                valueChanged.getValueAfter(),
                                "to application",
                                applicationName);
                        requests.add(this.appOperations.addEnvironmentVariable(applicationName,
                                valueChanged.getKey(),
                                valueChanged.getValueAfter()));
                        break;
                    case CHANGED:
                        log.debug("Requesting change of environment variable",
                                valueChanged.getKey(),
                                "from value",
                                valueChanged.getValueBefore(),
                                "to value",
                                valueChanged.getValueAfter(),
                                "for application",
                                applicationName);
                        requests.add(this.appOperations.addEnvironmentVariable(applicationName,
                                valueChanged.getKey(),
                                valueChanged.getValueAfter()));
                        break;
                    case REMOVED:
                        log.debug("Requesting removal of environment variable",
                                valueChanged.getKey(),
                                "from application",
                                applicationName);
                        requests.add(this.appOperations.removeEnvironmentVariable(applicationName,
                                valueChanged.getKey()));
                        break;
                    default:
                        throw new AssertionError("Encountered unknown change type " + valueChanged.getChangeType());
                }
            }
            return Flux.concat(requests);
        }
        return Flux.empty();
    }


    private Publisher<Void> getChangedRoutesRequests(List<CfChange> changes) {
        Optional<CfChange> optionalRoutesChange = getChange(changes,
                change -> change.getPropertyName().equals(ROUTES_FIELD_NAME));
        List<Mono<Void>> requests = new LinkedList<>();

        if (optionalRoutesChange.isPresent()) {
            CfContainerChange routesChanges = (CfContainerChange) optionalRoutesChange.get();
            logChange(routesChanges);

            for (CfContainerValueChanged valueChanged : routesChanges.getValueChangesBy(ChangeType.ADDED)) {
                log.debug("Requesting addition of route",
                        valueChanged.getValue(),
                        "to application",
                        applicationName);
                requests.add(this.appOperations.addRoute(applicationName, valueChanged.getValue()));
            }

            for (CfContainerValueChanged valueChanged : routesChanges.getValueChangesBy(ChangeType.REMOVED)) {
                log.debug("Requesting removal of route",
                        valueChanged.getValue(),
                        "from application",
                        applicationName);
                requests.add(this.appOperations.removeRoute(applicationName, valueChanged.getValue()));
            }

        }
        return Flux.concat(requests);
    }


    private Mono<Void> getScaleInstancesRequest(List<CfChange> changes) {
        Optional<CfChange> instancesChange = getChange(changes,
                change -> change.getPropertyName().equals(INSTANCES_FIELD_NAME));

        if (instancesChange.isPresent()) {
            logChange(instancesChange.get());

            ApplicationBean bean = (ApplicationBean) instancesChange.get().getAffectedObject();
            // only changing instances can be done inplace
            return this.appOperations.scale(applicationName,
                    null,
                    null,
                    bean.getManifest().getInstances());
        }
        return Mono.empty();
    }

    private Optional<CfChange> getChange(List<CfChange> changes, Predicate<CfChange> predicate) {
        return changes
                .stream()
                .filter(predicate)
                .findFirst();
    }

    private void logChange(CfChange change) {
        log.debug("Property", change.getPropertyName(), "for app", applicationName, "will be updated.");
    }
}
