package cloud.foundry.cli.crosscutting.mapping;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cloud.foundry.cli.crosscutting.exceptions.InvalidFileTypeException;
import cloud.foundry.cli.crosscutting.mapping.FileUtils;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.hc.core5.http.ProtocolException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Test for {@link FileUtils}
 */
public class FileUtilsTest {

    private static String RESOURCE_PATH = "./src/test/resources/basic/";
    private static String SIMPLE_LIST_FILE_PATH = "SimpleList.yaml";

    @Test
    public void testOpenLocalOrRemoteFileWithLocalFile(@TempDir Path tempDir) throws IOException {
        // given
        String expectedYaml =   "- first" + System.lineSeparator()  +
                "- second" + System.lineSeparator()  +
                "- third" + System.lineSeparator() ;
        File file = createTempFile(tempDir, "files", "SimpleList.yaml", expectedYaml);

        // when
        InputStream yamlInputStream = FileUtils.openLocalOrRemoteFile(file.getPath());

        // then
        assertThat(readFile(yamlInputStream), is(expectedYaml));
        yamlInputStream.close();
    }

    @Test
    public void testOpenLocalOrRemoteFileWithRemoteFile() throws IOException {
        // given
        // http://localhost:8070/SimpleList.yaml
        WireMockServer server = MockServerBuilder.builder()
                .addRoute("SimpleList.yaml", readFile(RESOURCE_PATH + SIMPLE_LIST_FILE_PATH))
                .build();
        server.start();

        // when
        InputStream yamlInputStream = FileUtils.openLocalOrRemoteFile(server.url("SimpleList.yaml"));

        // then
        assertThat(readFile(yamlInputStream), is(readFile(RESOURCE_PATH + SIMPLE_LIST_FILE_PATH)));

        // cleanup
        server.stop();
        yamlInputStream.close();
    }

    @Test
    public void testOpenLocalFileOnEmptyFileReturnsEmptyString(@TempDir Path tempDir)
            throws IOException {
        // given
        File file = createTempFile(tempDir, "", "Empty.yaml", "");

        // when
        InputStream yamlInputStream = FileUtils.openLocalFile(file.getPath());

        // then
        assertThat(readFile(yamlInputStream), is(""));

        // cleanup
        yamlInputStream.close();
    }

    @Test
    public void testOpenLocalFileReturnsFileContent(@TempDir Path tempDir)
            throws IOException {
        // given
        String expectedYaml =   "- first" + System.lineSeparator()  +
                "- second" + System.lineSeparator()  +
                "- third" + System.lineSeparator() ;
        File file = createTempFile(tempDir, "files", "SimpleList.yaml", expectedYaml);

        // when
        InputStream yamlInputStream = FileUtils.openLocalFile(file.getPath());

        // then
        assertThat(readFile(yamlInputStream), is(expectedYaml));

        // cleanup
        yamlInputStream.close();
    }

    @Test
    public void testOpenLocalFileOnDirectoryWithCommaSucceeds(@TempDir Path tempDir)
            throws IOException {
        // given
        String expectedYaml =   "- first" + System.lineSeparator() +
                "- second" + System.lineSeparator() +
                "- third" + System.lineSeparator();
        File file = createTempFile(tempDir, "comma.path", "Extension.YMl", expectedYaml);

        // when
        InputStream yamlInputStream = FileUtils.openLocalFile(file.getPath());

        // then
        assertThat(readFile(yamlInputStream), is(expectedYaml));

        // cleanup
        yamlInputStream.close();
    }

    @Test
    public void testOpenLocalFileOnValidFileTypesSucceeds(@TempDir Path tempDir)
            throws IOException {
        // given
        String expectedYaml =   "- first" + System.lineSeparator()  +
                "- second" + System.lineSeparator()  +
                "- third" + System.lineSeparator() ;
        File fileYaml = createTempFile(tempDir, "","Extension.YMl", expectedYaml);
        File fileYml = createTempFile(tempDir, "","Extension.YAML", expectedYaml);

        // when
        InputStream yamlInputStream = FileUtils.openLocalFile(fileYaml.getPath());
        InputStream ymlInputStream = FileUtils.openLocalFile(fileYml.getPath());

        // then
        assertThat(readFile(yamlInputStream), is(expectedYaml));
        assertThat(readFile(ymlInputStream), is(expectedYaml));

        // cleanup
        yamlInputStream.close();
        ymlInputStream.close();
    }

    @Test
    public void testOpenLocalFileOnCaseInsensitiveFileExtensionSucceeds(@TempDir Path tempDir)
            throws IOException {
        // given
        File file = createTempFile(tempDir, "", "Empty.YMl", "");

        // when
        InputStream yamlInputStream = FileUtils.openLocalFile(file.getPath());

        // then
        assertThat(readFile(yamlInputStream), is(""));

        // cleanup
        yamlInputStream.close();
    }

    @Test
    public void testOpenLocalFileOnMissingFileThrowsException() {
        // when and  then
        assertThrows(FileNotFoundException.class,
                () -> FileUtils.openLocalFile(RESOURCE_PATH + "Missing.yaml"));
    }

    @Test
    public void testOpenLocalFileOnMissingFilePathThrowsException() {
        // when and  then
        assertThrows(FileNotFoundException.class,
                () -> FileUtils.openLocalFile(RESOURCE_PATH + "no/path/Missing.yaml"));
    }

    @Test
    public void testOpenLocalFileOnInvalidFileExtensionThrowsException() {
        // when and  then
        assertThrows(InvalidFileTypeException.class,
                () -> FileUtils.openLocalFile(RESOURCE_PATH + "InvalidFileExtension.txt"));
    }

    @Test
    public void testOpenLocalFileOnFileWithoutExtensionThrowsException() {
        // when and  then
        assertThrows(InvalidFileTypeException.class,
                () -> FileUtils.openLocalFile(RESOURCE_PATH + "NoFileExtension"));
    }

    private File createTempFile(Path tempDir, String directories, String filename, String content)
            throws IOException {
        Path path = tempDir.resolve(directories);

        File tempFile = Files
                .createDirectories(path)
                .resolve(filename)
                .toFile();

        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            IOUtils.write(content, out, Charset.defaultCharset());
        }
        return tempFile;
    }

    @Test
    public void testOpenRemoteFileEmptySucceeds() throws IOException {
        // given
        // http://localhost:XXXXX/Empty.yaml
        WireMockServer server = MockServerBuilder.builder()
                .addRoute("Empty.yaml", "")
                .build();
        server.start();

        // when
        InputStream yamlInputStream = FileUtils.openRemoteFile(server.url("Empty.yaml"));

        // then
        assertThat(readFile(yamlInputStream), is(""));

        //Cleanup
        server.stop();
        yamlInputStream.close();
    }

    @Test
    public void testOpenRemoteFileWithContentSucceeds() throws IOException {
        // given
        // http://localhost:XXXXX/SimpleList.yaml
        WireMockServer server = MockServerBuilder.builder()
                .addRoute("SimpleList.yaml", readFile(RESOURCE_PATH + SIMPLE_LIST_FILE_PATH))
                .build();
        server.start();

        // when
        InputStream yamlInputStream = FileUtils.openRemoteFile(server.url("SimpleList.yaml"));

        // then
        assertThat(readFile(yamlInputStream), is(readFile(RESOURCE_PATH + SIMPLE_LIST_FILE_PATH)));

        //Cleanup
        server.stop();
        yamlInputStream.close();
    }

    @Test
    public void testOpenRemoteFileWithPathSucceeds() throws IOException {
        // given
        // http://localhost:XXXX/resources/SimpleList.yaml
        WireMockServer server = MockServerBuilder.builder()
                .addRoute("resources/SimpleList.yaml", readFile(RESOURCE_PATH + SIMPLE_LIST_FILE_PATH))
                .build();
        server.start();

        // when
        InputStream yamlInputStream = FileUtils.openRemoteFile(server.url("resources/SimpleList.yaml"));

        // then
        assertThat(readFile(yamlInputStream), is(readFile(RESOURCE_PATH + SIMPLE_LIST_FILE_PATH)));

        //Cleanup
        server.stop();
        yamlInputStream.close();
    }

    @Test
    public void testOpenRemoteFileOnCaseInsensitiveFileExtensionSucceeds() throws IOException {
        // given
        // http://localhost:XXXX/resources/SimpleList.yAMl
        // http://localhost:XXXX/resources/SimpleList.ymL
        WireMockServer server = MockServerBuilder.builder()
                .addRoute("resources/SimpleList.yAMl", readFile(RESOURCE_PATH + SIMPLE_LIST_FILE_PATH))
                .addRoute("resources/SimpleList.ymL", readFile(RESOURCE_PATH + SIMPLE_LIST_FILE_PATH))
                .build();
        server.start();

        // when
        InputStream yamlInputStream = FileUtils.openRemoteFile(server.url("resources/SimpleList.yAMl"));
        InputStream ymlInputStream = FileUtils.openRemoteFile(server.url("resources/SimpleList.ymL"));

        // then
        assertThat(readFile(yamlInputStream), is(readFile(RESOURCE_PATH + SIMPLE_LIST_FILE_PATH)));
        assertThat(readFile(ymlInputStream), is(readFile(RESOURCE_PATH + SIMPLE_LIST_FILE_PATH)));

        //Cleanup
        server.stop();
        yamlInputStream.close();
        ymlInputStream.close();
    }

    @Test
    public void testOpenRemoteFileOnValidFileTypesSucceeds() throws IOException {
        // given
        // http://localhost:XXXX/SimpleList.yml
        WireMockServer server = MockServerBuilder.builder()
                .addRoute("SimpleList.yml", readFile(RESOURCE_PATH + SIMPLE_LIST_FILE_PATH))
                .build();
        server.start();

        // when
        InputStream yamlInputStream = FileUtils.openRemoteFile(server.url("SimpleList.yml"));

        // then
        assertThat(readFile(yamlInputStream), is(readFile(RESOURCE_PATH + SIMPLE_LIST_FILE_PATH)));

        //Cleanup
        server.stop();
        yamlInputStream.close();
    }

    @Test
    public void testOpenRemoteFileOnPathWithoutFileExtensionSucceeds() throws IOException {
        // given
        // http://localhost:XXXX/resources/SimpleList
        WireMockServer server = MockServerBuilder.builder()
                .addRoute("SimpleList", readFile(RESOURCE_PATH + SIMPLE_LIST_FILE_PATH))
                .build();
        server.start();

        // when
        InputStream yamlInputStream = FileUtils.openRemoteFile(server.url("SimpleList"));

        // then
        assertThat(readFile(yamlInputStream), is(readFile(RESOURCE_PATH + SIMPLE_LIST_FILE_PATH)));

        //Cleanup
        server.stop();
        yamlInputStream.close();
    }

    @Test
    public void testOpenRemoteFileOnPathIncludingPointSucceeds() throws IOException {
        // given
        // http://localhost:XXXX/resources.path/SimpleList.yaml
        WireMockServer server = MockServerBuilder.builder()
                .addRoute("resources.path/SimpleList.yaml", readFile(RESOURCE_PATH + SIMPLE_LIST_FILE_PATH))
                .build();
        server.start();

        // when
        InputStream yamlInputStream = FileUtils.openRemoteFile(server.url("resources.path/SimpleList.yaml"));

        // then
        assertThat(readFile(yamlInputStream), is(readFile(RESOURCE_PATH + SIMPLE_LIST_FILE_PATH)));

        //Cleanup
        server.stop();
        yamlInputStream.close();
    }

    @Test
    public void testOpenRemoteFileOnNonExistingHostThrowsException() {
        // given
        // http://localhost:XXXX/SimpleList.yaml
        WireMockServer server = MockServerBuilder.builder()
                .build();
        server.start();

        // when
        Exception exception = assertThrows(Exception.class,
                () -> FileUtils.openRemoteFile(server.url("").replace("8070", "9999")));

        //Cleanup
        server.stop();
    }

    @Test
    public void testOpenRemoteFileOnMissingFileThrowsException() {
        // given
        // http://localhost:XXXX
        WireMockServer server = MockServerBuilder.builder()
                .build();
        server.start();

        // when
        Exception exception = assertThrows(Exception.class,
                () -> FileUtils.openRemoteFile(server.url("Missing.yml")));

        //Cleanup
        server.stop();
    }

    @Test
    public void testOpenRemoteFileOnMissingFilePathThrowsException() throws IOException {
        // given
        // http://localhost:XXXX/resources/SimpleList.yaml
        WireMockServer server = MockServerBuilder.builder()
                .addRoute("resources/SimpleList.yaml", readFile(RESOURCE_PATH + SIMPLE_LIST_FILE_PATH))
                .build();
        server.start();

        // when
        Exception exception = assertThrows(Exception.class,
                () -> FileUtils.openRemoteFile(server.url("resrcs/SimpleList.yml")));

        //Cleanup
        server.stop();
    }

    @Test
    public void testOpenRemoteFileOnInvalidFileExtensionThrowsException() {
        // given
        // http://localhost:XXXX/SimpleList.txt
        WireMockServer server = MockServerBuilder.builder()
                .addRoute("SimpleList.txt")
                .build();
        server.start();

        // when
        InvalidFileTypeException exception = assertThrows(InvalidFileTypeException.class,
                () -> FileUtils.openRemoteFile(server.url("SimpleList.txt")));
        assertThat(exception.getMessage(), containsString("invalid file extension"));

        //Cleanup
        server.stop();
    }

    private static class MockServerBuilder {

        WireMockServer wireMockServer;

        String content;
        List<MappingBuilder> routes;

        int port;

        private MockServerBuilder() {
            WireMockConfiguration serverConfig = WireMockConfiguration.options().dynamicPort();
            wireMockServer = new WireMockServer(serverConfig);
            content = "";
            routes = new LinkedList<>();
        }

        public static MockServerBuilder builder() {
            return new MockServerBuilder();
        }

        public MockServerBuilder setPort(int port) {
            this.port = port;
            return this;
        }

        public MockServerBuilder addRoute(String path, String content) {
            routes.add(MockRoute
                    .builder()
                    .setPath(path)
                    .setContent(content)
                    .build());
            return this;
        }

        public MockServerBuilder addRoute(String path) {
            routes.add(MockRoute
                    .builder()
                    .setPath(path)
                    .build());
            return this;
        }

        public MockServerBuilder addRoute(MockRoute route) {
            routes.add(route.build());
            return this;
        }

        public WireMockServer build() {
            for (MappingBuilder route : routes) {
                wireMockServer.stubFor(route);
            }
            return wireMockServer;
        }
    }

    private static class MockRoute {

        String path;
        String content;
        int status;
        Map<String, String> header;

        private MockRoute() {
            path = "";
            content = "";
            status = 200;
            header = new HashMap<>();
        }

        public static MockRoute builder() {
            return new MockRoute();
        }

        public MockRoute setPath(String path) {
            this.path = path;
            return this;
        }

        public MockRoute setContent(String content) {
            this.content = content;
            return this;
        }

        public MockRoute addHeader(String key, String value) {
            header.put(key, value);
            return this;
        }

        public MockRoute setStatus(int status) {
            this.status = status;
            return this;
        }

        public MappingBuilder build() {
            MappingBuilder mappingBuilder = get(urlEqualTo("/" + path));
            ResponseDefinitionBuilder respBuilder = aResponse();

            for (Map.Entry<String, String> headEntry : header.entrySet()) {
                respBuilder.withHeader(headEntry.getKey(), headEntry.getValue());
            }

            respBuilder.withBody(content);
            respBuilder.withStatus(status);
            return mappingBuilder.willReturn(respBuilder);
        }
    }

    private String readFile(String filename) throws IOException {
        return IOUtils.toString(new FileInputStream(new File(filename)), Charset.defaultCharset());
    }

    private String readFile(InputStream inputStream) throws IOException {
        return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
    }
}
