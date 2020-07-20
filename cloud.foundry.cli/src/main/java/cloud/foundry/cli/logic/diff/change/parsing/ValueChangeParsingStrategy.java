package cloud.foundry.cli.logic.diff.change.parsing;

import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationManifestBean;
import cloud.foundry.cli.logic.diff.change.CfChange;
import cloud.foundry.cli.logic.diff.change.ChangeParser;
import cloud.foundry.cli.logic.diff.change.object.CfObjectValueChanged;
import cloud.foundry.cli.logic.diff.change.object.CfRemovedObject;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.Change;
import org.javers.core.diff.ListCompareAlgorithm;
import org.javers.core.diff.changetype.ValueChange;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * This class parses change objects of type {@link ValueChange} to a single
 * custom change object of type {@link CfObjectValueChanged} returned as list.
 * In case of a change in the manifest attribute of the
 * {@link cloud.foundry.cli.crosscutting.mapping.beans.ApplicationBean} object multiple changes can be returned that
 * happened on the {@link ApplicationManifestBean} object
 */
public class ValueChangeParsingStrategy extends AbstractParsingStrategy {

    private static final Log log = Log.getLog(ValueChangeParsingStrategy.class);

    private Javers javers;
    private ChangeParser changeParser;

    public ValueChangeParsingStrategy(ChangeParser changeParser) {
        this.javers = JaversBuilder.javers()
                .withListCompareAlgorithm(ListCompareAlgorithm.AS_SET)
                .registerValueObject(ApplicationManifestBean.class)
                .build();
        this.changeParser = changeParser;
    }

    @Override
    public List<Class<? extends Change>> getMatchingTypes() {
        return Arrays.asList(ValueChange.class);
    }

    @Override
    protected List<CfChange> doParse(Change change) {
        ValueChange valueChange = (ValueChange) change;

        // Special treatment for app manifest to ease further processing. Since itself is an object it is necessary to
        // for all changes in the manifest to set the affected object to their parent application bean object
        if (valueChange.getPropertyName().equals("manifest")) {
            List<Change> manifestChanges = this.javers.compare(
                    valueChange.getLeft(),
                    valueChange.getRight()).getChanges();

            return manifestChanges.stream()
                    .flatMap(manifestChange -> this.changeParser.parse(manifestChange).stream())
                    .peek(manifestChange -> {
                        manifestChange.setAffectedObject(change.getAffectedObject().get());
                        List<String> path = extractPathFrom(change);
                        path.add("manifest");
                        manifestChange.setPath(path);

                    } )
                    .collect(Collectors.toList());
        }

        log.verbose("Parsing change type", change.getClass().getSimpleName(), "to custom change type",
                CfObjectValueChanged.class.getSimpleName(), "with property", valueChange.getPropertyName(), "and changed value from",
                valueChange.getLeft(), "to", valueChange.getRight());
        List<CfChange> cfChanges = Collections.singletonList(new CfObjectValueChanged(change.getAffectedObject().get(),
                valueChange.getPropertyName(),
                this.extractPathFrom(change),
                Objects.toString(valueChange.getLeft(), null),
                Objects.toString(valueChange.getRight(), null)
        ));
        log.debug("Parsing change type", change.getClass().getSimpleName(), "to custom change type",
                CfObjectValueChanged.class.getSimpleName(), "with property", valueChange.getPropertyName(), "and changed value from",
                valueChange.getLeft(), "to", valueChange.getRight(), "completed");
        return cfChanges;
    }

}
