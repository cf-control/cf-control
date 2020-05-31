package cloud.foundry.cli.crosscutting.mapping;

import cloud.foundry.cli.crosscutting.exceptions.RefResolvingException;
import cloud.foundry.cli.crosscutting.exceptions.YamlTreeNodeNotFoundException;
import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.crosscutting.util.FileUtils;
import org.apache.hc.core5.http.ProtocolException;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class RefResolver implements YamlTreeVisitor {

    private static final String REF_KEY = "$ref";

    private final Object yamlTreeRoot;
    private final Yaml yamlParser;
    private Object overridingNode;


    private RefResolver(Object yamlTreeRoot, Yaml yamlParser) {
        this.yamlTreeRoot = yamlTreeRoot;
        this.yamlParser = yamlParser;
        overridingNode = yamlTreeRoot;
    }

    public static Object resolveRefs(Object yamlTreeRoot, Yaml yamlParser) {
        Log.debug("Resolve", RefResolver.REF_KEY + "-occurrences");

        RefResolver refResolvingYamlTreeVisitor =
                new RefResolver(yamlTreeRoot, yamlParser);
        Object resolvedYamlTreeRoot = refResolvingYamlTreeVisitor.doResolveRefs();

        Log.debug("Resolving completed");
        return resolvedYamlTreeRoot;
    }

    @Override
    public void visitMapping(Map<Object, Object> mappingNode) {
        if (!mappingNode.containsKey(REF_KEY)) {
            for (Map.Entry<Object, Object> entry : mappingNode.entrySet()) {
                YamlTreeVisitor.visit(this, entry.getValue());

                // after visiting the value node of the mapping, the node itself might have changed due to a ref
                // resolution
                entry.setValue(overridingNode);
            }

            // this mapping node remains unchanged
            overridingNode = mappingNode;
            return;
        }

        Object refValueNode = mappingNode.get(REF_KEY);
        Log.debug("Encountered", RefResolver.REF_KEY + "-occurrence with value", String.valueOf(refValueNode));
        if (!(refValueNode instanceof String)) {
            throw new RefResolvingException("Encountered a '" + REF_KEY + "'-occurrence where its value is not of" +
                    " type string");
        }
        String refValue = (String) refValueNode;
        int beginningOfPointerIndex = refValue.lastIndexOf("#");
        String filePath;
        String yamlPointerString = null;
        if (beginningOfPointerIndex == -1) {
            filePath = refValue;
        } else {
            filePath = refValue.substring(0, beginningOfPointerIndex);
            yamlPointerString = refValue.substring(beginningOfPointerIndex);
        }
        Log.debug("Reading content of", filePath);
        String fileContent;
        try {
            fileContent = FileUtils.readLocalOrRemoteFile(filePath);
        } catch (IOException | ProtocolException exception) {
            throw new RefResolvingException(exception);
        }
        if (fileContent.isEmpty()) {
            throw new RefResolvingException("The file content of '" + filePath + "' is empty");
        }
        Object referredYamlTree = yamlParser.load(fileContent);
        if (yamlPointerString != null) {
            Log.debug("Getting contents at", yamlPointerString);
            YamlPointer yamlPointer;
            try {
                yamlPointer = new YamlPointer(yamlPointerString);
            } catch (IllegalArgumentException illegalArgumentException) {
                throw new RefResolvingException(illegalArgumentException);
            }
            try {
                referredYamlTree = YamlTreeDescender.descend(referredYamlTree, yamlPointer);
            } catch (YamlTreeNodeNotFoundException nodeNotFoundException) {
                throw new RefResolvingException(nodeNotFoundException);
            }
        }
        // this current mapping should be overridden by the referred yaml tree
        overridingNode = referredYamlTree;
    }

    @Override
    public void visitSequence(List<Object> sequenceNode) {
        for (int sequenceNodeIndex = 0; sequenceNodeIndex < sequenceNode.size(); ++sequenceNodeIndex) {
            YamlTreeVisitor.visit(this, sequenceNode.get(sequenceNodeIndex));

            // after visiting the sequence node, the node itself might have changed due to a ref resolution
            sequenceNode.set(sequenceNodeIndex, overridingNode);
        }
        // this sequence node remains unchanged
        overridingNode = sequenceNode;
    }

    @Override
    public void visitScalar(Object scalar) {
        // this scalar node remains unchanged
        overridingNode = scalar;
    }

    private Object doResolveRefs() {
        YamlTreeVisitor.visit(this, yamlTreeRoot);
        return overridingNode;
    }
}
