package cloud.foundry.cli.operations;

import cloud.foundry.cli.crosscutting.beans.SpaceDevelopersBean;
import cloud.foundry.cli.crosscutting.exceptions.CreationException;

import cloud.foundry.cli.crosscutting.logging.Log;
import org.cloudfoundry.client.v2.spaces.AssociateSpaceDeveloperByUsernameRequest;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.useradmin.ListSpaceUsersRequest;
import org.cloudfoundry.operations.useradmin.SpaceUsers;
import reactor.core.publisher.Mono;


/**
 * Handles the operations for manipulating space developers on a cloud foundry
 * instance.
 */
public class SpaceDevelopersOperations extends AbstractOperations<DefaultCloudFoundryOperations> {
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
    public Mono<SpaceDevelopersBean> getAll() {
        ListSpaceUsersRequest request = ListSpaceUsersRequest.builder()
                .spaceName(cloudFoundryOperations.getSpace())
                .organizationName(cloudFoundryOperations.getOrganization())
                .build();

        return cloudFoundryOperations
                .userAdmin()
                .listSpaceUsers(request)
                .map(spaceUsers -> new SpaceDevelopersBean(spaceUsers.getDevelopers()));
    }

    /**
     * Assign a user as a space developer
     *
     * @throws CreationException when assignation was not successful
     * @param username email of user to assign as space developer
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
}