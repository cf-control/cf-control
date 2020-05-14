package cloud.foundry.cli.operations;

import cloud.foundry.cli.crosscutting.beans.Bean;
import cloud.foundry.cli.crosscutting.beans.SpaceDevelopersBean;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.useradmin.ListSpaceUsersRequest;

import java.util.Arrays;
import java.util.List;


public class SpaceDevelopersOperations extends AbstractOperations<DefaultCloudFoundryOperations> {

    public SpaceDevelopersOperations(DefaultCloudFoundryOperations cfOperations) {
        super(cfOperations);
    }

    @Override
    public void create(Bean bean) {

    }

    @Override
    public void delete(Bean bean) {

    }

    @Override
    public void update(Bean bean) {

    }

    @Override
    public Bean get(Bean bean) {
        return null;
    }

    /**
     * List all space developers
     *
     * @return list of space developers in YAML format
     */
    @Override
    public List<SpaceDevelopersBean> getAll() {
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
        return Arrays.asList(spaceDevelopersBean);
    }

}
