package cloud.foundry.cli.operations.spacedevelopers;

import cloud.foundry.cli.operations.SpaceDevelopersOperations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

public class SpaceDevelopersOperationsLoggingTest {


    private SpaceDevelopersOperations spaceDevOpMock;
    private SpaceDevelopersOperationsLogging spaceDevOpLogging;

    @BeforeEach
    void setup() {
        spaceDevOpMock = mock(SpaceDevelopersOperations.class);
        spaceDevOpLogging = new SpaceDevelopersOperationsLogging(spaceDevOpMock);
    }

    @Test
    public void testGetAll() {
        // given
        when(spaceDevOpMock.getAll()).thenReturn(Mono.empty());

        // when
        spaceDevOpLogging.getAll();

        // then
        verify(spaceDevOpMock, times(1)).getAll();
    }

    @Test
    public void testGetSpaceId() {
        // given
        when(spaceDevOpMock.getSpaceId()).thenReturn(Mono.empty());

        // when
        spaceDevOpLogging.getSpaceId();

        // then
        verify(spaceDevOpMock, times(1)).getSpaceId();
    }

    @Test
    public void testAssign() {
        // given
        when(spaceDevOpMock.assign(anyString(), anyString())).thenReturn(Mono.empty());

        // when
        spaceDevOpLogging.assign("user", "spaceId");

        // then
        verify(spaceDevOpMock, times(1)).assign("user", "spaceId");
    }

    @Test
    public void testRemove() {
        // given
        when(spaceDevOpMock.remove(anyString(), anyString())).thenReturn(Mono.empty());

        // when
        spaceDevOpLogging.remove("user", "spaceId");

        // then
        verify(spaceDevOpMock, times(1)).remove("user", "spaceId");
    }
}
