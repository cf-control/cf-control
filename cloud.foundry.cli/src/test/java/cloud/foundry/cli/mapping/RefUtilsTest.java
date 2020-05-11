package cloud.foundry.cli.mapping;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

public class RefUtilsTest {

    private static String RESOURCE_PATH = "./src/test/resources/refresolver/";

    @Test
    public void testReadLocalFileOnEmptyFileReturnsEmptyString() throws IOException {
        //Arrange
        String expectedYaml = "";

        //Act
        String actualYaml = RefUtils.readLocalFile(RESOURCE_PATH + "referred/Empty.yaml");

        //Verify
        assertThat(actualYaml, is(expectedYaml));
    }

    @Test
    public void testReadLocalFileReturnsFileContent() throws IOException {
        //Arrange
        String expectedYaml =   "- first\n" +
                "- second\n" +
                "- third";

        //Act
        String actualYaml = RefUtils.readLocalFile(RESOURCE_PATH + "referred/SimpleList.yaml");

        //Verify
        assertThat(actualYaml, is(expectedYaml));
    }

    @Test
    public void testReadLocalFileOnDirectoryWithCommaSucceeds() throws IOException {
        //Arrange
        String expectedYaml =   "- first\n" +
                "- second\n" +
                "- third";

        //Act
        String actualYaml = RefUtils.readLocalFile(RESOURCE_PATH + "comma.path/SimpleList.yaml");

        //Verify
        assertThat(actualYaml, is(expectedYaml));
    }

    @Test
    public void testReadLocalFileOnValidFileTypesSucceeds() throws IOException {
        //Arrange
        String expectedYaml =   "- first\n" +
                "- second\n" +
                "- third";

        //Act
        String actualYaml = RefUtils.readLocalFile(RESOURCE_PATH + "referred/SimpleList.yaml");
        String actualYml = RefUtils.readLocalFile(RESOURCE_PATH + "referred/SimpleList.yml");

        //Verify
        assertThat(actualYaml, is(expectedYaml));
        assertThat(actualYml, is(expectedYaml));
    }

    @Test
    public void testReadLocalFileOnCaseInsensitiveFileExtensionSucceeds(@TempDir Path tempDir) throws IOException {
        //Arrange
        Path path = tempDir.resolve("test.YMl");
        Files.write(path, Collections.singleton(""));

        //Act
        String res = RefUtils.readLocalFile(path.toString());

        //Verify
        assertThat(res, is("\n"));
    }


    @Test
    public void testReadLocalFileOnMissingFilePermissionThrowsException(@TempDir Path tempDir) throws IOException {
        //Arrange
        Path path = tempDir.resolve("test.yml");
        Files.write(path, Collections.singleton(""));
        path.toFile().setReadable(false, false);

        //Act and Verify
        FileNotFoundException thrown = assertThrows(FileNotFoundException.class,
                () -> RefUtils.readLocalFile(path.toString()));
        assertThat(thrown.getMessage(), CoreMatchers.containsString("Permission denied"));
    }

    @Test
    public void testReadLocalFileOnMissingFileThrowsException(@TempDir Path tempDir) throws IOException {
        //Act and Verify
        assertThrows(FileNotFoundException.class,
                () -> RefUtils.readLocalFile(RESOURCE_PATH + "Missing.yaml"));
    }

    @Test
    public void testReadLocalFileOnMissingFilePathThrowsException() {
        //Act and Verify
        assertThrows(IOException.class,
                () -> RefUtils.readLocalFile(RESOURCE_PATH + "no/path/Missing.yaml"));
    }

    @Test
    public void testReadLocalFileOnInvalidFileExtensionThrowsException() {
        //Act and Verify
        assertThrows(InvalidFileTypeException.class,
                () -> RefUtils.readLocalFile(RESOURCE_PATH + "InvalidFileExtension.txt"));
    }

    @Test
    public void testReadLocalFileOnFileWithoutExtensionThrowsException() {
        //Act and Verify
        assertThrows(InvalidFileTypeException.class,
                () -> RefUtils.readLocalFile(RESOURCE_PATH + "NoFileExtension"));
    }
}
