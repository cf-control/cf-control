package cloud.foundry.cli.mapping;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class RefResolverTest {

    private static final Yaml DEFAULT_PROCESSOR = new Yaml();
    private static final String RESOURCES_PATH = "./src/test/resources/refresolver/";

    @Test
    public void testTopLevelOccurrence() throws FileNotFoundException {
        assertSemanticEquality("TopLevelOccurrenceReferring.yaml",
                "TopLevelOccurrenceReferred.yaml");
    }

    private void assertSemanticEquality(String yamlFileContainingRef, String yamlFileExpected)
            throws FileNotFoundException {
        Object treeRoot = parseYamlFileAsTree(RESOURCES_PATH + yamlFileContainingRef);

        Object treeResult = RefResolver.resolve(treeRoot, DEFAULT_PROCESSOR);
        String yamlResult = convertTreeToYaml(treeResult);

        Object treeRootExpected = parseYamlFileAsTree(RESOURCES_PATH + yamlFileExpected);
        String yamlResultExpected = convertTreeToYaml(treeRootExpected);
        Assertions.assertEquals(yamlResult, yamlResultExpected);
    }

    private String convertTreeToYaml(Object tree) {
        return DEFAULT_PROCESSOR.dump(tree);
    }

    private Object parseYamlFileAsTree(String filePath) throws FileNotFoundException {
        FileInputStream inputStream = new FileInputStream(new File(filePath));
        return DEFAULT_PROCESSOR.load(inputStream);
    }

}
