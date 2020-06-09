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

import cloud.foundry.cli.crosscutting.mapping.beans.ServiceBean;
import cloud.foundry.cli.crosscutting.exceptions.CreationException;
import org.cloudfoundry.client.v2.ClientV2Exception;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationManifest;
import org.cloudfoundry.operations.applications.GetApplicationManifestRequest;
import org.cloudfoundry.operations.services.CreateServiceInstanceRequest;
import org.cloudfoundry.operations.services.GetServiceInstanceRequest;
import org.cloudfoundry.operations.services.UpdateServiceInstanceRequest;
import org.cloudfoundry.operations.services.RenameServiceInstanceRequest;
import org.cloudfoundry.operations.services.ServiceInstance;
import org.cloudfoundry.operations.services.ServiceInstanceSummary;
import org.cloudfoundry.operations.services.Services;
import org.cloudfoundry.operations.services.ServiceInstanceType;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ServicesOperationsTest {

    @Test
    public void testGetServicesWithMockData() {
        // given
        ServiceInstance serviceInstanceMock = ServiceInstance.builder()
            .id("serviceId")
            .application("test-flask")
            .name("serviceName")
            .plan("standardPlan")
            .service("service")
            .tags(Arrays.asList("tag"))
            .type(ServiceInstanceType.MANAGED)
            .build();

        ServiceInstanceSummary summary = ServiceInstanceSummary.builder()
            .id("serviceId")
            .application("test-flask")
            .name("serviceName")
            .plan("standardPlan")
            .service("service")
            .tags(Arrays.asList("tag"))
            .type(ServiceInstanceType.MANAGED)
            .build();

        List<ServiceInstanceSummary> withServices = Arrays.asList(summary);
        List<ServiceInstance> serviceInstances = Arrays.asList(serviceInstanceMock);
        
        DefaultCloudFoundryOperations cfMock = mockGetAllMethod(withServices, serviceInstances);
        ServicesOperations servicesOperations = new ServicesOperations(cfMock);

        // when
        Map<String, ServiceBean> services = servicesOperations.getAll().block();

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
        List<ServiceInstance> withoutServiceInstances = Collections.emptyList();
        DefaultCloudFoundryOperations cfMock = mockGetAllMethod(withoutServices,withoutServiceInstances);
        ServicesOperations servicesOperations = new ServicesOperations(cfMock);

        // when
        Map<String, ServiceBean> services = servicesOperations.getAll().block();

        // then
        assertTrue(services.isEmpty());
    }

    @Test
    public void testCreateExceptionWhenCreatingService() {
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

    private ServiceBean getServiceBeanMock() {
        ServiceBean serviceBean = mock(ServiceBean.class);
        when(serviceBean.getService()).thenReturn("elephantsql");
        when(serviceBean.getPlan()).thenReturn("standard");
        when(serviceBean.getTags()).thenReturn(Arrays.asList("Tag1", "Tag2"));
        return serviceBean;
    }

    private DefaultCloudFoundryOperations mockGetAllMethod(List<ServiceInstanceSummary> summaries,
        List<ServiceInstance> serviceInstances) {
        DefaultCloudFoundryOperations cfMock = Mockito.mock(DefaultCloudFoundryOperations.class);
        Services servicesMock = Mockito.mock(Services.class);
        Flux<ServiceInstanceSummary> fluxMock = Flux.fromIterable(summaries);

        when(cfMock.services()).thenReturn(servicesMock);
        when(servicesMock.listInstances()).thenReturn(fluxMock);

        when(servicesMock.getInstance(any(GetServiceInstanceRequest.class)))
            .thenAnswer((Answer<Mono<ServiceInstance>>) invocation -> {
                GetServiceInstanceRequest request = invocation.getArgument(0);
                
                for (ServiceInstance serviceInstance : serviceInstances) {
                    if (serviceInstance.getName().equals(request.getName())) {
                        return Mono.just(serviceInstance);
                    }
                }
                throw new RuntimeException("fixme");

            });

        return cfMock;
    }

}
