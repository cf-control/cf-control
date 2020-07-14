package cloud.foundry.cli.crosscutting.mapping.validation;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ObjectPropertyValidationTest {

    private static class DummyClass {
        String scalarField;
        Map<String, String> mapField;
        List<String> listField;
    }

    @Test
    public void testCheckFieldExistsOnExistingFieldSucceeds() {
        ScalarField scalarField = new ScalarField(DummyClass.class, "scalarField", String.class);

        ObjectPropertyValidation.checkFieldExists(scalarField);
    }

    @Test
    public void testCheckFieldExistsOnMissingFieldFails() {
        ScalarField scalarField = new ScalarField(DummyClass.class, "missingField", String.class);

        assertThrows(AssertionError.class, () -> ObjectPropertyValidation.checkFieldExists(scalarField));
    }

    @Test
    public void testCheckFieldExistsWrongTypeFails() {
        ScalarField scalarField = new ScalarField(DummyClass.class, "missingField", Integer.class);

        assertThrows(AssertionError.class, () -> ObjectPropertyValidation.checkFieldExists(scalarField));
    }

    @Test
    public void testCheckMapExistsOnExistingMapSucceeds() {
        MapField mapField = new MapField(DummyClass.class, "mapField", String.class, String.class);

        ObjectPropertyValidation.checkMapExists(mapField);
    }

    @Test
    public void testCheckMapExistsOnMissingFieldFails() {
        MapField mapField = new MapField(DummyClass.class, "missingField", String.class, String.class);

        assertThrows(AssertionError.class, () -> ObjectPropertyValidation.checkMapExists(mapField));
    }

    @Test
    public void testCheckMapExistsOnWrongKeyTypeFails() {
        MapField mapField = new MapField(DummyClass.class, "mapField", Integer.class, String.class);

        assertThrows(AssertionError.class, () -> ObjectPropertyValidation.checkMapExists(mapField));
    }

    @Test
    public void testCheckMapExistsOnWrongValueTypeFails() {
        MapField mapField = new MapField(DummyClass.class, "mapField", String.class, Integer.class);

        assertThrows(AssertionError.class, () -> ObjectPropertyValidation.checkMapExists(mapField));
    }

    @Test
    public void testCheckListExistsOnExistingListSucceeds() {
        ListField listField = new ListField(DummyClass.class, "listField", String.class);

        ObjectPropertyValidation.checkListExists(listField);
    }

    @Test
    public void testCheckListExistsOnMissingListFails() {
        ListField listField = new ListField(DummyClass.class, "missingField", String.class);

        assertThrows(AssertionError.class, () -> ObjectPropertyValidation.checkListExists(listField));
    }

    @Test
    public void testCheckListExistsOnWrongTypeFails() {
        ListField listField = new ListField(DummyClass.class, "listField", Integer.class);

        assertThrows(AssertionError.class, () -> ObjectPropertyValidation.checkListExists(listField));
    }

    @Test
    public void testCheckPropertiesExist() {
        ScalarField scalarField = new ScalarField(DummyClass.class, "scalarField", String.class);
        ListField listField = new ListField(DummyClass.class, "listField", String.class);
        MapField mapField = new MapField(DummyClass.class, "mapField", String.class, String.class);

        ObjectPropertyValidation.checkPropertiesExist(Arrays.asList(scalarField, listField, mapField));
    }

    @Test
    public void testCheckPropertiesExistFailsWhenMissingMapField() {
        ScalarField scalarField = new ScalarField(DummyClass.class, "scalarField", String.class);
        ListField listField = new ListField(DummyClass.class, "listField", String.class);
        MapField mapField = new MapField(DummyClass.class, "missingField", String.class, String.class);

        assertThrows(AssertionError.class,
                () -> ObjectPropertyValidation.checkPropertiesExist(Arrays.asList(scalarField, listField, mapField)));
    }

    @Test
    public void testCheckPropertiesExistFailsWhenMissingListField() {
        ScalarField scalarField = new ScalarField(DummyClass.class, "scalarField", String.class);
        ListField listField = new ListField(DummyClass.class, "missingField", String.class);
        MapField mapField = new MapField(DummyClass.class, "mapField", String.class, String.class);

        assertThrows(AssertionError.class,
                () -> ObjectPropertyValidation.checkPropertiesExist(Arrays.asList(scalarField, listField, mapField)));
    }

    @Test
    public void testCheckPropertiesExistFailsWhenMissingScalarField() {
        ScalarField scalarField = new ScalarField(DummyClass.class, "missingField", String.class);
        ListField listField = new ListField(DummyClass.class, "listField", String.class);
        MapField mapField = new MapField(DummyClass.class, "mapField", String.class, String.class);

        assertThrows(AssertionError.class,
                () -> ObjectPropertyValidation.checkPropertiesExist(Arrays.asList(scalarField, listField, mapField)));
    }

}
