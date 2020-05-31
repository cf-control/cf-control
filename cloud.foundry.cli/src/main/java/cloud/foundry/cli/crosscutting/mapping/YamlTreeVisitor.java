package cloud.foundry.cli.crosscutting.mapping;

import java.util.List;
import java.util.Map;

/**
 * TODO documentation
 */
public interface YamlTreeVisitor {

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
