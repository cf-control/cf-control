package cloud.foundry.cli.operations;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import cloud.foundry.cli.operations.space.DefaultSpaceOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.spaces.CreateSpaceRequest;
import org.cloudfoundry.operations.spaces.SpaceSummary;
import org.cloudfoundry.operations.spaces.Spaces;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;


public class DefaultSpaceOperationsTest {

    @Test
    public void testGetAllSucceeds() {
        //given
        final String firstSpaceName = "someSpaceName";
        final String secondSpaceName = "someOtherSpaceName";

        List<SpaceSummary> spaceSummaries = Arrays.asList(
                SpaceSummary.builder()
                        .name(firstSpaceName)
                        .id("someId").build(),
                SpaceSummary.builder()
                        .name(secondSpaceName)
                        .id("someOtherId").build()
        );
        Flux<SpaceSummary> spaceSummariesFlux = Flux.fromIterable(spaceSummaries);

        DefaultCloudFoundryOperations cloudFoundryOperationsMock = mock(DefaultCloudFoundryOperations.class);
        Spaces spacesMock = mock(Spaces.class);

        when(cloudFoundryOperationsMock.spaces()).thenReturn(spacesMock);
        when(spacesMock.list()).thenReturn(spaceSummariesFlux);

        DefaultSpaceOperations spaceOperations = new DefaultSpaceOperations(cloudFoundryOperationsMock);

        //when
        Mono<List<String>> result = spaceOperations.getAll();

        //then
        List<String> resultingSpaceNames = result.block();
        assertThat(resultingSpaceNames, containsInAnyOrder(firstSpaceName, secondSpaceName));
    }

    @Test
    public void testCreateWithSpaceNameNullThrowsNullptr() {
        // given
        DefaultCloudFoundryOperations cloudFoundryOperationsMock = mock(DefaultCloudFoundryOperations.class);
        DefaultSpaceOperations spaceOperations = new DefaultSpaceOperations(cloudFoundryOperationsMock);

        // when + then
        assertThrows(NullPointerException.class, () ->
                spaceOperations.create(null));
    }

    @Test
    public void testCreateSucceeds() {
        //given
        DefaultCloudFoundryOperations cloudFoundryOperationsMock = mock(DefaultCloudFoundryOperations.class);

        Spaces spacesMock = mock(Spaces.class);
        Mono expectedResultMono = Mono.just(mock(Void.class));
        when(cloudFoundryOperationsMock.spaces()).thenReturn(spacesMock);
        when(spacesMock.create(any(CreateSpaceRequest.class))).thenReturn(expectedResultMono);

        DefaultSpaceOperations spaceOperations = new DefaultSpaceOperations(cloudFoundryOperationsMock);

        //when
        Mono<Void> resultMono = spaceOperations.create("testName");
        resultMono.block();

        //then
        assertThat(resultMono, notNullValue());
        verify(spacesMock, times(1)).create(any(CreateSpaceRequest.class));
    }
}
