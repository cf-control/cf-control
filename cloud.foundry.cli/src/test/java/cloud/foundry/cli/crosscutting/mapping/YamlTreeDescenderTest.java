package cloud.foundry.cli.crosscutting.mapping;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cloud.foundry.cli.crosscutting.exceptions.YamlTreeNodeNotFoundException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Collections;

/**
 * Test for {@link YamlTreeDescender}
 */
public class YamlTreeDescenderTest {

    /*
      values:
        - &anchor
          name: null
          luckyNumber: 42
        - false
      same: *anchor
     */

    private static Map<Object, Object> root;
    private static String valuesKey = "values";
    private static List<Object> list;
    private static Map<Object, Object> firstListElement;
    private static String nameKey = "name";
    private static String luckyNumberKey = "luckyNumber";
    private static Integer luckyNumberValue = 42;
    private static Boolean secondListElement = false;
    private static String sameKey = "same";

    @BeforeAll
    public static void setupYamlTree() {

        firstListElement = new LinkedHashMap<>();
        firstListElement.put(nameKey, null);
        firstListElement.put(luckyNumberKey, luckyNumberValue);
        firstListElement = Collections.unmodifiableMap(firstListElement);

        list = new LinkedList<>();
        list.add(firstListElement);
        list.add(secondListElement);
        list = Collections.unmodifiableList(list);

        root = new LinkedHashMap<>();
        root.put(valuesKey, list);
        root.put(sameKey, firstListElement);
        root = Collections.unmodifiableMap(root);
    }

    @Test
    public void testDescendToMappingScalar() {
        YamlPointer yamlPointer = new YamlPointer("#/" + valuesKey + "/0/" + luckyNumberKey);

        Object resultingYamlTreeNode = YamlTreeDescender.descend(root, yamlPointer);

        assertThat(resultingYamlTreeNode, is(luckyNumberValue));
    }

    @Test
    public void testDescendToListScalar() {
        YamlPointer yamlPointer = new YamlPointer("#/" + valuesKey + "/1");

        Object resultingYamlTreeNode = YamlTreeDescender.descend(root, yamlPointer);

        assertThat(resultingYamlTreeNode, is(secondListElement));
    }

    @Test
    public void testDescendToNullScalar() {
        YamlPointer yamlPointer = new YamlPointer("#/" + valuesKey + "/0/" + nameKey);

        Object resultingYamlTreeNode = YamlTreeDescender.descend(root, yamlPointer);

        assertThat(resultingYamlTreeNode, is(nullValue()));
    }

    @Test
    public void testDescendToList() {
        YamlPointer yamlPointer = new YamlPointer("#/" + valuesKey);

        Object resultingYamlTreeNode = YamlTreeDescender.descend(root, yamlPointer);

        assertThat(resultingYamlTreeNode, is(list));
    }

    @Test
    public void testDescendToMap() {
        YamlPointer yamlPointer = new YamlPointer("#/" + valuesKey + "/0");

        Object resultingYamlTreeNode = YamlTreeDescender.descend(root, yamlPointer);

        assertThat(resultingYamlTreeNode, is(firstListElement));
    }

    @Test
    public void testDescendToRoot() {
        YamlPointer yamlPointer = new YamlPointer("#/");

        Object resultingYamlTreeNode = YamlTreeDescender.descend(root, yamlPointer);

        assertThat(resultingYamlTreeNode, is(root));
    }

    @Test
    public void testDescendToAnchorAlias() {
        YamlPointer yamlPointerAnchor = new YamlPointer("#/" + valuesKey + "/0");
        YamlPointer yamlPointerAlias = new YamlPointer("#/" + sameKey);

        Object resultingAnchorYamlTreeNode = YamlTreeDescender.descend(root, yamlPointerAnchor);
        Object resultingAliasYamlTreeNode = YamlTreeDescender.descend(root, yamlPointerAlias);

        assertThat(resultingAnchorYamlTreeNode, is(resultingAliasYamlTreeNode));
    }

    @Test
    public void testKeyNotInMap() {
        YamlPointer yamlPointer = new YamlPointer("#/nonExistentKey");

        assertThrows(YamlTreeNodeNotFoundException.class, () -> YamlTreeDescender.descend(root, yamlPointer));
    }

    @Test
    public void testUnparsableListIndex() {
        YamlPointer yamlPointer = new YamlPointer("#/" + valuesKey + "/index");

        assertThrows( YamlTreeNodeNotFoundException.class, () -> YamlTreeDescender.descend(root, yamlPointer));
    }

    @Test
    public void testListIndexOutOfRange() {
        YamlPointer yamlPointerLower = new YamlPointer("#/" + valuesKey + "/-1");
        YamlPointer yamlPointerUpper = new YamlPointer("#/" + valuesKey + "/2");

        assertThrows(YamlTreeNodeNotFoundException.class, () -> YamlTreeDescender.descend(root, yamlPointerLower));
        assertThrows(YamlTreeNodeNotFoundException.class, () -> YamlTreeDescender.descend(root, yamlPointerUpper));
    }

    @Test
    public void testDescendBeyondScalar() {
        YamlPointer yamlPointer = new YamlPointer("#/" + valuesKey + "/0/" + luckyNumberKey + "/nonExistent");

        assertThrows(YamlTreeNodeNotFoundException.class, () -> YamlTreeDescender.descend(root, yamlPointer));
    }
}
