package cloud.foundry.cli.mapping;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

public class FileUtilsTest {

    private static String RESOURCE_PATH = "./src/test/resources/refresolver/";
    private static String SIMPLE_LIST_FILE_PATH = "referred/SimpleList.yaml";

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
        String expectedYaml =   "- first\n" +
                                "- second\n" +
                                "- third\n";
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
        String expectedYaml =   "- first\n" +
                                "- second\n" +
                                "- third\n";
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
        String expectedYaml =   "- first\n" +
                "- second\n" +
                "- third\n";
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


    @Test
    public void testReadLocalFileOnMissingFilePermissionThrowsException(@TempDir Path tempDir)
            throws IOException {
        //Arrange
        File file = createTempFile(tempDir, "","Empty.yml", "");
        file.setReadable(false, false);

        //Act and Verify
        FileNotFoundException thrown = assertThrows(FileNotFoundException.class,
                () -> FileUtils.readLocalFile(file.getPath()));
        assertThat(thrown.getMessage(), containsString("Permission denied"));
    }

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
        List<String> contents = content.isEmpty()
                ? Collections.emptyList()
                : Arrays.asList(content.split("\n"));
        return Files
                .write(Files.createDirectories(path).resolve(filename), contents)
                .toFile();
    }

    @Test
    public void testReadRemoteFileEmptySucceeds() throws IOException {
        //Arrange
        // http://localhost:8070/Empty.yaml
        WireMockServer server = MockServerBuilder
                .create()
                .setPath("Empty.yaml")
                .setFileContent("")
                .build();
        server.start();

        //Act
        String actualYaml = FileUtils.readRemoteFile(server.url(""));

        //Verify
        assertThat(actualYaml, is(""));

        //Cleanup
        server.stop();
    }

    @Test
    public void testReadRemoteFileWithContentSucceeds() throws IOException {
        //Arrange
        // http://localhost:8070/SimpleList.yaml
        WireMockServer server = MockServerBuilder
                .create()
                .setPath("SimpleList.yaml")
                .setFileContent(readFileContent(SIMPLE_LIST_FILE_PATH))
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
    public void testReadRemoteFileWithPathSucceeds() throws IOException {
        //Arrange
        // http://localhost:8070/resources/SimpleList.yaml
        WireMockServer server = MockServerBuilder
                .create()
                .setPath("resources/SimpleList.yaml")
                .setFileContent(readFileContent(SIMPLE_LIST_FILE_PATH))
                .build();
        server.start();

        //Act
        String actualYaml = FileUtils.readRemoteFile(server.url("resources/SimpleList.yaml"));

        //Verify
        assertThat(actualYaml, is(readFileContent(SIMPLE_LIST_FILE_PATH)));

        //Cleanup
        server.stop();
    }

    @Test
    public void testReadRemoteFileOnDirectoryWithCommaSucceeds() throws IOException {
        //Arrange
        // http://localhost:8070/resources.path/SimpleList.yaml
        WireMockServer server = MockServerBuilder
                .create()
                .setPath("resources.path/SimpleList.yaml")
                .setFileContent(readFileContent(SIMPLE_LIST_FILE_PATH))
                .build();
        server.start();

        //Act
        String actualYaml = FileUtils.readRemoteFile(server.url("resources.path/SimpleList.yaml"));

        //Verify
        assertThat(actualYaml, is(readFileContent(SIMPLE_LIST_FILE_PATH)));

        //Cleanup
        server.stop();
        server.shutdown();
    }

    @Test
    public void testReadRemoteFileOnCaseInsensitiveFileExtensionSucceeds() throws IOException {
        //Arrange
        // http://localhost:8070/resources.path/SimpleList.yaml
        WireMockServer server = MockServerBuilder
                .create()
                .setPath("resources.path/SimpleList.yAMl")
                .setFileContent(readFileContent(SIMPLE_LIST_FILE_PATH))
                .build();
        server.start();

        //Act
        String actualYaml = FileUtils.readRemoteFile(server.url("SimpleList.yAMl"));

        //Verify
        assertThat(actualYaml, is(readFileContent( SIMPLE_LIST_FILE_PATH)));

        //Cleanup
        server.stop();
    }

    @Test
    public void testReadRemoteFileOnValidFileTypesSucceeds() throws IOException {
        //Arrange
        // http://localhost:8070/SimpleList.yaml
        WireMockServer server = MockServerBuilder
                .create()
                .setPath("SimpleList.yml")
                .setFileContent(readFileContent(SIMPLE_LIST_FILE_PATH))
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
    public void testReadRemoteFileOnPathWithoutFileExtensionSucceeds() throws IOException {
        //Arrange
        // http://localhost:8070/resources/SimpleList
        WireMockServer server = MockServerBuilder
                .create()
                .setPath("SimpleList")
                .setFileContent(readFileContent(SIMPLE_LIST_FILE_PATH))
                .build();
        server.start();

        //Act
        String actualYaml = FileUtils.readRemoteFile(server.url("SimpleList"));

        //Verify
        assertThat(actualYaml, is(readFileContent(SIMPLE_LIST_FILE_PATH)));

        //Cleanup
        server.stop();
        server.shutdown();
    }

    @Test
    public void testReadRemoteFileOnFalseContentTypeThrowsException() throws IOException {
        //Arrange
        // http://localhost:8070/SimpleList.yaml
        WireMockServer server = MockServerBuilder
                .create()
                .build();
        server.start();

        //Act
        Exception exception = assertThrows(Exception.class, () -> FileUtils.readRemoteFile(server.url("").replace("8070", "9999")));

        //Cleanup
        server.stop();
    }


    @Test
    public void testReadRemoteFileOnNonExistingHostThrowsException() throws IOException {
        //Arrange
        // http://localhost:8070/SimpleList.yaml
        WireMockServer server = MockServerBuilder
                .create(8070)
                .build();
        server.start();

        //Act
        Exception exception = assertThrows(Exception.class, () -> FileUtils.readRemoteFile(server.url("").replace("8070", "9999")));

        //Cleanup
        server.stop();
        server.shutdownServer();
        server.resetAll();
    }

    @Test
    public void testReadRemoteFileOnMissingFileThrowsException() throws FileNotFoundException {
        //Arrange
        // http://localhost:8070/SimpleList.yaml
        WireMockServer server = MockServerBuilder
                .create()
                .build();
        server.start();

        //Act
        Exception exception = assertThrows(Exception.class, () -> FileUtils.readRemoteFile(server.url("Missing.yml")));

        //Cleanup
        server.stop();
    }

    @Test
    public void testReadRemoteFileOnMissingFilePathThrowsException() throws FileNotFoundException {
        //Arrange
        // http://localhost:8070/resources/SimpleList.yaml
        WireMockServer server = MockServerBuilder
                .create()
                .setPath("resources/SimpleList.yaml")
                .setFileContent(readFileContent(SIMPLE_LIST_FILE_PATH))
                .build();
        server.start();

        //Act
        Exception exception = assertThrows(Exception.class, () -> FileUtils.readRemoteFile(server.url("resrcs/SimpleList.yml")));

        //Cleanup
        server.stop();
    }

    @Test
    public void testReadRemoteFileOnInvalidFileExtensionThrowsException() {
        //Arrange
        // http://localhost:8070/SimpleList.txt
        WireMockServer server = MockServerBuilder
                .create()
                .setPath("SimpleList.txt")
                .build();
        server.start();

        //Act
        Exception exception = assertThrows(Exception.class, () -> FileUtils.readRemoteFile(server.url("SimpleList.txt")));

        //Cleanup
        server.stop();
    }

    private static class MockServerBuilder {

        WireMockServer wireMockServer;
        String content;
        String path;

        private MockServerBuilder(int port) {
            WireMockConfiguration serverConfig = WireMockConfiguration.options().port(port);
            wireMockServer = new WireMockServer(serverConfig);
            content = "";
            path = "";
        }

        private MockServerBuilder() {
            WireMockConfiguration serverConfig = WireMockConfiguration.options().dynamicPort();
            wireMockServer = new WireMockServer(serverConfig);
            content = "";
            path = "";
        }

        public static MockServerBuilder create(int port){
            return new MockServerBuilder(port);
        }

        public static MockServerBuilder create(){
            return new MockServerBuilder();
        }

        public MockServerBuilder setPath(String path) {
            this.path = path;
            return this;
        }

        public MockServerBuilder setFileContent(String content){
            this.content = content;
            return this;
        }

        public WireMockServer build(){
            wireMockServer.stubFor(get(urlEqualTo(path))
                    .willReturn(aResponse().withBody(content)));
            return wireMockServer;
        }
    }

    private String readFileContent(String filename) throws FileNotFoundException {
        File file = new File(RESOURCE_PATH + filename);
        Scanner scanner = new Scanner(file);
        return scanner.useDelimiter("\\Z").next();
    }
}
