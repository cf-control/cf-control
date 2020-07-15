package cloud.foundry.cli.crosscutting.mapping.validation;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class ObjectPropertyValidation {

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

    public static void checkListExists(Class<?> classWithField, String fieldName, Class<?> elementType) {
        Field field = checkFieldExists(classWithField, fieldName, List.class);
        ParameterizedType genericType = (ParameterizedType) field.getGenericType();
        Type[] genericTypes = genericType.getActualTypeArguments();

        assert genericTypes.length == 1;
        assert genericTypes[0].equals(elementType);
    }

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
