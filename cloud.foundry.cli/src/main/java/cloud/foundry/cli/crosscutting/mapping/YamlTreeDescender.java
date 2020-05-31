package cloud.foundry.cli.crosscutting.mapping;

import cloud.foundry.cli.crosscutting.exceptions.YamlTreeNodeNotFoundException;

import java.util.List;
import java.util.Map;

/**
 * This class is responsible to descend a yaml tree according to a specific yaml pointer.
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
     * TODO documentation
     */
    public static Object descend(Object yamlTreeRoot, YamlPointer yamlPointer) {
        YamlTreeDescender descendingVisitor = new YamlTreeDescender(yamlTreeRoot, yamlPointer);

        return descendingVisitor.doDescend();
    }

    /**
     * TODO documentation
     */
    @Override
    public void visitMapping(Map<Object, Object> mappingNode) {
        if (nodeIndex >= pointer.getNumberOfNodeNames()) {
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
     * TODO documentation
     */
    @Override
    public void visitSequence(List<Object> sequenceNode) {
        if (nodeIndex >= pointer.getNumberOfNodeNames()) {
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
     * TODO documentation
     */
    @Override
    public void visitScalar(Object scalar) {
        if (nodeIndex != pointer.getNumberOfNodeNames()) {
            throw new YamlTreeNodeNotFoundException("Cannot descend further to '" +
                    pointer.getNodeName(nodeIndex) + "' because a scalar was reached");
        }

        resultingYamlTreeNode = scalar;
    }

    private Object doDescend() {
        YamlTreeVisitor.visit(this, yamlTreeRoot);
        return resultingYamlTreeNode;
    }
}
