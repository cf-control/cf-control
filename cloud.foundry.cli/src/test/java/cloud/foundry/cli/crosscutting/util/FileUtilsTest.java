package cloud.foundry.cli.crosscutting.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cloud.foundry.cli.crosscutting.exceptions.InvalidFileTypeException;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

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


    //TODO: disabling this test for now,
    // since setting readable to false might not work in some environments due to access rights

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
        assertThrows(FileNotFoundException.class,
                () -> FileUtils.readLocalFile(RESOURCE_PATH + "Missing.yaml"));
    }

    @Test
    public void testReadLocalFileOnMissingFilePathThrowsException() {
        //Act and Verify
        assertThrows(FileNotFoundException.class,
                () -> FileUtils.readLocalFile(RESOURCE_PATH + "no/path/Missing.yaml"));
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

        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            IOUtils.write(content, out, Charset.defaultCharset());
        }
        return tempFile;
    }

    private String readFileContent(String filename) throws FileNotFoundException {
        File file = new File(RESOURCE_PATH + filename);
        Scanner scanner = new Scanner(file);
        return scanner.useDelimiter("\\Z").next();
    }
}
