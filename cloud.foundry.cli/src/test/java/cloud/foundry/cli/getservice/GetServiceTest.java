package cloud.foundry.cli.getservice;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import cloud.foundry.cli.getservice.logic.GetService;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.services.ServiceInstanceSummary;
import org.cloudfoundry.operations.services.ServiceInstanceType;
import org.cloudfoundry.operations.services.Services;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.LinkedList;
import java.util.List;



public class GetServiceTest {

    @Test
    public void testGetServicesWithMockData() {
        //given
        ServiceInstanceSummary serviceInstanceSummary = createMockServiceInstanceSummary();
        DefaultCloudFoundryOperations cfMock = createMockDefaultCloudFoundryOperations(serviceInstanceSummary);
        //when
        GetService getService = new GetService(cfMock);
        String s = getService.getServices();
        //then
        assertThat(s, is(
                "- !!cloud.foundry.cli.getservice.logic.ServiceInstanceSummaryBean\n" +
                "  applications: [test-flask]\n" +
                "  id: serviceId\n" +
                "  lastOperation: lastOp\n" +
                "  name: serviceName\n" +
                "  plan: standardPlan\n" +
                "  service: service\n" +
                "  tags: [tag]\n" +
                "  type: MANAGED\n"
        ));
    }

    @Test
    public void testGetServicesWithEmptyMockData() {
        //given
        DefaultCloudFoundryOperations cfMock = createMockDefaultCloudFoundryOperations(null);
        //when
        GetService getService = new GetService(cfMock);
        String s = getService.getServices();
        //then
        assertEquals(s, "[]\n");
    }

    private DefaultCloudFoundryOperations createMockDefaultCloudFoundryOperations
            (ServiceInstanceSummary serviceInstanceSummary) {
        DefaultCloudFoundryOperations cfMock = Mockito.mock(DefaultCloudFoundryOperations.class);
        Services servicesMock = Mockito.mock(Services.class);
        Flux<ServiceInstanceSummary> flux = Mockito.mock(Flux.class);
        Mono<List<ServiceInstanceSummary>> mono = Mockito.mock(Mono.class);
        List<ServiceInstanceSummary> list = new LinkedList<ServiceInstanceSummary>();
        if (serviceInstanceSummary != null) {
            list.add(serviceInstanceSummary);
        }
        Mockito.when(cfMock.services()).thenReturn(servicesMock);
        Mockito.when(servicesMock.listInstances()).thenReturn(flux);
        Mockito.when(flux.collectList()).thenReturn(mono);
        Mockito.when(mono.block()).thenReturn(list);
        return cfMock;
    }

    private ServiceInstanceSummary createMockServiceInstanceSummary() {
        ServiceInstanceSummary serviceInstanceSummary = Mockito.mock(ServiceInstanceSummary.class);
        LinkedList<String> apps = new LinkedList<>();
        apps.add("test-flask");
        Mockito.when(serviceInstanceSummary.getApplications()).thenReturn(apps);
        Mockito.when(serviceInstanceSummary.getId()).thenReturn("serviceId");
        Mockito.when(serviceInstanceSummary.getLastOperation()).thenReturn("lastOp");
        Mockito.when(serviceInstanceSummary.getName()).thenReturn("serviceName");
        Mockito.when(serviceInstanceSummary.getPlan()).thenReturn("standardPlan");
        Mockito.when(serviceInstanceSummary.getService()).thenReturn("service");
        LinkedList<String> tags = new LinkedList<>();
        tags.add("tag");
        Mockito.when(serviceInstanceSummary.getTags()).thenReturn(tags);
        Mockito.when(serviceInstanceSummary.getType()).thenReturn(ServiceInstanceType.MANAGED);
        return serviceInstanceSummary;
    }
}