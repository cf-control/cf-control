package cloud.foundry.cli.logic.apply;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import cloud.foundry.cli.logic.diff.change.container.CfContainerChange;
import cloud.foundry.cli.operations.SpaceDevelopersOperations;
import org.junit.jupiter.api.Test;


/**
 * Test for {@link SpaceDevelopersRequestsPlaner}
 */
public class SpaceDevelopersRequestsPlanerTest {

    @Test
    public void createSpaceDevelopersRequestsWithCfContainerChangeAcceptMethodCalled() {
        // given
        SpaceDevelopersOperations mockSpaceDevelopersOperations = mock(SpaceDevelopersOperations.class);
        CfContainerChange mockSpaceDevelopersChange = mock(CfContainerChange.class);

        // when
        SpaceDevelopersRequestsPlaner.createSpaceDevelopersRequests(
                mockSpaceDevelopersOperations, mockSpaceDevelopersChange);

        // then
        verify(mockSpaceDevelopersChange, times(1)).accept(any());
    }

    @Test
    public void createSpaceDevelopersRequestsWithNullShouldThrowANullPointerException() {
        // given
        SpaceDevelopersOperations mockSpaceDevelopersOperations = mock(SpaceDevelopersOperations.class);
        CfContainerChange spaceDevelopersChange = null;

        // then - when
        assertThrows(NullPointerException.class,
                () -> SpaceDevelopersRequestsPlaner
                        .createSpaceDevelopersRequests(mockSpaceDevelopersOperations, spaceDevelopersChange));
    }

}
