package cloud.foundry.cli.mapping;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import java.io.IOException;

public class RefUtilsTest {

    private static String RESOURCE_PATH = "./src/test/resources/refresolver/";

    @Test
    public void testGetOnEmptyFileReturnsEmptyString() throws IOException {
        //Arrange
        String expectedYaml = "";

        //Act
        String actualYaml = RefUtils.readExternalFile(RESOURCE_PATH + "referred/Empty.yaml");

        //Verify
        assertThat(actualYaml, is(expectedYaml));
    }
}
