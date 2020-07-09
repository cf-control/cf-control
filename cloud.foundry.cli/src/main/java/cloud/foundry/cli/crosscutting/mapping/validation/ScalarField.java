package cloud.foundry.cli.crosscutting.mapping.validation;

public class ScalarField implements Field{

    public Class<?> classWithField;
    public String fieldName;
    public Class<?> fieldType;

    public ScalarField(Class<?> classWithField, String fieldName, Class<?> fieldType) {
        this.classWithField = classWithField;
        this.fieldName = fieldName;
        this.fieldType = fieldType;
    }

    @Override
    public String getName() {
        return fieldName;
    }
}
