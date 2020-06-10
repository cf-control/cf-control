package cloud.foundry.cli.logic.diff;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cloud.foundry.cli.logic.diff.change.CfChange;
import cloud.foundry.cli.logic.diff.change.container.CfContainerChange;
import cloud.foundry.cli.logic.diff.change.container.CfContainerValueChanged;
import cloud.foundry.cli.logic.diff.change.map.CfMapChange;
import cloud.foundry.cli.logic.diff.change.map.CfMapValueChanged;
import cloud.foundry.cli.logic.diff.change.object.CfNewObject;
import cloud.foundry.cli.logic.diff.change.object.CfObjectValueChanged;
import cloud.foundry.cli.logic.diff.change.object.CfRemovedObject;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Test for {@link DiffTreeCreator}
 */
public class DiffTreeCreatorTest {

    @Test
    public void testCreateFromSingleNewObject() {
        Object affectedObject = Mockito.mock(Object.class);
        List<String> path = Collections.singletonList("root");
        CfNewObject newObject = new CfNewObject(affectedObject, "someName", path);

        DiffNode node = DiffTreeCreator.createFrom(Collections.singletonList(newObject));

        assertThat(node.getParentNode(), is(nullValue()));
        assertThat(node.getChildNodes().isEmpty(), is(true));
        assertThat(node.isNewObject(), is(true));
        assertThat(node.isRemovedObject(), is(false));

        List<CfChange> changes = node.getChanges();
        assertThat(changes.size(), is(1));
        assertThat(changes.get(0), is(newObject));
    }

    @Test
    public void testCreateFromSingleRemovedObject() {
        Object affectedObject = Mockito.mock(Object.class);
        List<String> path = Collections.singletonList("root");
        CfRemovedObject removedObject = new CfRemovedObject(affectedObject, "someName", path);

        DiffNode node = DiffTreeCreator.createFrom(Collections.singletonList(removedObject));

        assertThat(node.getParentNode(), is(nullValue()));
        assertThat(node.getChildNodes().isEmpty(), is(true));
        assertThat(node.isNewObject(), is(false));
        assertThat(node.isRemovedObject(), is(true));

        List<CfChange> changes = node.getChanges();
        assertThat(changes.size(), is(1));
        assertThat(changes.get(0), is(removedObject));
    }

    @Test
    public void testCreateFromSingleObjectValueChanged() {
        Object affectedObject = Mockito.mock(Object.class);
        List<String> path = Collections.singletonList("root");
        String valueBefore = "before";
        String valueAfter = "after";
        CfObjectValueChanged valueChanged = new CfObjectValueChanged(
                affectedObject, "someName", path, valueBefore, valueAfter);

        DiffNode node = DiffTreeCreator.createFrom(Collections.singletonList(valueChanged));

        assertThat(node.getParentNode(), is(nullValue()));
        assertThat(node.getChildNodes().isEmpty(), is(true));
        assertThat(node.isNewObject(), is(false));
        assertThat(node.isRemovedObject(), is(false));

        List<CfChange> changes = node.getChanges();
        assertThat(changes.size(), is(1));
        assertThat(changes.get(0), is(valueChanged));
    }

    @Test
    public void testCreateFromSingleMapChange() {
        Object affectedObject = Mockito.mock(Object.class);
        List<String> path = Collections.singletonList("root");
        List<CfMapValueChanged> changedValues = Collections.singletonList(Mockito.mock(CfMapValueChanged.class));
        CfMapChange mapChange = new CfMapChange(affectedObject, "someName", path, changedValues);

        DiffNode node = DiffTreeCreator.createFrom(Collections.singletonList(mapChange));

        assertThat(node.getParentNode(), is(nullValue()));
        assertThat(node.getChildNodes().isEmpty(), is(true));
        assertThat(node.isNewObject(), is(false));
        assertThat(node.isRemovedObject(), is(false));

        List<CfChange> changes = node.getChanges();
        assertThat(changes.size(), is(1));
        assertThat(changes.get(0), is(mapChange));
    }

    @Test
    public void testCreateFromSingleContainerChange() {
        Object affectedObject = Mockito.mock(Object.class);
        List<String> path = Collections.singletonList("root");
        List<CfContainerValueChanged> changedValues = Collections.singletonList(
                Mockito.mock(CfContainerValueChanged.class));
        CfContainerChange containerChange = new CfContainerChange(affectedObject, "someName", path, changedValues);

        DiffNode node = DiffTreeCreator.createFrom(Collections.singletonList(containerChange));

        assertThat(node.getParentNode(), is(nullValue()));
        assertThat(node.getChildNodes().isEmpty(), is(true));
        assertThat(node.isNewObject(), is(false));
        assertThat(node.isRemovedObject(), is(false));

        List<CfChange> changes = node.getChanges();
        assertThat(changes.size(), is(1));
        assertThat(changes.get(0), is(containerChange));
    }

    @Test
    public void testCreateFromMultipleChanges() {
        List<String> rootPath = Collections.singletonList("root");
        List<String> firstChildPath = Arrays.asList("root", "firstChild");
        List<String> secondChildPath = Arrays.asList("root", "secondChild");

        Object valueChangedAffected = Mockito.mock(Object.class);
        CfObjectValueChanged valueChanged = new CfObjectValueChanged(
                valueChangedAffected, "someNameForChangedValue", firstChildPath, "before", "after");

        Object mapChangeAffected = Mockito.mock(Object.class);
        List<CfMapValueChanged> changedMapValues = Collections.singletonList(Mockito.mock(CfMapValueChanged.class));
        CfMapChange mapChange = new CfMapChange(
                mapChangeAffected, "someNameForChangedMap", rootPath, changedMapValues);

        Object removedObjectAffected = Mockito.mock(Object.class);
        CfRemovedObject removedObject = new CfRemovedObject(
                removedObjectAffected, "someNameForRemovedObject", secondChildPath);

        Object containerChangeAffected = Mockito.mock(Object.class);
        List<CfContainerValueChanged> changedContainerValues = Collections.singletonList(
                Mockito.mock(CfContainerValueChanged.class));
        CfContainerChange containerChange = new CfContainerChange(
                containerChangeAffected, "someNameForChangedContainer", firstChildPath, changedContainerValues);

        DiffNode rootNode = DiffTreeCreator.createFrom(
                Arrays.asList(valueChanged, mapChange, removedObject, containerChange));

        assertThat(rootNode.getParentNode(), is(nullValue()));
        assertThat(rootNode.getChildNodes().size(), is(2));
        assertThat(rootNode.isNewObject(), is(false));
        assertThat(rootNode.isRemovedObject(), is(false));

        List<CfChange> rootChanges = rootNode.getChanges();
        assertThat(rootChanges.size(), is(1));
        assertThat(rootChanges.get(0), is(mapChange));

        DiffNode firstChildNode = rootNode.getChild("firstChild");
        assertThat(firstChildNode.getParentNode(), is(rootNode));
        assertThat(firstChildNode.getChildNodes().isEmpty(), is(true));
        assertThat(firstChildNode.isNewObject(), is(false));
        assertThat(firstChildNode.isRemovedObject(), is(false));

        List<CfChange> firstChildNodeChanges = firstChildNode.getChanges();
        assertThat(firstChildNodeChanges.size(), is(2));
        assertThat(firstChildNodeChanges.contains(valueChanged), is(true));
        assertThat(firstChildNodeChanges.contains(containerChange), is(true));

        DiffNode secondChildNode = rootNode.getChild("secondChild");
        assertThat(secondChildNode.getParentNode(), is(rootNode));
        assertThat(secondChildNode.getChildNodes().isEmpty(), is(true));
        assertThat(secondChildNode.isNewObject(), is(false));
        assertThat(secondChildNode.isRemovedObject(), is(true));

        List<CfChange> secondChildNodeChanges = secondChildNode.getChanges();
        assertThat(secondChildNodeChanges.size(), is(1));
        assertThat(secondChildNodeChanges.get(0), is(removedObject));
    }

    @Test
    public void testCreateFromNull() {
        assertThrows(NullPointerException.class,
                () -> DiffTreeCreator.createFrom(null));
    }
}
