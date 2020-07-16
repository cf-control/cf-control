package cloud.foundry.cli.logic;

import static com.google.common.base.Preconditions.checkNotNull;

import cloud.foundry.cli.crosscutting.exceptions.ApplyException;
import cloud.foundry.cli.crosscutting.exceptions.GetException;
import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.crosscutting.mapping.beans.ConfigBean;
import cloud.foundry.cli.logic.apply.ApplicationRequestsPlaner;
import cloud.foundry.cli.logic.apply.ServiceRequestsPlaner;
import cloud.foundry.cli.logic.apply.SpaceDevelopersRequestsPlaner;
import cloud.foundry.cli.logic.diff.DiffResult;
import cloud.foundry.cli.logic.diff.change.container.CfContainerChange;
import cloud.foundry.cli.operations.*;
import cloud.foundry.cli.services.LoginCommandOptions;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.List;

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
    private SpaceOperations spaceOperations;
    private ClientOperations clientOperations;


    /**
     * Creates a new instance that will use the provided cf operations internally.
     *
     * @param cfOperations the cf operations that should be used to communicate with
     *                     the cf instance
     * @throws NullPointerException if the argument is null
     */
    public ApplyLogic(@Nonnull DefaultCloudFoundryOperations cfOperations) {
        checkNotNull(cfOperations);

        this.spaceDevelopersOperations = new SpaceDevelopersOperations(cfOperations);
        this.servicesOperations = new ServicesOperations(cfOperations);
        this.applicationsOperations = new ApplicationsOperations(cfOperations);
        this.spaceOperations = new SpaceOperations(cfOperations);
        this.spaceDevelopersOperations = new SpaceDevelopersOperations(cfOperations);
        this.clientOperations = new ClientOperations(cfOperations);

        this.getLogic = new GetLogic();
        this.diffLogic = new DiffLogic();
    }

    public void setApplicationsOperations(ApplicationsOperations applicationsOperations) {
        this.applicationsOperations = applicationsOperations;
    }

    public void setSpaceOperations(SpaceOperations spaceOperations) {
        this.spaceOperations = spaceOperations;
    }

    public void setDiffLogic(DiffLogic diffLogic) {
        this.diffLogic = diffLogic;
    }

    public void setGetLogic(GetLogic getLogic) {
        this.getLogic = getLogic;
    }

    /**
     * @param desiredConfigBean
     * @param loginCommandOptions
     */
    public void applyAll(ConfigBean desiredConfigBean, LoginCommandOptions loginCommandOptions) {
        checkNotNull(desiredConfigBean);
        checkNotNull(loginCommandOptions);

        // create space if it does not exist
        applySpace(desiredConfigBean.getTarget().getSpace());

        ConfigBean liveConfigBean = getLogic.getAll(
                spaceDevelopersOperations,
                servicesOperations,
                applicationsOperations,
                clientOperations,
                loginCommandOptions);

        DiffResult wrappedDiff = this.diffLogic.createDiffResult(liveConfigBean, desiredConfigBean);

        ApplicationRequestsPlaner applicationRequestsPlaner
                = new ApplicationRequestsPlaner(applicationsOperations);

        CfContainerChange spaceDevelopersChange = wrappedDiff.getSpaceDevelopersChange();
        if (spaceDevelopersChange != null) {
            SpaceDevelopersRequestsPlaner.createSpaceDevelopersRequests(spaceDevelopersOperations, spaceDevelopersChange);
        }


        Flux.fromIterable(wrappedDiff.getServiceChanges().entrySet())
                .flatMap(element -> ServiceRequestsPlaner.createApplyRequests(
                        servicesOperations,
                        element.getKey(),
                        element.getValue()))
                .concatWith(Flux.fromIterable(wrappedDiff.getApplicationChanges().entrySet())
                        .flatMap(element -> applicationRequestsPlaner.createApplyRequests(element.getKey(),
                                element.getValue())))
                .onErrorContinue(log::error)
                .blockLast();
    }

    /**
     * Creates a space with the desired name if a space with such a name does not
     * exist in the live cf instance.
     *
     * @param desiredSpaceName the name of the desired space
     * @throws NullPointerException if the desired parameter is null
     * @throws ApplyException       in case of errors during the creation of the
     *                              desired space
     * @throws GetException         in case of errors during querying the space
     *                              names
     */
    public void applySpace(String desiredSpaceName) {
        checkNotNull(desiredSpaceName);

        Mono<List<String>> getAllRequest = spaceOperations.getAll();
        log.info("Fetching all space names...");

        List<String> spaceNames;
        try {
            spaceNames = getAllRequest.block();
        } catch (Exception e) {
            throw new GetException(e);
        }

        if (!spaceNames.contains(desiredSpaceName)) {
            log.info("Creating space with name:", desiredSpaceName);
            Mono<Void> createRequest = spaceOperations.create(desiredSpaceName);
            try {
                createRequest.block();
            } catch (Exception e) {
                throw new ApplyException(e);
            }
        } else {
            log.info("Space with name", desiredSpaceName, "already exists");
        }
    }

}

