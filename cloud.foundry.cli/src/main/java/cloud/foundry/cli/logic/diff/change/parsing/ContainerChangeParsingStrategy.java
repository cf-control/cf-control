package cloud.foundry.cli.logic.diff.change.parsing;

import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.logic.diff.change.CfChange;
import cloud.foundry.cli.logic.diff.change.ChangeType;
import cloud.foundry.cli.logic.diff.change.container.CfContainerChange;
import cloud.foundry.cli.logic.diff.change.container.CfContainerValueChanged;
import org.javers.core.diff.Change;
import org.javers.core.diff.changetype.container.ArrayChange;
import org.javers.core.diff.changetype.container.ContainerChange;
import org.javers.core.diff.changetype.container.ContainerElementChange;
import org.javers.core.diff.changetype.container.ListChange;
import org.javers.core.diff.changetype.container.ValueAdded;
import org.javers.core.diff.changetype.container.ValueRemoved;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * This class parses change objects of type {@link ContainerChange} to a single
 * custom change object of type {@link CfContainerChange} returned as list
 */
public class ContainerChangeParsingStrategy extends AbstractParsingStrategy {

    private static final Log log = Log.getLog(ContainerChangeParsingStrategy.class);

    @Override
    public List<Class<? extends Change>> getMatchingTypes() {
        return Arrays.asList(ContainerChange.class, ArrayChange.class, ListChange.class);
    }

    @Override
    protected List<CfChange> doParse(Change change) {
        ContainerChange containerChange = (ContainerChange) change;

        log.verbose("Parsing change type", change.getClass().getSimpleName(), "to custom change type",
                CfContainerChange.class.getSimpleName());
        List<CfContainerValueChanged> cfChanges = containerChange.getChanges()
                .stream()
                .map(elementChange -> parseListEntry(((ContainerChange) change).getPropertyName(), elementChange))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        log.debug("Parsing change type", change.getClass().getSimpleName(), "to custom change type",
                CfContainerChange.class.getSimpleName(), "completed");
        return Collections.singletonList(new CfContainerChange(change.getAffectedObject().get(),
                containerChange.getPropertyName(),
                extractPathFrom(change),
                cfChanges));
    }

    private CfContainerValueChanged parseListEntry(String propertyName, ContainerElementChange elementChange) {
        if (elementChange instanceof ValueAdded) {
            String value = ((ValueAdded) elementChange).getAddedValue().toString();

            log.verbose("Appending", propertyName, "container change with added entry:", value);
            return new CfContainerValueChanged(value, ChangeType.ADDED);
        } else if ( elementChange instanceof ValueRemoved) {
            String value = ((ValueRemoved) elementChange).getRemovedValue().toString();

            log.verbose("Appending", propertyName, "container change with removed entry:", value);
            return new CfContainerValueChanged(value, ChangeType.REMOVED);
        }
        log.debug("Ignoring unsupported list change type", elementChange.getClass());
        return null;
    }
}
