package cloud.foundry.cli.logic.diff.parsing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationBean;
import cloud.foundry.cli.logic.diff.change.CfChange;
import cloud.foundry.cli.logic.diff.change.map.CfMapChange;
import cloud.foundry.cli.logic.diff.change.parsing.MapChangeParsingStrategy;
import org.javers.core.commit.CommitMetadata;
import org.javers.core.diff.Change;
import org.javers.core.diff.changetype.ObjectRemoved;
import org.javers.core.diff.changetype.map.EntryAdded;
import org.javers.core.diff.changetype.map.EntryRemoved;
import org.javers.core.diff.changetype.map.EntryValueChange;
import org.javers.core.diff.changetype.map.MapChange;
import org.javers.core.metamodel.object.GlobalId;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class MapChangeParsingStrategyTest {

    @Test
    public void testParseSucceeds() {
        GlobalId globalId = mock(GlobalId.class);
        when(globalId.toString())
                .thenReturn("#root/application/envvars");

        EntryAdded addedEntry = new EntryAdded("keyAdded", "valueAdded");
        EntryRemoved removedEntry = new EntryRemoved("keyRemoved", "valueRemoved");
        EntryValueChange changedEntry = new EntryValueChange("keyChanged", "valueBefore", "valueAfter");


        MapChange change = mock(MapChange.class);
        when(change.getEntryChanges())
                .thenReturn(Arrays.asList(addedEntry, removedEntry, changedEntry));
        when(change.getPropertyName())
                .thenReturn("envvars");
        when(change.getAffectedGlobalId())
                .thenReturn(globalId);
        ApplicationBean applicationBean = new ApplicationBean();
        when(change.getAffectedObject())
                .thenReturn(Optional.of(applicationBean));

        MapChangeParsingStrategy strategy = new MapChangeParsingStrategy();

        List<CfChange> changes = strategy.parse(change);

        assertThat(changes, notNullValue());
        assertThat(changes.size(), is(1));
        assertThat(changes.get(0), instanceOf(CfMapChange.class));
        CfMapChange mapChange = (CfMapChange) changes.get(0);
        assertThat(mapChange.getPath(), contains("root", "application", "envvars"));
        assertThat(mapChange.getPropertyName(), is("envvars"));
        assertThat(mapChange.getChangedValues().size(), is(3));
        assertThat(mapChange.getAffectedObject(), is(applicationBean));
    }

    @Test
    public void testParseOnInvalidTypeThrowsException() {
        Change change = new ObjectRemoved(mock(GlobalId.class), Optional.of(new ApplicationBean()), Optional.of(mock(CommitMetadata.class)));
        MapChangeParsingStrategy strategy = new MapChangeParsingStrategy();

        assertThrows(IllegalArgumentException.class, () -> strategy.parse(change));
    }

}
