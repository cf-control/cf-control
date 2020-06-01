package cloud.foundry.cli.operations;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import cloud.foundry.cli.crosscutting.beans.ServiceBean;
import cloud.foundry.cli.crosscutting.exceptions.CreationException;
import org.cloudfoundry.client.v2.ClientV2Exception;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.services.CreateServiceInstanceRequest;
import org.cloudfoundry.operations.services.UpdateServiceInstanceRequest;
import org.cloudfoundry.operations.services.RenameServiceInstanceRequest;
import org.cloudfoundry.operations.services.ServiceInstance;
import org.cloudfoundry.operations.services.ServiceInstanceSummary;
import org.cloudfoundry.operations.services.Services;
import org.cloudfoundry.operations.services.ServiceInstanceType;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ServicesOperationsTest {

    @Test
    public void testGetServicesWithMockData() {
        // given
        ServiceInstanceSummary summary = ServiceInstanceSummary.builder()
                .id("serviceId")
                .application("test-flask")
                .name("serviceName")
                .plan("standardPlan")
                .service("service")
                .tags(List.of("tag"))
                .type(ServiceInstanceType.MANAGED)
                .build();
        List<ServiceInstanceSummary> withServices = List.of(summary);

        DefaultCloudFoundryOperations cfMock = mockGetAllMethod(withServices);
        ServicesOperations servicesOperations = new ServicesOperations(cfMock);

        // when
        List<ServiceBean> serviceBeans = servicesOperations.getAll().block();

        // then
        assertThat(services.size(), is(1));
        assertThat(services.containsKey("serviceName"), is(true));
        assertThat(services.get("serviceName").getService(), is("service"));
        assertThat(services.get("serviceName").getTags().size(), is(1));
        assertThat(services.get("serviceName").getTags(), contains("tag"));
    }

    @Test
    public void testGetServicesWithEmptyMockData() {
        // given
        List<ServiceInstanceSummary> withoutServices = Collections.emptyList();
        DefaultCloudFoundryOperations cfMock = mockGetAllMethod(withoutServices);
        ServicesOperations servicesOperations = new ServicesOperations(cfMock);

        // when
        List<ServiceBean> serviceBeans = servicesOperations.getAll().block();

        ServicesOperations servicesOperations = new ServicesOperations(cfMock);
        Map<String, ServiceBean> services = servicesOperations.getAll();
        // then
        // FIXME: it should return just an empty bracket like []
        String s = YamlCreator.createDefaultYamlProcessor().dump(serviceBeans);
        assertEquals("[\n  ]\n", s);
        // FIXME: it should return just an empty bracket like {]
        assertTrue(services.isEmpty());
    }

    @Test
    public void testCreateExceptionWhenCreatingService() throws CreationException {
        // given
        String serviceInstanceName = "serviceInstanceName";
        ServiceBean serviceBean = getServiceBeanMock();
        DefaultCloudFoundryOperations cfMock = Mockito.mock(DefaultCloudFoundryOperations.class);
        Services servicesMock = Mockito.mock(Services.class);
        Mono<Void> monoCreated = mock(Mono.class);
        Mockito.when(cfMock.services()).thenReturn(servicesMock);
        Mockito.when(servicesMock.createInstance(any(CreateServiceInstanceRequest.class)))
            .thenReturn(monoCreated);
        Mockito.when(monoCreated.block()).thenThrow(new ClientV2Exception(400,
            60002, "The service instance name is taken: Elephant", "CF-ServiceInstanceNameTaken"));
        // when + then
        ServicesOperations servicesOperations = new ServicesOperations(cfMock);
        assertThrows(CreationException.class, () -> {
            servicesOperations.create(serviceInstanceName, serviceBean);
        });
    }

    @Test
    public void testUpdateService_ThrowException() throws CreationException {
        // given
        String serviceInstanceName = "serviceInstanceName";
        ServiceBean serviceBeanMock = getServiceBeanMock();
        DefaultCloudFoundryOperations cfMock = Mockito.mock(DefaultCloudFoundryOperations.class);

        Services servicesMock = Mockito.mock(Services.class);
        when(cfMock.services()).thenReturn(servicesMock);
        when(servicesMock.renameInstance(any(RenameServiceInstanceRequest.class))).thenReturn(null);

        Mono<Void> monoUpdatedService = mock(Mono.class);
        when(servicesMock.updateInstance(any(UpdateServiceInstanceRequest.class))).thenReturn(null);
        Mockito.when(monoUpdatedService.block())
            .thenThrow(new NullPointerException("Service Instance can not update"));

        // when + then
        ServicesOperations servicesOperations = new ServicesOperations(cfMock);
        assertThrows(CreationException.class, () -> {
            servicesOperations.create(serviceInstanceName, serviceBeanMock);
        });
    }

    @Test
    public void testUpdateServiceInstance() throws CreationException {
        // given
        ServiceBean serviceBeanMock = getServiceBeanMock();
        String serviceInstanceName = "serviceInstanceName";
        DefaultCloudFoundryOperations cfMock = Mockito.mock(DefaultCloudFoundryOperations.class);
        Services servicesMock = Mockito.mock(Services.class);
        when(cfMock.services()).thenReturn(servicesMock);

        Mono<Void> monoUpdatedService = mock(Mono.class);
        when(servicesMock.updateInstance(any(UpdateServiceInstanceRequest.class))).thenReturn(monoUpdatedService);

        // when
        ServicesOperations servicesOperations = new ServicesOperations(cfMock);
        servicesOperations.updateServiceInstance(serviceInstanceName, serviceBeanMock);

        // then
        verify(servicesMock, times(1)).updateInstance(any(UpdateServiceInstanceRequest.class));
        verify(monoUpdatedService, times(1)).block();

    }

    @Test
    public void testRenameServiceInstance() throws CreationException {
        // given
        DefaultCloudFoundryOperations cfMock = Mockito.mock(DefaultCloudFoundryOperations.class);
        Services servicesMock = Mockito.mock(Services.class);
        when(cfMock.services()).thenReturn(servicesMock);

        Mono<Void> monoRenamed = mock(Mono.class);
        when(servicesMock.renameInstance(any(RenameServiceInstanceRequest.class))).thenReturn(monoRenamed);

        // when
        ServicesOperations servicesOperations = new ServicesOperations(cfMock);
        servicesOperations.renameServiceInstance("newname", "currentname");

        // then
        verify(servicesMock, times(1)).renameInstance(any(RenameServiceInstanceRequest.class));
        verify(monoRenamed, times(1)).block();
    }

    private ServiceInstance getServiceInstanceMock() {
        ServiceInstance serviceInstanceMock = mock(ServiceInstance.class);

        when(serviceInstanceMock.getLastOperation()).thenReturn("create");
        when(serviceInstanceMock.getStatus()).thenReturn("succeeded");
        Mockito.when(serviceInstanceMock.getName()).thenReturn("serviceName");
        Mockito.when(serviceInstanceMock.getPlan()).thenReturn("standardPlan");
        Mockito.when(serviceInstanceMock.getService()).thenReturn("service");
        LinkedList<String> tags = new LinkedList<>();
        tags.add("tag");
        Mockito.when(serviceInstanceMock.getTags()).thenReturn(tags);
        Mockito.when(serviceInstanceMock.getType()).thenReturn(ServiceInstanceType.MANAGED);
        return serviceInstanceMock;
    }

    private ServiceBean getServiceBeanMock() {
        ServiceBean serviceBean = mock(ServiceBean.class);
        when(serviceBean.getService()).thenReturn("elephantsql");
        when(serviceBean.getPlan()).thenReturn("standard");
        when(serviceBean.getTags()).thenReturn(Arrays.asList("Tag1", "Tag2"));
        return serviceBean;
    }

    private DefaultCloudFoundryOperations mockGetAllMethod(List<ServiceInstanceSummary> summaries) {
        DefaultCloudFoundryOperations cfMock = Mockito.mock(DefaultCloudFoundryOperations.class);
        Services servicesMock = Mockito.mock(Services.class);
        Flux<ServiceInstanceSummary> fluxMock = Flux.fromIterable(summaries);

        when(cfMock.services())
                .thenReturn(servicesMock);
        when(servicesMock.listInstances())
                .thenReturn(fluxMock);

        return cfMock;
    }

}
