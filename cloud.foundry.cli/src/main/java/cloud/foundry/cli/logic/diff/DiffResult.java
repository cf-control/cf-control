package cloud.foundry.cli.logic.diff;

import static com.google.common.base.Preconditions.checkNotNull;

import cloud.foundry.cli.crosscutting.mapping.validation.ListField;
import cloud.foundry.cli.crosscutting.mapping.validation.MapField;
import cloud.foundry.cli.crosscutting.mapping.validation.ObjectPropertyValidation;
import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationBean;
import cloud.foundry.cli.crosscutting.mapping.beans.ConfigBean;
import cloud.foundry.cli.crosscutting.mapping.beans.ServiceBean;
import cloud.foundry.cli.crosscutting.mapping.beans.SpecBean;
import cloud.foundry.cli.crosscutting.mapping.beans.TargetBean;
import cloud.foundry.cli.crosscutting.mapping.validation.ScalarField;
import cloud.foundry.cli.logic.diff.change.CfChange;
import cloud.foundry.cli.logic.diff.change.container.CfContainerChange;
import cloud.foundry.cli.logic.diff.change.object.CfObjectValueChanged;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * This class is the result of a diff operation.
 * It serves as a wrapper for a complete tree consisting of diff nodes. It provides convenience methods for
 * getting changes of certain beans.
 */
public class DiffResult {

    // lists the field names of bean classes that are needed in this class
    private static final String TARGET_FIELD_NAME = "target";
    private static final String SPEC_FIELD_NAME = "spec";
    private static final String APPS_FIELD_NAME = "apps";
    private static final String SERVICES_FIELD_NAME = "services";
    private static final String SPACE_DEVELOPERS_PROPERTY_NAME = "spaceDevelopers";
    private static final String API_VERSION_PROPERTY_NAME = "apiVersion";

    // ensure that the bean classes have fields with according names and types
    static {
        ObjectPropertyValidation.checkFieldExists(
                new ScalarField(ConfigBean.class, TARGET_FIELD_NAME, TargetBean.class));

        ObjectPropertyValidation.checkFieldExists(
                new ScalarField(ConfigBean.class, SPEC_FIELD_NAME, SpecBean.class));

        ObjectPropertyValidation.checkMapExists(
                new MapField(SpecBean.class, APPS_FIELD_NAME, String.class, ApplicationBean.class));

        ObjectPropertyValidation.checkMapExists(
                new MapField(SpecBean.class, SERVICES_FIELD_NAME, String.class, ServiceBean.class));

        ObjectPropertyValidation.checkListExists(
                new ListField(SpecBean.class, SPACE_DEVELOPERS_PROPERTY_NAME, String.class));

        ObjectPropertyValidation.checkFieldExists(
                new ScalarField(ConfigBean.class, API_VERSION_PROPERTY_NAME, String.class));
    }

    private final DiffNode rootNode;
    private final DiffNode targetNode;
    private final DiffNode specNode;
    private final DiffNode appsNode;
    private final DiffNode servicesNode;

    /**
     * Creates the diff result using the root node of a diff tree. It is assumed that the root node is the result of
     * diffing two {@link ConfigBean config beans}.
     * @param rootNode the root node of the diff tree
     * @throws NullPointerException if the argument is null
     */
    public DiffResult(@Nonnull DiffNode rootNode) {
        checkNotNull(rootNode);
        this.rootNode = rootNode;
        this.targetNode = rootNode.getChild(TARGET_FIELD_NAME);
        this.specNode = rootNode.getChild(SPEC_FIELD_NAME);
        this.appsNode = specNode == null ? null : specNode.getChild(APPS_FIELD_NAME);
        this.servicesNode = specNode == null ? null : specNode.getChild(SERVICES_FIELD_NAME);
    }

    /**
     * @return a map containing the name of the app as key and the corresponding list of changes as entry
     */
    public Map<String, List<CfChange>> getApplicationChanges() {
        return getAllChangesFromMapNode(appsNode);
    }

    /**
     * @return a map containing the name of the service as key and the corresponding list of changes as entry
     */
    public Map<String, List<CfChange>> getServiceChanges() {
        return getAllChangesFromMapNode(servicesNode);
    }

    private Map<String, List<CfChange>> getAllChangesFromMapNode(DiffNode node) {
        Map<String, List<CfChange>> mapChanges = new HashMap<>();

        if (node != null) {
            for (DiffNode childNode : node.getChildNodes()) {
                mapChanges.put(childNode.getPropertyName(), getAllChanges(childNode));
            }
        }

        return mapChanges;
    }

    /**
     * @return a list of changes that occurred on the target bean
     */
    public List<CfChange> getTargetChanges() {
        if (targetNode != null) {
            return getAllChanges(targetNode);
        }
        return Collections.emptyList();
    }

    private List<CfChange> getAllChanges(DiffNode node) {
        List<CfChange> result = new LinkedList<>();
        doGetAllChanges(node, result);
        return result;
    }

    private void doGetAllChanges(DiffNode node, List<CfChange> list) {
        list.addAll(node.getChanges());
        for (DiffNode child : node.getChildNodes()) {
            doGetAllChanges(child, list);
        }
    }

    /**
     * @return the change object if there was a change else null
     */
    public CfContainerChange getSpaceDevelopersChange() {
        return (CfContainerChange) getChange(specNode, SPACE_DEVELOPERS_PROPERTY_NAME);
    }

    /**
     * @return the change object if there was a change else null
     */
    public CfObjectValueChanged getApiVersionChange() {
        return (CfObjectValueChanged) getChange(rootNode, API_VERSION_PROPERTY_NAME);
    }

    private CfChange getChange(DiffNode node, String propertyName) {
        if (node != null) {
            return node.getChanges()
                    .stream()
                    .filter(change -> propertyName.equals(change.getPropertyName()))
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }


}

