package cloud.foundry.cli.logic.diff;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import cloud.foundry.cli.crosscutting.mapping.beans.Bean;
import cloud.foundry.cli.crosscutting.mapping.beans.SpecBean;
import cloud.foundry.cli.logic.diff.change.CfChange;
import cloud.foundry.cli.logic.diff.change.ChangeParser;
import cloud.foundry.cli.logic.diff.change.map.CfMapChange;
import cloud.foundry.cli.logic.diff.change.object.CfRemovedObject;
import cloud.foundry.cli.logic.diff.change.parsing.ContainerChangeParsingStrategy;
import cloud.foundry.cli.logic.diff.change.parsing.MapChangeParsingStrategy;
import cloud.foundry.cli.logic.diff.change.parsing.NewObjectParsingStrategy;
import cloud.foundry.cli.logic.diff.change.parsing.RemovedObjectParsingStrategy;
import cloud.foundry.cli.logic.diff.change.parsing.ValueChangeParsingStrategy;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.Diff;
import org.javers.core.diff.ListCompareAlgorithm;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class compares two given beans of the same type and builds a DiffNode tree data structure
 * with differences found between the bean objects.
 * Javers stores changes in a Change class, which holds information about the absolute path
 * from the root object to the actual level where a change has taken place.
 * The idea is to build a tree data structure where each node holds the changes of their level.
 */
public class Differ {

    private static final Javers JAVERS = JaversBuilder.javers()
            .withListCompareAlgorithm(ListCompareAlgorithm.AS_SET)
            .build();

    private final List<FilterCriteria> filterCriteria;
    private final ChangeParser changeParser;

    public Differ() {
        this.filterCriteria = new LinkedList<>();
        this.changeParser = new ChangeParser();
        this.changeParser.addParsingStrategy(new NewObjectParsingStrategy());
        this.changeParser.addParsingStrategy(new RemovedObjectParsingStrategy());
        this.changeParser.addParsingStrategy(new MapChangeParsingStrategy());
        this.changeParser.addParsingStrategy(new ContainerChangeParsingStrategy());
        this.changeParser.addParsingStrategy(new ValueChangeParsingStrategy(this.changeParser));
    }

    /**
     * If called before tree creation, ignores all CfRemovedObject objects. They will not be present
     * in the resulting tree.
     */
    public void ignoreRemovedObjects() {
        this.filterCriteria.add(change -> !(change instanceof CfRemovedObject));
    }

    /**
     * If called before tree creation, ignores adding the CfMapChange object to the spec node.
     * However, the change objects in the child nodes regarding this map change, remain unaltered.
     */
    public void ignoreSpecBeanMapChange() {
        this.filterCriteria.add(change ->
                !(change instanceof CfMapChange && change.getAffectedObject() instanceof SpecBean));
    }

    /**
     * Compares the two given configurations and creates a tree composed of @DiffNode objects.
     * @param liveConfig the configuration that is currently on the live system
     * @param desiredConfig the configuration state that the live system should change to
     * @return @DiffNode object which is the root of the tree
     * @throws NullPointerException when liveConfig is null
     *  when desiredConfig is null
     * @throws IllegalArgumentException when the two beans don't have the same type
     */
    public DiffNode createDiffTree(Bean liveConfig, Bean desiredConfig) {
        checkNotNull(liveConfig);
        checkNotNull(desiredConfig);
        checkArgument(liveConfig.getClass() == desiredConfig.getClass(), "Bean types don't match.");

        return doCreateDiffTree(liveConfig, desiredConfig);
    }

    private DiffNode doCreateDiffTree(Bean liveConfig, Bean desiredConfig) {
        Diff diff = JAVERS.compare(liveConfig, desiredConfig);

        // parse the change objects created by the JaVers diff to custom change objects
        List<CfChange> cfChanges = diff.getChanges()
                .stream()
                .map(this.changeParser::parse)
                .flatMap(Collection::stream)
                // apply all custom set filters
                .filter(this::applyFilterCriterion)
                .collect(Collectors.toList());

        return DiffTreeCreator.createFrom(cfChanges);
    }

    private boolean applyFilterCriterion(CfChange change) {
        // iterating over all filter conditions
        return filterCriteria
                .stream()
                // evaluate if condition is met and store boolean value
                .map(condition -> condition.isMet(change))
                // concatenate all boolean values with the and operator
                // => if one condition was not met return false
                .reduce(true, (acc, condition) ->  (acc && condition));
    }

}
