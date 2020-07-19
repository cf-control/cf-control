package cloud.foundry.cli.operations.space;

import cloud.foundry.cli.operations.SpaceOperations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.*;

/**
 * Test for {@link SpaceOperationsLogging}
 */
public class SpaceOperationsLoggingTest {


    private SpaceOperations spaceOpMock;
    private SpaceOperationsLogging spaceOpLogging;

    @BeforeEach
    void setup() {
        spaceOpMock = mock(SpaceOperations.class);
        spaceOpLogging = new SpaceOperationsLogging(spaceOpMock);
    }

    @Test
    public void testGetAll() {
        // given
        when(spaceOpMock.getAll()).thenReturn(Mono.empty());

        // when
        spaceOpLogging.getAll();

        // then
        verify(spaceOpMock, times(1)).getAll();
    }

    @Test
    public void testCreate() {
        // given
        when(spaceOpMock.create(anyString())).thenReturn(Mono.empty());

        // when
        spaceOpLogging.create("space");

        // then
        verify(spaceOpMock, times(1)).create("space");
    }
}
