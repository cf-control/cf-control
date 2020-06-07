package cloud.foundry.cli.logic.diff;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import cloud.foundry.cli.logic.diff.change.CfChange;
import cloud.foundry.cli.logic.diff.change.ChangeType;
import cloud.foundry.cli.logic.diff.change.object.CfObjectChange;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class DiffNodeTest {

    @Test
    public void testSingleNode() {
        DiffNode node = new DiffNode("propertyName");
        LinkedList<CfChange> changes = new LinkedList<>();
        for (int counter = 0; counter < 42; ++counter) {
            CfChange mockedChange = Mockito.mock(CfChange.class);
            changes.add(mockedChange);

            node.addChange(mockedChange);
        }

        assertThat(node.getParentNode(), is(nullValue()));
        assertThat(node.getChanges(), equalTo(changes));
        assertThat(node.getChildNodes().isEmpty(), is(true));
        assertThat(node.getPropertyName(), is("propertyName"));
        assertThat(node.getChild("someChild"), is(nullValue()));
        assertThat(node.isLeaf(), is(true));
        assertThat(node.isRoot(), is(true));
        assertThat(node.getDepth(), is(0));
        assertThat(node.isNewObject(), is(false));
        assertThat(node.isRemovedObject(), is(false));
    }

    @Test
    public void testEmptySingleNode() {
        DiffNode node = new DiffNode("propertyName");

        assertThat(node.getParentNode(), is(nullValue()));
        assertThat(node.getChanges().isEmpty(), is(true));
        assertThat(node.getChildNodes().isEmpty(), is(true));
        assertThat(node.getPropertyName(), is("propertyName"));
        assertThat(node.getChild("someChild"), is(nullValue()));
        assertThat(node.isLeaf(), is(true));
        assertThat(node.isRoot(), is(true));
        assertThat(node.getDepth(), is(0));
        assertThat(node.isNewObject(), is(false));
        assertThat(node.isRemovedObject(), is(false));
    }

    @Test
    public void testNewObjectNode() {
        DiffNode node = new DiffNode("propertyName");
        CfObjectChange newObjectChange = new CfObjectChange(null, ChangeType.ADDED);
        node.addChange(newObjectChange);

        assertThat(node.getChanges().size(), is(1));
        assertThat(node.getChanges().get(0), is(newObjectChange));
        assertThat(node.isNewObject(), is(true));
        assertThat(node.isRemovedObject(), is(false));
    }

    @Test
    public void testObjectRemovedNode() {
        DiffNode node = new DiffNode("propertyName");
        CfObjectChange objectRemovedChange = new CfObjectChange(null, ChangeType.REMOVED);
        node.addChange(objectRemovedChange);

        assertThat(node.getChanges().size(), is(1));
        assertThat(node.getChanges().get(0), is(objectRemovedChange));
        assertThat(node.isNewObject(), is(false));
        assertThat(node.isRemovedObject(), is(true));
    }

    @Test
    public void testNodeWithChild() {
        DiffNode root = new DiffNode("root");
        DiffNode child = new DiffNode("child");
        root.addChild(child);

        assertThat(root.getParentNode(), is(nullValue()));
        assertThat(root.getChildNodes().size(), is(1));
        assertThat(root.getChildNodes().toArray()[0], is(child));
        assertThat(root.getPropertyName(), is("root"));
        assertThat(root.getChild("someChild"), is(nullValue()));
        assertThat(root.getChild("child"), is(child));
        assertThat(root.getDepth(), is(0));
        assertThat(root.isLeaf(), is(false));
        assertThat(root.isRoot(), is(true));

        assertThat(child.getParentNode(), is(root));
        assertThat(child.getChildNodes().isEmpty(), is(true));
        assertThat(child.getPropertyName(), is("child"));
        assertThat(child.getDepth(), is(1));
        assertThat(child.isLeaf(), is(true));
        assertThat(child.isRoot(), is(false));
    }

    @Test
    public void testDepth() {
        // we create a list of nodes, with one being a child of the other
        List<DiffNode> nodes = new ArrayList<>();

        // initialize list with one root node
        // this'll make the following loop less annoying to implement
        DiffNode rootNode = new DiffNode("root");
        nodes.add(rootNode);

        // now, let's add all the nodes
        // note that 1337 has been picked randomly
        for (int i = 0; i < 1337; ++i) {
            DiffNode childNode = new DiffNode("child" + i);
            nodes.get(i).addChild(childNode);
            nodes.add(childNode);
        }

        // and finally, we check that the depths are correct
        // the first item (index 0) is the root we created above, so it should have a depth of 0
        // the second item (index 1) is a child of said root node, and should have a depth of 1 therefore
        // and so on, until we reach the end of the list
        for (int i = 0; i < nodes.size(); ++i) {
            assertThat(nodes.get(i).getDepth(), is(i));
        }
    }

    @Test
    public void testAddingSameChildTwice() {
        DiffNode root = new DiffNode("root");
        DiffNode child = new DiffNode("child");
        root.addChild(child);
        root.addChild(child);

        assertThat(root.getParentNode(), is(nullValue()));
        assertThat(root.getChildNodes().size(), is(1));
        assertThat(root.getChildNodes().toArray()[0], is(child));

        assertThat(child.getParentNode(), is(root));
        assertThat(child.getChildNodes().isEmpty(), is(true));
    }

    @Test
    public void testUnmodifiabilityOfGetterCollections() {
        DiffNode root = new DiffNode("root");
        Class<?> unmodifiableCollectionClass = Collections.unmodifiableCollection(Collections.EMPTY_LIST).getClass();

        assertThat(unmodifiableCollectionClass.isInstance(root.getChanges()), is(true));
        assertThat(unmodifiableCollectionClass.isInstance(root.getChildNodes()), is(true));
    }
}
