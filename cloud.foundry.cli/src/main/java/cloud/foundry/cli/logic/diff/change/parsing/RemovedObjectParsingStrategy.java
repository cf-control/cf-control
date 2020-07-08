package cloud.foundry.cli.logic.diff.change.parsing;

import cloud.foundry.cli.logic.diff.change.CfChange;
import cloud.foundry.cli.logic.diff.change.object.CfRemovedObject;
import org.javers.core.diff.Change;
import org.javers.core.diff.changetype.ObjectRemoved;

import java.util.Arrays;
import java.util.List;

/**
 * This class parses change objects of type {@link ObjectRemoved} to a single
 * custom change object of type {@link CfRemovedObject} returned as list
 */
public class RemovedObjectParsingStrategy extends AbstractParsingStrategy {


    @Override
    public List<Class<? extends Change>> getMatchingTypes() {
        return Arrays.asList(ObjectRemoved.class);
    }

    @Override
    protected List<CfChange> doParse(Change change) {
        return Arrays.asList(new CfRemovedObject(change.getAffectedObject().get(),
                "",
                extractPathFrom(change)));
    }

}
