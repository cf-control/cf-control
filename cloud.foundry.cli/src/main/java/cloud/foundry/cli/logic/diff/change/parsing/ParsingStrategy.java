package cloud.foundry.cli.logic.diff.change.parsing;

import cloud.foundry.cli.logic.diff.change.CfChange;
import org.javers.core.diff.Change;

import java.util.List;

/**
 * Interface for the parsing strategy that will be used in the {@link cloud.foundry.cli.logic.diff.change.ChangeParser}
 * class
 */
public interface ParsingStrategy {

    /**
     * This method returns a list of {@link Change} types that should be parsed in the implemented parsing strategy.
     * @return a list of class types of type {@link Change}
     */
    List<Class<? extends Change>> getMatchingTypes();

    /**
     * Parses the given change object to an adequate list of custom change objects.
     * Usually this list will only have one entry. But parsing the change object to multiple custom ones is also a
     * possibility.
     * @param change the object that should be parsed to the custom change object
     * @return a list of custom change objects
     */
    List<CfChange> parse(Change change);

}
