package cloud.foundry.cli.operations;

import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.spaces.SpaceSummary;
import org.cloudfoundry.operations.spaces.Spaces;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SpaceOperationsTest {

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

        SpaceOperations spaceOperations = new SpaceOperations(cloudFoundryOperationsMock);

        //when
        Mono<List<String>> result = spaceOperations.getAll();

        //then
        List<String> resultingSpaceNames = result.block();
        assertThat(resultingSpaceNames, containsInAnyOrder(firstSpaceName, secondSpaceName));
    }
}
