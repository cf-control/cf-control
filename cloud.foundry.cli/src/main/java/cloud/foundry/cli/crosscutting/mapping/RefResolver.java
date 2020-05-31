package cloud.foundry.cli.crosscutting.mapping;

import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.crosscutting.util.FileUtils;
import org.apache.hc.core5.http.ProtocolException;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class RefResolver implements YamlTreeVisitor {

    private static final String REF_INDICATOR = "$ref";

    private final Object yamlTreeRoot;
    private final Yaml yamlProcessor;
    private Object nodeToOverwrite;


    private RefResolver(Object yamlTreeRoot, Yaml yamlParser) {
        this.yamlTreeRoot = yamlTreeRoot;
        this.yamlProcessor = yamlParser;
        nodeToOverwrite = yamlTreeRoot;
    }

    public static Object resolveRefs(Object yamlTreeRoot, Yaml yamlParser) {
        Log.debug("Resolve", RefResolver.REF_INDICATOR + "-occurrences");

        RefResolver refResolvingYamlTreeVisitor =
                new RefResolver(yamlTreeRoot, yamlParser);
        Object resolvedYamlTreeRoot = refResolvingYamlTreeVisitor.doResolveRefs();

        Log.debug("Resolving completed");
        return resolvedYamlTreeRoot;
    }

    private Object doResolveRefs() {
        YamlTreeVisitor.visit(this, yamlTreeRoot);
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
        Log.debug("Encountered", RefResolver.REF_INDICATOR + "-occurrence with value", refValue);
        if (refValue.matches(".*#.*#.*")) {
            throw new IllegalArgumentException("More than one # occurences.");
        }
        int indexOfPointerBeginning = refValue.indexOf("#");
        String filePath;
        String yamlPointerString = null;
        if (indexOfPointerBeginning == -1) {
            filePath = refValue;
        } else {
            filePath = refValue.substring(0, indexOfPointerBeginning);
            yamlPointerString = refValue.substring(indexOfPointerBeginning);
        }
        Log.debug("Reading contents of", filePath);
        try {
            String file = FileUtils.readLocalOrRemoteFile(filePath);
            if (file.isEmpty()) {
                throw new RuntimeException("empty file");
            }
            Object yamlRefTree = this.yamlProcessor.load(file);
            if (yamlPointerString != null) {
                Log.debug("Getting contents at", yamlPointerString);
                YamlPointer yamlPointer = new YamlPointer(yamlPointerString);
                yamlRefTree = YamlTreeDescender.descend(yamlRefTree, yamlPointer);
            }
            //For bedding in the ref
            this.nodeToOverwrite = yamlRefTree;
        } catch (IOException | ProtocolException e) {
            throw new RuntimeException(e);
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
    }
}
