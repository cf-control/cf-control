package cloud.foundry.cli.crosscutting.mapping;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Map;

/**
 * This provides a visitor interface for yaml trees. The node types of a yaml tree structure are {@link Map mappings},
 * {@link List sequences} and {@link Object scalars}. Scalars are considered as atomic leafs of the tree. Scalars are
 * neither mappings nor sequences.
 */
public interface YamlTreeVisitor {

    /**
     * Calls the according visit method of a visitor for a yaml tree node.
     * @param visitor the visitor to visit the node
     * @param yamlTreeNode the yaml tree node to be visited
     * @throws NullPointerException if the visitor parameter is null
     */
    static void visit(YamlTreeVisitor visitor, Object yamlTreeNode) {
        checkNotNull(visitor);

        if (yamlTreeNode instanceof List) {
            visitor.visitSequence((List<Object>) yamlTreeNode);
        } else if (yamlTreeNode instanceof Map) {
            visitor.visitMapping((Map<Object, Object>) yamlTreeNode);
        } else {
            visitor.visitScalar(yamlTreeNode);
        }
    }

    /**
     * This method is called when the visitor visits a mapping node.
     * @param mappingNode the mapping node to be visited
     */
    void visitMapping(Map<Object, Object> mappingNode);

    /**
     * This method is called when the visitor visits a sequence node.
     * @param sequenceNode the sequence node to be visited
     */
    void visitSequence(List<Object> sequenceNode);

    /**
     * This method is called when the visitor visits a scalar node.
     * @param scalarNode the scalar node to be visited
     */
    void visitScalar(Object scalarNode);
}
