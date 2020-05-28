package cloud.foundry.cli.crosscutting.mapping;

import java.util.List;
import java.util.Map;

public interface YamlTreeVisitor {

    public void visitMapping(Map<Object, Object> mappingNode);

    public void visitSequence(List<Object> sequenceNode);

    public void visitScalar(Object scalar);

}
