package cloud.foundry.cli.crosscutting.mapping;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cloud.foundry.cli.crosscutting.exceptions.RefResolvingException;
import cloud.foundry.cli.crosscutting.exceptions.YamlTreeNodeNotFoundException;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;

/**
 * Test for {@link RefResolver}
 */
public class RefResolverTest {

    private static final Yaml DEFAULT_PROCESSOR = new Yaml(new SafeConstructor());
    private static final String RESOURCES_PATH = "./src/test/resources/";
    private static final String REFRESOLVER_FOLDER = "refresolver/";
    private static final String BASIC_FOLDER = "basic/";

    @Test
    public void testRemoteRef() throws FileNotFoundException {
        Object treeRoot = parseYamlFileAsTree(RESOURCES_PATH + REFRESOLVER_FOLDER + "RemoteRef.yaml");

        Object rootOfResolvedTree = RefResolver.resolveRefs(treeRoot);

        String resolvedYamlResult = convertTreeToString(rootOfResolvedTree);
        Object preresolvedTreeRoot = parseYamlFileAsTree(
                RESOURCES_PATH + REFRESOLVER_FOLDER + "ExpectedRemoteRef.yaml");
        String expectedYamlResult = convertTreeToString(preresolvedTreeRoot);
        assertThat(resolvedYamlResult, is(expectedYamlResult));
    }

    @Test
    public void testTopLevelRef() throws FileNotFoundException {
        Object treeRoot = parseYamlFileAsTree(RESOURCES_PATH + REFRESOLVER_FOLDER + "TopLevelRef.yaml");

        Object rootOfResolvedTree = RefResolver.resolveRefs(treeRoot);

        String resolvedYamlResult = convertTreeToString(rootOfResolvedTree);
        Object preresolvedTreeRoot = parseYamlFileAsTree(RESOURCES_PATH + BASIC_FOLDER + "VariousContents.yaml");
        String expectedYamlResult = convertTreeToString(preresolvedTreeRoot);
        assertThat(resolvedYamlResult, is(expectedYamlResult));
    }

    @Test
    public void testEscapeCharactersRef() throws FileNotFoundException {
        Object treeRoot = parseYamlFileAsTree(RESOURCES_PATH + REFRESOLVER_FOLDER + "EscapeCharactersRef.yaml");

        Object rootOfResolvedTree = RefResolver.resolveRefs(treeRoot);

        String resolvedYamlResult = convertTreeToString(rootOfResolvedTree);
        Object preresolvedTreeRoot = parseYamlFileAsTree(
                RESOURCES_PATH + REFRESOLVER_FOLDER + "ExpectedEscapeCharactersRef.yaml");
        String expectedYamlResult = convertTreeToString(preresolvedTreeRoot);
        assertThat(resolvedYamlResult, is(expectedYamlResult));
    }

    @Test
    public void testUrlRef() throws IOException {
        // setup http://localhost:8070/SimpleList.yaml
        WireMockServer firstServer = setupWireMockServer(8070, "SimpleList.yaml", RESOURCES_PATH + BASIC_FOLDER);

        // setup http://localhost:8090/VariousContents.yaml
        WireMockServer secondServer = setupWireMockServer(8090, "VariousContents.yaml", RESOURCES_PATH + BASIC_FOLDER);

        Object treeRoot = parseYamlFileAsTree(RESOURCES_PATH + REFRESOLVER_FOLDER + "UrlRef.yaml");

        Object rootOfResolvedTree = RefResolver.resolveRefs(treeRoot);

        String resolvedYamlResult = convertTreeToString(rootOfResolvedTree);
        Object preresolvedTreeRoot = parseYamlFileAsTree(RESOURCES_PATH + REFRESOLVER_FOLDER + "ExpectedUrlRef.yaml");
        String expectedYamlResult = convertTreeToString(preresolvedTreeRoot);
        assertThat(resolvedYamlResult, is(expectedYamlResult));

        firstServer.stop();
        secondServer.stop();
    }

    @Test
    public void testNonExistentFileRemoteRef() throws IOException {
        Object treeRoot = parseYamlFileAsTree(RESOURCES_PATH + REFRESOLVER_FOLDER + "NonExistentFileRemoteRef.yaml");

        RefResolvingException refResolvingException = assertThrows(RefResolvingException.class,
                () -> RefResolver.resolveRefs(treeRoot));
        assertThat(refResolvingException.getCause(), is(instanceOf(IOException.class)));
    }

    @Test
    public void testWronglyFormattedUrlRef() throws IOException {
        Object treeRoot = parseYamlFileAsTree(RESOURCES_PATH + REFRESOLVER_FOLDER + "WronglyFormattedUrlRef.yaml");

        RefResolvingException refResolvingException = assertThrows(RefResolvingException.class,
                () -> RefResolver.resolveRefs(treeRoot));
        assertThat(refResolvingException.getCause(), is(instanceOf(IOException.class)));
    }

    @Test
    public void testUnreachableUrlRef() throws IOException {
        Object treeRoot = parseYamlFileAsTree(RESOURCES_PATH + REFRESOLVER_FOLDER + "UnreachableUrlRef.yaml");

        assertThrows(RefResolvingException.class,
                () -> RefResolver.resolveRefs(treeRoot));
        // TODO check specific type of cause
    }

    @Test
    public void testInvalidPointerInRef() throws IOException {
        Object treeRoot = parseYamlFileAsTree(RESOURCES_PATH + REFRESOLVER_FOLDER + "InvalidPointerInRef.yaml");

        RefResolvingException refResolvingException = assertThrows(RefResolvingException.class,
                () -> RefResolver.resolveRefs(treeRoot));
        assertThat(refResolvingException.getCause(), is(instanceOf(YamlTreeNodeNotFoundException.class)));
    }

    private String convertTreeToString(Object tree) {
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

    private WireMockServer setupWireMockServer(int port, String filename, String path)
            throws FileNotFoundException {
        WireMockConfiguration serverConfig = WireMockConfiguration.options().port(port);
        WireMockServer server = new WireMockServer(serverConfig);

        server.stubFor(get(urlEqualTo("/" + filename))
                .willReturn(aResponse()
                        .withBody(readFileContent(path + filename))));

        server.start();
        return server;
    }

}
