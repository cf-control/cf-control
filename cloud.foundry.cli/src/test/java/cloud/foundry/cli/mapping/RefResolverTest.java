package cloud.foundry.cli.mapping;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class RefResolverTest {

    private static final Yaml DEFAULT_PROCESSOR = new Yaml();
    private static final String RESOURCES_PATH = "./src/test/resources/refresolver/";

    @Test
    @Timeout(value = 1000, unit = TimeUnit.MILLISECONDS)
    public void testLocalRef() throws FileNotFoundException {
        assertSemanticEquality("ExpectedLocalRef.yaml", "LocalRef.yaml");
    }

    @Test
    @Timeout(value = 1000, unit = TimeUnit.MILLISECONDS)
    public void testRemoteRef() throws FileNotFoundException {
        assertSemanticEquality("ExpectedRemoteRef.yaml", "RemoteRef.yaml");
    }

    @Test
    @Timeout(value = 1000, unit = TimeUnit.MILLISECONDS)
    public void testTopLevelRef() throws FileNotFoundException {
        assertSemanticEquality("referred/VariousContents.yaml", "TopLevelRef.yaml");
    }

    @Test
    @Timeout(value = 1000, unit = TimeUnit.MILLISECONDS)
    public void testEscapeCharactersRef() throws FileNotFoundException {
        assertSemanticEquality("ExpectedEscapeCharactersRef.yaml", "EscapeCharactersRef.yaml");
    }

    @Test
    @Timeout(value = 10000, unit = TimeUnit.MILLISECONDS)
    public void testUrlRef() throws IOException {
        // http://localhost:8070/resources/SimpleList.yaml will have the contents of SimpleList.yaml as response body
        WireMockServer firstServer = setupWireMockServer(8070, "SimpleList.yaml");

        // http://localhost:8090/resources/VariousContents.yaml will have the contents of VariousContents.yaml as response body
        WireMockServer secondServer = setupWireMockServer(8090, "VariousContents.yaml");

        assertSemanticEquality("ExpectedUrlRef.yaml", "UrlRef.yaml");

        firstServer.stop();
        secondServer.stop();
    }

    @Test
    @Timeout(value = 1000, unit = TimeUnit.MILLISECONDS)
    public void testNonExistentFileRemoteRef() throws IOException {
        Object treeRoot = parseYamlFileAsTree(RESOURCES_PATH + "NonExistentFileRemoteRef.yaml");

        Exception thrownException = assertThrows(
                Exception.class, // TODO replace by a custom exception class
                () -> {
                    RefResolver.resolve(treeRoot, DEFAULT_PROCESSOR);
                }
        );

        // TODO check exception contents
    }

    @Test
    @Timeout(value = 1000, unit = TimeUnit.MILLISECONDS)
    public void testWronglyFormattedUrlRef() throws IOException {
        Object treeRoot = parseYamlFileAsTree(RESOURCES_PATH + "WronglyFormattedUrlRef.yaml");

        Exception thrownException = assertThrows(
                Exception.class, // TODO replace by a custom exception class
                () -> {
                    RefResolver.resolve(treeRoot, DEFAULT_PROCESSOR);
                }
        );

        // TODO check exception contents
    }

    @Test
    @Timeout(value = 1000, unit = TimeUnit.MILLISECONDS)
    public void testUnreachableUrlRef() throws IOException {
        Object treeRoot = parseYamlFileAsTree(RESOURCES_PATH + "UnreachableUrlRef.yaml");

        Exception thrownException = assertThrows(
                Exception.class, // TODO replace by a custom exception class
                () -> {
                    RefResolver.resolve(treeRoot, DEFAULT_PROCESSOR);
                }
        );

        // TODO check exception contents
    }

    @Test
    @Timeout(value = 1000, unit = TimeUnit.MILLISECONDS)
    public void testNoYamlFileRef() throws IOException {
        Object treeRoot = parseYamlFileAsTree(RESOURCES_PATH + "NoYamlFileRef.yaml");

        Exception thrownException = assertThrows(
                Exception.class, // TODO replace by a custom exception class
                () -> {
                    RefResolver.resolve(treeRoot, DEFAULT_PROCESSOR);
                }
        );

        // TODO check exception contents
    }

    @Test
    @Timeout(value = 1000, unit = TimeUnit.MILLISECONDS)
    public void testEmptyFileRef() throws IOException {
        Object treeRoot = parseYamlFileAsTree(RESOURCES_PATH + "EmptyFileRef.yaml");

        Exception thrownException = assertThrows(
                Exception.class, // TODO replace by a custom exception class
                () -> {
                    RefResolver.resolve(treeRoot, DEFAULT_PROCESSOR);
                }
        );

        // TODO check exception contents
    }

    @Test
    @Timeout(value = 1000, unit = TimeUnit.MILLISECONDS)
    public void testInvalidPointerInRef() throws IOException {
        Object treeRoot = parseYamlFileAsTree(RESOURCES_PATH + "InvalidPointerInRef.yaml");

        Exception thrownException = assertThrows(
                Exception.class, // TODO replace by a custom exception class
                () -> {
                    RefResolver.resolve(treeRoot, DEFAULT_PROCESSOR);
                }
        );

        // TODO check exception contents
    }

    private void assertSemanticEquality(String preresolvedYamlFile, String yamlFileContainingRef)
            throws FileNotFoundException {
        Object treeRoot = parseYamlFileAsTree(RESOURCES_PATH + yamlFileContainingRef);

        Object rootOfResolvedTree = RefResolver.resolve(treeRoot, DEFAULT_PROCESSOR);
        String resolvedYamlResult = convertTreeToYaml(rootOfResolvedTree);

        Object preresolvedTreeRoot = parseYamlFileAsTree(RESOURCES_PATH + preresolvedYamlFile);
        String expectedYamlResult = convertTreeToYaml(preresolvedTreeRoot);
        Assertions.assertEquals(expectedYamlResult, resolvedYamlResult);
    }

    private String convertTreeToYaml(Object tree) {
        return DEFAULT_PROCESSOR.dump(tree);
    }

    private Object parseYamlFileAsTree(String filePath) throws FileNotFoundException {
        FileInputStream inputStream = new FileInputStream(new File(filePath));
        return DEFAULT_PROCESSOR.load(inputStream);
    }

    private String readFileContent(String filename) throws FileNotFoundException {
        File file = new File(filename);
        Scanner scanner = new Scanner(file);
        return scanner.useDelimiter("\\Z").next();
    }

    private WireMockServer setupWireMockServer(int port, String fileAsResponseBody)
            throws FileNotFoundException {
        WireMockConfiguration serverConfig = WireMockConfiguration.options().port(port);
        WireMockServer server = new WireMockServer(serverConfig);
        server.start();

        String pathToFileAsResponseBody = RESOURCES_PATH + "referred/" + fileAsResponseBody;

        server.stubFor(get(urlEqualTo("/resources/" + fileAsResponseBody))
                .willReturn(aResponse()
                        .withBody(readFileContent(pathToFileAsResponseBody))));

        return server;
    }

}
