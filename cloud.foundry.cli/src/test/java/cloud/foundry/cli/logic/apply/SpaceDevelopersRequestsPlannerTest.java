package cloud.foundry.cli.logic.apply;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

import java.util.LinkedList;
import java.util.List;

import cloud.foundry.cli.crosscutting.mapping.beans.SpaceDevelopersBean;
import cloud.foundry.cli.logic.diff.change.ChangeType;
import cloud.foundry.cli.logic.diff.change.container.CfContainerChange;
import cloud.foundry.cli.logic.diff.change.container.CfContainerValueChanged;
import cloud.foundry.cli.operations.SpaceDevelopersOperations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.junit.jupiter.api.Test;

/**
 * Test for {@link SpaceDevelopersRequestsPlanner}
 */
public class SpaceDevelopersRequestsPlannerTest {

    @Test
    public void createSpaceDevelopersRequestsWithCfContainerChangeAcceptMethodCalled() {
        // given
        String assignUserName = "toAdd";
        String removeUserName = "toRemove";
        String spaceId = "spaceID";

        SpaceDevelopersOperations mockSpaceDevelopersOperations = mock(SpaceDevelopersOperations.class);
        Mono<String> spaceIdMonoMock = mock(Mono.class);
        when(mockSpaceDevelopersOperations.getSpaceId()).thenReturn(spaceIdMonoMock);
        when(spaceIdMonoMock.block()).thenReturn(spaceId);

        SpaceDevelopersBean spaceDevelopersBeanMock = mock(SpaceDevelopersBean.class);
        CfContainerValueChanged containerValueAdded = new CfContainerValueChanged(assignUserName, ChangeType.ADDED);
        CfContainerValueChanged containerValueRemoved = new CfContainerValueChanged(removeUserName,
                ChangeType.REMOVED);
        List<CfContainerValueChanged> changedValues = new LinkedList<>();
        changedValues.add(containerValueAdded);
        changedValues.add(containerValueRemoved);

        List<String> path = new LinkedList<>();
        path.add("path");
        CfContainerChange mockSpaceDevelopersChange = new CfContainerChange(spaceDevelopersBeanMock, "",
                path, changedValues);

        // assign
        Void voidAssignMock = mock(Void.class);
        Mono<Void> assignMonoMock = Mono.just(voidAssignMock);
        when(mockSpaceDevelopersOperations.assign(assignUserName, spaceId)).thenReturn(assignMonoMock);

        // remove
        Void voidRemoveMock = mock(Void.class);
        Mono<Void> removeMonoMock = Mono.just(voidRemoveMock);
        when(mockSpaceDevelopersOperations.remove(removeUserName, spaceId)).thenReturn(removeMonoMock);

        // when
        Flux<Void> requests = SpaceDevelopersRequestsPlanner
                .createSpaceDevelopersRequests(mockSpaceDevelopersOperations, mockSpaceDevelopersChange);

        // then
        verify(mockSpaceDevelopersOperations, times(1)).assign(assignUserName, spaceId);
        verify(mockSpaceDevelopersOperations, times(1)).remove(removeUserName, spaceId);
        StepVerifier.create(requests)
                .expectNext(voidAssignMock)
                .expectNext(voidRemoveMock)
                .expectComplete()
                .verify();

    }

    @Test
    public void createSpaceDevelopersRequestsWithNullValueForSpaceDevelopersChangeShouldThrowANullPointerException() {
        // given
        SpaceDevelopersOperations mockSpaceDevelopersOperations = mock(SpaceDevelopersOperations.class);
        CfContainerChange spaceDevelopersChange = null;

        // then - when
        assertThrows(NullPointerException.class,
                () -> SpaceDevelopersRequestsPlanner
                        .createSpaceDevelopersRequests(mockSpaceDevelopersOperations, spaceDevelopersChange));
    }

    @Test
    public void createSpaceDevelopersRequestsWithNullValueForSpaceDevelopersOperationsShouldThrowANullPointerException
            () {

        // given
        SpaceDevelopersOperations mockSpaceDevelopersOperations = null;
        CfContainerChange spaceDevelopersChange = mock(CfContainerChange.class);;

        // then - when
        assertThrows(NullPointerException.class,
                () -> SpaceDevelopersRequestsPlanner
                        .createSpaceDevelopersRequests(mockSpaceDevelopersOperations, spaceDevelopersChange));
    }

}
