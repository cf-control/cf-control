package cloud.foundry.cli.operations;

import static com.google.common.base.Preconditions.checkNotNull;

import cloud.foundry.cli.crosscutting.logging.Log;
import org.cloudfoundry.client.v2.spaces.AssociateSpaceDeveloperByUsernameRequest;
import org.cloudfoundry.client.v2.spaces.AssociateSpaceDeveloperByUsernameResponse;
import org.cloudfoundry.client.v2.spaces.RemoveSpaceDeveloperByUsernameRequest;
import org.cloudfoundry.client.v2.spaces.RemoveSpaceDeveloperByUsernameResponse;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.useradmin.ListSpaceUsersRequest;
import org.cloudfoundry.operations.useradmin.SpaceUsers;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.List;


/**
 * Handles the operations for manipulating space developers on a cloud foundry instance.
 *
 * To retrieve the data from resulting Mono or Flux objects you can use subscription methods (block, subscribe, etc.)
 * provided by the reactor library. For more details on how to work with Mono's visit:
 * https://projectreactor.io/docs/core/release/reference/index.html#core-features
 */
public class SpaceDevelopersOperations extends AbstractOperations<DefaultCloudFoundryOperations> {

    private static final Log log = Log.getLog(SpaceDevelopersOperations.class);

    public SpaceDevelopersOperations(DefaultCloudFoundryOperations cfOperations) {
        super(cfOperations);
    }

    /**
     * Prepares a request for fetching space developers from the cloud foundry instance.
     * The resulting mono will not perform any logging by default.
     *
     * @return mono object which yields a list containing the space developers upon subscription
     */
    public Mono<List<String>> getAll() {
        ListSpaceUsersRequest request = ListSpaceUsersRequest.builder()
            .spaceName(cloudFoundryOperations.getSpace())
            .organizationName(cloudFoundryOperations.getOrganization())
            .build();

        return cloudFoundryOperations
            .userAdmin()
            .listSpaceUsers(request)
            .map(SpaceUsers::getDevelopers);
    }

    /**
     * Prepares a request for fetching the space id to the cf instance. The space id is needed for
     * assigning/removing space developers.
     * The resulting mono will not perform any logging by default.
     *
     * @return mono object which yields the space id upon subscription
     */
    public Mono<String> getSpaceId() {
        return cloudFoundryOperations.getSpaceId();
    }

    /**
     * Prepares a request for assigning a space developer to the cf instance.
     * The resulting mono is preconfigured such that it will perform logging.
     *
     * @param username email of user to assign as space developer
     * @param spaceId the id of the space
     * @return mono object which yields the response upon subscription
     * @throws NullPointerException if any of the arguments are null
     */
    public Mono<Void> assign(@Nonnull String username, @Nonnull String spaceId) {
        checkNotNull(username);
        checkNotNull(spaceId);

        AssociateSpaceDeveloperByUsernameRequest request = AssociateSpaceDeveloperByUsernameRequest.builder()
                .username(username)
                .spaceId(spaceId)
                .build();

        return cloudFoundryOperations.getCloudFoundryClient()
                .spaces()
                .associateDeveloperByUsername(request)
                .doOnSubscribe(subscription -> log.debug("Assigning a space developer:", username))
                .doOnSuccess(subscription -> log.debug("Space developer: ", username, " was assigned"))
                .then(Mono.empty());
    }

    /**
     * Prepares a request for removing a space developer of the cf instance.
     * The resulting mono is preconfigured such that it will perform logging.
     *
     * @param username email of user to remove as space developer
     * @param spaceId the id of the space
     * @return mono object which yields the response upon subscription
     * @throws NullPointerException if any of the arguments are null
     */
    public Mono<Void> remove(String username, String spaceId) {
        checkNotNull(username);
        checkNotNull(spaceId);

        RemoveSpaceDeveloperByUsernameRequest request = RemoveSpaceDeveloperByUsernameRequest
                .builder()
                .spaceId(spaceId)
                .username(username)
                .build();

        return cloudFoundryOperations.getCloudFoundryClient()
                .spaces()
                .removeDeveloperByUsername(request)
                .doOnSubscribe(subscription -> log.debug("Removing a space developer:", username))
                .doOnSuccess(subscription -> log.debug("Space developer: ", username, " was removed"))
                .then(Mono.empty());
    }
}
