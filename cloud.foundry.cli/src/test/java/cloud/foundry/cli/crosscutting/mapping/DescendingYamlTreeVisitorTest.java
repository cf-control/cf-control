package cloud.foundry.cli.crosscutting.mapping;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cloud.foundry.cli.crosscutting.exceptions.YamlTreeNodeNotFoundException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.Map;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class DescendingYamlTreeVisitorTest {

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
    @Timeout(value = 1000, unit = TimeUnit.MILLISECONDS)
    public void testDescendToMappingScalar() {
        YamlPointer yamlPointer = new YamlPointer("#/" + valuesKey + "/0/" + luckyNumberKey);
        DescendingYamlTreeVisitor visitor = new DescendingYamlTreeVisitor(root);

        visitor.descend(yamlPointer);
        Object resultingYamlTreeNode = visitor.getResultingYamlTreeNode();

        assertThat(resultingYamlTreeNode, is(luckyNumberValue));
    }

    @Test
    @Timeout(value = 1000, unit = TimeUnit.MILLISECONDS)
    public void testDescendToListScalar() {
        YamlPointer yamlPointer = new YamlPointer("#/" + valuesKey + "/1");
        DescendingYamlTreeVisitor visitor = new DescendingYamlTreeVisitor(root);

        visitor.descend(yamlPointer);
        Object resultingYamlTreeNode = visitor.getResultingYamlTreeNode();

        assertThat(resultingYamlTreeNode, is(secondListElement));
    }

    @Test
    @Timeout(value = 1000, unit = TimeUnit.MILLISECONDS)
    public void testDescendToNullScalar() {
        YamlPointer yamlPointer = new YamlPointer("#/" + valuesKey + "/0/" + nameKey);
        DescendingYamlTreeVisitor visitor = new DescendingYamlTreeVisitor(root);

        visitor.descend(yamlPointer);
        Object resultingYamlTreeNode = visitor.getResultingYamlTreeNode();

        assertThat(resultingYamlTreeNode, is(nullValue()));
    }

    @Test
    @Timeout(value = 1000, unit = TimeUnit.MILLISECONDS)
    public void testDescendToList() {
        YamlPointer yamlPointer = new YamlPointer("#/" + valuesKey);
        DescendingYamlTreeVisitor visitor = new DescendingYamlTreeVisitor(root);

        visitor.descend(yamlPointer);
        Object resultingYamlTreeNode = visitor.getResultingYamlTreeNode();

        assertThat(resultingYamlTreeNode, is(list));
    }

    @Test
    @Timeout(value = 1000, unit = TimeUnit.MILLISECONDS)
    public void testDescendToMap() {
        YamlPointer yamlPointer = new YamlPointer("#/" + valuesKey + "/0");
        DescendingYamlTreeVisitor visitor = new DescendingYamlTreeVisitor(root);

        visitor.descend(yamlPointer);
        Object resultingYamlTreeNode = visitor.getResultingYamlTreeNode();

        assertThat(resultingYamlTreeNode, is(firstListElement));
    }

    @Test
    @Timeout(value = 1000, unit = TimeUnit.MILLISECONDS)
    public void testDescendToRoot() {
        YamlPointer yamlPointer = new YamlPointer("#/");
        DescendingYamlTreeVisitor visitor = new DescendingYamlTreeVisitor(root);

        visitor.descend(yamlPointer);
        Object resultingYamlTreeNode = visitor.getResultingYamlTreeNode();

        assertThat(resultingYamlTreeNode, is(root));
    }

    @Test
    @Timeout(value = 1000, unit = TimeUnit.MILLISECONDS)
    public void testDescendToAnchorAlias() {
        YamlPointer yamlPointerAnchor = new YamlPointer("#/" + valuesKey + "/0");
        YamlPointer yamlPointerAlias = new YamlPointer("#/" + sameKey);
        DescendingYamlTreeVisitor visitor = new DescendingYamlTreeVisitor(root);

        visitor.descend(yamlPointerAnchor);
        Object resultingAnchorYamlTreeNode = visitor.getResultingYamlTreeNode();

        visitor.descend(yamlPointerAlias);
        Object resultingAliasYamlTreeNode = visitor.getResultingYamlTreeNode();

        assertThat(resultingAnchorYamlTreeNode, is(resultingAliasYamlTreeNode));
    }

    @Test
    @Timeout(value = 1000, unit = TimeUnit.MILLISECONDS)
    public void testKeyNotInMap() {
        YamlPointer yamlPointer = new YamlPointer("#/nonExistentKey");
        DescendingYamlTreeVisitor visitor = new DescendingYamlTreeVisitor(root);

        assertThrows(
                YamlTreeNodeNotFoundException.class,
                () -> visitor.descend(yamlPointer)
        );
    }

    @Test
    @Timeout(value = 1000, unit = TimeUnit.MILLISECONDS)
    public void testUnparsableListIndex() {
        YamlPointer yamlPointer = new YamlPointer("#/" + valuesKey + "/index");
        DescendingYamlTreeVisitor visitor = new DescendingYamlTreeVisitor(root);

        assertThrows(
                YamlTreeNodeNotFoundException.class,
                () -> visitor.descend(yamlPointer)
        );
    }

    @Test
    @Timeout(value = 1000, unit = TimeUnit.MILLISECONDS)
    public void testListIndexOutOfRange() {
        YamlPointer yamlPointerLower = new YamlPointer("#/" + valuesKey + "/-1");
        YamlPointer yamlPointerUpper = new YamlPointer("#/" + valuesKey + "/2");
        DescendingYamlTreeVisitor visitor = new DescendingYamlTreeVisitor(root);

        assertThrows(
                YamlTreeNodeNotFoundException.class,
                () -> visitor.descend(yamlPointerLower)
        );
        assertThrows(
                YamlTreeNodeNotFoundException.class,
                () -> visitor.descend(yamlPointerUpper)
        );
    }

    @Test
    @Timeout(value = 1000, unit = TimeUnit.MILLISECONDS)
    public void testDescendBeyondScalar() {
        YamlPointer yamlPointer = new YamlPointer("#/" + valuesKey + "/0/" + luckyNumberKey + "/nonExistent");
        DescendingYamlTreeVisitor visitor = new DescendingYamlTreeVisitor(root);

        assertThrows(
                YamlTreeNodeNotFoundException.class,
                () -> visitor.descend(yamlPointer)
        );
    }

    @Test
    @Timeout(value = 1000, unit = TimeUnit.MILLISECONDS)
    public void testIllegalVisitCall() {
        DescendingYamlTreeVisitor visitor = new DescendingYamlTreeVisitor(root);

        assertThrows(
                IllegalStateException.class,
                () -> YamlTreeVisitor.visit(visitor, root)
        );

        YamlPointer someYamlPointer = new YamlPointer("#/" + sameKey);
        visitor.descend(someYamlPointer);

        assertThrows(
                IllegalStateException.class,
                () -> YamlTreeVisitor.visit(visitor, root)
        );
    }
}
