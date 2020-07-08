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
import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationManifestBean;
import cloud.foundry.cli.logic.diff.change.CfChange;
import cloud.foundry.cli.logic.diff.change.ChangeParser;
import cloud.foundry.cli.logic.diff.change.object.CfObjectValueChanged;
import cloud.foundry.cli.logic.diff.change.parsing.ValueChangeParsingStrategy;
import org.javers.core.commit.CommitMetadata;
import org.javers.core.diff.Change;
import org.javers.core.diff.changetype.ObjectRemoved;
import org.javers.core.diff.changetype.ValueChange;
import org.javers.core.metamodel.object.GlobalId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

public class ValueChangeParsingStrategyTest {

    @Test
    public void testParseSucceedsWhenChangeInApplicationManifestBean() {
        ApplicationBean applicationBean = new ApplicationBean();

        ApplicationManifestBean manifestBeanBefore = new ApplicationManifestBean();
        manifestBeanBefore.setDisk(1024);
        ApplicationManifestBean manifestBeanAfter = new ApplicationManifestBean();
        manifestBeanAfter.setDisk(2048);

        applicationBean.setManifest(manifestBeanAfter);

        GlobalId globalId = mock(GlobalId.class);
        when(globalId.toString())
                .thenReturn("#config/spec/apps/someapp");

        ValueChange change = mock(ValueChange.class);
        when(change.getLeft())
                .thenReturn(manifestBeanBefore);
        when(change.getRight())
                .thenReturn(manifestBeanAfter);
        when(change.getAffectedGlobalId())
                .thenReturn(globalId);
        when(change.getPropertyName())
                .thenReturn("manifest");
        when(change.getAffectedObject())
                .thenReturn(Optional.of(applicationBean));

        ChangeParser changeParser = new ChangeParser();
        ValueChangeParsingStrategy strategy = new ValueChangeParsingStrategy(changeParser);
        changeParser.addParsingStrategy(strategy);

        List<CfChange> changes = strategy.parse(change);

        assertThat(changes, notNullValue());
        assertThat(changes.size(), is(1));
        assertThat(changes.get(0), instanceOf(CfObjectValueChanged.class));
        CfObjectValueChanged objectValueChanged = (CfObjectValueChanged) changes.get(0);
        assertThat(objectValueChanged.getPath(), contains("config", "spec", "apps", "someapp", "manifest"));
        assertThat(objectValueChanged.getPropertyName(), is("disk"));
        assertThat(objectValueChanged.getValueBefore(), is("1024"));
        assertThat(objectValueChanged.getValueAfter(), is("2048"));
        assertThat(objectValueChanged.getAffectedObject(), is(applicationBean));
    }

    @Test
    public void testParseSucceedsWhenNotOnApplicationManifestBean() {
        ApplicationBean applicationBean = new ApplicationBean();

        GlobalId globalId = mock(GlobalId.class);
        when(globalId.toString())
                .thenReturn("#config/spec/apps/someapp");

        ValueChange change = mock(ValueChange.class);
        when(change.getLeft())
                .thenReturn("somemeta");
        when(change.getRight())
                .thenReturn("someothermeta");
        when(change.getAffectedGlobalId())
                .thenReturn(globalId);
        when(change.getPropertyName())
                .thenReturn("meta");
        when(change.getAffectedObject())
                .thenReturn(Optional.of(applicationBean));

        ValueChangeParsingStrategy strategy = new ValueChangeParsingStrategy(null);

        List<CfChange> changes = strategy.parse(change);

        assertThat(changes, notNullValue());
        assertThat(changes.size(), is(1));
        assertThat(changes.get(0), instanceOf(CfObjectValueChanged.class));
        CfObjectValueChanged objectValueChanged = (CfObjectValueChanged) changes.get(0);
        assertThat(objectValueChanged.getPath(), contains("config", "spec", "apps", "someapp"));
        assertThat(objectValueChanged.getPropertyName(), is("meta"));
        assertThat(objectValueChanged.getValueBefore(), is("somemeta"));
        assertThat(objectValueChanged.getValueAfter(), is("someothermeta"));
        assertThat(objectValueChanged.getAffectedObject(), is(applicationBean));
    }

    @Test
    public void testParseOnInvalidTypeThrowsException() {
        Change change = new ObjectRemoved(mock(GlobalId.class),
                Optional.of(new ApplicationBean()),
                Optional.of(mock(CommitMetadata.class)));
        ValueChangeParsingStrategy strategy = new ValueChangeParsingStrategy(null);

        assertThrows(IllegalArgumentException.class, () -> strategy.parse(change));
    }

}
