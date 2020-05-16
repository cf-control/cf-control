package cloud.foundry.cli.operations;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import java.util.Arrays;

import cloud.foundry.cli.crosscutting.util.YamlCreator;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.useradmin.SpaceUsers;
import org.cloudfoundry.operations.useradmin.UserAdmin;
import org.junit.jupiter.api.Test;

import reactor.core.publisher.Mono;

class SpaceDevelopersOperationsTest {
    @Test
    public void testGetSpaceDevelopers() {
        // given
        DefaultCloudFoundryOperations cfOperationsMock = mockDefaultCloudFoundryOperations();
        SpaceUsers spaceUsersMock = mockSpaceUsers(cfOperationsMock);
        when(spaceUsersMock.getDevelopers()).thenReturn(Arrays.asList("one", "two", "three"));
        // when
        SpaceDevelopersOperations spaceDevelopersOperations = new SpaceDevelopersOperations(cfOperationsMock);
        String spaceDevelopers = YamlCreator.createDefaultYamlProcessor().dump(spaceDevelopersOperations.getAll());
        // then
        assertThat(spaceDevelopers, is("- spaceDevelopers:\n  - one\n  - two\n  - three\n"));
    }

    @Test
    public void testGetSpaceDevelopers_returnOneDeveloper() {
        // given
        DefaultCloudFoundryOperations cfOperationsMock = mockDefaultCloudFoundryOperations();
        SpaceUsers spaceUsersMock = mockSpaceUsers(cfOperationsMock);
        when(spaceUsersMock.getDevelopers()).thenReturn(singletonList("one"));
        //when
        SpaceDevelopersOperations spaceDevelopersOperations = new SpaceDevelopersOperations(cfOperationsMock);
        String spaceDevelopers = YamlCreator.createDefaultYamlProcessor().dump(spaceDevelopersOperations.getAll());
        //then
        assertThat(spaceDevelopers, is("- spaceDevelopers:\n  - one\n"));
    }

    @Test
    public void testGetSpaceDevelopers_returnNull() {
        //given
        DefaultCloudFoundryOperations cfOperationsMock = mockDefaultCloudFoundryOperations();
        SpaceUsers spaceUsersMock = mockSpaceUsers(cfOperationsMock);
        when(spaceUsersMock.getDevelopers()).thenReturn(emptyList());
        //when
        SpaceDevelopersOperations spaceDevelopersOperations = new SpaceDevelopersOperations(cfOperationsMock);
        String spaceDevelopers = YamlCreator.createDefaultYamlProcessor().dump(spaceDevelopersOperations.getAll());
        //then
        assertThat(spaceDevelopers, is("- spaceDevelopers: [\n    ]\n"));
    }

    private  DefaultCloudFoundryOperations mockDefaultCloudFoundryOperations() {
        DefaultCloudFoundryOperations cfOperationsMock = mock(DefaultCloudFoundryOperations.class);
        when(cfOperationsMock.getSpace()).thenReturn("development");
        when(cfOperationsMock.getOrganization()).thenReturn("cloud.foundry.cli");
        return cfOperationsMock;
    }

    private SpaceUsers mockSpaceUsers(DefaultCloudFoundryOperations cfOperationsMock) {
        UserAdmin userAdminMock = mock(UserAdmin.class);
        when(cfOperationsMock.userAdmin()).thenReturn(userAdminMock);
        Mono<SpaceUsers> monoMock = mock(Mono.class);
        when(userAdminMock.listSpaceUsers(any())).thenReturn(monoMock);
        SpaceUsers spaceUsersMock = mock(SpaceUsers.class);
        when(monoMock.block()).thenReturn(spaceUsersMock);
        return spaceUsersMock;
    }
}
