package cloud.foundry.cli.logic.diff.parsing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationBean;
import cloud.foundry.cli.logic.diff.change.CfChange;
import cloud.foundry.cli.logic.diff.change.container.CfContainerChange;
import cloud.foundry.cli.logic.diff.change.parsing.ContainerChangeParsingStrategy;
import org.hamcrest.Matchers;
import org.javers.core.commit.CommitMetadata;
import org.javers.core.diff.Change;
import org.javers.core.diff.changetype.ObjectRemoved;
import org.javers.core.diff.changetype.container.ListChange;
import org.javers.core.diff.changetype.container.ValueAdded;
import org.javers.core.diff.changetype.container.ValueRemoved;
import org.javers.core.metamodel.object.GlobalId;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class ContainerChangeParsingStrategyTest {

    @Test
    public void testParseSucceeds() {
        GlobalId globalId = mock(GlobalId.class);
        when(globalId.toString())
                .thenReturn("#root/application/services");

        ValueAdded addedEntry = new ValueAdded( "valueAdded");
        ValueRemoved removedEntry = new ValueRemoved("valueRemoved");

        ListChange change = mock(ListChange.class);
        when(change.getChanges())
                .thenReturn(Arrays.asList(addedEntry, removedEntry));
        when(change.getPropertyName())
                .thenReturn("services");
        when(change.getAffectedGlobalId())
                .thenReturn(globalId);
        ApplicationBean applicationBean = new ApplicationBean();
        when(change.getAffectedObject())
                .thenReturn(Optional.of(applicationBean));

        ContainerChangeParsingStrategy strategy = new ContainerChangeParsingStrategy();

        List<CfChange> changes = strategy.parse(change);

        assertThat(changes, Matchers.notNullValue());
        assertThat(changes.size(), Matchers.is(1));
        assertThat(changes.get(0), Matchers.instanceOf(CfContainerChange.class));
        CfContainerChange containerChange = (CfContainerChange) changes.get(0);
        assertThat(containerChange.getPath(), Matchers.contains("root", "application", "services"));
        assertThat(containerChange.getPropertyName(), Matchers.is("services"));
        assertThat(containerChange.getChangedValues().size(), Matchers.is(2));
        assertThat(containerChange.getAffectedObject(), Matchers.is(applicationBean));
    }

    @Test
    public void testParseOnInvalidTypeThrowsException() {
        Change change = new ObjectRemoved(mock(GlobalId.class), Optional.of(new ApplicationBean()), Optional.of(mock(CommitMetadata.class)));
        ContainerChangeParsingStrategy strategy = new ContainerChangeParsingStrategy();

        assertThrows(IllegalArgumentException.class, () -> strategy.parse(change));
    }

}
