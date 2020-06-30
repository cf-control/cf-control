package cloud.foundry.cli.operations;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.spaces.AssociateSpaceDeveloperByUsernameRequest;
import org.cloudfoundry.client.v2.spaces.RemoveSpaceDeveloperByUsernameRequest;
import org.cloudfoundry.client.v2.spaces.Spaces;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.useradmin.ListSpaceUsersRequest;
import org.cloudfoundry.operations.useradmin.SpaceUsers;
import org.cloudfoundry.operations.useradmin.UserAdmin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import reactor.core.publisher.Mono;

class SpaceDevelopersOperationsTest {

    /**
     * This is the mock of cf operations for every test case.
     * It is updated by the method calls to the SpaceDeveloperMocks class.
     */
    private static DefaultCloudFoundryOperations cfOperationsMock;
    private static SpaceDevelopersOperations spaceDevelopersOperations;
    private static SpaceDeveloperMocks mocks;

    @BeforeEach
    public void reinitializeFields() {
        cfOperationsMock = mock(DefaultCloudFoundryOperations.class);
        spaceDevelopersOperations = new SpaceDevelopersOperations(cfOperationsMock);
        mocks = new SpaceDeveloperMocks();
    }

    @Test
    public void testGetSpaceDevelopers() {
        // given
        String space = "someSpace";
        String org = "someOrg";
        List<String> expectedSpaceDevelopers = Arrays.asList("one", "two", "three");
        AtomicReference<ListSpaceUsersRequest> requestReference = mocks
                .mockForGetAllSpaceDevelopers(space, org, expectedSpaceDevelopers);

        // when
        Mono<List<String>> resultingMono = spaceDevelopersOperations.getAll();

        // then
        mocks.checkCalledMethodsForSpaceDeveloperListing();
        assertThat(resultingMono.block(), is(expectedSpaceDevelopers));

        ListSpaceUsersRequest request = requestReference.get();
        assertThat(request, notNullValue());
        assertThat(request.getSpaceName(), is(space));
        assertThat(request.getOrganizationName(), is(org));
    }

    @Test
    public void testGetSpaceDevelopers_WithEmptyList() {
        // given
        String space = "someSpace";
        String org = "someOrg";
        List<String> expectedSpaceDevelopers = Collections.emptyList();
        AtomicReference<ListSpaceUsersRequest> requestReference = mocks
                .mockForGetAllSpaceDevelopers(space, org, expectedSpaceDevelopers);

        // when
        Mono<List<String>> result = spaceDevelopersOperations.getAll();

        // then
        mocks.checkCalledMethodsForSpaceDeveloperListing();
        assertThat(result.block(), is(expectedSpaceDevelopers));

        ListSpaceUsersRequest request = requestReference.get();
        assertThat(request, notNullValue());
        assertThat(request.getSpaceName(), is(space));
        assertThat(request.getOrganizationName(), is(org));
    }

    @Test
    public void testAssignSpaceDeveloper() {
        // given
        String spaceDeveloperToAssign = "someDev";
        String spaceId = "someSpaceId";
        AtomicReference<AssociateSpaceDeveloperByUsernameRequest> requestReference = mocks
                .mockForSpaceDeveloperAssignment();

        // when
        Mono<Void> result = spaceDevelopersOperations
                .assign(spaceDeveloperToAssign, spaceId);

        // then
        mocks.checkCalledMethodsForSpaceDeveloperAssignment();
        assertThat(result, notNullValue());

        AssociateSpaceDeveloperByUsernameRequest request = requestReference.get();
        assertThat(request, notNullValue());
        assertThat(request.getUsername(), is(spaceDeveloperToAssign));
        assertThat(request.getSpaceId(), is(spaceId));
    }

    @Test
    public void testAssignSpaceDeveloper_WithNull() {
        assertThrows(NullPointerException.class, () ->
                spaceDevelopersOperations.assign(null, "someSpaceId"));

        assertThrows(NullPointerException.class, () ->
                spaceDevelopersOperations.assign("someDev", null));
    }

    @Test
    public void testRemoveSpaceDeveloper() {
        // given
        String spaceDeveloperToRemove = "someDev";
        String spaceId = "someSpaceId";
        AtomicReference<RemoveSpaceDeveloperByUsernameRequest> requestReference = mocks
                .mockForSpaceDeveloperRemoval();

        // when
        Mono<Void> result = spaceDevelopersOperations
                .remove(spaceDeveloperToRemove, spaceId);

        // then
        mocks.checkCalledMethodsForSpaceDeveloperRemoval();
        assertThat(result, notNullValue());

        RemoveSpaceDeveloperByUsernameRequest request = requestReference.get();
        assertThat(request, notNullValue());
        assertThat(request.getUsername(), is(spaceDeveloperToRemove));
        assertThat(request.getSpaceId(), is(spaceId));
    }

    @Test
    public void testRemoveSpaceDeveloper_WithNull() {
        assertThrows(NullPointerException.class, () ->
                spaceDevelopersOperations.remove(null, "someSpaceId"));

        assertThrows(NullPointerException.class, () ->
                spaceDevelopersOperations.remove("someDev", null));
    }

    @Test
    public void testGetSpaceId() {
        // given
        String spaceId = "someSpaceId";
        when(cfOperationsMock.getSpaceId()).thenReturn(Mono.just(spaceId));

        // when
        Mono<String> result = spaceDevelopersOperations.getSpaceId();

        // then
        verify(cfOperationsMock).getSpaceId();
        assertThat(result, notNullValue());
        assertThat(result.block(), is(spaceId));
    }

    /**
     * Contains all relevant objects that were instantiated during mocking for space developer operations.
     */
    private static class SpaceDeveloperMocks {

        private UserAdmin userAdminMock;
        private CloudFoundryClient cfClientMock;
        private Spaces spacesMock;

        /**
         * Prepares necessary mocks for getting space developers. As soon as the listing request is passed to the
         * cf operations, the returned reference will point to the request, otherwise the reference points to null.
         * @return a reference to the future passed listing request
         */
        public AtomicReference<ListSpaceUsersRequest> mockForGetAllSpaceDevelopers(String space, String organization,
                List<String> spaceDevelopersInCf) {

            when(cfOperationsMock.getSpace()).thenReturn(space);
            when(cfOperationsMock.getOrganization()).thenReturn(organization);

            userAdminMock = mock(UserAdmin.class);
            when(cfOperationsMock.userAdmin()).thenReturn(userAdminMock);

            AtomicReference<ListSpaceUsersRequest> listingRequest = new AtomicReference<>(null);
            when(userAdminMock.listSpaceUsers(any(ListSpaceUsersRequest.class)))
                    .then(invocation -> {
                        listingRequest.set(invocation.getArgument(0));
                        return Mono.just(spaceDevelopersInCf)
                               .map(list -> SpaceUsers.builder().addAllDevelopers(list).build());
                    });

            return listingRequest;
        }

        /**
         * Prepares necessary mocks for assigning space developers. As soon as the assignment request is passed to the
         * cf operations, the returned reference will point to the request, otherwise the reference points to null.
         * @return a reference to the future passed assignment request
         */
        public AtomicReference<AssociateSpaceDeveloperByUsernameRequest> mockForSpaceDeveloperAssignment() {
            mockForSpaces();

            AtomicReference<AssociateSpaceDeveloperByUsernameRequest> assignmentRequest = new AtomicReference<>(null);
            when(spacesMock.associateDeveloperByUsername(any(AssociateSpaceDeveloperByUsernameRequest.class)))
                    .then(invocation -> {
                        assignmentRequest.set(invocation.getArgument(0));
                        return Mono.empty();
                    });

            return assignmentRequest;
        }

        /**
         * Prepares necessary mocks for removing space developers. As soon as the removal request is passed to the cf
         * operations, the returned reference will point to the request, otherwise the reference points to null.
         * @return a reference to the future passed removal request
         */
        private AtomicReference<RemoveSpaceDeveloperByUsernameRequest> mockForSpaceDeveloperRemoval() {
            mockForSpaces();

            AtomicReference<RemoveSpaceDeveloperByUsernameRequest> removalRequest = new AtomicReference<>(null);
            when(spacesMock.removeDeveloperByUsername(any(RemoveSpaceDeveloperByUsernameRequest.class)))
                    .then(invocation -> {
                        removalRequest.set(invocation.getArgument(0));
                        return Mono.empty();
                    });

            return removalRequest;
        }

        private void mockForSpaces() {
            cfClientMock = mock(CloudFoundryClient.class);
            when(cfOperationsMock.getCloudFoundryClient()).thenReturn(cfClientMock);

            spacesMock = mock(Spaces.class);
            when(cfClientMock.spaces()).thenReturn(spacesMock);
        }

        /**
         * Checks if the methods for space developer listing have been called as expected.
         */
        public void checkCalledMethodsForSpaceDeveloperListing() {
            verify(cfOperationsMock).userAdmin();
            verify(userAdminMock).listSpaceUsers(any(ListSpaceUsersRequest.class));
        }

        /**
         * Checks if the methods for space developer assignment have been called as expected.
         */
        public void checkCalledMethodsForSpaceDeveloperAssignment() {
            verify(cfOperationsMock).getCloudFoundryClient();
            verify(cfClientMock).spaces();
            verify(spacesMock).associateDeveloperByUsername(any(AssociateSpaceDeveloperByUsernameRequest.class));
        }

        /**
         * Checks if the methods for space developer removal have been called as expected.
         */
        public void checkCalledMethodsForSpaceDeveloperRemoval() {
            verify(cfOperationsMock).getCloudFoundryClient();
            verify(cfClientMock).spaces();
            verify(spacesMock).removeDeveloperByUsername(any(RemoveSpaceDeveloperByUsernameRequest.class));
        }
    }
}
