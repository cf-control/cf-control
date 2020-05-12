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
        
        DefaultCloudFoundryOperations cfOperationsMock = Mockito.mock(DefaultCloudFoundryOperations.class);
        Mockito.when(cfOperationsMock.getSpace()).thenReturn("development");
        Mockito.when(cfOperationsMock.getOrganization()).thenReturn("cloud.foundry.cli");
        
        UserAdmin userAdminMock = Mockito.mock(UserAdmin.class);
        Mockito.when(cfOperationsMock.userAdmin()).thenReturn(userAdminMock);
        
        Mono<SpaceUsers> monoMock = (Mono<SpaceUsers>) Mockito.mock(Mono.class);
        Mockito.when(userAdminMock.listSpaceUsers(any())).thenReturn(monoMock);
        
        SpaceUsers spaceUsersMock = Mockito.mock(SpaceUsers.class);
        Mockito.when(monoMock.block()).thenReturn(spaceUsersMock);
        Mockito.when(spaceUsersMock.getDevelopers())
            .thenReturn(Arrays.asList("one", "two", "three", "four"));
        
        SpaceDevelopersProvider spaceDeveloperProvider = new SpaceDevelopersProvider(
            cfOperationsMock);
        
        String spaceDevelopers = spaceDeveloperProvider.getSpaceDevelopers();
        
        assertThat(spaceDevelopers, is(Arrays.asList("one", "two", "three", "four")));
    }

    @Test
    public void testGetOneSpaceDeveloper() {
        // given
        // when
        // then
    }

    @Test
    public void testGetNullSpaceDeveloper() {
        // given
        // when
        // then
    }
}
