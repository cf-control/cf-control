package cloud.foundry.cli.logic.diff.parsing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationBean;
import cloud.foundry.cli.logic.diff.change.CfChange;
import cloud.foundry.cli.logic.diff.change.object.CfNewObject;
import cloud.foundry.cli.logic.diff.change.parsing.NewObjectParsingStrategy;
import org.hamcrest.Matchers;
import org.javers.core.commit.CommitMetadata;
import org.javers.core.diff.Change;
import org.javers.core.diff.changetype.NewObject;
import org.javers.core.diff.changetype.ObjectRemoved;
import org.javers.core.metamodel.object.GlobalId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

public class NewObjectParsingStrategyTest {

    @Test
    public void testParseSucceeds() {
        GlobalId globalId = mock(GlobalId.class);
        when(globalId.toString())
                .thenReturn("#root/application");
        ApplicationBean applicationBean = new ApplicationBean();
        Change change = new NewObject(globalId, Optional.of(applicationBean), Optional.of(mock(CommitMetadata.class)));
        NewObjectParsingStrategy strategy = new NewObjectParsingStrategy();

        List<CfChange> changes = strategy.parse(change);

        assertThat(changes, Matchers.notNullValue());
        assertThat(changes.size(), Matchers.is(1));
        assertThat(changes.get(0), Matchers.instanceOf(CfNewObject.class));
        assertThat(changes.get(0).getPath(), Matchers.contains("root", "application"));
        assertThat(changes.get(0).getPropertyName(), Matchers.is(""));
        assertThat(changes.get(0).getAffectedObject(), Matchers.is(applicationBean));
    }

    @Test
    public void testParseOnInvalidTypeThrowsException() {
        Change change = new ObjectRemoved(mock(GlobalId.class),
                Optional.of(new ApplicationBean()),
                Optional.of(mock(CommitMetadata.class)));
        NewObjectParsingStrategy strategy = new NewObjectParsingStrategy();

        assertThrows(IllegalArgumentException.class, () -> strategy.parse(change));
    }

}
