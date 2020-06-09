package cloud.foundry.cli.operations;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static java.util.stream.Collectors.toList;

import cloud.foundry.cli.crosscutting.exceptions.CreationException;

import cloud.foundry.cli.crosscutting.exceptions.InvalidOperationException;
import cloud.foundry.cli.crosscutting.logging.Log;
import org.cloudfoundry.client.v2.spaces.AssociateSpaceDeveloperByUsernameRequest;
import org.cloudfoundry.client.v2.spaces.RemoveSpaceDeveloperByUsernameRequest;
import org.cloudfoundry.client.v2.spaces.RemoveSpaceDeveloperByUsernameResponse;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.useradmin.ListSpaceUsersRequest;
import org.cloudfoundry.operations.useradmin.SpaceUsers;
import reactor.core.publisher.Mono;

import java.util.List;


/**
 * Handles the operations for manipulating space developers on a cloud foundry
 * instance.
 */
public class SpaceDevelopersOperations extends AbstractOperations<DefaultCloudFoundryOperations> {

    private static final String REMOVE_USER_AS_SPACE_DEVELOPER = "Remove user as space developer";

    private static final String INVALID_VALUE_OF_SPACE_ID = "The value of 'SpaceId' should not be null or empty!";
    public static final String USERNAME_LIST_MUST_BE_NOT_NULL_OR_EMPTY =
            "The 'usernameList' must be not null or empty!";

    public SpaceDevelopersOperations(DefaultCloudFoundryOperations cfOperations) {
        super(cfOperations);
    }

    /**
     * This method fetches space developers from the cloud foundry instance.
     * To retrieve data given by the Mono object you can use the subscription methods (block, subscribe, etc.) provided
     * by the reactor library.
     * For more details on how to work with Mono's visit:
     * https://projectreactor.io/docs/core/release/reference/index.html#core-features
     * @return Mono object of SpaceDeveloperBean which contains the space developers
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
     * Assign a user as a space developer
     *
     * @param username email of user to assign as space developer
     * @throws CreationException when assignation was not successful
     * @throws NullPointerException when username is null
     * @throws IllegalArgumentException when username is empty
     */
    public void assignSpaceDeveloper(String username) throws CreationException {
        Log.debug("Assign a space developer:", username);

        checkNotNull(username);
        checkArgument(!username.isEmpty(), "Username must not be empty.");

        String spaceId = cloudFoundryOperations.getSpaceId().block();
        String organization = cloudFoundryOperations.getOrganization();
        String space = cloudFoundryOperations.getSpace();
        ListSpaceUsersRequest spaceUsersRequest = ListSpaceUsersRequest.builder()
            .spaceName(space)
            .organizationName(organization)
            .build();
        SpaceUsers spaceUsers = cloudFoundryOperations
            .userAdmin()
            .listSpaceUsers(spaceUsersRequest)
            .block();
        if (!spaceUsers.getDevelopers().contains(username)) {
            AssociateSpaceDeveloperByUsernameRequest request = AssociateSpaceDeveloperByUsernameRequest.builder()
                .username(username)
                .spaceId(spaceId)
                .build();
            try {
                cloudFoundryOperations.getCloudFoundryClient()
                    .spaces()
                    .associateDeveloperByUsername(request)
                    .block();
            } catch (Exception e) {
                throw new CreationException(e);
            }
        }
    }

    /**
     * Removes users as space developer.
     *
     * @param usernameList List of users to be removed as a space developer.
     * @throws InvalidOperationException if usernameList is null/empty or spaceId is invalid.
     */
    public void removeSpaceDeveloper(List<String> usernameList) throws InvalidOperationException {
        Log.debug("Remove space developer(s):", String.valueOf(usernameList.toString()));

        assertValidUsernameList(usernameList);
        String spaceId = cloudFoundryOperations.getSpaceId().block();
        assertValidSpaceId(spaceId);

        usernameList.stream()
                .map(username -> doRemoveSpaceDeveloper(spaceId, username))
                .collect(toList())
                .forEach(Mono::block);
    }

    /**
     * Checks if <code>usernameList</code> is not null or empty.
     *
     * @param usernameList List of space developer users.
     * @throws InvalidOperationException if usernameList is invalid.
     */
    private void assertValidUsernameList(List<String> usernameList) throws InvalidOperationException {
        if (usernameList == null || usernameList.isEmpty()) {
            throw new InvalidOperationException(USERNAME_LIST_MUST_BE_NOT_NULL_OR_EMPTY);
        }
    }

    /**
     * Checks if <code>spaceId</code> is not null or empty.
     *
     * @param spaceId The value for spaceId.
     * @throws InvalidOperationException if spaceId is invalid.
     */
    private void assertValidSpaceId(String spaceId) throws InvalidOperationException {
        if (isBlank(spaceId)) {
            throw new InvalidOperationException(INVALID_VALUE_OF_SPACE_ID);
        }
    }

    /**
     * Remove a particular user as space developer.
     *
     * @param spaceId  The value for spaceId.
     * @param username The value for username.
     * @return the response from the Disassociate Developer with the Space by Username request.
     */
    private Mono<RemoveSpaceDeveloperByUsernameResponse> doRemoveSpaceDeveloper(String spaceId, String username) {
        RemoveSpaceDeveloperByUsernameRequest request = RemoveSpaceDeveloperByUsernameRequest
                .builder()
                .spaceId(spaceId)
                .username(username)
                .build();

        Log.info(REMOVE_USER_AS_SPACE_DEVELOPER, username);

        return cloudFoundryOperations.getCloudFoundryClient()
                .spaces()
                .removeDeveloperByUsername(request);
    }
}
