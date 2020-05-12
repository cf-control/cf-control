package cloud.foundry.cli.getservice.logic;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.useradmin.SpaceUsers;
import org.cloudfoundry.operations.useradmin.UserAdmin;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import reactor.core.publisher.Mono;

class SpaceDevelopersProviderTest {
    @Test
    public void testGetListSpaceDevelopers() {
        // given
        DefaultCloudFoundryOperations cfOperationsMock = Mockito
            .mock(DefaultCloudFoundryOperations.class);
        Mockito.when(cfOperationsMock.getSpace()).thenReturn("development");
        Mockito.when(cfOperationsMock.getOrganization()).thenReturn("cloud.foundry.cli");
       
        UserAdmin userAdminMock = Mockito.mock(UserAdmin.class);
        Mockito.when(cfOperationsMock.userAdmin()).thenReturn(userAdminMock);
        
        Mono<SpaceUsers> monoMock = (Mono<SpaceUsers>) Mockito.mock(Mono.class);
        Mockito.when(userAdminMock.listSpaceUsers(any())).thenReturn(monoMock);
        
        SpaceUsers spaceUsersMock = Mockito.mock(SpaceUsers.class);
        Mockito.when(monoMock.block()).thenReturn(spaceUsersMock);
        Mockito.when(spaceUsersMock.getDevelopers())
            .thenReturn(Arrays.asList("one", "two", "three"));
        // when
        SpaceDevelopersProvider spaceDeveloperProvider = new SpaceDevelopersProvider(
            cfOperationsMock);
        String spaceDevelopers = spaceDeveloperProvider.getSpaceDevelopers();
        // then
        assertThat(spaceDevelopers, is("spaceDevelopers: [one, two, three]\n"));
    }

    @Test
    public void testGetOneSpaceDeveloper() {
        // given
        DefaultCloudFoundryOperations cfOperationsMock = Mockito
            .mock(DefaultCloudFoundryOperations.class);
        Mockito.when(cfOperationsMock.getSpace()).thenReturn("development");
        Mockito.when(cfOperationsMock.getOrganization()).thenReturn("cloud.foundry.cli");
        
        UserAdmin userAdminMock = Mockito.mock(UserAdmin.class);
        Mockito.when(cfOperationsMock.userAdmin()).thenReturn(userAdminMock);
        
        Mono<SpaceUsers> monoMock = (Mono<SpaceUsers>) Mockito.mock(Mono.class);
        Mockito.when(userAdminMock.listSpaceUsers(any())).thenReturn(monoMock);
        
        SpaceUsers spaceUsersMock = Mockito.mock(SpaceUsers.class);
        Mockito.when(monoMock.block()).thenReturn(spaceUsersMock);
        Mockito.when(spaceUsersMock.getDevelopers())
            .thenReturn(Arrays.asList("one"));
        
        SpaceDevelopersProvider spaceDeveloperProvider = new SpaceDevelopersProvider(
            cfOperationsMock);
        String spaceDevelopers = spaceDeveloperProvider.getSpaceDevelopers();
        assertThat(spaceDevelopers, is("spaceDevelopers: [one]\n"));
    }

    @Test
    public void testGetNullSpaceDeveloper() {
        DefaultCloudFoundryOperations cfOperationsMock = Mockito
            .mock(DefaultCloudFoundryOperations.class);
        Mockito.when(cfOperationsMock.getSpace()).thenReturn("development");
        Mockito.when(cfOperationsMock.getOrganization()).thenReturn("cloud.foundry.cli");
        
        UserAdmin userAdminMock = Mockito.mock(UserAdmin.class);
        Mockito.when(cfOperationsMock.userAdmin()).thenReturn(userAdminMock);
        
        Mono<SpaceUsers> monoMock = (Mono<SpaceUsers>) Mockito.mock(Mono.class);
        Mockito.when(userAdminMock.listSpaceUsers(any())).thenReturn(monoMock);
        
        SpaceUsers spaceUsersMock = Mockito.mock(SpaceUsers.class);
        Mockito.when(monoMock.block()).thenReturn(spaceUsersMock);
        Mockito.when(spaceUsersMock.getDevelopers())
            .thenReturn(Arrays.asList());
        
        SpaceDevelopersProvider spaceDeveloperProvider = new SpaceDevelopersProvider(
            cfOperationsMock);
        
        String spaceDevelopers = spaceDeveloperProvider.getSpaceDevelopers();
        
        assertThat(spaceDevelopers, is("spaceDevelopers: []\n"));
    }
}
