package cloud.foundry.cli.crosscutting.mapping;

import cloud.foundry.cli.crosscutting.exceptions.YamlTreeNodeNotFoundException;

import java.util.List;
import java.util.Map;

public class DescendingYamlTreeVisitor implements YamlTreeVisitor {

    private Object yamlTreeRoot;

    private YamlPointer currentPointer;
    private int currentNodeIndex;
    private Object resultingYamlTreeNode;

    public DescendingYamlTreeVisitor(Object yamlTreeRoot) {
        this.yamlTreeRoot = yamlTreeRoot;
        this.currentPointer = null;
        this.resultingYamlTreeNode = null;
    }

    public Object getResultingYamlTreeNode() {
        return resultingYamlTreeNode;
    }

    public void descend(YamlPointer pointer) {
        currentPointer = pointer;
        currentNodeIndex = 0;
        YamlTreeVisitor.visit(this, yamlTreeRoot);
    }

    @Override
    public void visitMapping(Map<Object, Object> mappingNode) {
        assertCurrentPointerIsSet();

        if (currentNodeIndex >= currentPointer.getNumberOfNodeNames()) {
            resultingYamlTreeNode = mappingNode;
            return;
        }

        String currentNodeName = currentPointer.getNodeName(currentNodeIndex);
        if (!mappingNode.containsKey(currentNodeName)) {
            throw new YamlTreeNodeNotFoundException("Could not find the key in a mapping",
                    currentPointer, currentNodeIndex);
        }
        Object descendantNode = mappingNode.get(currentNodeName);

        ++currentNodeIndex;
        YamlTreeVisitor.visit(this, descendantNode);
    }

    @Override
    public void visitSequence(List<Object> sequenceNode) {
        assertCurrentPointerIsSet();

        if (currentNodeIndex >= currentPointer.getNumberOfNodeNames()) {
            resultingYamlTreeNode = sequenceNode;
            return;
        }

        String currentNodeName = currentPointer.getNodeName(currentNodeIndex);
        int listIndex;
        try {
            listIndex = Integer.parseInt(currentNodeName);
        } catch (NumberFormatException e) {
            throw new YamlTreeNodeNotFoundException("Could not convert a list index to an integer",
                    currentPointer, currentNodeIndex);
        }
        if (listIndex < 0 || listIndex >= sequenceNode.size()) {
            throw new YamlTreeNodeNotFoundException("A list index is out of range",
                    currentPointer, currentNodeIndex);
        }

        Object descendantNode = sequenceNode.get(listIndex);

        ++currentNodeIndex;
        YamlTreeVisitor.visit(this, descendantNode);
    }

    @Override
    public void visitScalar(Object scalar) {
        assertCurrentPointerIsSet();

        if (currentNodeIndex != currentPointer.getNumberOfNodeNames()) {
            throw new YamlTreeNodeNotFoundException("The pointer references a non-existent node",
                    currentPointer, currentNodeIndex);
        }

        resultingYamlTreeNode = scalar;
    }

    private void assertCurrentPointerIsSet() {
        if (currentPointer == null) {
            throw new IllegalStateException("The yaml pointer was not set before a yaml tree node was visited");
        }
    }
}
