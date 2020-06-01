package cloud.foundry.cli.crosscutting.mapping;

import static com.google.common.base.Preconditions.checkNotNull;

import cloud.foundry.cli.crosscutting.exceptions.RefResolvingException;
import cloud.foundry.cli.crosscutting.exceptions.YamlTreeNodeNotFoundException;
import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.crosscutting.util.FileUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * This class is responsible to resolve ref-occurrences in yaml trees. They can only appear in mapping nodes. They
 * refer to a yaml file (possibly on a different server) and optionally specify a yaml pointer for that yaml file.
 *
 * The class performs the resolving task by implementing the {@link YamlTreeVisitor} interface. Its visitor-based
 * implementation is entirely hidden to the user of this class.
 */
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

    /**
     * Resolves ref-occurrences in the specified yaml tree. Whenever a ref occurs the linked file is read and processed
     * into an additional yaml tree using the specified yaml parser. If a yaml pointer is specified, the additional yaml
     * tree is descended to the node that is specified in the pointer. The additional yaml tree then overrides the node
     * where the ref occurred.
     * @param yamlTreeRoot the root of the yaml tree in which to resolve ref-occurrences
     * @param yamlParser the yaml parser that processes the contents of linked files to yaml trees
     * @return the new root of the specified yaml tree after the ref-resolving process
     * @throws RefResolvingException if an error during the ref-resolution process occurs
     * @throws NullPointerException if the yaml parser parameter is null
     */
    public static Object resolveRefs(Object yamlTreeRoot, Yaml yamlParser) {
        checkNotNull(yamlParser);

        Log.debug("Resolve", RefResolver.REF_KEY + "-occurrences");

        RefResolver refResolvingYamlTreeVisitor =
                new RefResolver(yamlTreeRoot, yamlParser);
        Object resolvedYamlTreeRoot = refResolvingYamlTreeVisitor.doResolveRefs();

        Log.debug("Resolving completed");
        return resolvedYamlTreeRoot;
    }

    /**
     * Resolves ref-occurrences in the specified mapping node and in all its descendant nodes.
     * @param mappingNode the mapping node to be resolved
     * @throws RefResolvingException if the specified mapping node contains a ref-occurrence that cannot be resolved
     *                               or if a descendant node of the mapping cannot be resolved
     */
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
        int beginningOfPointerIndex = refValue.lastIndexOf(YamlPointer.POINTER_START);
        String filePath;
        String yamlPointerString = null;
        if (beginningOfPointerIndex == -1) {
            filePath = refValue;
        } else {
            filePath = refValue.substring(0, beginningOfPointerIndex);
            yamlPointerString = refValue.substring(beginningOfPointerIndex);
        }
        Log.debug("Reading content of", filePath);
        Object referredYamlTree;
        try (InputStream inputStream = FileUtils.openLocalOrRemoteFile(filePath)) {
            referredYamlTree = yamlParser.load(inputStream);
        } catch (IOException exception) {
            throw new RefResolvingException(exception);
        }

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

    /**
     * Resolves ref-occurrences in all descendant nodes of the specified sequence node.
     * @param sequenceNode the sequence node to be resolved
     * @throws RefResolvingException if a descendant node of the sequence cannot be resolved
     */
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

    /**
     * Leaves the scalar node unchanged
     * @param scalarNode the scalar node to be resolved
     */
    @Override
    public void visitScalar(Object scalarNode) {
        // this scalar node remains unchanged
        overridingNode = scalarNode;
    }

    private Object doResolveRefs() {
        YamlTreeVisitor.visit(this, yamlTreeRoot);
        return overridingNode;
    }
}
