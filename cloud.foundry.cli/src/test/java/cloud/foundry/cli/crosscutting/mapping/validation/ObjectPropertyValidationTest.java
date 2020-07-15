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
        ObjectPropertyValidation.checkFieldExists(DummyClass.class, "scalarField", String.class);
    }

    @Test
    public void testCheckFieldExistsOnMissingFieldFails() {
        assertThrows(AssertionError.class, () -> ObjectPropertyValidation.checkFieldExists(DummyClass.class,
                "missingField",
                String.class));
    }

    @Test
    public void testCheckFieldExistsWrongTypeFails() {
        assertThrows(AssertionError.class, () -> ObjectPropertyValidation.checkFieldExists(DummyClass.class,
                "missingField",
                Integer.class));
    }

    @Test
    public void testCheckMapExistsOnExistingMapSucceeds() {
        ObjectPropertyValidation.checkMapExists(DummyClass.class, "mapField", String.class, String.class);
    }

    @Test
    public void testCheckMapExistsOnMissingFieldFails() {
        assertThrows(AssertionError.class, () -> ObjectPropertyValidation.checkMapExists(DummyClass.class,
                "missingField",
                String.class,
                String.class));
    }

    @Test
    public void testCheckMapExistsOnWrongKeyTypeFails() {
        assertThrows(AssertionError.class, () -> ObjectPropertyValidation.checkMapExists(DummyClass.class,
                "mapField",
                Integer.class,
                String.class));
    }

    @Test
    public void testCheckMapExistsOnWrongValueTypeFails() {
        assertThrows(AssertionError.class, () -> ObjectPropertyValidation.checkMapExists(DummyClass.class,
                "mapField",
                String.class,
                Integer.class));
    }

    @Test
    public void testCheckListExistsOnExistingListSucceeds() {
        ObjectPropertyValidation.checkListExists(DummyClass.class, "listField", String.class);
    }

    @Test
    public void testCheckListExistsOnMissingListFails() {
        assertThrows(AssertionError.class, () -> ObjectPropertyValidation.checkListExists(DummyClass.class,
                "missingField",
                String.class));
    }

    @Test
    public void testCheckListExistsOnWrongTypeFails() {
        assertThrows(AssertionError.class, () -> ObjectPropertyValidation.checkListExists(DummyClass.class,
                "listField",
                Integer.class));
    }


}
