package cloud.foundry.cli.logic.diff;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.javers.core.diff.Change;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.LinkedList;

public class DiffTreeCreatorTest {

    @Test
    public void testInsertion() {
        DiffNode root = new DiffNode("root");
        LinkedList<String> pathToRoot = new LinkedList<>(Arrays.asList("root"));
        LinkedList<String> pathToChild = new LinkedList<>(Arrays.asList("root", "child"));
        LinkedList<String> pathToGrandchild = new LinkedList<>(Arrays.asList("root", "child", "grandchild"));
        Change rootChange = Mockito.mock(Change.class);
        Change childChange = Mockito.mock(Change.class);
        Change firstGrandchildChange = Mockito.mock(Change.class);
        Change secondGrandchildChange = Mockito.mock(Change.class);

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
        Change someChange = Mockito.mock(Change.class);

        assertThrows(IllegalArgumentException.class,
                () -> DiffTreeCreator.insert(root, illegalPath, someChange));
    }

    @Test
    public void testEmptyPath() {
        DiffNode root = new DiffNode("root");
        LinkedList<String> emptyPath = new LinkedList<>();
        Change someChange = Mockito.mock(Change.class);

        assertThrows(IllegalArgumentException.class,
                () -> DiffTreeCreator.insert(root, emptyPath, someChange));
    }
}