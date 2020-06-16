package cloud.foundry.cli.operations;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import cloud.foundry.cli.crosscutting.exceptions.CreationException;
import cloud.foundry.cli.crosscutting.exceptions.InvalidOperationException;

import cloud.foundry.cli.crosscutting.exceptions.UpdateException;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.spaces.AssociateSpaceDeveloperByUsernameRequest;
import org.cloudfoundry.client.v2.spaces.AssociateSpaceDeveloperByUsernameResponse;
import org.cloudfoundry.client.v2.spaces.RemoveSpaceDeveloperByUsernameResponse;
import org.cloudfoundry.client.v2.spaces.Spaces;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.useradmin.ListSpaceUsersRequest;
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
        List<String> withDevelopers = Arrays.asList("one", "two", "three");
        mockGetAllMethod(withDevelopers);

        // when
        List<String> spaceDevelopers = spaceDevelopersOperations.getAll().block();

        // then
        assertThat(spaceDevelopers.size(), is(3));
        assertThat(spaceDevelopers, contains("one", "two", "three"));
    }

    @Test
    public void testGetSpaceDevelopers_WithEmptyList() {
        // given
        List<String> withoutDevelopers = Collections.emptyList();
        mockGetAllMethod(withoutDevelopers);

        // when
        List<String> spaceDevelopers = spaceDevelopersOperations.getAll().block();

        // then
        assertThat(spaceDevelopers.size(), is(0));
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
    public void testAssignSpaceDeveloper_ThrowException() {
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

    @Test
    public void testRemoveSpaceDeveloper() throws InvalidOperationException, UpdateException {
        // given
        CloudFoundryClient cfClientMock = mock(CloudFoundryClient.class);
        when(cfOperationsMock.getCloudFoundryClient()).thenReturn(cfClientMock);

        Spaces spacesMock = mock(Spaces.class);
        when(cfClientMock.spaces()).thenReturn(spacesMock);

        Mono<RemoveSpaceDeveloperByUsernameResponse> monoMock = mock(Mono.class);
        when(spacesMock.removeDeveloperByUsername(any())).thenReturn(monoMock);
        when(cfOperationsMock.getCloudFoundryClient().spaces().removeDeveloperByUsername(any())).thenReturn(monoMock);

        RemoveSpaceDeveloperByUsernameResponse removeSpaceDeveloperByUsernameResponseMock =
                mock(RemoveSpaceDeveloperByUsernameResponse.class);
        when(monoMock.block()).thenReturn(removeSpaceDeveloperByUsernameResponseMock);

        // when
        spaceDevelopersOperations.removeSpaceDeveloper(Arrays.asList("one", "two"));

        // then
        verify(spacesMock, times(2)).removeDeveloperByUsername(any());
        verify(monoMock, times(2)).block();
    }

    @Test
    public void testRemoveSpaceDeveloperShouldThrowAnInvalidOperationExceptionWhenSpaceIdIsBlank() {
        // given
        when(cfOperationsMock.getSpaceId()).thenReturn(Mono.just(""));

        // then
        assertThrows(InvalidOperationException.class, () -> {
            // when
            spaceDevelopersOperations.removeSpaceDeveloper(Arrays.asList("one", "two"));
        });
    }

    @Test
    public void testRemoveSpaceDeveloperShouldThrowAnInvalidOperationExceptionWhenArgumentIsEmpty() {
        // then
        assertThrows(InvalidOperationException.class, () -> {
            // given - when
            spaceDevelopersOperations.removeSpaceDeveloper(Collections.emptyList());
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
    private SpaceUsers mockSpaceUsers(DefaultCloudFoundryOperations cfOperationsMock) {
        UserAdmin userAdminMock = mock(UserAdmin.class);
        Mono<SpaceUsers> monoMock = mock(Mono.class);
        SpaceUsers spaceUsersMock = mock(SpaceUsers.class);

        when(cfOperationsMock.userAdmin()).thenReturn(userAdminMock);
        when(userAdminMock.listSpaceUsers(any())).thenReturn(monoMock);
        when(monoMock.block()).thenReturn(spaceUsersMock);

        return spaceUsersMock;
    }

    private UserAdmin mockGetAllMethod(List<String> developers) {
        UserAdmin userAdminMock = mock(UserAdmin.class);

        when(cfOperationsMock.userAdmin()).thenReturn(userAdminMock);
        when(userAdminMock.listSpaceUsers(any(ListSpaceUsersRequest.class)))
                .thenReturn(Mono.just(SpaceUsers
                        .builder()
                        .addAllDevelopers(developers)
                        .build()));

        return userAdminMock;
    }
}
