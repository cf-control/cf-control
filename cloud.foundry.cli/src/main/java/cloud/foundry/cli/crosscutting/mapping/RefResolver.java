package cloud.foundry.cli.crosscutting.mapping;

import cloud.foundry.cli.crosscutting.exceptions.RefResolvingException;
import cloud.foundry.cli.crosscutting.exceptions.YamlTreeNodeNotFoundException;
import cloud.foundry.cli.crosscutting.logging.Log;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
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

    private static final Log log = Log.getLog(RefResolver.class);

    /**
     * This key describes ref-occurrences in mappings.
     */
    public static final String REF_KEY = "$ref";

    private final Object yamlTreeRoot;
    private final String parentYamlFilePath;

    private Object overridingNode;

    private RefResolver(Object yamlTreeRoot, String parentYamlFilePath) {
        this.yamlTreeRoot = yamlTreeRoot;
        this.parentYamlFilePath = parentYamlFilePath;
        overridingNode = yamlTreeRoot;
    }

    /**
     * Resolves ref-occurrences in the specified yaml tree. Whenever a ref occurs the linked file is read and processed
     * into an additional yaml tree. If a yaml pointer is specified, the additional yaml
     * tree is descended to the node that is specified in the pointer. The additional yaml tree then overrides the node
     * where the ref occurred.
     * @param yamlTreeRoot the root of the yaml tree in which to resolve ref-occurrences
     * @param rootFilePath path to the root YAML file (used to resolve relative $ref entries)
     * @return the new root of the specified yaml tree after the ref-resolving process
     * @throws RefResolvingException if an error during the ref-resolution process occurs
     */
    public static Object resolveRefs(Object yamlTreeRoot, String rootFilePath) {
        log.debug("Resolving occurrences of", RefResolver.REF_KEY);

        RefResolver refResolvingYamlTreeVisitor = new RefResolver(yamlTreeRoot, rootFilePath);
        Object resolvedYamlTreeRoot = refResolvingYamlTreeVisitor.doResolveRefs();

        log.debug("Resolving completed");
        return resolvedYamlTreeRoot;
    }

    private Object doResolveRefs() {
        YamlTreeVisitor.visit(this, yamlTreeRoot);
        return overridingNode;
    }

    private void visitRegularMapping(Map<Object, Object> regularMappingNode) {
        for (Map.Entry<Object, Object> entry : regularMappingNode.entrySet()) {
            YamlTreeVisitor.visit(this, entry.getValue());

            // after visiting the value node of the mapping, the node itself might have changed due to a ref
            // resolution
            entry.setValue(overridingNode);
        }

        // this mapping node remains unchanged
        overridingNode = regularMappingNode;
    }

    private void visitRefMapping(Map<Object, Object> refMappingNode) {
        Object refValueNode = refMappingNode.get(REF_KEY);
        log.debug("Encountered", RefResolver.REF_KEY, "occurrence with value", String.valueOf(refValueNode));
        if (!(refValueNode instanceof String)) {
            throw new RefResolvingException("Encountered a '" + REF_KEY + "' occurrence where its value is not of" +
                    " type string");
        }
        String refValue = (String) refValueNode;
        String filePath = extractFilePath(refValue);
        YamlPointer yamlPointer = extractYamlPointer(refValue);

        String parentYamlFileDirectoryPath = Paths.get(parentYamlFilePath).getParent().toAbsolutePath().toString();
        String absoluteFilePath;

        try {
            absoluteFilePath = FileUtils.calculateAbsolutePath(filePath, parentYamlFileDirectoryPath);
        }
        catch (InvalidPathException e) {
            String message = "Unable to interpret path: " + e.getMessage();
            throw new RefResolvingException(message, e);
        }

        log.debug("Reading YAML file", filePath);
        Object referredYamlTree;
        try {
            referredYamlTree = YamlMapper.loadYamlTreeFromFilePath(absoluteFilePath);
        } catch (IOException ioException) {
            throw new RefResolvingException("Unable to read a referenced file: " + ioException.getMessage(),
                    ioException);
        }

        if (yamlPointer != null) {
            try {
                referredYamlTree = YamlTreeDescender.descend(referredYamlTree, yamlPointer);
            } catch (YamlTreeNodeNotFoundException nodeNotFoundException) {
                throw new RefResolvingException("A referenced node could not be found: " +
                        nodeNotFoundException.getMessage(), nodeNotFoundException);
            }
        }
        // this current mapping should be overridden by the referred yaml tree
        overridingNode = referredYamlTree;
    }

    private String extractFilePath(String refValue) {
        int beginningOfPointerIndex = refValue.lastIndexOf(YamlPointer.POINTER_START);
        if (beginningOfPointerIndex == -1) {
            return refValue;
        } else {
            return refValue.substring(0, beginningOfPointerIndex);
        }
    }

    private YamlPointer extractYamlPointer(String refValue) {
        int beginningOfPointerIndex = refValue.lastIndexOf(YamlPointer.POINTER_START);
        if (beginningOfPointerIndex == -1) {
            return null;
        }
        String yamlPointerString = refValue.substring(beginningOfPointerIndex);

        YamlPointer yamlPointer;
        try {
            yamlPointer = new YamlPointer(yamlPointerString);
        } catch (IllegalArgumentException illegalArgumentException) {
            throw new RefResolvingException("Encountered the pointer " + yamlPointerString +
                    " with an invalid syntax:" + illegalArgumentException.getMessage(), illegalArgumentException);
        }
        return yamlPointer;
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
            visitRegularMapping(mappingNode);
            return;
        }
        visitRefMapping(mappingNode);
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
}
