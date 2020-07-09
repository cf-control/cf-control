package cloud.foundry.cli.crosscutting.mapping.validation;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class ObjectPropertyValidation {

    public static Field checkFieldExists(ScalarField scalarField) {
        Field field;
        try {
            field = scalarField.classWithField.getDeclaredField(scalarField.fieldName);
        } catch (NoSuchFieldException noSuchFieldException) {
            throw new AssertionError(noSuchFieldException);
        }

        assert field.getType().equals(scalarField.fieldType);

        return field;
    }

    public static void checkListExists(ListField listField) {
        Field field = checkFieldExists(new ScalarField(listField.classWithField, listField.fieldName, List.class));
        ParameterizedType genericType = (ParameterizedType) field.getGenericType();
        Type[] genericTypes = genericType.getActualTypeArguments();

        assert genericTypes.length == 1;
        assert genericTypes[0].equals(listField.elementType);
    }

    public static void checkMapExists(MapField mapField) {
        Field field = checkFieldExists(new ScalarField(mapField.classWithField, mapField.fieldName, Map.class));
        ParameterizedType genericType = (ParameterizedType) field.getGenericType();
        Type[] genericTypes = genericType.getActualTypeArguments();

        assert genericTypes.length == 2;
        assert genericTypes[0].equals(mapField.keyType);
        assert genericTypes[1].equals(mapField.valueType);
    }
}
