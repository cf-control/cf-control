package cloud.foundry.cli.getservice;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

import cloud.foundry.cli.getservice.logic.GetService;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationHealthCheck;
import org.cloudfoundry.operations.applications.ApplicationManifest;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.applications.Applications;
import org.cloudfoundry.operations.applications.GetApplicationManifestRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class GetServiceGetApplicationsTest {

    /**
     * Creates and configures mock object for CF API client
     * We only have to patch it so far as that it will return our own list of ApplicationSummary instances
     * @param appSummaries List of ApplicationSummary objects that the mock object shall return
     * @return mock {@link DefaultCloudFoundryOperations} object
     */
    private DefaultCloudFoundryOperations createMockCloudFoundryOperations(List<ApplicationSummary> appSummaries,
                                                                           List<ApplicationManifest> manifests) {
        // the way the ApplicationSummary list is fetched is rather complex thanks to this Mono stuff
        // we need to create _four_ mock objects of which one returns another on a specific method call
        // we do this in reverse order, as it's easier that way

        // first, we create the mock object we want to return later on
        // it's configured after creating all the other mock objects
        DefaultCloudFoundryOperations cfMock = Mockito.mock(DefaultCloudFoundryOperations.class);

        // first, let's have the fun of creating the three objects needed to list the applications
        Mono<List<ApplicationSummary>> summaryListMono = Mockito.mock(Mono.class);
        Mockito.when(summaryListMono.block()).thenReturn(appSummaries);

        Flux<ApplicationSummary> flux = Mockito.mock(Flux.class);
        Mockito.when(flux.collectList()).thenReturn(summaryListMono);

        Applications applicationsMock = Mockito.mock(Applications.class);
        Mockito.when(applicationsMock.list()).thenReturn(flux);

        // now, let's have the same fun for the manifests, which are queried in a different way
        // luckily, we already have the applicationsMock, which we also need to hook on here
        // unfortunately, the method matches a string on some map, so we have to rebuild something similar
        // the following lambda construct does exactly that: search for the right manifest by name in the lsit we've
        // been passed, and return that if possible (or otherwise throw some exception)
        // TODO: check which exception to throw
        // this.cfOperations.applications().getApplicationManifest(manifestRequest).block();
        Mockito.when(applicationsMock.getApplicationManifest(any(GetApplicationManifestRequest.class)))
                .thenAnswer((Answer<Mono<ApplicationManifest>>) invocation -> {
                    GetApplicationManifestRequest request = invocation.getArgument(0);
                    String name = request.getName();

                    // simple linear search; this is not about performance, really
                    for (ApplicationManifest manifest : manifests) {
                        if (manifest.getName() == name) {
                            // we need to return a mock object that supports the .block()
                            Mono<ApplicationManifest> applicationManifestMono = Mockito.mock(Mono.class);

                            Mockito.when(applicationManifestMono.block()).thenReturn(manifest);

                            return applicationManifestMono;
                        }
                    }

                    throw new RuntimeException("fixme");
                });


        Mockito.when(cfMock.applications()).thenReturn(applicationsMock);

        return cfMock;
    }

    /**
     * Creates an {@link ApplicationManifest} with partially random data to increase test reliability.
     * @return application manifest containing test data
     */
    // FIXME: randomize some data
    private ApplicationManifest createMockApplicationManifest() {
        Map<String, String> envVars = new HashMap<>();
        envVars.put("key", "value");

        // note: here we have to insert a path, too!
        // another note: routes and hosts cannot both be set, so we settle with hosts
        // yet another note: docker image and buildpack cannot both be set, so we settle with buildpack
        ApplicationManifest manifest = ApplicationManifest.builder()
                .buildpack("test_buildpack")
                .command("test command")
                .disk(1234)
                .domains("example.test", "some.more.test")
                .environmentVariables(envVars)
                .healthCheckHttpEndpoint("http://healthcheck.local")
                .healthCheckType(ApplicationHealthCheck.HTTP)
                .hosts("testhostalpha", "testhost17")
                .instances(42)
                .memory(Integer.MAX_VALUE)
                .name("notyetrandomname")
                .noHostname(false)
                .noRoute(false)
                .path(Paths.get("/test/uri"))
                .randomRoute(true)
                .services("serviceomega")
                .stack("nope")
                .timeout(987654321)
                .build();

        return manifest;
    }

    /**
     * Creates an {@link ApplicationSummary} from an {@link ApplicationManifest} for testing purposes.
     * @return application summary
     */
    // FIXME: randomize some data
    private ApplicationSummary createMockApplicationSummary(ApplicationManifest manifest) {
        // we basically only need the manifest as we need to keep the names the same
        // however, the summary builder complains if a few more attributes aren't set either, so we have to set more
        // than just the name
        ApplicationSummary summary = ApplicationSummary.builder()
                .name(manifest.getName())
                .diskQuota(100)
                .id("summary_id")
                .instances(manifest.getInstances())
                .memoryLimit(manifest.getMemory())
                .requestedState("SOMESTATE")
                .runningInstances(1)
                .build();
        return summary;
    }

    @Test
    public void testGetApplicationsWithEmptyMockData() {
        // prepare mock CF API client with an empty applications list
        DefaultCloudFoundryOperations cfMock = createMockCloudFoundryOperations(new ArrayList<>(), new ArrayList<>());

        // forge YAML document
        GetService getService = new GetService(cfMock);
        String yamlDoc = getService.getApplications();

        // check if it's really empty
        assertEquals(yamlDoc, "[\n  ]\n");
    }

    @Test
    public void testGetApplicationsWithMockData() {
        // create a mock CF API client
        // first, we need to prepare some ApplicationSummary and ApplicationManifest
        // (we're fine with one of both for now)
        // those are then used to create a CF mock API object, which will be able to return those then the right way
        ApplicationManifest manifest = createMockApplicationManifest();
        ApplicationSummary summary = createMockApplicationSummary(manifest);

        List<ApplicationManifest> manifests = new ArrayList<>();
        manifests.add(manifest);

        List<ApplicationSummary> summaries = new ArrayList<>();
        summaries.add(summary);

        // now, let's create the mock object from that list
        DefaultCloudFoundryOperations cfMock = createMockCloudFoundryOperations(summaries, manifests);

        // now, we can generate a YAML doc for our ApplicationSummary
        GetService getService = new GetService(cfMock);
        String yamlDoc = getService.getApplications();

        // ... and make sure it contains exactly what we'd expect
        assertThat(yamlDoc, is(
                "- manifest:\n" +
                "    buildpack: test_buildpack\n" +
                "    command: test command\n" +
                "    disk: 1234\n" +
                "    docker: null\n" +
                "    domains:\n" +
                "    - example.test\n" +
                "    - some.more.test\n" +
                "    environmentVariables:\n" +
                "      key: value\n" +
                "    healthCheckHttpEndpoint: http://healthcheck.local\n" +
                "    healthCheckType: HTTP\n" +
                "    hosts:\n" +
                "    - testhostalpha\n" +
                "    - testhost17\n" +
                "    instances: 42\n" +
                "    memory: 2147483647\n" +
                "    name: notyetrandomname\n" +
                "    noHostname: false\n" +
                "    noRoute: false\n" +
                "    randomRoute: true\n" +
                "    routePath: null\n" +
                "    routes: null\n" +
                "    services:\n" +
                "    - serviceomega\n" +
                "    stack: nope\n" +
                "    timeout: 987654321\n" +
                "  path: /test/uri\n"
        ));
    }


}