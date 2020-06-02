package cloud.foundry.cli.operations;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import cloud.foundry.cli.crosscutting.exceptions.CreationException;

import cloud.foundry.cli.crosscutting.logging.Log;
import org.cloudfoundry.client.v2.spaces.AssociateSpaceDeveloperByUsernameRequest;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.useradmin.ListSpaceUsersRequest;
import org.cloudfoundry.operations.useradmin.SpaceUsers;

import java.util.List;

/**
 * Handles the operations for manipulating space developers on a cloud foundry
 * instance.
 */
public class SpaceDevelopersOperations extends AbstractOperations<DefaultCloudFoundryOperations> {
    public SpaceDevelopersOperations(DefaultCloudFoundryOperations cfOperations) {
        super(cfOperations);
    }

    /**
     * List all space developers
     *
     * @return list of space developers
     */
    public List<String> getAll() {
        ListSpaceUsersRequest request = ListSpaceUsersRequest.builder()
            .spaceName(cloudFoundryOperations.getSpace())
            .organizationName(cloudFoundryOperations.getOrganization())
            .build();
        List<String> spaceDevelopers = cloudFoundryOperations
            .userAdmin()
            .listSpaceUsers(request)
            .block()
            .getDevelopers();

        return spaceDevelopers;
    }

    /**
     * Assign a user as a space developer
     *
     * @throws CreationException when assignation was not successful
     * @param username email of user to assign as space developer
     * @throws NullPointerException when username is null
     * @throws IllegalArgumentException when username is empty
     */
    public void assignSpaceDeveloper(String username) throws CreationException {
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
}