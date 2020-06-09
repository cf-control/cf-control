package cloud.foundry.cli.crosscutting.mapping;

import static com.google.common.base.Preconditions.checkElementIndex;
import static com.google.common.base.Preconditions.checkNotNull;

import cloud.foundry.cli.crosscutting.exceptions.YamlTreeNodeNotFoundException;

import java.util.List;
import java.util.Map;

/**
 * This class is responsible to descend a yaml tree according to a specific yaml pointer.
 *
 * It performs this task by implementing the {@link YamlTreeVisitor} interface. Its visitor-based implementation is
 * entirely hidden to the user of this class.
 */
public class YamlTreeDescender implements YamlTreeVisitor {

    private final Object yamlTreeRoot;
    private final YamlPointer pointer;

    private int nodeIndex;
    private Object resultingYamlTreeNode;

    private YamlTreeDescender(Object yamlTreeRoot, YamlPointer pointer) {
        this.yamlTreeRoot = yamlTreeRoot;
        this.pointer = pointer;
        this.nodeIndex = 0;
        this.resultingYamlTreeNode = null;
    }

    /**
     * Descends the yaml tree according to the specified nodes in the yaml pointer. The node of the tree to which the
     * pointer points to is returned.
     * @param yamlTreeRoot the root of a yaml tree from which the descending procedure is started
     * @param yamlPointer the pointer that specifies the nodes to descend
     * @return the node to which the pointer points to
     * @throws YamlTreeNodeNotFoundException if a node that is specified in the pointer cannot be found
     * @throws NullPointerException if the yaml pointer parameter is null
     */
    public static Object descend(Object yamlTreeRoot, YamlPointer yamlPointer) {
        checkNotNull(yamlPointer);

        YamlTreeDescender descendingVisitor = new YamlTreeDescender(yamlTreeRoot, yamlPointer);

        return descendingVisitor.doDescend();
    }

    private Object doDescend() {
        YamlTreeVisitor.visit(this, yamlTreeRoot);
        return resultingYamlTreeNode;
    }

    /**
     * Descends into a value node of the specified mapping or stops, if the target node of the pointer was reached.
     * @param mappingNode the mapping node to be descended
     * @throws YamlTreeNodeNotFoundException if the pointer specifies a key that is not present in the mapping node
     *                                       or if a descendant node of the mapping cannot be found
     */
    @Override
    public void visitMapping(Map<Object, Object> mappingNode) {
        if (nodeIndex == pointer.getNumberOfNodeNames()) {
            resultingYamlTreeNode = mappingNode;
            return;
        }

        String currentNodeName = pointer.getNodeName(nodeIndex);
        if (!mappingNode.containsKey(currentNodeName)) {
            throw new YamlTreeNodeNotFoundException("Could not find the key '" + currentNodeName + "'");
        }
        Object descendantNode = mappingNode.get(currentNodeName);

        ++nodeIndex;
        YamlTreeVisitor.visit(this, descendantNode);
    }

    /**
     * Descends into a node of the specified sequence or stops, if the target node of the pointer was reached.
     * @param sequenceNode the sequence node to be descended
     * @throws YamlTreeNodeNotFoundException if the pointer specifies an invalid node index for this sequence
     *                                       or if a descendant node of the sequence cannot be found
     */
    @Override
    public void visitSequence(List<Object> sequenceNode) {
        if (nodeIndex == pointer.getNumberOfNodeNames()) {
            resultingYamlTreeNode = sequenceNode;
            return;
        }

        String currentNodeName = pointer.getNodeName(nodeIndex);
        int listIndex;
        try {
            listIndex = Integer.parseInt(currentNodeName);
        } catch (NumberFormatException e) {
            throw new YamlTreeNodeNotFoundException("Could not convert '" + currentNodeName + "' to a list index");
        }
        if (listIndex < 0 || listIndex >= sequenceNode.size()) {
            throw new YamlTreeNodeNotFoundException("The list index '" + listIndex + "' is out of range");
        }

        Object descendantNode = sequenceNode.get(listIndex);

        ++nodeIndex;
        YamlTreeVisitor.visit(this, descendantNode);
    }

    /**
     * Stops the descending process.
     * @param scalarNode the scalar node to be descended
     * @throws YamlTreeNodeNotFoundException if the pointer specifies a node beyond the scalar node parameter
     */
    @Override
    public void visitScalar(Object scalarNode) {
        if (nodeIndex < pointer.getNumberOfNodeNames()) {
            throw new YamlTreeNodeNotFoundException("Cannot descend further to '" +
                    pointer.getNodeName(nodeIndex) + "' because a scalar was reached");
        }

        resultingYamlTreeNode = scalarNode;
    }
}
