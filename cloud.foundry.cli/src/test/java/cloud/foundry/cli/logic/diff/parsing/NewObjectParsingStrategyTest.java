package cloud.foundry.cli.logic.diff.parsing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationBean;
import cloud.foundry.cli.crosscutting.mapping.beans.SpecBean;
import cloud.foundry.cli.logic.diff.change.CfChange;
import cloud.foundry.cli.logic.diff.change.ChangeType;
import cloud.foundry.cli.logic.diff.change.container.CfContainerChange;
import cloud.foundry.cli.logic.diff.change.container.CfContainerValueChanged;
import cloud.foundry.cli.logic.diff.change.object.CfNewObject;
import cloud.foundry.cli.logic.diff.change.parsing.NewObjectParsingStrategy;
import org.hamcrest.Matchers;
import org.javers.core.commit.CommitMetadata;
import org.javers.core.diff.Change;
import org.javers.core.diff.changetype.NewObject;
import org.javers.core.diff.changetype.ObjectRemoved;
import org.javers.core.metamodel.object.GlobalId;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
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
        assertThat(changes.get(0).getPropertyName(), is(""));
        assertThat(changes.get(0).getAffectedObject(), is(applicationBean));
    }

    @Test
    public void testParseWithNewSpecBeanObjectAndGivenSpaceDevelopersSucceeds() {
        GlobalId globalId = mock(GlobalId.class);
        when(globalId.toString())
                .thenReturn("#root/spec");
        List<String> spaceDevelopers = new LinkedList<>();
        spaceDevelopers.add("spaceDev1");
        spaceDevelopers.add("spaceDev2");
        SpecBean specBean = new SpecBean();
        specBean.setSpaceDevelopers(spaceDevelopers);

        Change change = new NewObject(globalId, Optional.of(specBean), Optional.of(mock(CommitMetadata.class)));
        NewObjectParsingStrategy strategy = new NewObjectParsingStrategy();

        List<CfChange> changes = strategy.parse(change);

        assertThat(changes, Matchers.notNullValue());
        assertThat(changes.size(), Matchers.is(2));

        assertThat(changes.get(1), Matchers.instanceOf(CfNewObject.class));
        assertThat(changes.get(1).getPath(), Matchers.contains("root", "spec"));
        assertThat(changes.get(1).getPropertyName(), is(""));
        assertThat(changes.get(1).getAffectedObject(), is(specBean));

        assertThat(changes.get(0), Matchers.instanceOf(CfContainerChange.class));
        assertThat(changes.get(0).getPath(), Matchers.contains("root", "spec", "spaceDevelopers"));
        assertThat(changes.get(0).getPropertyName(), is("spaceDevelopers"));
        assertThat(changes.get(0).getAffectedObject(), is(specBean));
        CfContainerChange cfContainerChange = (CfContainerChange) changes.get(0);
        assertThat(cfContainerChange.getChangedValues().size(), is(2));
        assertThat(cfContainerChange.getChangedValues().get(0), instanceOf(CfContainerValueChanged.class));
        assertThat(cfContainerChange.getChangedValues().get(0).getChangeType(), is(ChangeType.ADDED));
        assertThat(cfContainerChange.getChangedValues().get(0).getValue(), is("spaceDev1"));
        assertThat(cfContainerChange.getChangedValues().get(1), instanceOf(CfContainerValueChanged.class));
        assertThat(cfContainerChange.getChangedValues().get(1).getChangeType(), is(ChangeType.ADDED));
        assertThat(cfContainerChange.getChangedValues().get(1).getValue(), is("spaceDev2"));
    }

    @Test
    public void testParseWithNewSpecBeanObjectAndNullSpaceDevelopersSucceeds() {
        GlobalId globalId = mock(GlobalId.class);
        when(globalId.toString())
                .thenReturn("#root/spec");
        SpecBean specBean = new SpecBean();

        Change change = new NewObject(globalId, Optional.of(specBean), Optional.of(mock(CommitMetadata.class)));
        NewObjectParsingStrategy strategy = new NewObjectParsingStrategy();

        List<CfChange> changes = strategy.parse(change);

        assertThat(changes, Matchers.notNullValue());
        assertThat(changes.size(), Matchers.is(1));

        assertThat(changes.get(0), Matchers.instanceOf(CfNewObject.class));
        assertThat(changes.get(0).getPath(), Matchers.contains("root", "spec"));
        assertThat(changes.get(0).getPropertyName(), is(""));
        assertThat(changes.get(0).getAffectedObject(), is(specBean));
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
