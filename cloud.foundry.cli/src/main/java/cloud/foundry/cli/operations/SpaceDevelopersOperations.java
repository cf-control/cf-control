package cloud.foundry.cli.operations;

import cloud.foundry.cli.crosscutting.beans.SpaceDevelopersBean;
import cloud.foundry.cli.crosscutting.exceptions.CreationException;

import cloud.foundry.cli.crosscutting.logging.Log;
import org.cloudfoundry.client.v2.spaces.AssociateSpaceDeveloperByUsernameRequest;
import org.cloudfoundry.client.v2.spaces.AssociateSpaceDeveloperByUsernameResponse;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.useradmin.ListSpaceUsersRequest;
import org.cloudfoundry.operations.useradmin.SpaceUsers;

import java.util.Arrays;
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
                throw new CreationException("FAILED \n " + e.getMessage());
            }
        }
    }
}