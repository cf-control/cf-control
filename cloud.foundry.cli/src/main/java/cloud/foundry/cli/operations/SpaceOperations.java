package cloud.foundry.cli.operations;

import static com.google.common.base.Preconditions.checkNotNull;

import cloud.foundry.cli.crosscutting.logging.Log;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.spaces.CreateSpaceRequest;
import org.cloudfoundry.operations.spaces.SpaceSummary;
import reactor.core.publisher.Mono;

import java.util.List;

public class SpaceOperations extends AbstractOperations<DefaultCloudFoundryOperations> {

    private static final Log log = Log.getLog(SpaceOperations.class);

    public SpaceOperations(DefaultCloudFoundryOperations cloudFoundryOperations) {
        super(cloudFoundryOperations);
    }

    /**
     * Prepares a request for fetching all spaces from the cloud foundry
     * instance. The resulting mono will not perform any logging by default.
     *
     * @return mono which can be subscribed on to trigger the fetching of the space names
     */
    public Mono<List<String>> getAll() {
        return this.cloudFoundryOperations
                .spaces()
                .list()
                // ignore space ids for now (they could be cached to avoid querying them again,
                // when manipulating space developers)
                .map(SpaceSummary::getName)
                .collectList()
                .doOnSubscribe(subscription -> log.info("Querying all spaces"))
                .doOnSuccess(stringApplicationBeanMap -> log.info("Querying all spaces completed"));
    }

    /**
     * Prepares a request for creating a space in the cf instance.
     * The resulting mono is preconfigured such that it will perform logging.
     *
     * @param spaceName The name of the space to be created
     * @return mono which can be subscribed on to trigger the creation of the space
     * @throws NullPointerException when the space name is null
     */
    public Mono<Void> create(String spaceName) {
        checkNotNull(spaceName);

        CreateSpaceRequest createSpaceRequest = CreateSpaceRequest.builder()
                .name(spaceName)
                .build();
        return this.cloudFoundryOperations.spaces().create(createSpaceRequest)
                .doOnSubscribe(aVoid -> log.debug("Creating space", spaceName))
                .doOnSuccess(aVoid -> log.verbose("Creating space", spaceName, "completed"));
    }

}
