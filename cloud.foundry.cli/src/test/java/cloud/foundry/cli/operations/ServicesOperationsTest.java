package cloud.foundry.cli.operations;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cloud.foundry.cli.crosscutting.mapping.beans.ServiceBean;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.routes.ListRoutesRequest;
import org.cloudfoundry.operations.routes.Route;
import org.cloudfoundry.operations.routes.Routes;
import org.cloudfoundry.operations.services.*;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
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
        ServiceBean serviceBeanMock = mockServiceBean();
        DefaultCloudFoundryOperations cfMock = Mockito.mock(DefaultCloudFoundryOperations.class);
        Services servicesMock = Mockito.mock(Services.class);

        Mono<Void> mono = Mono.empty();
        when(cfMock.services()).thenReturn(servicesMock);
        when(servicesMock.createInstance(any(CreateServiceInstanceRequest.class)))
            .thenReturn(mono);

        ServicesOperations servicesOperations = new ServicesOperations(cfMock);

        // when
        Mono<Void> actualMono = servicesOperations.create(serviceInstanceName, serviceBeanMock);
        actualMono.block();

        // then
        assertThat(actualMono, notNullValue());
        verify(serviceBeanMock, times(1)).getParams();
        verify(serviceBeanMock, times(1)).getPlan();
        verify(serviceBeanMock, times(1)).getTags();
        verify(serviceBeanMock, times(1)).getService();
        verify(servicesMock, times(1)).createInstance(any(CreateServiceInstanceRequest.class));
        StepVerifier.create(actualMono)
            .expectComplete()
            .verify();
    }

    @Test
    public void testCreateOnNullArgumentsThrowsException() {
        // given
        DefaultCloudFoundryOperations cfOperationsMock = mock(DefaultCloudFoundryOperations.class);
        ServicesOperations servicesOperations = new ServicesOperations(cfOperationsMock);

        // when
        assertThrows(NullPointerException.class, () -> servicesOperations.create(null, new ServiceBean()));
        assertThrows(NullPointerException.class, () -> servicesOperations.create("oldname", null));
    }

    @Test
    public void testUpdate() {
        // given
        // for remove service instance
        Route route1 = Route.builder().service("someservice").domain("domain")
            .host("host").id("id").space("space").build();
        Route route2 = Route.builder().service("someservice2").domain("domain")
            .host("host").id("id").space("space").build();
        Route route3 = Route.builder().service("someservice").domain("otherdomain")
            .host("host").id("id").space("space").build();

        ServiceInstance serviceInstance = ServiceInstance.builder()
            .service("someservice")
            .name("servicename")
            .applications("app1", "app2")
            .id("someid")
            .type(ServiceInstanceType.MANAGED)
            .build();

        ServiceKey serviceKey = ServiceKey.builder().id("someid").name("name").build();
        ServiceKey serviceKey2 = ServiceKey.builder().id("someid").name("name2").build();

        DefaultCloudFoundryOperations cfOperationsMock = mock(DefaultCloudFoundryOperations.class);
        Services servicesMock = mock(Services.class);
        Routes routesMock = mock(Routes.class);


        mockGetServiceInstance(cfOperationsMock, servicesMock, serviceInstance);
        mockListRoutes(cfOperationsMock, routesMock, Arrays.asList(route1, route2, route3));
        mockUnbindRoute(cfOperationsMock, servicesMock);
        mockBindRoute(cfOperationsMock, servicesMock);
        mockUnbindApp(cfOperationsMock, servicesMock);
        mockBindApp(cfOperationsMock, servicesMock);
        mockListServiceKeys(cfOperationsMock, servicesMock, Arrays.asList(serviceKey, serviceKey2));
        mockDeleteServiceKey(cfOperationsMock, servicesMock);
        mockCreateServiceKey(cfOperationsMock, servicesMock);
        mockDeleteServiceInstance(cfOperationsMock, servicesMock);

        // for create service instance
        ServiceBean serviceBeanMock = mockServiceBean();
        Mono<Void> mono = Mono.empty();
        when(cfOperationsMock.services()).thenReturn(servicesMock);
        when(servicesMock.createInstance(any(CreateServiceInstanceRequest.class)))
            .thenReturn(mono);

        // when
        ServicesOperations servicesOperations = new ServicesOperations(cfOperationsMock);
        Mono<Void> request = servicesOperations.update("someservice", serviceBeanMock);
        request.block();

        // then
        assertThat(request, notNullValue());

        verify(servicesMock, times(3)).getInstance(any());
        verify(routesMock, times(2)).list(any());
        verify(servicesMock, times(2)).unbindRoute(any());
        verify(servicesMock, times(2)).unbind(any());
        verify(servicesMock, times(2)).listServiceKeys(any());
        verify(servicesMock, times(2)).deleteServiceKey(any());
        verify(servicesMock, times(1)).deleteInstance(any(DeleteServiceInstanceRequest.class));
        verify(servicesMock, times(2)).bindRoute(any());
        verify(servicesMock, times(2)).bind(any());
        verify(servicesMock, times(2)).createServiceKey(any());

        verify(serviceBeanMock, times(1)).getParams();
        verify(serviceBeanMock, times(1)).getPlan();
        verify(serviceBeanMock, times(1)).getTags();
        verify(serviceBeanMock, times(1)).getService();
        verify(servicesMock, times(1)).createInstance(any(CreateServiceInstanceRequest.class));

        StepVerifier.create(request)
            .expectNext()
            .expectComplete()
            .verify();
    }

    @Test
    public void testUpdateOnNullArgumentsThrowsException() {
        // given
        DefaultCloudFoundryOperations cfOperationsMock = mock(DefaultCloudFoundryOperations.class);
        ServicesOperations servicesOperations = new ServicesOperations(cfOperationsMock);

        // when
        assertThrows(NullPointerException.class, () -> servicesOperations.update(null, new ServiceBean()));
        assertThrows(NullPointerException.class, () -> servicesOperations.update("oldname", null));
    }

    @Test
    public void testRename() {
        // given
        DefaultCloudFoundryOperations cfMock = Mockito.mock(DefaultCloudFoundryOperations.class);
        Services servicesMock = Mockito.mock(Services.class);
        when(cfMock.services()).thenReturn(servicesMock);

        Mono<Void> mono = Mono.empty();
        when(servicesMock.renameInstance(any(RenameServiceInstanceRequest.class))).thenReturn(mono);

        // when
        ServicesOperations servicesOperations = new ServicesOperations(cfMock);
        Mono<Void> actualMono = servicesOperations.rename("newname", "currentname");
        actualMono.block();

        // then
        assertThat(actualMono, notNullValue());
        verify(servicesMock, times(1)).renameInstance(any(RenameServiceInstanceRequest.class));
        StepVerifier.create(actualMono)
            .expectComplete()
            .verify();
    }

    @Test
    public void testRenameOnNullNamesThrowsException() {
        // given
        DefaultCloudFoundryOperations cfOperationsMock = mock(DefaultCloudFoundryOperations.class);
        ServicesOperations servicesOperations = new ServicesOperations(cfOperationsMock);

        // when
        assertThrows(NullPointerException.class, () -> servicesOperations.rename(null, "newname"));
        assertThrows(NullPointerException.class, () -> servicesOperations.rename("oldname", null));
    }

    @Test
    public void testRemoveServiceInstance() {
        // given
        Route route1 = Route.builder().service("someservice").domain("domain")
            .host("host").id("id").space("space").build();
        Route route2 = Route.builder().service("someservice2").domain("domain")
            .host("host").id("id").space("space").build();
        Route route3 = Route.builder().service("someservice").domain("otherdomain")
            .host("host").id("id").space("space").build();

        ServiceInstance serviceInstance = ServiceInstance.builder()
            .service("someservice")
            .name("servicename")
            .applications("app1", "app2")
            .id("someid")
            .type(ServiceInstanceType.MANAGED)
            .build();

        ServiceKey serviceKey = ServiceKey.builder().id("someid").name("name").build();
        ServiceKey serviceKey2 = ServiceKey.builder().id("someid").name("name2").build();

        DefaultCloudFoundryOperations cfOperationsMock = mock(DefaultCloudFoundryOperations.class);
        Services servicesMock = mock(Services.class);
        Routes routesMock = mock(Routes.class);

        mockGetServiceInstance(cfOperationsMock, servicesMock, serviceInstance);
        mockListRoutes(cfOperationsMock, routesMock, Arrays.asList(route1, route2, route3));
        mockUnbindRoute(cfOperationsMock, servicesMock);
        mockUnbindApp(cfOperationsMock, servicesMock);
        mockListServiceKeys(cfOperationsMock, servicesMock, Arrays.asList(serviceKey, serviceKey2));
        mockDeleteServiceKey(cfOperationsMock, servicesMock);
        mockDeleteServiceInstance(cfOperationsMock, servicesMock);

        ServicesOperations servicesOperations = new ServicesOperations(cfOperationsMock);

        // when
        Mono<Void> request = servicesOperations.remove("someservice");
        request.block();

        // then
        verify(servicesMock, times(2)).getInstance(any());
        verify(routesMock, times(1)).list(any());
        verify(servicesMock, times(2)).unbindRoute(any());
        verify(servicesMock, times(2)).unbind(any());
        verify(servicesMock, times(1)).listServiceKeys(any());
        verify(servicesMock, times(2)).deleteServiceKey(any());
        StepVerifier.create(request)
            .expectNext()
            .expectComplete()
            .verify();
    }

    @Test
    public void testRemoveServiceInstanceWithoutConditions() {
        // given
        ServiceInstance serviceInstance = ServiceInstance.builder()
            .service("someservice")
            .name("servicename")
            .id("someid")
            .type(ServiceInstanceType.MANAGED)
            .build();

        DefaultCloudFoundryOperations cfOperationsMock = mock(DefaultCloudFoundryOperations.class);
        Services servicesMock = mock(Services.class);
        Routes routesMock = mock(Routes.class);

        mockGetServiceInstance(cfOperationsMock, servicesMock, serviceInstance);
        mockListRoutes(cfOperationsMock, routesMock, Collections.emptyList());
        mockUnbindRoute(cfOperationsMock, servicesMock);
        mockUnbindApp(cfOperationsMock, servicesMock);
        mockListServiceKeys(cfOperationsMock, servicesMock, Collections.emptyList());
        mockDeleteServiceKey(cfOperationsMock, servicesMock);
        mockDeleteServiceInstance(cfOperationsMock, servicesMock);

        ServicesOperations servicesOperations = new ServicesOperations(cfOperationsMock);

        // when
        Mono<Void> request = servicesOperations.remove("someservice");
        request.block();

        // then
        verify(servicesMock, times(2)).getInstance(any(GetServiceInstanceRequest.class));
        verify(servicesMock, times(1)).deleteInstance(any(DeleteServiceInstanceRequest.class));
        verify(servicesMock, times(1)).listServiceKeys(any(ListServiceKeysRequest.class));
        verify(routesMock, times(1)).list(any(ListRoutesRequest.class));
        verify(servicesMock, times(0)).deleteServiceKey(any(DeleteServiceKeyRequest.class));
        verify(servicesMock, times(0)).unbind(any(UnbindServiceInstanceRequest.class));
        verify(servicesMock, times(0)).unbindRoute(any(UnbindRouteServiceInstanceRequest.class));
        StepVerifier
            .create(request)
            .expectComplete()
            .verify();
    }

    @Test
    public void testRemoveOnNullNameThrowsException() {
        // given
        DefaultCloudFoundryOperations cfOperationsMock = mock(DefaultCloudFoundryOperations.class);
        ServicesOperations servicesOperations = new ServicesOperations(cfOperationsMock);

        // when
        assertThrows(NullPointerException.class, () -> servicesOperations.remove(null));
    }

    @Test
    public void testUnbindRoutesSucceeds() {
        // given
        Route route1 = Route.builder().service("someservice").domain("domain")
            .host("host").id("id").space("space").build();
        Route route2 = Route.builder().service("someservice2").domain("domain")
            .host("host").id("id").space("space").build();
        Route route3 = Route.builder().service("someservice").domain("otherdomain")
            .host("host").id("id").space("space").build();

        DefaultCloudFoundryOperations cfOperationsMock = mock(DefaultCloudFoundryOperations.class);
        Routes routesMock = mock(Routes.class);
        mockListRoutes(cfOperationsMock, routesMock, Arrays.asList(route1, route2, route3));

        Services servicesMock = mock(Services.class);
        mockUnbindRoute(cfOperationsMock, servicesMock);

        ServicesOperations servicesOperations = new ServicesOperations(cfOperationsMock);

        // when
        Flux<Void> requests = servicesOperations.unbindRoutes("someservice");
        requests.blockLast();

        // then
        verify(routesMock, times(1)).list(any());
        verify(servicesMock, times(2)).unbindRoute(any());
        StepVerifier.create(requests)
            .expectComplete()
            .verify();
    }

    @Test
    public void testUnbindRoutesWhenThereAreNoRoutes() {
        // given
        DefaultCloudFoundryOperations cfOperationsMock = mock(DefaultCloudFoundryOperations.class);
        Routes routesMock = mock(Routes.class);
        mockListRoutes(cfOperationsMock, routesMock, Collections.emptyList());

        Services servicesMock = mock(Services.class);
        mockUnbindRoute(cfOperationsMock, servicesMock);

        ServicesOperations servicesOperations = new ServicesOperations(cfOperationsMock);

        // when
        Flux<Void> requests = servicesOperations.unbindRoutes("someservice");
        requests.blockLast();

        // then
        verify(routesMock, times(1)).list(any());
        verify(servicesMock, times(0)).unbindRoute(any());
        StepVerifier.create(requests)
            .expectComplete()
            .verify();
    }

    @Test
    public void testUnbindRouteOnNullNameThrowsException() {
        // given
        DefaultCloudFoundryOperations cfOperationsMock = mock(DefaultCloudFoundryOperations.class);
        ServicesOperations servicesOperations = new ServicesOperations(cfOperationsMock);

        // when
        assertThrows(NullPointerException.class, () -> servicesOperations.unbindRoutes(null));
    }

    @Test
    public void testUnbindAppsSucceeds() {
        // given
        ServiceInstance serviceInstance = ServiceInstance.builder()
            .service("someservice")
            .name("servicename")
            .applications("app1", "app2")
            .id("someid")
            .type(ServiceInstanceType.USER_PROVIDED)
            .build();

        DefaultCloudFoundryOperations cfOperationsMock = mock(DefaultCloudFoundryOperations.class);
        Services servicesMock = mock(Services.class);
        mockGetServiceInstance(cfOperationsMock, servicesMock, serviceInstance);
        mockUnbindApp(cfOperationsMock, servicesMock);

        ServicesOperations servicesOperations = new ServicesOperations(cfOperationsMock);

        // when
        Flux<Void> requests = servicesOperations.unbindApps("someservice");
        requests.blockLast();

        // then
        verify(servicesMock, times(1)).getInstance(any());
        verify(servicesMock, times(2)).unbind(any());
        StepVerifier.create(requests)
            .expectComplete()
            .verify();
    }

    @Test
    public void testUnbindAppsWhenThereAreNoApps() {
        // given
        ServiceInstance serviceInstance = ServiceInstance.builder()
            .service("someservice")
            .name("servicename")
            .id("someid")
            .type(ServiceInstanceType.MANAGED)
            .build();

        DefaultCloudFoundryOperations cfOperationsMock = mock(DefaultCloudFoundryOperations.class);
        Services servicesMock = mock(Services.class);
        mockGetServiceInstance(cfOperationsMock, servicesMock, serviceInstance);
        mockUnbindApp(cfOperationsMock, servicesMock);

        ServicesOperations servicesOperations = new ServicesOperations(cfOperationsMock);
        // when
        Flux<Void> requests = servicesOperations.unbindApps("someservice");
        requests.blockLast();

        // then
        verify(servicesMock, times(1)).getInstance(any());
        verify(servicesMock, times(0)).unbind(any());
        StepVerifier.create(requests)
            .expectComplete()
            .verify();
    }

    @Test
    public void testUnbindAppsOnNullNameThrowsException() {
        // given
        DefaultCloudFoundryOperations cfOperationsMock = mock(DefaultCloudFoundryOperations.class);
        ServicesOperations servicesOperations = new ServicesOperations(cfOperationsMock);

        // when
        assertThrows(NullPointerException.class, () -> servicesOperations.unbindApps(null));
    }

    @Test
    public void testDeleteKeysSucceeds() {
        // given
        ServiceKey serviceKey = ServiceKey.builder().id("someid").name("name").build();
        ServiceKey serviceKey2 = ServiceKey.builder().id("someid").name("name2").build();

        ServiceInstance serviceInstance = ServiceInstance.builder()
            .service("someservice")
            .name("servicename")
            .id("someid")
            .type(ServiceInstanceType.MANAGED)
            .build();

        DefaultCloudFoundryOperations cfOperationsMock = mock(DefaultCloudFoundryOperations.class);
        Services servicesMock = mock(Services.class);
        mockGetServiceInstance(cfOperationsMock, servicesMock, serviceInstance);
        mockListServiceKeys(cfOperationsMock, servicesMock, Arrays.asList(serviceKey, serviceKey2));
        mockDeleteServiceKey(cfOperationsMock, servicesMock);

        ServicesOperations servicesOperations = new ServicesOperations(cfOperationsMock);

        // when
        Flux<Void> requests = servicesOperations.deleteKeys("someservice");
        requests.blockLast();

        // then
        verify(servicesMock, times(1)).getInstance(any());
        verify(servicesMock, times(1)).listServiceKeys(any());
        verify(servicesMock, times(2)).deleteServiceKey(any());
        StepVerifier.create(requests)
            .expectComplete()
            .verify();
    }

    @Test
    public void testDeleteKeysWhenThereAreNoKeys() {
        // given
        ServiceInstance serviceInstance = ServiceInstance.builder()
            .service("someservice")
            .name("servicename")
            .id("someid")
            .type(ServiceInstanceType.MANAGED)
            .build();

        DefaultCloudFoundryOperations cfOperationsMock = mock(DefaultCloudFoundryOperations.class);
        Services servicesMock = mock(Services.class);
        mockGetServiceInstance(cfOperationsMock, servicesMock, serviceInstance);
        mockListServiceKeys(cfOperationsMock, servicesMock, Collections.emptyList());
        mockDeleteServiceKey(cfOperationsMock, servicesMock);

        ServicesOperations servicesOperations = new ServicesOperations(cfOperationsMock);
        // when
        Flux<Void> requests = servicesOperations.deleteKeys("someservice");
        requests.blockLast();

        // then
        verify(servicesMock, times(1)).getInstance(any());
        verify(servicesMock, times(1)).listServiceKeys(any());
        verify(servicesMock, times(0)).deleteServiceKey(any());
        StepVerifier.create(requests)
            .expectComplete()
            .verify();
    }

    @Test
    public void testDeleteKeysWhenServiceIsUserProvided() {
        // given
        ServiceInstance serviceInstance = ServiceInstance.builder()
            .service("someservice")
            .name("servicename")
            .id("someid")
            .type(ServiceInstanceType.USER_PROVIDED)
            .build();

        DefaultCloudFoundryOperations cfOperationsMock = mock(DefaultCloudFoundryOperations.class);
        Services servicesMock = mock(Services.class);
        mockGetServiceInstance(cfOperationsMock, servicesMock, serviceInstance);
        mockListServiceKeys(cfOperationsMock, servicesMock, Collections.emptyList());
        mockDeleteServiceKey(cfOperationsMock, servicesMock);

        ServicesOperations servicesOperations = new ServicesOperations(cfOperationsMock);
        // when
        Flux<Void> requests = servicesOperations.deleteKeys("someservice");
        requests.blockLast();

        // then
        verify(servicesMock, times(1)).getInstance(any());
        verify(servicesMock, times(0)).listServiceKeys(any());
        verify(servicesMock, times(0)).deleteServiceKey(any());
        StepVerifier.create(requests)
            .expectComplete()
            .verify();
    }

    @Test
    public void testDeleteKeysOnNullNameThrowsException() {
        // given
        DefaultCloudFoundryOperations cfOperationsMock = mock(DefaultCloudFoundryOperations.class);
        ServicesOperations servicesOperations = new ServicesOperations(cfOperationsMock);

        // when
        assertThrows(NullPointerException.class, () -> servicesOperations.deleteKeys(null));
    }

    @Test
    public void testUnbindApp() {
        // given
        DefaultCloudFoundryOperations cfOperationsMock = mock(DefaultCloudFoundryOperations.class);
        Services servicesMock = mock(Services.class);
        mockUnbindApp(cfOperationsMock, servicesMock);

        ServicesOperations servicesOperations = new ServicesOperations(cfOperationsMock);
        // when
        Mono<Void> request = servicesOperations.unbindApp("someservice", "someapp");
        request.block();

        // then
        UnbindServiceInstanceRequest unbindServiceInstanceRequest = UnbindServiceInstanceRequest
            .builder()
            .applicationName("someapp")
            .serviceInstanceName("someservice")
            .build();
        verify(servicesMock, times(1)).unbind(unbindServiceInstanceRequest);
        StepVerifier.create(request)
            .expectComplete()
            .verify();
    }

    @Test
    public void testUnbindAppOnNullNameThrowsException() {
        // given
        DefaultCloudFoundryOperations cfOperationsMock = mock(DefaultCloudFoundryOperations.class);
        ServicesOperations servicesOperations = new ServicesOperations(cfOperationsMock);

        // when
        assertThrows(NullPointerException.class, () -> servicesOperations.unbindApp(null, "someapp"));
        assertThrows(NullPointerException.class, () -> servicesOperations.unbindApp("someservice", null));
    }

    private void mockListRoutes(DefaultCloudFoundryOperations cfOperationsMock, Routes routesMock,
        List<Route> routes) {
        when(cfOperationsMock.routes())
            .thenReturn(routesMock);

        when(routesMock.list(any()))
            .thenReturn(Flux.fromIterable(routes));
    }

    private void mockUnbindRoute(DefaultCloudFoundryOperations cfOperationsMock, Services servicesMock) {
        when(cfOperationsMock.services())
            .thenReturn(servicesMock);

        when(servicesMock.unbindRoute(any(UnbindRouteServiceInstanceRequest.class)))
            .thenReturn(Mono.empty());
    }

    private void mockBindRoute(DefaultCloudFoundryOperations cfOperationsMock, Services servicesMock) {
        when(cfOperationsMock.services())
            .thenReturn(servicesMock);

        when(servicesMock.bindRoute(any(BindRouteServiceInstanceRequest.class)))
            .thenReturn(Mono.empty());
    }

    private void mockGetServiceInstance(DefaultCloudFoundryOperations cfOperationsMock,
        Services servicesMock,
        ServiceInstance serviceInstance) {
        when(cfOperationsMock.services())
            .thenReturn(servicesMock);

        when(servicesMock.getInstance(any(GetServiceInstanceRequest.class)))
            .thenReturn(Mono.just(serviceInstance));
    }

    private void mockUnbindApp(DefaultCloudFoundryOperations cfOperationsMock, Services servicesMock) {
        when(cfOperationsMock.services())
            .thenReturn(servicesMock);

        when(servicesMock.unbind(any(UnbindServiceInstanceRequest.class)))
            .thenReturn(Mono.empty());
    }

    private void mockBindApp(DefaultCloudFoundryOperations cfOperationsMock, Services servicesMock) {
        when(cfOperationsMock.services())
            .thenReturn(servicesMock);

        when(servicesMock.bind(any(BindServiceInstanceRequest.class)))
            .thenReturn(Mono.empty());
    }

    private void mockListServiceKeys(DefaultCloudFoundryOperations cfOperationsMock,
        Services servicesMock,
        List<ServiceKey> serviceKeys) {
        when(cfOperationsMock.services())
            .thenReturn(servicesMock);

        when(servicesMock.listServiceKeys(any(ListServiceKeysRequest.class)))
            .thenReturn(Flux.fromIterable(serviceKeys));
    }

    private void mockDeleteServiceKey(DefaultCloudFoundryOperations cfOperationsMock, Services servicesMock) {
        when(cfOperationsMock.services())
            .thenReturn(servicesMock);

        when(servicesMock.deleteServiceKey(any(DeleteServiceKeyRequest.class)))
            .thenReturn(Mono.empty());
    }

    private void mockCreateServiceKey(DefaultCloudFoundryOperations cfOperationsMock, Services servicesMock) {
        when(cfOperationsMock.services())
            .thenReturn(servicesMock);

        when(servicesMock.createServiceKey(any(CreateServiceKeyRequest.class)))
            .thenReturn(Mono.empty());
    }

    private void mockDeleteServiceInstance(DefaultCloudFoundryOperations cfOperationsMock, Services serivcesMock) {
        when(cfOperationsMock.services())
            .thenReturn(serivcesMock);

        when(serivcesMock.deleteInstance(any(DeleteServiceInstanceRequest.class)))
            .thenReturn(Mono.empty());
    }

    private ServiceBean mockServiceBean() {
        ServiceBean serviceBean = mock(ServiceBean.class);
        when(serviceBean.getService()).thenReturn("elephantsql");
        when(serviceBean.getPlan()).thenReturn("standard");
        when(serviceBean.getTags()).thenReturn(Arrays.asList("Tag1", "Tag2"));

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("count", 5);
        params.put("upgrade", true);
        when(serviceBean.getParams()).thenReturn(params);
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
