package cloud.foundry.cli.getservice.logic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.useradmin.ListSpaceUsersRequest;
import org.cloudfoundry.operations.useradmin.SpaceUsers;
import org.yaml.snakeyaml.Yaml;

import reactor.core.publisher.Mono;

public class SpaceDevelopersProvider {
    private DefaultCloudFoundryOperations cfOperations;

    public SpaceDevelopersProvider(DefaultCloudFoundryOperations cfOperations) {
        this.cfOperations = cfOperations;
    }

    /**
     * List all space developers
     * 
     * @return list of space developers in YAML format
     */
    public String getSpaceDevelopers() {
        ListSpaceUsersRequest request = ListSpaceUsersRequest.builder()
            .spaceName(cfOperations.getSpace()).organizationName(cfOperations.getOrganization())
            .build();
        Mono<SpaceUsers> spaceUsers = cfOperations.userAdmin().listSpaceUsers(request);
        SpaceUsers users = spaceUsers.block();
        List<String> listDevelopers = users.getDevelopers();
        Map<String, List<String>> data = new HashMap<String, List<String>>();
        data.put("spaceDevelopers", listDevelopers);
        return new Yaml().dump(data);
    }
}
