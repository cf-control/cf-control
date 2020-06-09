package cloud.foundry.cli.logic.diff;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cloud.foundry.cli.logic.diff.change.CfChange;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.LinkedList;

/**
 * Test for {@link DiffTreeCreator}
 */
public class DiffTreeCreatorTest {

    @Test
    public void testInsertion() {
        DiffNode root = new DiffNode("root");
        LinkedList<String> pathToRoot = new LinkedList<>(Arrays.asList("root"));
        LinkedList<String> pathToChild = new LinkedList<>(Arrays.asList("root", "child"));
        LinkedList<String> pathToGrandchild = new LinkedList<>(Arrays.asList("root", "child", "grandchild"));
        CfChange rootChange = Mockito.mock(CfChange.class);
        CfChange childChange = Mockito.mock(CfChange.class);
        CfChange firstGrandchildChange = Mockito.mock(CfChange.class);
        CfChange secondGrandchildChange = Mockito.mock(CfChange.class);

        // the lists are cloned because they are modified during the insertion procedure
        DiffTreeCreator.insert(root, (LinkedList<String>) pathToGrandchild.clone(), firstGrandchildChange);
        DiffTreeCreator.insert(root, (LinkedList<String>) pathToRoot.clone(), rootChange);
        DiffTreeCreator.insert(root, (LinkedList<String>) pathToChild.clone(), childChange);
        DiffTreeCreator.insert(root, (LinkedList<String>) pathToGrandchild.clone(), secondGrandchildChange);


        assertThat(root.getParentNode(), is(nullValue()));
        assertThat(root.getChildNodes().size(), is(1));
        assertThat(root.getChanges().size(), is(1));
        assertThat(root.getChanges().get(0), is(rootChange));

        DiffNode child = root.getChild("child");
        assertThat(child, is(notNullValue()));
        assertThat(child.getChildNodes().size(), is(1));
        assertThat(child.getParentNode(), is(root));
        assertThat(child.getChanges().size(), is(1));
        assertThat(child.getChanges().get(0), is(childChange));

        DiffNode grandchild = child.getChild("grandchild");
        assertThat(grandchild, is(notNullValue()));
        assertThat(grandchild.getChildNodes().isEmpty(), is(true));
        assertThat(grandchild.getParentNode(), is(child));
        assertThat(grandchild.getChanges().size(), is(2));
        assertThat(grandchild.getChanges().get(0), is(firstGrandchildChange));
        assertThat(grandchild.getChanges().get(1), is(secondGrandchildChange));
    }

    @Test
    public void testIllegalPath() {
        DiffNode root = new DiffNode("root");
        LinkedList<String> illegalPath = new LinkedList<>(Arrays.asList("notRoot", "someChild"));
        CfChange someChange = Mockito.mock(CfChange.class);

        assertThrows(IllegalArgumentException.class,
                () -> DiffTreeCreator.insert(root, illegalPath, someChange));
    }

    @Test
    public void testEmptyPath() {
        DiffNode root = new DiffNode("root");
        LinkedList<String> emptyPath = new LinkedList<>();
        CfChange someChange = Mockito.mock(CfChange.class);

        assertThrows(IllegalArgumentException.class,
                () -> DiffTreeCreator.insert(root, emptyPath, someChange));
    }

    @Test
    public void testNullAsArguments() {
        DiffNode root = new DiffNode("root");
        LinkedList<String> path = new LinkedList<>(Arrays.asList("someRoot", "someChild"));
        CfChange change = Mockito.mock(CfChange.class);

        assertThrows(NullPointerException.class,
                () -> DiffTreeCreator.insert(null, path, change));

        assertThrows(NullPointerException.class,
                () -> DiffTreeCreator.insert(root, null, change));

        assertThrows(NullPointerException.class,
                () -> DiffTreeCreator.insert(root, path, null));
    }
}
