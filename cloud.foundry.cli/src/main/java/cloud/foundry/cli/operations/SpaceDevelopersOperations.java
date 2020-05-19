package cloud.foundry.cli.operations;

import cloud.foundry.cli.crosscutting.beans.SpaceDevelopersBean;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.useradmin.ListSpaceUsersRequest;

import java.util.Arrays;
import java.util.List;

/**
 * Handles the operations for manipulating space developers on a cloud foundry instance.
 */
public class SpaceDevelopersOperations extends AbstractOperations<DefaultCloudFoundryOperations> {

    public SpaceDevelopersOperations(DefaultCloudFoundryOperations cfOperations) {
        super(cfOperations);
    }

    /**
     * List all space developers
     *
     * @return list of space developers in YAML format
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

}
