package cloud.foundry.cli.crosscutting.mapping.validation;

public class ListField implements Field {

    public Class<?> classWithField;
    public String fieldName;
    public Class<?> elementType;

    public ListField(Class<?> classWithField, String fieldName, Class<?> elementType) {
        this.classWithField = classWithField;
        this.fieldName = fieldName;
        this.elementType = elementType;
    }

    @Override
    public String getName() {
        return fieldName;
    }
}
