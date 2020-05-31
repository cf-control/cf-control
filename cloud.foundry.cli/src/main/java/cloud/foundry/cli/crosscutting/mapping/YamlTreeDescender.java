package cloud.foundry.cli.crosscutting.mapping;

import cloud.foundry.cli.crosscutting.exceptions.YamlTreeNodeNotFoundException;

import java.util.List;
import java.util.Map;

public class YamlTreeDescender implements YamlTreeVisitor {

    private Object yamlTreeRoot;

    private YamlPointer currentPointer;
    private int currentNodeIndex;
    private Object resultingYamlTreeNode;

    private YamlTreeDescender(Object yamlTreeRoot, YamlPointer pointer) {
        this.yamlTreeRoot = yamlTreeRoot;
        this.currentPointer = pointer;
        this.currentNodeIndex = 0;
        this.resultingYamlTreeNode = null;
    }

    public static Object descend(Object yamlTreeRoot, YamlPointer yamlPointer) {
        YamlTreeDescender descendingVisitor = new YamlTreeDescender(yamlTreeRoot, yamlPointer);

        return descendingVisitor.doDescend();
    }

    private Object doDescend() {
        YamlTreeVisitor.visit(this, yamlTreeRoot);
        return resultingYamlTreeNode;
    }

    @Override
    public void visitMapping(Map<Object, Object> mappingNode) {
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
        if (currentNodeIndex != currentPointer.getNumberOfNodeNames()) {
            throw new YamlTreeNodeNotFoundException("The pointer references a non-existent node",
                    currentPointer, currentNodeIndex);
        }

        resultingYamlTreeNode = scalar;
    }
}
