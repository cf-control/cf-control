package cloud.foundry.cli.logic.diff.change.parsing;

import cloud.foundry.cli.logic.diff.change.CfChange;
import org.javers.core.diff.Change;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Abstract super class for all parsing strategies.
 * The parsing strategies will be used in the {@link cloud.foundry.cli.logic.diff.change.ChangeParser } object to
 * convert a JaVers change object to a list of custom change object depending on the type of change.
 * How conversion will happen, will be determined in the implemented parsing strategy.
 */
public abstract class AbstractParsingStrategy implements ParsingStrategy {

    /**
     * Converts a given JaVers change in to an more appropriate list of custom change objects
     * The way the change object will be parsed is determined in the {@link AbstractParsingStrategy } class
     * @param change the object that should be parsed to the custom change objects
     * @return the parsed list of custom change objects
     */
    @Override
    public List<CfChange> parse(Change change) {
        assertMatchingTypes(change);

        return doParse(change);
    }

    /**
     * template method that will handle the actual parsing process
     * @param change the object that should be parsed to the custom change object
     * @return a list of custom change objects
     */
    protected abstract List<CfChange> doParse(Change change);

    /**
     * asserts if the given change type fits the change types determined in the getMatchingTypes() method
     * @param change the jaVers change object
     */
    private void assertMatchingTypes(Change change) {
        if (!getMatchingTypes().contains(change.getClass())) {
            throw new IllegalArgumentException("Invalid change type. Was '" +
                    change.getClass() + "'. Should be one of " + getMatchingTypes());
        }
    }

    /**
     * for example:
     * change.getAffectedGlobalId() = cloud.foundry.cli.crosscutting.bean.ConfigBean/#spec/apps/someApp/manifest
     *                           -> [cloud.foundry.cli.crosscutting.bean.ConfigBean, spec, apps, someApp, manifest]
     */
    protected LinkedList<String> extractPathFrom(Change change) {
        String rootSymbol = "#";
        String pathSeparatorSymbol = "/";
        LinkedList<String> path = new LinkedList<>(Arrays.asList(change
                .getAffectedGlobalId()
                .toString()
                .replace(rootSymbol, "")
                .split(pathSeparatorSymbol)));
        return path;
    }
}
