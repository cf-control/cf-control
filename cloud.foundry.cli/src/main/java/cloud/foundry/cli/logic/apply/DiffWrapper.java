package cloud.foundry.cli.logic.apply;

import static com.google.common.base.Preconditions.checkNotNull;

import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationBean;
import cloud.foundry.cli.crosscutting.mapping.beans.ConfigBean;
import cloud.foundry.cli.crosscutting.mapping.beans.SpecBean;
import cloud.foundry.cli.logic.diff.DiffNode;
import cloud.foundry.cli.logic.diff.change.CfChange;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collections;
import java.util.HashSet;

/**
 * This class is the result of a diff operation.
 * It serves as a wrapper for a complete tree consisting of diff nodes. It provides convenience methods for
 * getting changes of certain beans.
 */
public class DiffWrapper {

    // lists the field names of bean classes that are needed in this class
    private static final String SPEC_FIELD_NAME = "spec";
    private static final String APPS_FIELD_NAME = "apps";

    // ensure that the bean classes have fields with according names and types
    static {
        checkFieldExists(ConfigBean.class, SPEC_FIELD_NAME, SpecBean.class);
        checkMapExists(SpecBean.class, APPS_FIELD_NAME, String.class, ApplicationBean.class);
    }

    private final DiffNode rootNode;
    private final DiffNode specNode;
    private final DiffNode appsNode;

    public DiffWrapper(@Nonnull DiffNode rootNode) {
        checkNotNull(rootNode);
        this.rootNode = rootNode;
        this.specNode = rootNode.getChild(SPEC_FIELD_NAME);
        this.appsNode = specNode == null ? null : specNode.getChild(APPS_FIELD_NAME);
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
}

