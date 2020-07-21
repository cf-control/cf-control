package cloud.foundry.cli.logic.diff.change.parsing;

import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.logic.diff.change.CfChange;
import cloud.foundry.cli.logic.diff.change.object.CfNewObject;
import org.javers.core.diff.Change;
import org.javers.core.diff.changetype.NewObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * This class parses change objects of type {@link NewObject} to a single
 * custom change object of type {@link CfNewObject} returned as list
 */
public class NewObjectParsingStrategy extends AbstractParsingStrategy {

    private static final Log log = Log.getLog(NewObjectParsingStrategy.class);

    @Override
    public List<Class<? extends Change>> getMatchingTypes() {
        return Arrays.asList(NewObject.class);
    }

    @Override
    protected List<CfChange> doParse(Change change) {
        log.verbose("Parsing change type", change.getClass().getSimpleName(), "to custom change type",
                CfNewObject.class.getSimpleName(), "with object", change.getAffectedObject().get());
        List<CfChange> cfChanges = Collections.singletonList(new CfNewObject(change.getAffectedObject().get(),
                "",
                extractPathFrom(change)));
        log.debug("Parsing change type", change.getClass().getSimpleName(), "to custom change type",
                CfNewObject.class.getSimpleName(), "with object", change.getAffectedObject().get(), "completed");
        return cfChanges;
    }

}
