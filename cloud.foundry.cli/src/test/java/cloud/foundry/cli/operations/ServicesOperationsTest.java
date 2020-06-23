package cloud.foundry.cli.operations;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import cloud.foundry.cli.crosscutting.mapping.beans.ServiceBean;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.routes.ListRoutesRequest;
import org.cloudfoundry.operations.routes.Route;
import org.cloudfoundry.operations.routes.Routes;
import org.cloudfoundry.operations.services.CreateServiceInstanceRequest;
import org.cloudfoundry.operations.services.DeleteServiceInstanceRequest;
import org.cloudfoundry.operations.services.DeleteServiceKeyRequest;
import org.cloudfoundry.operations.services.GetServiceInstanceRequest;
import org.cloudfoundry.operations.services.ListServiceKeysRequest;
import org.cloudfoundry.operations.services.UpdateServiceInstanceRequest;
import org.cloudfoundry.operations.services.RenameServiceInstanceRequest;
import org.cloudfoundry.operations.services.ServiceInstance;
import org.cloudfoundry.operations.services.ServiceInstanceSummary;
import org.cloudfoundry.operations.services.Services;
import org.cloudfoundry.operations.services.UnbindRouteServiceInstanceRequest;
import org.cloudfoundry.operations.services.UnbindServiceInstanceRequest;
import org.cloudfoundry.operations.services.ServiceInstanceType;
import org.cloudfoundry.operations.services.ServiceKey;
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

    private static final String USER_PROVIDED_SERVICE_INSTANCE = "user_provided_service_instance";
    private static final String MANAGER_PROVIDED_SERVICE_INSTANCE = "manager_provided_service_instance";

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
        DefaultCloudFoundryOperations cfMock = mockGetAllMethod(withoutServices, withoutServiceInstances);
        ServicesOperations servicesOperations = new ServicesOperations(cfMock);

        // when
        Map<String, ServiceBean> services = servicesOperations.getAll().block();

        // then
        assertTrue(services.isEmpty());
    }

    @Test
    public void testCreate() {
        // given
        String serviceInstanceName = "serviceInstanceName";
        ServiceBean serviceBean = mockServiceBean();
        DefaultCloudFoundryOperations cfMock = Mockito.mock(DefaultCloudFoundryOperations.class);
        Services servicesMock = Mockito.mock(Services.class);
        Mono<Void> monoCreated = mock(Mono.class);
        Mockito.when(cfMock.services()).thenReturn(servicesMock);
        Mockito.when(servicesMock.createInstance(any(CreateServiceInstanceRequest.class)))
            .thenReturn(monoCreated);
        Mockito.when(monoCreated.doOnSubscribe(any())).thenReturn(monoCreated);
        Mockito.when(monoCreated.doOnSuccess(any())).thenReturn(monoCreated);
        // when + then
        ServicesOperations servicesOperations = new ServicesOperations(cfMock);
        Mono<Void> toCreate = servicesOperations.create(serviceInstanceName, serviceBean);
        assertEquals(toCreate, monoCreated);
    }

    @Test
    public void testUpdate() {
        // given
        ServiceBean serviceBeanMock = mockServiceBean();
        String serviceInstanceName = "serviceInstanceName";
        DefaultCloudFoundryOperations cfMock = Mockito.mock(DefaultCloudFoundryOperations.class);
        Services servicesMock = Mockito.mock(Services.class);
        when(cfMock.services()).thenReturn(servicesMock);

        Mono<Void> monoUpdatedService = mock(Mono.class);
        when(servicesMock.updateInstance(any(UpdateServiceInstanceRequest.class))).thenReturn(monoUpdatedService);
        when(monoUpdatedService.doOnSubscribe(any())).thenReturn(monoUpdatedService);

        // when
        ServicesOperations servicesOperations = new ServicesOperations(cfMock);
        Mono<Void> voidMono = servicesOperations.update(serviceInstanceName, serviceBeanMock);

        // then
        assertEquals(monoUpdatedService, voidMono);
    }

    @Test
    public void testRename() {
        // given
        DefaultCloudFoundryOperations cfMock = Mockito.mock(DefaultCloudFoundryOperations.class);
        Services servicesMock = Mockito.mock(Services.class);
        when(cfMock.services()).thenReturn(servicesMock);

        Mono<Void> monoRenamed = mock(Mono.class);
        when(servicesMock.renameInstance(any(RenameServiceInstanceRequest.class))).thenReturn(monoRenamed);
        when(monoRenamed.doOnSubscribe(any())).thenReturn(monoRenamed);

        // when
        ServicesOperations servicesOperations = new ServicesOperations(cfMock);
        Mono<Void> actualMono = servicesOperations.rename("newname", "currentname");

        // then
        assertEquals(monoRenamed, actualMono);
    }

    @Test
    public void testRemoveServiceInstance() {
        // given
        String serviceInstanceName = "serviceInstanceName";
        boolean isValid = true;
        Services servicesMock = mock(Services.class);
        Routes routesMock = mock(Routes.class);

        DefaultCloudFoundryOperations cfMock = mockRemoveServiceInstanceMethod(servicesMock, isValid);
        ServiceKey serviceKeyMock = mockServiceKey(servicesMock);
        Route routeMock = mockRoute(routesMock, isValid);

        when(cfMock.routes()).thenReturn(routesMock);

        doUnbindRoute(cfMock, routeMock);
        doDeleteKey(cfMock, serviceInstanceName, serviceKeyMock);
        when(servicesMock.unbind(any(UnbindServiceInstanceRequest.class)))
            .thenReturn((Mono<Void>) mock(Mono.class));
        when(servicesMock.deleteInstance(any(DeleteServiceInstanceRequest.class))).thenReturn(mock(Mono.class));

        // when
        ServicesOperations servicesOperations = new ServicesOperations(cfMock);
        servicesOperations.remove(serviceInstanceName);

        // then
        verify(servicesMock, times(1)).deleteInstance(any(DeleteServiceInstanceRequest.class));
        verify(servicesMock, times(1)).deleteServiceKey(any(DeleteServiceKeyRequest.class));
        verify(servicesMock, times(2)).unbind(any(UnbindServiceInstanceRequest.class));
        verify(servicesMock, times(1)).unbindRoute(any(UnbindRouteServiceInstanceRequest.class));
    }

    @Test
    public void testRemoveServiceInstanceWithoutConditions() {
        // given
        String serviceInstanceName = "serviceInstanceName";
        boolean isValid = false;
        Services servicesMock = mock(Services.class);
        Routes routesMock = mock(Routes.class);

        DefaultCloudFoundryOperations cfMock = mockRemoveServiceInstanceMethod(servicesMock, isValid);
        Route routeMock = mockRoute(routesMock, isValid);
        ServiceKey serviceKeyMock = mockServiceKey(servicesMock);

        when(cfMock.services()).thenReturn(servicesMock);
        when(cfMock.routes()).thenReturn(routesMock);

        doUnbindRoute(cfMock, routeMock);
        doDeleteKey(cfMock, serviceInstanceName, serviceKeyMock);
        when(servicesMock.unbind(any(UnbindServiceInstanceRequest.class)))
            .thenReturn((Mono<Void>) mock(Mono.class));
        when(servicesMock.deleteInstance(any(DeleteServiceInstanceRequest.class))).thenReturn(mock(Mono.class));

        // when
        ServicesOperations servicesOperations = new ServicesOperations(cfMock);
        servicesOperations.remove(serviceInstanceName);

        // then
        verify(servicesMock, times(1)).deleteInstance(any(DeleteServiceInstanceRequest.class));
        verify(servicesMock, times(0)).deleteServiceKey(any(DeleteServiceKeyRequest.class));
        verify(servicesMock, times(0)).unbind(any(UnbindServiceInstanceRequest.class));
        verify(servicesMock, times(0)).unbindRoute(any(UnbindRouteServiceInstanceRequest.class));
    }

    private void doUnbindRoute(DefaultCloudFoundryOperations cloudFoundryOperations, Route route) {
        when(cloudFoundryOperations.services()
            .unbindRoute(
                UnbindRouteServiceInstanceRequest
                    .builder()
                    .serviceInstanceName(route.getService())
                    .domainName(route.getDomain())
                    .hostname(route.getHost())
                    .build())).thenReturn(Mono.empty());
    }

    private void doDeleteKey(DefaultCloudFoundryOperations cloudFoundryOperations, String serviceInstanceName,
        ServiceKey serviceKey) {
        when(cloudFoundryOperations
            .services()
            .deleteServiceKey(DeleteServiceKeyRequest.builder()
                .serviceInstanceName(serviceInstanceName)
                .serviceKeyName(serviceKey.getName())
                .build()))
                    .thenReturn(Mono.empty());
    }

    private ServiceBean mockServiceBean() {
        ServiceBean serviceBean = mock(ServiceBean.class);
        when(serviceBean.getService()).thenReturn("elephantsql");
        when(serviceBean.getPlan()).thenReturn("standard");
        when(serviceBean.getTags()).thenReturn(Arrays.asList("Tag1", "Tag2"));
        return serviceBean;
    }

    private ServiceInstance mockServiceInstance(Boolean isValid) {
        ServiceInstance serviceInstanceMock = mock(ServiceInstance.class);
        ServiceInstanceType serviceInstanceTypeMock = mock(ServiceInstanceType.class);

        if (isValid) {
            when(serviceInstanceMock.getApplications()).thenReturn(Arrays.asList("app1", "app2"));
            when(serviceInstanceMock.getType()).thenReturn(serviceInstanceTypeMock);
            when(serviceInstanceTypeMock.getValue()).thenReturn(MANAGER_PROVIDED_SERVICE_INSTANCE);

        } else {
            when(serviceInstanceMock.getApplications()).thenReturn(Collections.emptyList());
            when(serviceInstanceMock.getType()).thenReturn(serviceInstanceTypeMock);
            when(serviceInstanceTypeMock.getValue()).thenReturn(USER_PROVIDED_SERVICE_INSTANCE);
        }
        return serviceInstanceMock;
    }

    private ServiceKey mockServiceKey(Services servicesMock) {
        ServiceKey serviceKeyMock = mock(ServiceKey.class);
        Flux<ServiceKey> fluxMock = Flux.just(serviceKeyMock);

        when(serviceKeyMock.getName()).thenReturn("keyname");
        when(servicesMock.listServiceKeys(any(ListServiceKeysRequest.class)))
            .thenReturn(fluxMock);

        return serviceKeyMock;
    }

    private Route mockRoute(Routes routesMock, boolean isValid) {
        Route routeMock = mock(Route.class);
        Flux<Route> fluxMock = Flux.just(routeMock);

        when(routesMock.list(any(ListRoutesRequest.class))).thenReturn(fluxMock);
        when(routeMock.getDomain()).thenReturn("domain");
        when(routeMock.getHost()).thenReturn("host");
        if (isValid) {
            when(routeMock.getService()).thenReturn("serviceInstanceName");
        } else {
            when(routeMock.getService()).thenReturn("route");
        }
        return routeMock;

    }

    private DefaultCloudFoundryOperations mockRemoveServiceInstanceMethod(Services servicesMock, boolean isValid) {
        DefaultCloudFoundryOperations cfMock = mock(DefaultCloudFoundryOperations.class);
        ServiceInstance serviceInstanceMock = mockServiceInstance(isValid);
        Mono<ServiceInstance> monoserviceInstanceMock = mock(Mono.class);

        when(cfMock.services()).thenReturn(servicesMock);
        when(servicesMock.getInstance(any(GetServiceInstanceRequest.class)))
            .thenReturn(monoserviceInstanceMock);
        when(monoserviceInstanceMock.block()).thenReturn(serviceInstanceMock);

        return cfMock;
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
