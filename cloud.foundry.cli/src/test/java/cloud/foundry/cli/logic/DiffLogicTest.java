package cloud.foundry.cli.logic;

import static org.junit.jupiter.api.Assertions.assertThrows;

import cloud.foundry.cli.crosscutting.exceptions.DiffException;
import cloud.foundry.cli.crosscutting.mapping.beans.ConfigBean;
import cloud.foundry.cli.crosscutting.mapping.beans.SpecBean;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link DiffLogic}
 */
public class DiffLogicTest {

    @Test
    public void testCreateDiffTreeOnNullThrowsException() {
        assertThrows(DiffException.class, () -> new DiffLogic().createDiffTree(null, new ConfigBean()));
        assertThrows(DiffException.class, () -> new DiffLogic().createDiffTree(new ConfigBean(), null));
    }

    @Test
    public void testCreateDiffOutputOnNullThrowsException() {
        assertThrows(DiffException.class, () -> new DiffLogic().createDiffOutput(null, new ConfigBean()));
        assertThrows(DiffException.class, () -> new DiffLogic().createDiffOutput(new ConfigBean(), null));
    }

    @Test
    public void testCreateDiffTreeOfDifferentBeanTypesThrowsException() {
        assertThrows(DiffException.class,
                () -> new DiffLogic().createDiffTree(new SpecBean(), new ConfigBean()));
    }

    @Test
    public void testCreateDiffOutputOfDifferentBeanTypesThrowsException() {
        assertThrows(DiffException.class,
                () -> new DiffLogic().createDiffOutput(new SpecBean(), new ConfigBean()));
    }
}
