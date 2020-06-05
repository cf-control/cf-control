package cloud.foundry.cli.logic.diff.output;

import cloud.foundry.cli.logic.diff.DiffNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class DiffNodeTest {

    @Test
    public void testIsRoot() {
        DiffNode rootNode = new DiffNode("root");
        assert rootNode.isRoot();

        DiffNode childNode = new DiffNode("child", rootNode);
        assert !childNode.isRoot();
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
            DiffNode childNode = new DiffNode("child" + i, nodes.get(i));
            nodes.add(childNode);
        }

        // and finally, we check that the depths are correct
        // the first item (index 0) is the root we created above, so it should have a depth of 0
        // the second item (index 1) is a child of said root node, and should have a depth of 1 therefore
        // and so on, until we reach the end of the list
        for (int i = 0; i < nodes.size(); ++i) {
            assert nodes.get(i).getDepth() == i;
        }
    }

}
