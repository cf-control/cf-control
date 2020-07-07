package cloud.foundry.cli.logic;

import cloud.foundry.cli.crosscutting.exceptions.UpdateException;
import cloud.foundry.cli.operations.ApplicationsOperations;
import cloud.foundry.cli.operations.ServicesOperations;
import org.cloudfoundry.client.v2.ClientV2Exception;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class RenameLogicTest {

    private static final String NEW_NAME = "newName";
    private static final String CURRENT_NAME = "currentName";

    @Test
    public void renameService_Works() {
        //given
        ServicesOperations servicesOperations = mock(ServicesOperations.class);
        Mono monoMock = mock(Mono.class);
        when(servicesOperations.rename(NEW_NAME, CURRENT_NAME)).thenReturn(monoMock);

        //when
        RenameLogic renameLogic = new RenameLogic();
        renameLogic.renameService(servicesOperations, NEW_NAME, CURRENT_NAME);

        //then
        verify(servicesOperations).rename(NEW_NAME, CURRENT_NAME);
        verify(monoMock).block();
    }

    @Test
    public void renameService_ServiceNotFound_UpdateExceptionThrown() {
        //given
        ServicesOperations servicesOperationsMock = mock(ServicesOperations.class);
        Mono monoMock = mock(Mono.class);
        when(servicesOperationsMock.rename(NEW_NAME, CURRENT_NAME)).thenReturn(monoMock);
        when(monoMock.block()).thenThrow(new ClientV2Exception(0,0,"Service not found","Error"));

        //when + then
        assertThrows(UpdateException.class, () -> new RenameLogic().renameService(servicesOperationsMock,
                NEW_NAME, CURRENT_NAME));
    }

    @Test
    public void renameService_ServicesOperationsNull_NullptrExceptionThrown() {
        assertThrows(NullPointerException.class, () -> new RenameLogic().renameService(null,
                NEW_NAME, CURRENT_NAME));
    }

        @Test
    public void renameService_NewNameNull_NullptrExceptionThrown() {
        DefaultCloudFoundryOperations cfOperationsMock = mock(DefaultCloudFoundryOperations.class);
        assertThrows(NullPointerException.class,
                () -> new RenameLogic().renameService(new ServicesOperations(cfOperationsMock), null,
                        CURRENT_NAME));
    }

        @Test
    public void renameService_CurrentNameNull_NullptrExceptionThrown() {
        DefaultCloudFoundryOperations cfOperationsMock = mock(DefaultCloudFoundryOperations.class);
            assertThrows(NullPointerException.class,
                    () -> new RenameLogic().renameService(new ServicesOperations(cfOperationsMock), NEW_NAME,
                            null));
    }

    @Test
    public void renameApplication_Works() {
        //given
        ApplicationsOperations applicationsOperations = mock(ApplicationsOperations.class);
        Mono monoMock = mock(Mono.class);
        when(applicationsOperations.rename(NEW_NAME, CURRENT_NAME)).thenReturn(monoMock);

        //when
        RenameLogic renameLogic = new RenameLogic();
        renameLogic.renameApplication(applicationsOperations, NEW_NAME, CURRENT_NAME);

        //then
        verify(applicationsOperations).rename(NEW_NAME, CURRENT_NAME);
        verify(monoMock).block();
    }

    @Test
    public void renameApplication_ApplicationNotFound_UpdateExceptionThrown() {
        //given
        ApplicationsOperations applicationsOperations = mock(ApplicationsOperations.class);
        Mono monoMock = mock(Mono.class);
        when(applicationsOperations.rename(NEW_NAME, CURRENT_NAME)).thenReturn(monoMock);
        when(monoMock.block()).thenThrow(new ClientV2Exception(0,0,"Application not found","Error"));

        //when + then
        assertThrows(UpdateException.class, () -> new RenameLogic().renameApplication(applicationsOperations,
                NEW_NAME, CURRENT_NAME));
    }

    @Test
    public void renameApplication_ApplicationsOperationsNull_NullptrExceptionThrown() {
        assertThrows(NullPointerException.class, () -> new RenameLogic().renameApplication(null,
                NEW_NAME, CURRENT_NAME));
    }

    @Test
    public void renameApplication_NewNameNull_NullptrExceptionThrown() {
        DefaultCloudFoundryOperations cfOperationsMock = mock(DefaultCloudFoundryOperations.class);
        assertThrows(NullPointerException.class,
                () -> new RenameLogic().renameApplication(new ApplicationsOperations(cfOperationsMock), null,
                        CURRENT_NAME));
    }

    @Test
    public void renameApplication_CurrentNameNull_NullptrExceptionThrown() {
        DefaultCloudFoundryOperations cfOperationsMock = mock(DefaultCloudFoundryOperations.class);
        assertThrows(NullPointerException.class,
                () -> new RenameLogic().renameApplication(new ApplicationsOperations(cfOperationsMock), NEW_NAME,
                        null));
    }


}