package cloud.foundry.cli.crosscutting.util;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cloud.foundry.cli.crosscutting.exceptions.InvalidFileTypeException;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.HttpResponseException;
import org.apache.hc.core5.http.ProtocolException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;

public class FileUtilsTest {

    private static String RESOURCE_PATH = "./src/test/resources/basic/";
    private static String SIMPLE_LIST_FILE_PATH = "SimpleList.yaml";

    @Test
    public void testReadLocalFileOnEmptyFileReturnsEmptyString(@TempDir Path tempDir)
            throws IOException {
        //Arrange
        File file = createTempFile(tempDir, "", "Empty.yaml", "");

        //Act
        String actualYaml = FileUtils.readLocalFile(file.getPath());

        //Verify
        assertThat(actualYaml, is(""));
    }

    @Test
    public void testReadLocalFileReturnsFileContent(@TempDir Path tempDir)
            throws IOException {
        //Arrange
        String expectedYaml =   "- first" + System.lineSeparator()  +
                "- second" + System.lineSeparator()  +
                "- third" + System.lineSeparator() ;
        File file = createTempFile(tempDir, "files", "SimpleList.yaml", expectedYaml);

        //Act
        String actualYaml = FileUtils.readLocalFile(file.getPath());

        //Verify
        assertThat(actualYaml, is(expectedYaml));
    }

    @Test
    public void testReadLocalFileOnDirectoryWithCommaSucceeds(@TempDir Path tempDir)
            throws IOException {
        //Arrange
        String expectedYaml =   "- first" + System.lineSeparator() +
                "- second" + System.lineSeparator() +
                "- third" + System.lineSeparator();
        File file = createTempFile(tempDir, "comma.path", "Extension.YMl", expectedYaml);

        //Act
        String actualYaml = FileUtils.readLocalFile(file.getPath());

        //Verify
        assertThat(actualYaml, is(expectedYaml));
    }

    @Test
    public void testReadLocalFileOnValidFileTypesSucceeds(@TempDir Path tempDir)
            throws IOException {
        //Arrange
        String expectedYaml =   "- first" + System.lineSeparator()  +
                "- second" + System.lineSeparator()  +
                "- third" + System.lineSeparator() ;
        File fileYaml = createTempFile(tempDir, "","Extension.YMl", expectedYaml);
        File fileYml = createTempFile(tempDir, "","Extension.YAML", expectedYaml);

        //Act
        String actualYaml = FileUtils.readLocalFile(fileYaml.getPath());
        String actualYml = FileUtils.readLocalFile(fileYml.getPath());

        //Verify
        assertThat(actualYaml, is(expectedYaml));
        assertThat(actualYml, is(expectedYaml));
    }

    @Test
    public void testReadLocalFileOnCaseInsensitiveFileExtensionSucceeds(@TempDir Path tempDir)
            throws IOException {
        //Arrange
        File file = createTempFile(tempDir, "", "Empty.YMl", "");

        //Act
        String actualYaml = FileUtils.readLocalFile(file.getPath());

        //Verify
        assertThat(actualYaml, is(""));
    }


    //TODO: disabling this test for now, since setting readable to false might not work in some environments due to access rights

    /*@Test
    public void testReadLocalFileOnMissingFilePermissionThrowsException(@TempDir Path tempDir)
            throws IOException {
        //Arrange
        File file = createTempFile(tempDir, "","Empty.yml", "");
        file.setReadable(false, false);

        //Act and Verify
        FileNotFoundException thrown = assertThrows(FileNotFoundException.class,
                () -> FileUtils.readLocalFile(file.getPath()));
        assertThat(thrown.getMessage(), containsString("Permission denied"));
    }*/

    @Test
    public void testReadLocalFileOnMissingFileThrowsException() {

        //Act and Verify
        IOException exception = assertThrows(FileNotFoundException.class,
                () -> FileUtils.readLocalFile(RESOURCE_PATH + "Missing.yaml"));
        assertThat(exception.getMessage(), containsString("No such file or directory"));
    }

    @Test
    public void testReadLocalFileOnMissingFilePathThrowsException() {
        //Act and Verify
        //TODO

        IOException exception = assertThrows(FileNotFoundException.class,
                () -> FileUtils.readLocalFile(RESOURCE_PATH + "no/path/Missing.yaml"));
        assertThat(exception.getMessage(), containsString("No such file or directory"));
    }

    @Test
    public void testReadLocalFileOnInvalidFileExtensionThrowsException() {
        //Act and Verify
        assertThrows(InvalidFileTypeException.class,
                () -> FileUtils.readLocalFile(RESOURCE_PATH + "InvalidFileExtension.txt"));
    }

    @Test
    public void testReadLocalFileOnFileWithoutExtensionThrowsException() {
        //Act and Verify
        assertThrows(InvalidFileTypeException.class,
                () -> FileUtils.readLocalFile(RESOURCE_PATH + "NoFileExtension"));
    }

    private File createTempFile(Path tempDir, String directories, String filename, String content)
            throws IOException {
        Path path = tempDir.resolve(directories);

        File tempFile = Files
                .createDirectories(path)
                .resolve(filename)
                .toFile();

        try(FileOutputStream out = new FileOutputStream(tempFile)){
            IOUtils.write(content, out, Charset.defaultCharset());
        }
        return tempFile;
    }

    @Test
    public void testReadRemoteFileEmptySucceeds() throws IOException, ProtocolException {
        //Arrange
        // http://localhost:8070/Empty.yaml
        WireMockServer server = MockServerBuilder.builder()
                .addRoute("Empty.yaml")
                .build();
        server.start();

        //Act
        String actualYaml = FileUtils.readRemoteFile(server.url("Empty.yaml"));

        //Verify
        assertThat(actualYaml, is(""));

        //Cleanup
        server.stop();
    }

    @Test
    public void testReadRemoteFileWithContentSucceeds() throws IOException, ProtocolException {
        //Arrange
        // http://localhost:8070/SimpleList.yaml
        WireMockServer server = MockServerBuilder.builder()
                .addRoute("SimpleList.yaml", readFileContent(SIMPLE_LIST_FILE_PATH))
                .build();
        server.start();

        //Act
        String actualYaml = FileUtils.readRemoteFile(server.url("SimpleList.yaml"));

        //Verify
        assertThat(actualYaml, is(readFileContent(SIMPLE_LIST_FILE_PATH)));

        //Cleanup
        server.stop();
    }

    @Test
    public void testReadRemoteFileWithPathSucceeds() throws IOException, ProtocolException {
        //Arrange
        // http://localhost:XXXX/resources/SimpleList.yaml
        WireMockServer server = MockServerBuilder.builder()
                .addRoute("resources/SimpleList.yaml", readFileContent(SIMPLE_LIST_FILE_PATH))
                .build();
        server.start();

        //Act
        System.out.println(server.getStubMappings());
        System.out.println(server.url("resources/SimpleList.yaml"));
        String actualYaml = FileUtils.readRemoteFile(server.url("resources/SimpleList.yaml"));

        //Verify
        assertThat(actualYaml, is(readFileContent(SIMPLE_LIST_FILE_PATH)));

        //Cleanup
        server.stop();
    }

    @Test
    public void testReadRemoteFileOnCaseInsensitiveFileExtensionSucceeds() throws IOException, ProtocolException {
        //Arrange
        // http://localhost:XXXX/resources/SimpleList.yAMl
        // http://localhost:XXXX/resources/SimpleList.ymL
        WireMockServer server = MockServerBuilder.builder()
                .addRoute("resources/SimpleList.yAMl", readFileContent(SIMPLE_LIST_FILE_PATH))
                .addRoute("resources/SimpleList.ymL", readFileContent(SIMPLE_LIST_FILE_PATH))
                .build();
        server.start();

        //Act
        String actualYaml = FileUtils.readRemoteFile(server.url("resources/SimpleList.yAMl"));
        String actualYml = FileUtils.readRemoteFile(server.url("resources/SimpleList.ymL"));

        //Verify
        assertThat(actualYaml, is(readFileContent( SIMPLE_LIST_FILE_PATH)));
        assertThat(actualYml, is(readFileContent( SIMPLE_LIST_FILE_PATH)));

        //Cleanup
        server.stop();
    }

    @Test
    public void testReadRemoteFileOnValidFileTypesSucceeds() throws IOException, ProtocolException {
        //Arrange
        // http://localhost:XXXX/SimpleList.yml
        WireMockServer server = MockServerBuilder.builder()
                .addRoute("SimpleList.yml", readFileContent(SIMPLE_LIST_FILE_PATH))
                .build();
        server.start();

        //Act
        String actualYaml = FileUtils.readRemoteFile(server.url("SimpleList.yml"));

        //Verify
        assertThat(actualYaml, is(readFileContent(SIMPLE_LIST_FILE_PATH)));

        //Cleanup
        server.stop();
    }

    @Test
    public void testReadRemoteFileOnPathWithoutFileExtensionSucceeds() throws IOException, ProtocolException {
        //Arrange
        // http://localhost:XXXX/resources/SimpleList
        WireMockServer server = MockServerBuilder.builder()
                .addRoute("SimpleList", readFileContent(SIMPLE_LIST_FILE_PATH))
                .build();
        server.start();

        //Act
        String actualYaml = FileUtils.readRemoteFile(server.url("SimpleList"));

        //Verify
        assertThat(actualYaml, is(readFileContent(SIMPLE_LIST_FILE_PATH)));

        //Cleanup
        server.stop();
    }

    @Test
    public void testReadRemoteFileOnPathIncludingPointSucceeds() throws IOException, ProtocolException {
        //Arrange
        // http://localhost:XXXX/resources.path/SimpleList.yaml
        WireMockServer server = MockServerBuilder.builder()
                .addRoute("resources.path/SimpleList.yaml", readFileContent(SIMPLE_LIST_FILE_PATH))
                .build();
        server.start();

        //Act
        String yaml = FileUtils.readRemoteFile(server.url("resources.path/SimpleList.yaml"));

        //Verify
        assertThat(yaml, is(readFileContent(SIMPLE_LIST_FILE_PATH)));

        //Cleanup
        server.stop();
    }


    @Test
    public void testReadRemoteFileOnFalseContentTypeThrowsException() {
        //Arrange
        // http://localhost:XXXX/SimpleList.yaml
        WireMockServer server = MockServerBuilder.builder()
                .build();
        server.start();

        //Act
        Exception exception = assertThrows(Exception.class,
                () -> FileUtils.readRemoteFile(server.url("").replace("8070", "9999")));

        //Cleanup
        server.stop();
    }


    @Test
    public void testReadRemoteFileOnNonExistingHostThrowsException() {
        //Arrange
        // http://localhost:XXXX/SimpleList.yaml
        WireMockServer server = MockServerBuilder.builder()
                .build();
        server.start();

        //Act
        Exception exception = assertThrows(Exception.class,
                () -> FileUtils.readRemoteFile(server.url("").replace("8070", "9999")));

        //Cleanup
        server.stop();
    }

    @Test
    public void testReadRemoteFileOnMissingFileThrowsException() {
        //Arrange
        // http://localhost:XXXX
        WireMockServer server = MockServerBuilder.builder()
                .build();
        server.start();

        //Act
        Exception exception = assertThrows(Exception.class,
                () -> FileUtils.readRemoteFile(server.url("Missing.yml")));

        //Cleanup
        server.stop();
    }

    @Test
    public void testReadRemoteFileOnMissingFilePathThrowsException() throws FileNotFoundException {
        //Arrange
        // http://localhost:XXXX/resources/SimpleList.yaml
        WireMockServer server = MockServerBuilder.builder()
                .addRoute("resources/SimpleList.yaml", readFileContent(SIMPLE_LIST_FILE_PATH))
                .build();
        server.start();

        //Act
        Exception exception = assertThrows(Exception.class,
                () -> FileUtils.readRemoteFile(server.url("resrcs/SimpleList.yml")));

        //Cleanup
        server.stop();
    }

    @Test
    public void testReadRemoteFileOnInvalidFileExtensionThrowsException() {
        //Arrange
        // http://localhost:XXXX/SimpleList.txt
        WireMockServer server = MockServerBuilder.builder()
                .addRoute("SimpleList.txt")
                .build();
        server.start();

        //Act
        InvalidFileTypeException exception = assertThrows(InvalidFileTypeException.class,
                () -> FileUtils.readRemoteFile(server.url("SimpleList.txt")));
        assertThat(exception.getMessage(), containsString("invalid file extension"));

        //Cleanup
        server.stop();
    }

    @Test
    public void testReadRemoteFileOnInvalidContentTypeThrowsException() {
        //Arrange
        // http://localhost:XXXX/jsondata
        WireMockServer server = MockServerBuilder.builder()
                .addRoute(MockRoute
                        .builder()
                        .setPath("jsondata")
                        .addHeader("Content-Type", "application/json"))
                .build();
        server.start();

        //Act
        HttpResponseException exception = assertThrows(HttpResponseException.class,
                () -> FileUtils.readRemoteFile(server.url("jsondata")));
        assertThat(exception.getReasonPhrase(), containsString("invalid content type"));

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
            MappingBuilder mappingBuilder =  get(urlEqualTo("/" + path));
            ResponseDefinitionBuilder respBuilder = aResponse();

            for (Map.Entry<String, String> headEntry : header.entrySet()) {
                respBuilder.withHeader(headEntry.getKey(), headEntry.getValue());
            }

            respBuilder.withBody(content);
            respBuilder.withStatus(status);
            return mappingBuilder.willReturn(respBuilder);
        }
    }

    private String readFileContent(String filename) throws FileNotFoundException {
        File file = new File(RESOURCE_PATH + filename);
        Scanner scanner = new Scanner(file);
        return scanner.useDelimiter("\\Z").next();
    }
}
