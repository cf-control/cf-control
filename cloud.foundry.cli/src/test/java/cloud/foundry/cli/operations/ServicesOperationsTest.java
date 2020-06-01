package cloud.foundry.cli.operations;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import cloud.foundry.cli.crosscutting.beans.ServiceBean;
import cloud.foundry.cli.crosscutting.exceptions.CreationException;
import cloud.foundry.cli.crosscutting.util.YamlCreator;
import org.cloudfoundry.client.v2.ClientV2Exception;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.services.BindServiceInstanceRequest;
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
        String s = YamlCreator.createDefaultYamlProcessor().dump(serviceBeans);
        assertThat(s, is(
            "- applications:\n" +
                "  - test-flask\n" +
                "  id: serviceId\n" +
                "  lastOperation: null\n" +
                "  name: serviceName\n" +
                "  plan: standardPlan\n" +
                "  service: service\n" +
                "  tags:\n" +
                "  - tag\n" +
                "  type: MANAGED\n"));
    }

    @Test
    public void testGetServicesWithEmptyMockData() {
        // given
        List<ServiceInstanceSummary> withoutServices = Collections.emptyList();
        DefaultCloudFoundryOperations cfMock = mockGetAllMethod(withoutServices);
        ServicesOperations servicesOperations = new ServicesOperations(cfMock);

        // when
        List<ServiceBean> serviceBeans = servicesOperations.getAll().block();

        // then
        // FIXME: it should return just an empty bracket like []
        String s = YamlCreator.createDefaultYamlProcessor().dump(serviceBeans);
        assertEquals("[\n  ]\n", s);
    }

    @Test
    public void testCreateExceptionWhenCreatingService() throws CreationException {
        // given
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
            servicesOperations.create(serviceBean);
        });
    }

    @Test
    public void testCreateExceptionWhenBindingApps() throws CreationException {
        // given
        ServiceBean serviceBean = getServiceBeanMock();
        List<String> apps = new LinkedList<>();
        apps.add("test");
        when(serviceBean.getApplications()).thenReturn(apps);
        DefaultCloudFoundryOperations cfMock = Mockito.mock(DefaultCloudFoundryOperations.class);
        Services servicesMock = Mockito.mock(Services.class);
        Mono<Void> monoCreated = mock(Mono.class);
        Mono<Void> monoBind = mock(Mono.class);
        Mockito.when(cfMock.services()).thenReturn(servicesMock);
        Mockito.when(servicesMock.createInstance(any(CreateServiceInstanceRequest.class)))
            .thenReturn(monoCreated);
        Mockito.when(monoCreated.block()).thenReturn(null);
        Mockito.when(servicesMock.bind(any(BindServiceInstanceRequest.class))).thenReturn(monoBind);
        Mockito.when(monoBind.block()).thenThrow(new IllegalArgumentException("Application test does not exist"));
        // when + then
        ServicesOperations servicesOperations = new ServicesOperations(cfMock);
        assertThrows(CreationException.class, () -> {
            servicesOperations.create(serviceBean);
        });
    }

    @Test
    public void testUpdateService_ThrowException() throws CreationException {
        // given
        ServiceBean serviceBeanMock = getServiceBeanMock();
        List<String> apps = new LinkedList<>();
        apps.add("test");
        when(serviceBeanMock.getApplications()).thenReturn(apps);

        DefaultCloudFoundryOperations cfMock = Mockito.mock(DefaultCloudFoundryOperations.class);
        Services servicesMock = Mockito.mock(Services.class);
        when(cfMock.services()).thenReturn(servicesMock);
        when(servicesMock.renameInstance(any(RenameServiceInstanceRequest.class))).thenReturn(null);

        Mono<Void> monoUpdatedService = mock(Mono.class);
        when(servicesMock.updateInstance(any(UpdateServiceInstanceRequest.class))).thenReturn(null);
        Mockito.when(monoUpdatedService.block()).thenThrow(new NullPointerException("Service Instance can not update"));

        Mono<Void> monoBind = mock(Mono.class);
        when(servicesMock.bind(any(BindServiceInstanceRequest.class))).thenReturn(null);
        Mockito.when(monoBind.block()).thenThrow(new IllegalArgumentException("Application test does not exist"));

        // when + then
        ServicesOperations servicesOperations = new ServicesOperations(cfMock);
        assertThrows(CreationException.class, () -> {
            servicesOperations.create(serviceBeanMock);
        });
    }

    @Test
    public void testUpdateService() throws CreationException {
        // given
        ServiceBean serviceBeanMock = getServiceBeanMock();
        List<String> apps = new LinkedList<>();
        apps.add("test");
        when(serviceBeanMock.getApplications()).thenReturn(apps);

        DefaultCloudFoundryOperations cfMock = Mockito.mock(DefaultCloudFoundryOperations.class);
        Services servicesMock = Mockito.mock(Services.class);
        when(cfMock.services()).thenReturn(servicesMock);

        Mono<Void> monoRenamed = mock(Mono.class);
        when(servicesMock.renameInstance(any(RenameServiceInstanceRequest.class))).thenReturn(monoRenamed);

        Mono<Void> monoUpdatedService = mock(Mono.class);
        when(servicesMock.updateInstance(any(UpdateServiceInstanceRequest.class))).thenReturn(monoUpdatedService);

        Mono<Void> monoBind = mock(Mono.class);
        when(servicesMock.bind(any(BindServiceInstanceRequest.class))).thenReturn(monoBind);

        // when
        ServicesOperations servicesOperations = new ServicesOperations(cfMock);
        servicesOperations.update(serviceBeanMock);

        //then
        verify(servicesMock, times(1)).renameInstance(any(RenameServiceInstanceRequest.class));
        verify(monoRenamed, times(1)).block();

        verify(servicesMock, times(1)).updateInstance(any(UpdateServiceInstanceRequest.class));
        verify(monoUpdatedService, times(1)).block();

        verify(servicesMock, times(1)).bind(any(BindServiceInstanceRequest.class));
        verify(monoBind, times(1)).block();

    }

    private ServiceBean getServiceBeanMock() {
        ServiceBean serviceBean = mock(ServiceBean.class);
        when(serviceBean.getId()).thenReturn("123");
        when(serviceBean.getService()).thenReturn("elephantsql");
        when(serviceBean.getName()).thenReturn("Elephant");
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