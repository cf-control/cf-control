package cloud.foundry.cli.crosscutting.mapping;

import cloud.foundry.cli.crosscutting.exceptions.InvalidPointerException;
import cloud.foundry.cli.crosscutting.util.FileUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class RefResolvingYamlTreeVisitor implements YamlTreeVisitor {

    private static final String REF_INDICATOR = "$ref";

    private final Object yamlTreeRoot;
    private final Yaml yamlProcessor;
    private boolean alreadyResolved;
    private Object nodeToOverwrite;


    public RefResolvingYamlTreeVisitor(Object yamlTreeRoot, Yaml yamlParser) {
        this.yamlTreeRoot = yamlTreeRoot;
        this.yamlProcessor = yamlParser;
        alreadyResolved = false;
        nodeToOverwrite = null;
    }

    public Object resolveRefs() {
        //Man kann einen RefResolver nur einmal benutzen
        if (alreadyResolved) {
            throw new RuntimeException("Already Resolved error");
        }
        //start visit process
        nodeToOverwrite = yamlTreeRoot;
        YamlTreeVisitor.visit(this, yamlTreeRoot);
        alreadyResolved = true;
        return nodeToOverwrite;
    }


    @Override
    public void visitMapping(Map<Object, Object> mappingNode) {

        if (!mappingNode.containsKey(REF_INDICATOR)) {
            //We expect that the keys are scalar, so we only need to iterate over the values
            for (Map.Entry<Object, Object> entry : mappingNode.entrySet()) {
                YamlTreeVisitor.visit(this, entry.getValue());
                //Überschreibe o mit nodeToOverwrite
                entry.setValue(nodeToOverwrite);
            }
            //Kein ref gefunden
            nodeToOverwrite = mappingNode;
            return;
        }
        Object refValueNode = mappingNode.get(REF_INDICATOR);
        if (!(refValueNode instanceof String)) {
            throw new IllegalArgumentException("Ref no String");
        }
        String refValue = (String) refValueNode;
        if (refValue.matches(".*#.*#.*")) {
            throw new IllegalArgumentException("More than one # occurences.");
        }
        String[] splittedRefValue = refValue.split("#");
        //Regex only tolerates exactly 1 # in a Ref
        assert splittedRefValue.length <= 2;
        String filePath = splittedRefValue[0];
        String yamlPointerString = (splittedRefValue.length == 1) ? null : splittedRefValue[1];
        //TODO: Check if local or remote File, for now try local File
        try {
            String file = FileUtils.readLocalFile(filePath);
            Object yamlRefTree = this.yamlProcessor.load(file);
            if (yamlPointerString != null) {
                YamlPointer yamlPointer = new YamlPointer(yamlPointerString);
                DescendingYamlTreeVisitor descendingYamlTreeVisitor = new DescendingYamlTreeVisitor(yamlRefTree);
                descendingYamlTreeVisitor.descend(yamlPointer);
                yamlRefTree = descendingYamlTreeVisitor.getResultingYamlTreeNode();
            }
            //For bedding in the ref
            this.nodeToOverwrite = yamlRefTree;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidPointerException e) {
            throw e;
        }
    }

    @Override
    public void visitSequence(List<Object> sequenceNode) {

        for (int i = 0; i < sequenceNode.size(); i++) {
            YamlTreeVisitor.visit(this, sequenceNode.get(i));
            //Überschreibe o mit nodeToOverwrite
            sequenceNode.set(i, nodeToOverwrite);
        }
        nodeToOverwrite = sequenceNode;
    }

    @Override
    public void visitScalar(Object scalar) {
        nodeToOverwrite = scalar;
        return;
    }
}
