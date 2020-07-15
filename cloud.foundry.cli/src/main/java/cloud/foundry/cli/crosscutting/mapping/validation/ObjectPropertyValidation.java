package cloud.foundry.cli.crosscutting.mapping.validation;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * This class provides utility methods for static field property assertion.
 */
public class ObjectPropertyValidation {

    /**
     * Tests if the class has the given field with the given field type
     * @param classWithField class to check the field
     * @param fieldName name of the field
     * @param fieldType type of the field
     * @return reflection object of the field
     */
    public static Field checkFieldExists(Class<?> classWithField, String fieldName, Class<?> fieldType) {
        Field field;
        try {
            field = classWithField.getDeclaredField(fieldName);
        } catch (NoSuchFieldException noSuchFieldException) {
            throw new AssertionError(noSuchFieldException);
        }

        assert field.getType().equals(fieldType);

        return field;
    }

    /**
     * Tests if the class has the given list field with the given list type
     * @param classWithField class to check the list field
     * @param fieldName name of the list field
     * @param elementType type of the list field
     */
    public static void checkListExists(Class<?> classWithField, String fieldName, Class<?> elementType) {
        Field field = checkFieldExists(classWithField, fieldName, List.class);
        ParameterizedType genericType = (ParameterizedType) field.getGenericType();
        Type[] genericTypes = genericType.getActualTypeArguments();

        assert genericTypes.length == 1;
        assert genericTypes[0].equals(elementType);
    }

    /**
     * Tests if the class has the given list field with the given list type
     * @param classWithField class to check the map field
     * @param fieldName name of the map field
     * @param keyType type of the key of the map field
     * @param valueType type of the value of the map field
     */
    public static void checkMapExists(Class<?> classWithField, String fieldName,
                                       Class<?> keyType, Class<?> valueType) {
        Field field = checkFieldExists(classWithField, fieldName, Map.class);
        ParameterizedType genericType = (ParameterizedType) field.getGenericType();
        Type[] genericTypes = genericType.getActualTypeArguments();

        assert genericTypes.length == 2;
        assert genericTypes[0].equals(keyType);
        assert genericTypes[1].equals(valueType);
    }
}
