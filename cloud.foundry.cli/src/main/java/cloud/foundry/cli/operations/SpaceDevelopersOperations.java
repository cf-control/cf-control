package cloud.foundry.cli.operations;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static java.util.stream.Collectors.toList;

import cloud.foundry.cli.crosscutting.beans.SpaceDevelopersBean;
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

    public SpaceDevelopersOperations(DefaultCloudFoundryOperations cfOperations) {
        super(cfOperations);
    }

    /**
     * List all space developers
     *
     * @return list of space developers
     */
    public SpaceDevelopersBean getAll() {
        ListSpaceUsersRequest request = ListSpaceUsersRequest.builder()
                .spaceName(cloudFoundryOperations.getSpace())
                .organizationName(cloudFoundryOperations.getOrganization())
                .build();
        List<String> spaceDevelopers = cloudFoundryOperations
                .userAdmin()
                .listSpaceUsers(request)
                .block()
                .getDevelopers();
        SpaceDevelopersBean spaceDevelopersBean = new SpaceDevelopersBean();
        spaceDevelopersBean.setSpaceDevelopers(spaceDevelopers);
        return spaceDevelopersBean;
    }

    /**
     * Assign a user as a space developer
     *
     * @param username email of user to assign as space developer
     * @throws CreationException when assignation was not successful
     */
    public void assignSpaceDeveloper(String username) throws CreationException {
        assert (!username.isEmpty() && username != null);
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
            Log.info("Assigning role SpaceDeveloper to user", username, "in org", organization, "/ space", space);
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
     */
    public void removeSpaceDeveloper(List<String> usernameList) throws InvalidOperationException {
        String spaceId = cloudFoundryOperations.getSpaceId().block();
        if (isBlank(spaceId)) {
            throw new InvalidOperationException(INVALID_VALUE_OF_SPACE_ID);
        }

        usernameList.stream()
                .map(username -> doRemoveSpaceDeveloper(spaceId, username))
                .collect(toList())
                .forEach(Mono::block);
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