package cloud.foundry.cli.logic;

import static com.google.common.base.Preconditions.*;

import cloud.foundry.cli.crosscutting.exceptions.ApplyException;
import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.crosscutting.mapping.CfOperationsCreator;
import cloud.foundry.cli.crosscutting.mapping.beans.ConfigBean;
import cloud.foundry.cli.logic.apply.ApplicationRequestsPlanner;
import cloud.foundry.cli.logic.apply.ServiceRequestsPlanner;
import cloud.foundry.cli.logic.apply.SpaceDevelopersRequestsPlanner;
import cloud.foundry.cli.logic.diff.DiffResult;
import cloud.foundry.cli.logic.diff.change.CfChange;
import cloud.foundry.cli.logic.diff.change.container.CfContainerChange;
import cloud.foundry.cli.operations.*;
import cloud.foundry.cli.services.OptionalLoginCommandOptions;

import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class takes care of applying desired cloud foundry configurations to a
 * live system.
 */
public class ApplyLogic {

    private static final Log log = Log.getLog(ApplyLogic.class);

    private GetLogic getLogic;
    private DiffLogic diffLogic;

    private SpaceDevelopersOperations spaceDevelopersOperations;
    private ServicesOperations servicesOperations;
    private ApplicationsOperations applicationsOperations;
    private TargetOperations targetOperations;
    private SpaceOperations spaceOperations;

    /**
     * Creates a new instance that will use the provided cf operations internally.
     * Starts newly created apps automatically by default
     * @param cfOperations the cf operations that should be used to communicate with
     *                     the cf instance
     * @throws NullPointerException if the argument is null
     */
    public ApplyLogic(@Nonnull DefaultCloudFoundryOperations cfOperations) {
        this(cfOperations, true);
    }

    /**
     * Creates a new instance that will use the provided cf operations internally.
     * @param autoStart sets whether app should start automatically when deployed
     * @param cfOperations the cf operations that should be used to communicate with
     *                     the cf instance
     * @throws NullPointerException if the argument is null
     */
    public ApplyLogic(@Nonnull DefaultCloudFoundryOperations cfOperations, boolean autoStart) {
        checkNotNull(cfOperations);

        this.servicesOperations = new ServicesOperations(cfOperations);
        this.applicationsOperations = new ApplicationsOperations(cfOperations, autoStart);
        this.spaceOperations = new SpaceOperations(cfOperations);
        this.spaceDevelopersOperations = new SpaceDevelopersOperations(cfOperations);
        this.targetOperations = new TargetOperations(cfOperations);

        this.getLogic = new GetLogic();
        this.diffLogic = new DiffLogic();
    }

    public void setApplicationsOperations(ApplicationsOperations applicationsOperations) {
        this.applicationsOperations = applicationsOperations;
    }

    public void setSpaceOperations(SpaceOperations spaceOperations) {
        this.spaceOperations = spaceOperations;
    }

    public void setSpaceDevelopersOperations(SpaceDevelopersOperations spaceDevelopersOperations) {
        this.spaceDevelopersOperations = spaceDevelopersOperations;
    }

    public void setServicesOperations(ServicesOperations servicesOperations) {
        this.servicesOperations = servicesOperations;
    }

    public void setTargetOperations(TargetOperations targetOperations) {
        this.targetOperations = targetOperations;
    }

    public void setDiffLogic(DiffLogic diffLogic) {
        this.diffLogic = diffLogic;
    }

    public void setGetLogic(GetLogic getLogic) {
        this.getLogic = getLogic;
    }

    /**
     * Provides the service of manipulating the state of a cloud foundry instance
     * such that it matches with a desired configuration ({@link ConfigBean}).
     *
     * @param desiredConfigBean   desired configuration for a cloud foundry instance
     * @throws NullPointerException if one of the desired parameters is null.
     */
    public void apply(ConfigBean desiredConfigBean) {
        checkNotNull(desiredConfigBean);
        checkNotNull(desiredConfigBean.getTarget(), "Target bean may not be null.");
        checkNotNull(desiredConfigBean.getTarget().getSpace(), "Space may not be null.");

        try {
            Mono<List<String>> getAllRequest = spaceOperations.getAll();

            log.info("Fetching names of all spaces");
            List<String> spaceNames = getAllRequest.block();
            log.verbose("Fetching names of all spaces completed");

            // getting

            ConfigBean liveConfigBean = new ConfigBean();

            String desiredSpaceName = targetOperations.getSpace();
            // when it's a new space the getAll process can be skipped, since there is nothing to compare the config to
            if (!spaceNames.contains(desiredSpaceName)) {
                log.info("Creating space", desiredSpaceName);

                Mono<Void> createRequest = spaceOperations.create(desiredSpaceName);
                createRequest.block();
                log.verbose("Creating space", desiredSpaceName, "completed");

                // switch to desired space
                log.info("Switching to space", desiredSpaceName);
            } else {
                log.info("Space", desiredSpaceName, "already exists, skipping");

                liveConfigBean = getLogic.getAll(
                        spaceDevelopersOperations,
                        servicesOperations,
                        applicationsOperations,
                        targetOperations);
            }

            // diffing

            DiffResult wrappedDiff = diffLogic.createDiffResult(liveConfigBean, desiredConfigBean);

            CfContainerChange spaceDevelopersChange = wrappedDiff.getSpaceDevelopersChange();
            Map<String, List<CfChange>> servicesChanges = wrappedDiff.getServiceChanges();
            Map<String, List<CfChange>> appsChanges = wrappedDiff.getApplicationChanges();

            if (spaceDevelopersChange == null && servicesChanges.isEmpty() && appsChanges.isEmpty()) {
                log.info("No changes found, no applying necessary.");
                return;
            }

            // applying

            Flux<Void> spaceDevelopersRequests = Flux.empty();
            if (spaceDevelopersChange != null) {
                spaceDevelopersRequests = SpaceDevelopersRequestsPlanner
                        .createSpaceDevelopersRequests(spaceDevelopersOperations, spaceDevelopersChange);
            }

            ServiceRequestsPlanner serviceRequestsPlanner = new ServiceRequestsPlanner(servicesOperations);

            Flux<Void> servicesRequests = Flux.fromIterable(servicesChanges.entrySet())
                    .flatMap(element -> serviceRequestsPlanner.createApplyRequests(
                            element.getKey(),
                            element.getValue()));

            ApplicationRequestsPlanner appRequestsPlanner = new ApplicationRequestsPlanner(applicationsOperations);

            Flux<Void> appsRequests = Flux.fromIterable(appsChanges.entrySet())
                    .flatMap(element -> appRequestsPlanner.createApplyRequests(element.getKey(),
                            element.getValue()));

            // let's be optimistic
            // prove me wrong!
            final AtomicBoolean success = new AtomicBoolean(true);

            Flux.merge(spaceDevelopersRequests, servicesRequests)
                    .concatWith(appsRequests)
                    .onErrorContinue((throwable, consumer) -> {
                        log.error(throwable);
                        success.set(false);
                    })
                    .blockLast();

            if (!success.get()) {
                throw new RuntimeException("Failed to apply configuration: exceptions thrown during execution");
            }

        } catch (Exception ex) {
            throw new ApplyException(ex);
        }
    }
}

