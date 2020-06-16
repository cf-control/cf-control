package cloud.foundry.cli.logic;

import static org.junit.jupiter.api.Assertions.assertThrows;

import cloud.foundry.cli.crosscutting.mapping.beans.ConfigBean;
import cloud.foundry.cli.crosscutting.mapping.beans.SpecBean;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link DiffLogic}
 */
public class DiffLogicTest {

    @Test
    public void testCreateDiffTreeOnNullThrowsException() {
        assertThrows(NullPointerException.class, () -> new DiffLogic().createDiffResult(null, new ConfigBean()));
        assertThrows(NullPointerException.class, () -> new DiffLogic().createDiffResult(new ConfigBean(), null));
    }

    @Test
    public void testCreateDiffOutputOnNullThrowsException() {
        assertThrows(NullPointerException.class, () -> new DiffLogic().createDiffOutput(null, new ConfigBean()));
        assertThrows(NullPointerException.class, () -> new DiffLogic().createDiffOutput(new ConfigBean(), null));
    }

    @Test
    public void testCreateDiffTreeOfDifferentBeanTypesThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new DiffLogic().createDiffResult(new SpecBean(), new ConfigBean()));
    }

    @Test
    public void testCreateDiffOutputOfDifferentBeanTypesThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new DiffLogic().createDiffOutput(new SpecBean(), new ConfigBean()));
    }
}
