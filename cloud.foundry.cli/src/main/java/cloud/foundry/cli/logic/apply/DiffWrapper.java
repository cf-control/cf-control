package cloud.foundry.cli.logic.apply;

import static com.google.common.base.Preconditions.checkNotNull;

import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationBean;
import cloud.foundry.cli.crosscutting.mapping.beans.ConfigBean;
import cloud.foundry.cli.crosscutting.mapping.beans.ServiceBean;
import cloud.foundry.cli.crosscutting.mapping.beans.SpecBean;
import cloud.foundry.cli.crosscutting.mapping.beans.TargetBean;
import cloud.foundry.cli.logic.diff.DiffNode;
import cloud.foundry.cli.logic.diff.change.CfChange;
import cloud.foundry.cli.logic.diff.change.container.CfContainerChange;
import cloud.foundry.cli.logic.diff.change.object.CfObjectValueChanged;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
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
public class DiffWrapper {

    // lists the field names of bean classes that are needed in this class
    private static final String TARGET_FIELD_NAME = "target";
    private static final String SPEC_FIELD_NAME = "spec";
    private static final String APPS_FIELD_NAME = "apps";
    private static final String SERVICES_FIELD_NAME = "services";
    private static final String SPACE_DEVELOPERS_PROPERTY_NAME = "spaceDevelopers";
    private static final String API_VERSION_PROPERTY_NAME = "apiVersion";

    // ensure that the bean classes have fields with according names and types
    static {
        checkFieldExists(ConfigBean.class, TARGET_FIELD_NAME, TargetBean.class);
        checkFieldExists(ConfigBean.class, SPEC_FIELD_NAME, SpecBean.class);
        checkMapExists(SpecBean.class, APPS_FIELD_NAME, String.class, ApplicationBean.class);
        checkMapExists(SpecBean.class, SERVICES_FIELD_NAME, String.class, ServiceBean.class);
        checkFieldExists(SpecBean.class, SPACE_DEVELOPERS_PROPERTY_NAME, List.class);
        checkFieldExists(ConfigBean.class, API_VERSION_PROPERTY_NAME, String.class);
    }

    private final DiffNode rootNode;
    private final DiffNode targetNode;
    private final DiffNode specNode;
    private final DiffNode appsNode;
    private final DiffNode servicesNode;

    public DiffWrapper(@Nonnull DiffNode rootNode) {
        checkNotNull(rootNode);
        this.rootNode = rootNode;
        this.targetNode = rootNode.getChild(TARGET_FIELD_NAME);
        this.specNode = rootNode.getChild(SPEC_FIELD_NAME);
        this.appsNode = specNode == null ? null : specNode.getChild(APPS_FIELD_NAME);
        this.servicesNode = specNode == null ? null : specNode.getChild(SERVICES_FIELD_NAME);
    }

    private static Field checkFieldExists(Class<?> classWithField, String fieldName, Class<?> fieldType) {
        Field field;
        try {
            field = classWithField.getDeclaredField(fieldName);
        } catch (NoSuchFieldException noSuchFieldException) {
            throw new AssertionError(noSuchFieldException);
        }

        assert field.getType().equals(fieldType);

        return field;
    }

    private static void checkMapExists(Class<?> classWithField, String fieldName,
                                       Class<?> keyType, Class<?> valueType) {
        Field field = checkFieldExists(classWithField, fieldName, Map.class);
        ParameterizedType genericType = (ParameterizedType) field.getGenericType();
        Type[] genericTypes = genericType.getActualTypeArguments();

        assert genericTypes.length == 2;
        assert genericTypes[0].equals(keyType);
        assert genericTypes[1].equals(valueType);
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
        Map<String, List<CfChange>> serviceChanges = new HashMap<>();

        if (node != null) {
            for (DiffNode childNode : node.getChildNodes()) {
                serviceChanges.put(childNode.getPropertyName(), getAllChanges(childNode));
            }
        }

        return serviceChanges;
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

