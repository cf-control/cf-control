package cloud.foundry.cli.logic.diff.change.parsing;

import cloud.foundry.cli.logic.diff.change.CfChange;
import cloud.foundry.cli.logic.diff.change.object.CfNewObject;
import org.javers.core.diff.Change;
import org.javers.core.diff.changetype.NewObject;

import java.util.Arrays;
import java.util.List;

/**
 * This class parses change objects of type {@link NewObject} to a single
 * custom change object of type {@link CfNewObject} returned as list
 */
public class NewObjectParsingStrategy extends AbstractParsingStrategy {

    @Override
    public List<Class<? extends Change>> getMatchingTypes() {
        return Arrays.asList(NewObject.class);
    }

    @Override
    public List<CfChange> doParse(Change change) {
        return Arrays.asList(new CfNewObject(change.getAffectedObject().get(),
                "",
                extractPathFrom(change)));
    }

}
