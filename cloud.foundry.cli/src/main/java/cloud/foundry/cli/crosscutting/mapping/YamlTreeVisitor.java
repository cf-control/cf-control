package cloud.foundry.cli.crosscutting.mapping;

import java.util.List;
import java.util.Map;

/**
 * This provides a visitor interface for yaml trees. The node types of a yaml tree structure are {@link Map mappings},
 * {@link List sequences} and {@link Object scalars}. Scalars are considered as atomic leafs of the tree. Scalars are
 * neither mappings nor sequences.
 */
public interface YamlTreeVisitor {

    /**
     * TODO documentation
     */
    static void visit(YamlTreeVisitor visitor, Object yamlTreeNode) {
        if (yamlTreeNode instanceof List) {
            visitor.visitSequence((List<Object>) yamlTreeNode);
        } else if (yamlTreeNode instanceof Map) {
            visitor.visitMapping((Map<Object, Object>) yamlTreeNode);
        } else {
            visitor.visitScalar(yamlTreeNode);
        }
    }

    /**
     * TODO documentation
     */
    void visitMapping(Map<Object, Object> mappingNode);

    /**
     * TODO documentation
     */
    void visitSequence(List<Object> sequenceNode);

    /**
     * TODO documentation
     */
    void visitScalar(Object scalar);
}
