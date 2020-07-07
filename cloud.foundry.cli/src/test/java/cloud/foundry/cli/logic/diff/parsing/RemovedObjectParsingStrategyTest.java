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
import cloud.foundry.cli.logic.diff.change.object.CfRemovedObject;
import cloud.foundry.cli.logic.diff.change.parsing.RemovedObjectParsingStrategy;
import org.javers.core.commit.CommitMetadata;
import org.javers.core.diff.Change;
import org.javers.core.diff.changetype.NewObject;
import org.javers.core.diff.changetype.ObjectRemoved;
import org.javers.core.metamodel.object.GlobalId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

public class RemovedObjectParsingStrategyTest {

    @Test
    public void testParseSucceeds() {
        GlobalId globalId = mock(GlobalId.class);
        when(globalId.toString())
                .thenReturn("#root/application");
        ApplicationBean applicationBean = new ApplicationBean();
        Change change = new ObjectRemoved(globalId, Optional.of(applicationBean), Optional.of(mock(CommitMetadata.class)));
        RemovedObjectParsingStrategy strategy = new RemovedObjectParsingStrategy();

        List<CfChange> changes = strategy.parse(change);

        assertThat(changes, notNullValue());
        assertThat(changes.size(), is(1));
        assertThat(changes.get(0), instanceOf(CfRemovedObject.class));
        assertThat(changes.get(0).getPath(), contains("root", "application"));
        assertThat(changes.get(0).getPropertyName(), is(""));
        assertThat(changes.get(0).getAffectedObject(), is(applicationBean));
    }

    @Test
    public void testParseOnInvalidTypeThrowsException() {
        Change change = new NewObject(mock(GlobalId.class), Optional.of(new ApplicationBean()), Optional.of(mock(CommitMetadata.class)));
        RemovedObjectParsingStrategy strategy = new RemovedObjectParsingStrategy();

        assertThrows(IllegalArgumentException.class, () -> strategy.parse(change));
    }

}
