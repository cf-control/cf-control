package cloud.foundry.cli.operations;

import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.spaces.SpaceSummary;
import org.cloudfoundry.operations.spaces.Spaces;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SpaceOperationsTest {

    @Test
    public void testGetAllSucceeds(){
        //given
        DefaultCloudFoundryOperations cloudFoundryOperationsMock = mock(DefaultCloudFoundryOperations.class);
        Spaces spacesMock = mock(Spaces.class);
        Flux<SpaceSummary> spaceSummaryFluxMock = mock(Flux.class);
        Flux<String> spaceNameFluxMock = mock(Flux.class);
        Mono<List<String>> spaceNamesMono = mock(Mono.class);

        when(cloudFoundryOperationsMock.spaces()).thenReturn(spacesMock);
        when(spacesMock.list()).thenReturn(spaceSummaryFluxMock);
        when(spaceSummaryFluxMock.map(spaceSummary -> spaceSummary.getName())).thenReturn(spaceNameFluxMock);
        when(spaceNameFluxMock.collectList()).thenReturn(spaceNamesMono);

        //when
        SpaceOperations spaceOperations = new SpaceOperations(cloudFoundryOperationsMock);
        Mono<List<String>> result = spaceOperations.getAll();

        //then
        assertThat(result, is(spaceNamesMono));
    }
}
