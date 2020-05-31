package cloud.foundry.cli.crosscutting.mapping;

import java.util.List;
import java.util.Map;

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
}
