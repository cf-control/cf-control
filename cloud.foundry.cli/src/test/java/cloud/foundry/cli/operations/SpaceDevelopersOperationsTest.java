package cloud.foundry.cli.operations;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static java.util.Collections.emptyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.mock;

import java.util.Arrays;

import cloud.foundry.cli.crosscutting.exceptions.CreationException;
import cloud.foundry.cli.crosscutting.util.YamlProcessorCreator;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.spaces.AssociateSpaceDeveloperByUsernameRequest;
import org.cloudfoundry.client.v2.spaces.AssociateSpaceDeveloperByUsernameResponse;
import org.cloudfoundry.client.v2.spaces.Spaces;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.useradmin.SpaceUsers;
import org.cloudfoundry.operations.useradmin.UserAdmin;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import reactor.core.publisher.Mono;

class SpaceDevelopersOperationsTest {
    private static DefaultCloudFoundryOperations cfOperationsMock;
    private static SpaceDevelopersOperations spaceDevelopersOperations;

    @BeforeAll
    public static void setupMock() {
        cfOperationsMock = mockDefaultCloudFoundryOperations();
        spaceDevelopersOperations = new SpaceDevelopersOperations(cfOperationsMock);
    }

    @Test
    public void testGetSpaceDevelopers() {
        // given
        SpaceUsers spaceUsersMock = mockSpaceUsers(cfOperationsMock);
        when(spaceUsersMock.getDevelopers()).thenReturn(Arrays.asList("one", "two", "three"));
        // when
        String spaceDevelopers = YamlProcessorCreator.createDefault().dump(spaceDevelopersOperations.getAll());
        // then
        assertThat(spaceDevelopers, is("spaceDevelopers:\n- one\n- two\n- three\n"));
    }

    @Test
    public void testGetSpaceDevelopers_WithEmptyList() {
        // given
        SpaceUsers spaceUsersMock = mockSpaceUsers(cfOperationsMock);
        when(spaceUsersMock.getDevelopers()).thenReturn(emptyList());
        // when
        String spaceDevelopers = YamlProcessorCreator.createDefault().dump(spaceDevelopersOperations.getAll());
        // then
        assertThat(spaceDevelopers, is("spaceDevelopers: [\n  ]\n"));
    }

    @Test
    public void testAssignSpaceDeveloper_WithExistingUser() throws CreationException {
        // when
        SpaceUsers spaceUsersMock = mockSpaceUsers(cfOperationsMock);
        when(spaceUsersMock.getDevelopers()).thenReturn(Arrays.asList("one", "two", "three"));
        Mono<AssociateSpaceDeveloperByUsernameResponse> monoMock = mock(Mono.class);
        // call
        spaceDevelopersOperations.assignSpaceDeveloper("two");
        // then
        verify(spaceUsersMock, times(1)).getDevelopers();
        verify(monoMock, times(0)).block();
    }

    @Test
    public void testAssignSpaceDeveloper() throws CreationException {
        SpaceUsers spaceUsersMock = mockSpaceUsers(cfOperationsMock);
        when(spaceUsersMock.getDevelopers()).thenReturn(Arrays.asList("one", "two", "three"));
        CloudFoundryClient cfClientMock = mock(CloudFoundryClient.class);
        when(cfOperationsMock.getCloudFoundryClient()).thenReturn(cfClientMock);
        Spaces spacesMock = mock(Spaces.class);
        when(cfClientMock.spaces()).thenReturn(spacesMock);
        Mono<AssociateSpaceDeveloperByUsernameResponse> monoMock = mock(Mono.class);
        when(spacesMock.associateDeveloperByUsername(any()))
            .thenReturn(monoMock);
        AssociateSpaceDeveloperByUsernameResponse associateSpaceDeveloperByUsernameReponsetMock = mock(
            AssociateSpaceDeveloperByUsernameResponse.class);
        when(monoMock.block()).thenReturn(associateSpaceDeveloperByUsernameReponsetMock);
        // call
        spaceDevelopersOperations.assignSpaceDeveloper("six");
        // then
        verify(spaceUsersMock, times(1)).getDevelopers();
        verify(cfClientMock, times(1)).spaces();
        verify(spacesMock, times(1)).associateDeveloperByUsername(any());
        verify(monoMock, times(1)).block();
    }

    @Test
    public void testAssignSpaceDeveloper_ThrowException() throws CreationException {
        SpaceUsers spaceUsersMock = mockSpaceUsers(cfOperationsMock);
        when(spaceUsersMock.getDevelopers()).thenReturn(Arrays.asList("one", "two", "three"));
        CloudFoundryClient cfClientMock = mock(CloudFoundryClient.class);
        when(cfOperationsMock.getCloudFoundryClient()).thenReturn(cfClientMock);
        Spaces spacesMock = mock(Spaces.class);
        when(cfClientMock.spaces()).thenReturn(spacesMock);
        AssociateSpaceDeveloperByUsernameRequest associateSpaceDeveloperByUsernameRequest = mock(
            AssociateSpaceDeveloperByUsernameRequest.class);
        Mono<AssociateSpaceDeveloperByUsernameResponse> monoMock = mock(Mono.class);
        when(spacesMock.associateDeveloperByUsername(associateSpaceDeveloperByUsernameRequest))
            .thenReturn(monoMock);
        AssociateSpaceDeveloperByUsernameResponse associateSpaceDeveloperByUsernameReponsetMock = mock(
            AssociateSpaceDeveloperByUsernameResponse.class);
        when(monoMock.block()).thenReturn(associateSpaceDeveloperByUsernameReponsetMock);
        // then
        assertThrows(CreationException.class, () -> {
            spaceDevelopersOperations.assignSpaceDeveloper("four");
        });
    }

    /**
     * Mock the DefaultCloudFoundryOperations
     *
     * @return DefaultCloudFoundryOperations
     */
    private static DefaultCloudFoundryOperations mockDefaultCloudFoundryOperations() {
        DefaultCloudFoundryOperations cfOperationsMock = mock(DefaultCloudFoundryOperations.class);
        when(cfOperationsMock.getSpace()).thenReturn("development");
        when(cfOperationsMock.getOrganization()).thenReturn("cloud.foundry.cli");
        when(cfOperationsMock.getSpaceId()).thenReturn(Mono.just("1"));
        return cfOperationsMock;
    }

    /**
     * Mock the SpaceUsers
     *
     * @param cfOperationsMock
     * @return SpaceUsers
     */
    private static SpaceUsers mockSpaceUsers(DefaultCloudFoundryOperations cfOperationsMock) {
        UserAdmin userAdminMock = mock(UserAdmin.class);
        when(cfOperationsMock.userAdmin()).thenReturn(userAdminMock);
        Mono<SpaceUsers> monoMock = mock(Mono.class);
        when(userAdminMock.listSpaceUsers(any())).thenReturn(monoMock);
        SpaceUsers spaceUsersMock = mock(SpaceUsers.class);
        when(monoMock.block()).thenReturn(spaceUsersMock);
        return spaceUsersMock;
    }
}
