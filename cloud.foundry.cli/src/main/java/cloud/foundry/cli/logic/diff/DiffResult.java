package cloud.foundry.cli.logic.diff;

import cloud.foundry.cli.crosscutting.mapping.beans.ConfigBean;
import cloud.foundry.cli.crosscutting.mapping.beans.ServiceBean;
import cloud.foundry.cli.crosscutting.mapping.beans.SpecBean;
import org.javers.core.diff.Change;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * This class is the result of a diff operation.
 * It serves as a wrapper for a complete tree consisting of diff nodes. It provides convenience methods for
 * getting changes of certain beans.
 */
public class DiffResult {

    // lists the field names of bean classes that are needed in this class
    private static final String SPEC_FIELD_NAME = "spec";
    private static final String SERVICES_FIELD_NAME = "services";

    // ensure that the bean classes have fields with according names and types
    static {
        checkFieldExists(ConfigBean.class, SPEC_FIELD_NAME, SpecBean.class);
        checkMapExists(SpecBean.class, SERVICES_FIELD_NAME, String.class, ServiceBean.class);
    }

    private DiffNode rootNode;

    public DiffResult(DiffNode rootNode) {
        this.rootNode = rootNode;
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

    private static void checkMapExists(Class<?> classWithField, String fieldName, Class<?> keyType, Class<?> valueType) {
        Field field = checkFieldExists(classWithField, fieldName, Map.class);
        ParameterizedType genericType = (ParameterizedType) field.getGenericType();
        Type[] genericTypes = genericType.getActualTypeArguments();

        assert genericTypes.length == 2;
        assert genericTypes[0].equals(keyType);
        assert genericTypes[1].equals(valueType);
    }

    private DiffNode getServicesNode() {
        DiffNode specNode = rootNode.getChild(SPEC_FIELD_NAME);
        return specNode == null ? null : specNode.getChild(SERVICES_FIELD_NAME);
    }

    public Map<String, List<Change>> getServiceChangesByServiceName() {
        DiffNode servicesNode = getServicesNode();
        if (servicesNode == null) {
            return Collections.EMPTY_MAP;
        }
        //TODO get names and changes from the services node and generate the resulting map
        return null;
    }
}
