package cloud.foundry.cli.mapping;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

public class FileUtilsTest {

    private static String RESOURCE_PATH = "./src/test/resources/refresolver/";

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
        File file = createTempFile(tempDir, "", "SimpleList.yaml", expectedYaml);

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
        String res = FileUtils.readLocalFile(file.getPath());

        //Verify
        assertThat(res, is(""));
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
}
