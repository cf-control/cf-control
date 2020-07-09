package cloud.foundry.cli.crosscutting.mapping.validation;

public class MapField implements Field {

    public Class<?> classWithField;
    public String fieldName;
    public Class<?> keyType;
    public Class<?> valueType;

    public MapField(Class<?> classWithField, String fieldName, Class<?> keyType, Class<?> valueType) {
        this.classWithField = classWithField;
        this.fieldName = fieldName;
        this.keyType = keyType;
        this.valueType = valueType;
    }

    @Override
    public String getName() {
        return fieldName;
    }
}
