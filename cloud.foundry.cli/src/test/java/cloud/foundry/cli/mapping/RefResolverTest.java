package cloud.foundry.cli.mapping;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class RefResolverTest {

    private static final Yaml DEFAULT_PARSER = new Yaml();
    private static final String RESOURCES_PATH = "src/test/resources/refresolver/";

    @Test
    public void testTopLevelListNoPath() throws FileNotFoundException {
        Object treeRoot = parseYamlFileAsTree(RESOURCES_PATH + "TopLevelListNoPathRef.yaml");

        Object treeResult = RefResolver.resolve(treeRoot, DEFAULT_PARSER);
        String yamlResult = convertTreeToYaml(treeResult);

        String yamlExpected = readFile(RESOURCES_PATH + "TopLevelListNoPathExpected.yaml");
        Assertions.assertEquals(yamlResult, yamlExpected);
    }

    private String convertTreeToYaml(Object tree) {
        return DEFAULT_PARSER.dump(tree);
    }

    private Object parseYamlFileAsTree(String filePath) throws FileNotFoundException {
        FileInputStream inputStream = new FileInputStream(new File(filePath));
        return DEFAULT_PARSER.load(inputStream);
    }

    private String readFile(String filePath) throws FileNotFoundException {
        Scanner scanner = new Scanner( new File(filePath), "UTF-8" );
        String fileContent = scanner.useDelimiter("\\A").next();
        scanner.close();
        return fileContent;
    }

}
