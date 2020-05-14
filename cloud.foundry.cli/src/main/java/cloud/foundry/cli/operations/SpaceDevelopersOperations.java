package cloud.foundry.cli.operations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cloud.foundry.cli.crosscutting.beans.Bean;
import cloud.foundry.cli.crosscutting.beans.SpaceDevelopersBean;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.useradmin.ListSpaceUsersRequest;
import org.cloudfoundry.operations.useradmin.SpaceUsers;

import reactor.core.publisher.Mono;

public class SpaceDevelopersOperations extends AbstractOperations<DefaultCloudFoundryOperations>{

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

    /**
     * List all space developers
     *
     * @return list of space developers in YAML format
     */
    public Object get() {
        ListSpaceUsersRequest request = ListSpaceUsersRequest.builder()
            .spaceName(cloudFoundryOperations.getSpace()).organizationName(cloudFoundryOperations.getOrganization())
            .build();
        Mono<SpaceUsers> spaceUsers = cloudFoundryOperations.userAdmin().listSpaceUsers(request);
        SpaceUsers users = spaceUsers.block();
        List<String> listDevelopers = users.getDevelopers();
        Map<String, List<String>> data = new HashMap<String, List<String>>();
        data.put("spaceDevelopers", listDevelopers);
        return null;
        //return new Yaml().dump(data);
    }
}
