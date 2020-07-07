package cloud.foundry.cli.logic.diff.change.parsing;

import cloud.foundry.cli.logic.diff.change.CfChange;
import org.javers.core.diff.Change;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Abstract super class for all parsing strategies.
 */
public abstract class AbstractParsingStrategy implements ParsingStrategy {

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
