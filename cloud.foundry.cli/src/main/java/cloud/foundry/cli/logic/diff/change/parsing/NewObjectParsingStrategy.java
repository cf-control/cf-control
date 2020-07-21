package cloud.foundry.cli.logic.diff.change.parsing;

import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.crosscutting.mapping.beans.SpecBean;
import cloud.foundry.cli.logic.diff.change.CfChange;
import cloud.foundry.cli.logic.diff.change.ChangeType;
import cloud.foundry.cli.logic.diff.change.container.CfContainerChange;
import cloud.foundry.cli.logic.diff.change.container.CfContainerValueChanged;
import cloud.foundry.cli.logic.diff.change.object.CfNewObject;
import org.javers.core.diff.Change;
import org.javers.core.diff.changetype.NewObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

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
        List<CfChange> cfChanges = new LinkedList<>();

        // since JaVers doesn't explicitly create a change object for the space developers when spec bean is a new object
        // so creating it manually
        if(change.getAffectedObject().get() instanceof SpecBean) {
            List<String> spaceDevelopers = ((SpecBean) change.getAffectedObject().get()).getSpaceDevelopers();
            if (spaceDevelopers != null) {
                String propertyName = "spaceDevelopers";

               List<CfContainerValueChanged> addedSpaceDevelopers = spaceDevelopers.stream()
                       .peek(s ->  log.debug("Appending", propertyName,"container change with added entry:", s))
                       .map(s -> new CfContainerValueChanged(s, ChangeType.ADDED))
                       .collect(Collectors.toList());
               List<String> path = this.extractPathFrom(change);
               path.add(propertyName);
               CfContainerChange spaceDevelopersChange = new CfContainerChange(change.getAffectedObject().get(),
                       propertyName,
                       path,
                       addedSpaceDevelopers);
               cfChanges.add(spaceDevelopersChange);
            }
        }

        log.verbose("Parsing change type", change.getClass().getSimpleName(), "to custom change type",
                CfNewObject.class.getSimpleName(), "with object", change.getAffectedObject().get());
        cfChanges.add(new CfNewObject(change.getAffectedObject().get(),
                "",
                extractPathFrom(change)));
        log.debug("Parsing change type", change.getClass().getSimpleName(), "to custom change type",
                CfNewObject.class.getSimpleName(), "with object", change.getAffectedObject().get(), "completed");
        return cfChanges;
    }

}
