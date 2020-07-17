package cloud.foundry.cli.logic;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import cloud.foundry.cli.crosscutting.exceptions.UpdateException;
import cloud.foundry.cli.operations.ApplicationsOperations;
import cloud.foundry.cli.operations.OperationsFactory;
import cloud.foundry.cli.operations.ServicesOperations;
import org.cloudfoundry.client.v2.ClientV2Exception;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

public class RenameLogicTest {

    private static final String NEW_NAME = "newName";
    private static final String CURRENT_NAME = "currentName";

    private OperationsFactory operationsFactoryMock;
    private ApplicationsOperations applicationsOperationsMock;
    private ServicesOperations servicesOperationsMock;
    private RenameLogic renameLogic;

    @BeforeEach
    void setup() {
        this.operationsFactoryMock = mock(OperationsFactory.class);
        this.servicesOperationsMock = mock(ServicesOperations.class);
        this.applicationsOperationsMock = mock(ApplicationsOperations.class);
        when(operationsFactoryMock.createServiceOperations()).thenReturn(this.servicesOperationsMock);
        when(operationsFactoryMock.createApplicationsOperations()).thenReturn(this.applicationsOperationsMock);

        renameLogic = new RenameLogic(operationsFactoryMock);
    }

    @Test
    public void renameService_Works() {
        //given
        Mono monoMock = mock(Mono.class);
        when(servicesOperationsMock.rename(NEW_NAME, CURRENT_NAME)).thenReturn(monoMock);

        //when
        renameLogic.renameService(NEW_NAME, CURRENT_NAME);

        //then
        verify(servicesOperationsMock).rename(NEW_NAME, CURRENT_NAME);
        verify(monoMock).block();
    }

    @Test
    public void renameService_ServiceNotFound_UpdateExceptionThrown() {
        //given
        Mono monoMock = mock(Mono.class);
        when(servicesOperationsMock.rename(NEW_NAME, CURRENT_NAME)).thenReturn(monoMock);
        when(monoMock.block()).thenThrow(new ClientV2Exception(0,0,"Service not found","Error"));

        //when + then
        assertThrows(UpdateException.class, () -> renameLogic.renameService(NEW_NAME, CURRENT_NAME));
    }

        @Test
    public void renameService_NewNameNull_NullptrExceptionThrown() {
        assertThrows(NullPointerException.class,
                () -> renameLogic.renameService( null,
                        CURRENT_NAME));
    }

        @Test
    public void renameService_CurrentNameNull_NullptrExceptionThrown() {
            assertThrows(NullPointerException.class,
                    () -> renameLogic.renameService(NEW_NAME,
                            null));
    }

    @Test
    public void renameApplication_Works() {
        //given
        Mono monoMock = mock(Mono.class);
        when(applicationsOperationsMock.rename(NEW_NAME, CURRENT_NAME)).thenReturn(monoMock);

        //when
        renameLogic.renameApplication(NEW_NAME, CURRENT_NAME);

        //then
        verify(applicationsOperationsMock).rename(NEW_NAME, CURRENT_NAME);
        verify(monoMock).block();
    }

    @Test
    public void renameApplication_ApplicationNotFound_UpdateExceptionThrown() {
        //given
        Mono monoMock = mock(Mono.class);
        when(applicationsOperationsMock.rename(NEW_NAME, CURRENT_NAME)).thenReturn(monoMock);
        when(monoMock.block()).thenThrow(new ClientV2Exception(0,0,"Application not found","Error"));

        //when + then
        assertThrows(UpdateException.class, () -> renameLogic.renameApplication( NEW_NAME, CURRENT_NAME));
    }

    @Test
    public void renameApplication_NewNameNull_NullptrExceptionThrown() {
        assertThrows(NullPointerException.class,
                () -> renameLogic.renameApplication(null, CURRENT_NAME));
    }

    @Test
    public void renameApplication_CurrentNameNull_NullptrExceptionThrown() {
        assertThrows(NullPointerException.class,
                () -> renameLogic.renameApplication(NEW_NAME, null));
    }


}
