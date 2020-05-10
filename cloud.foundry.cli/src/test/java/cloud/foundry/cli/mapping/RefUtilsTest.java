package cloud.foundry.cli.mapping;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;

public class RefUtilsTest {

    private static String RESOURCE_PATH = "./src/test/resources/refresolver/";


    @Test
    public void testreadLocalFileOnEmptyFileReturnsEmptyString() throws IOException {
        //Arrange
        String expectedYaml = "";

        //Act
        String actualYaml = RefUtils.readLocalFile(RESOURCE_PATH + "referred/Empty.yaml");

        //Verify
        assertThat(actualYaml, is(expectedYaml));
    }

    @Test
    public void testreadLocalFileReturnsFileContent() throws IOException {
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
    public void testreadLocalFileOnDirectoryWithCommaSucceeds() throws IOException {
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
    public void testreadLocalFileOnValidFileTypesSucceeds() throws IOException {
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
    public void testreadLocalFileOnMissingFileThrowsException() {
        //Act and Verify
        assertThrows(FileNotFoundException.class,
                () -> RefUtils.readLocalFile(RESOURCE_PATH + "Missing.yaml"));
    }

    @Test
    public void testreadLocalFileOnMissingFilePathThrowsException() {
        //Act and Verify
        assertThrows(IOException.class,
                () -> RefUtils.readLocalFile(RESOURCE_PATH + "no/path/Missing.yaml"));
    }

    @Test
    public void testreadLocalFileOnInvalidFileExtensionThrowsException() {
        //Act and Verify
        assertThrows(InvalidFileTypeException.class,
                () -> RefUtils.readLocalFile(RESOURCE_PATH + "InvalidFileExtension.txt"));
    }

    @Test
    public void testreadLocalFileOnFileWithoutExtensionThrowsException() {
        //Act and Verify
        assertThrows(InvalidFileTypeException.class,
                () -> RefUtils.readLocalFile(RESOURCE_PATH + "NoFileExtension"));
    }
}
