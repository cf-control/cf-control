package cloud.foundry.cli.crosscutting.mapping;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Map;

public class YamlTreeUtils {

    public static Object getDescendantNode(Object yamlTree, String yamlPointer) {
        // TODO custom exceptions in case of error:
        // e.g. node in path does not exist
        return null;
    }

    public static void visit(YamlTreeVisitor visitor, Object yamlTreeNode) {
        if (yamlTreeNode instanceof List) {
            visitor.visitSequence((List<Object>) yamlTreeNode);
        } else if (yamlTreeNode instanceof Map) {
            visitor.visitMapping((Map<Object, Object>) yamlTreeNode);
        } else {
            visitor.visitScalar(yamlTreeNode);
        }
    }
}
