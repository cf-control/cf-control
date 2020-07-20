package cloud.foundry.cli.logic.diff.change.parsing;

import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.logic.diff.change.CfChange;
import cloud.foundry.cli.logic.diff.change.object.CfNewObject;
import cloud.foundry.cli.logic.diff.change.object.CfRemovedObject;
import org.javers.core.diff.Change;
import org.javers.core.diff.changetype.ObjectRemoved;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * This class parses change objects of type {@link ObjectRemoved} to a single
 * custom change object of type {@link CfRemovedObject} returned as list
 */
public class RemovedObjectParsingStrategy extends AbstractParsingStrategy {

    private static final Log log = Log.getLog(RemovedObjectParsingStrategy.class);

    @Override
    public List<Class<? extends Change>> getMatchingTypes() {
        return Arrays.asList(ObjectRemoved.class);
    }

    @Override
    protected List<CfChange> doParse(Change change) {
        log.verbose("Parsing change type", change.getClass().getSimpleName(), "to custom change type",
                CfRemovedObject.class.getSimpleName(), "with object", change.getAffectedObject().get());
        List<CfChange> cfChanges = Collections.singletonList(new CfRemovedObject(change.getAffectedObject().get(),
                "",
                extractPathFrom(change)));
        log.verbose("Parsing change type", change.getClass().getSimpleName(), "to custom change type",
                CfRemovedObject.class.getSimpleName(), "with object", change.getAffectedObject().get(), "completed");
        return cfChanges;
    }

}
