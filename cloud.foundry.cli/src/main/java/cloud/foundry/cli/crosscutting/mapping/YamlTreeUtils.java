package cloud.foundry.cli.crosscutting.mapping;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Map;

public class YamlTreeUtils {

    public static Object getDescendantNode(Object yamlTree, String yamlPointer) {
        YamlPointer pointer = new YamlPointer(yamlPointer);

        DescendingYamlTreeVisitor descendingVisitor = new DescendingYamlTreeVisitor(yamlTree);
        descendingVisitor.descend(pointer);

        return descendingVisitor.getResultingYamlTreeNode();
    }
}
