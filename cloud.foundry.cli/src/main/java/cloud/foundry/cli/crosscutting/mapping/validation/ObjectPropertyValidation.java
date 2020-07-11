package cloud.foundry.cli.crosscutting.mapping.validation;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ObjectPropertyValidation {

    public static Field checkFieldExists(ScalarField scalarField) {
        Field field;
        try {
            field = scalarField.classWithField.getDeclaredField(scalarField.getName());
        } catch (NoSuchFieldException noSuchFieldException) {
            throw new AssertionError(noSuchFieldException);
        }

        assert field.getType().equals(scalarField.fieldType);

        return field;
    }

    public static void checkListExists(ListField listField) {
        Field field = checkFieldExists(new ScalarField(listField.classWithField, listField.getName(), List.class));
        ParameterizedType genericType = (ParameterizedType) field.getGenericType();
        Type[] genericTypes = genericType.getActualTypeArguments();

        assert genericTypes.length == 1;
        assert genericTypes[0].equals(listField.elementType);
    }

    public static void checkMapExists(MapField mapField) {
        Field field = checkFieldExists(new ScalarField(mapField.classWithField, mapField.getName(), Map.class));
        ParameterizedType genericType = (ParameterizedType) field.getGenericType();
        Type[] genericTypes = genericType.getActualTypeArguments();

        assert genericTypes.length == 2;
        assert genericTypes[0].equals(mapField.keyType);
        assert genericTypes[1].equals(mapField.valueType);
    }

    public static void checkPropertiesExist(Collection<cloud.foundry.cli.crosscutting.mapping.validation.Field> fields) {
        for (cloud.foundry.cli.crosscutting.mapping.validation.Field field : fields) {
            if (field instanceof MapField) {
                ObjectPropertyValidation.checkMapExists((MapField)field);
            } else if (field instanceof ListField) {
                ObjectPropertyValidation.checkListExists((ListField)field);
            } else {
                ObjectPropertyValidation.checkFieldExists((ScalarField)field);
            }
        }
    }
}
