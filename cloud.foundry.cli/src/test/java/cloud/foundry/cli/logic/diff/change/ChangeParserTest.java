package cloud.foundry.cli.logic.diff.change;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationManifestBean;
import cloud.foundry.cli.crosscutting.mapping.beans.Bean;
import cloud.foundry.cli.crosscutting.mapping.beans.ConfigBean;
import cloud.foundry.cli.crosscutting.mapping.beans.SpecBean;
import cloud.foundry.cli.crosscutting.mapping.beans.TargetBean;
import cloud.foundry.cli.logic.diff.change.container.CfContainerChange;
import cloud.foundry.cli.logic.diff.change.container.CfContainerValueChanged;
import cloud.foundry.cli.logic.diff.change.map.CfMapChange;
import cloud.foundry.cli.logic.diff.change.map.CfMapValueChanged;
import cloud.foundry.cli.logic.diff.change.object.CfNewObject;
import cloud.foundry.cli.logic.diff.change.object.CfRemovedObject;
import cloud.foundry.cli.logic.diff.change.object.CfObjectValueChanged;
import cloud.foundry.cli.logic.diff.change.parsing.ContainerChangeParsingStrategy;
import cloud.foundry.cli.logic.diff.change.parsing.MapChangeParsingStrategy;
import cloud.foundry.cli.logic.diff.change.parsing.NewObjectParsingStrategy;
import cloud.foundry.cli.logic.diff.change.parsing.RemovedObjectParsingStrategy;
import cloud.foundry.cli.logic.diff.change.parsing.ValueChangeParsingStrategy;
import org.javers.core.Changes;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.ListCompareAlgorithm;
import org.javers.core.diff.changetype.NewObject;
import org.javers.core.diff.changetype.ObjectRemoved;
import org.javers.core.diff.changetype.ReferenceChange;
import org.javers.core.diff.changetype.ValueChange;
import org.javers.core.diff.changetype.container.ContainerChange;
import org.javers.core.diff.changetype.container.ValueAdded;
import org.javers.core.diff.changetype.container.ValueRemoved;
import org.javers.core.diff.changetype.map.EntryAdded;
import org.javers.core.diff.changetype.map.EntryRemoved;
import org.javers.core.diff.changetype.map.EntryValueChange;
import org.javers.core.diff.changetype.map.MapChange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Test for {@link ChangeParser}
 */
public class ChangeParserTest {

    ChangeParser changeParser;

    @BeforeEach
    public void setup() {
        this.changeParser = new ChangeParser();
        this.changeParser.addParsingStrategy(new NewObjectParsingStrategy());
        this.changeParser.addParsingStrategy(new RemovedObjectParsingStrategy());
        this.changeParser.addParsingStrategy(new MapChangeParsingStrategy());
        this.changeParser.addParsingStrategy(new ContainerChangeParsingStrategy());
        this.changeParser.addParsingStrategy(new ContainerChangeParsingStrategy());
        this.changeParser.addParsingStrategy(new ValueChangeParsingStrategy(this.changeParser));
    }

    @Test
    public void testParseNewObject() {
        // given
        ConfigBean configBeanLive = new ConfigBean();
        ConfigBean configBeanDesired = new ConfigBean();
        configBeanDesired.setTarget(new TargetBean());

        NewObject newObject = createJaversChanges(configBeanLive, configBeanDesired)
                .getChangesByType(NewObject.class)
                .get(0);



        // when
        List<CfChange> cfChange = changeParser.parse(newObject);

        // then
        assertThat(cfChange.size(), is(1));
        assertTrue(cfChange.get(0) instanceof CfNewObject);
        assertThat(cfChange.get(0).getAffectedObject(), is(newObject.getAffectedObject().get()));
    }

    @Test
    public void testParseRemovedObject() {
        // given
        ConfigBean configBeanLive = new ConfigBean();
        ConfigBean configBeanDesired = new ConfigBean();
        configBeanLive.setTarget(new TargetBean());
        ObjectRemoved objectRemoved = createJaversChanges(configBeanLive, configBeanDesired)
                .getChangesByType(ObjectRemoved.class)
                .get(0);


        // when
        List<CfChange> cfChange = changeParser.parse(objectRemoved);

        // then
        assertThat(cfChange.size(), is(1));
        assertTrue(cfChange.get(0) instanceof CfRemovedObject);
        assertThat(cfChange.get(0).getAffectedObject(), is(objectRemoved.getAffectedObject().get()));
    }

    @Test
    public void testParseValueChange() {
        // given
        ConfigBean configBeanLive = new ConfigBean();
        configBeanLive.setApiVersion("2.0");
        ConfigBean configBeanDesired = new ConfigBean();
        configBeanDesired.setApiVersion("2.1");
        ValueChange valueChange = createJaversChanges(configBeanLive, configBeanDesired)
                .getChangesByType(ValueChange.class)
                .get(0);


        // when
        List<CfChange> cfChange = changeParser.parse(valueChange);

        // then

        assertThat(cfChange.size(), is(1));
        assertTrue(cfChange.get(0) instanceof CfObjectValueChanged);
        assertThat(cfChange.get(0).getAffectedObject(), is(valueChange.getAffectedObject().get()));
        assertThat(cfChange.get(0).getPropertyName(), is(valueChange.getPropertyName()));
        assertThat(((CfObjectValueChanged) cfChange.get(0)).getValueBefore(), is(valueChange.getLeft().toString()));
        assertThat(((CfObjectValueChanged) cfChange.get(0)).getValueAfter(), is(valueChange.getRight().toString()));
    }


    @Test
    public void testParseMapChangeNewEntry() {
        // given
        ApplicationManifestBean appManfiestLive = new ApplicationManifestBean();
        appManfiestLive.setEnvironmentVariables(new HashMap<>());
        ApplicationManifestBean appManifestDesired = new ApplicationManifestBean();
        appManifestDesired.setEnvironmentVariables(Collections.singletonMap("key", "value"));

        MapChange mapChange = createJaversChanges(appManfiestLive, appManifestDesired)
                .getChangesByType(MapChange.class)
                .get(0);

        // when
        List<CfChange> cfChange = changeParser.parse(mapChange);

        // then
        assertThat(cfChange.size(), is(1));
        assertTrue(cfChange.get(0) instanceof CfMapChange);
        CfMapChange cfMapChange = (CfMapChange) cfChange.get(0);
        assertThat(cfMapChange.getChangedValues().size(), is(1));
        assertThat(cfMapChange.getValueChangesBy(ChangeType.ADDED).size(), is(1));
        assertThat(cfMapChange.getPropertyName(), is("environmentVariables"));
        assertThat(cfMapChange.getPropertyName(), is(mapChange.getPropertyName()));

        CfMapValueChanged mapChangeValue = cfMapChange.getValueChangesBy(ChangeType.ADDED).get(0);
        EntryAdded entryAdded = mapChange.getEntryAddedChanges().get(0);
        assertThat(mapChangeValue.getKey(), is(entryAdded.getKey().toString()));
        assertThat(mapChangeValue.getChangeType(), is(ChangeType.ADDED));
        assertThat(mapChangeValue.getValueBefore(), is(""));
        assertThat(mapChangeValue.getValueAfter(), is(entryAdded.getValue().toString()));
    }

    @Test
    public void testParseMapChangeRemovedEntry() {
        // given
        ApplicationManifestBean appManfiestLive = new ApplicationManifestBean();
        appManfiestLive.setEnvironmentVariables(Collections.singletonMap("key", "value"));
        ApplicationManifestBean appManifestDesired = new ApplicationManifestBean();
        appManifestDesired.setEnvironmentVariables(new HashMap<>());

        MapChange mapChange = createJaversChanges(appManfiestLive, appManifestDesired)
                .getChangesByType(MapChange.class)
                .get(0);

        // when
        List<CfChange> cfChange = changeParser.parse(mapChange);

        // then
        assertThat(cfChange.size(), is(1));
        assertTrue(cfChange.get(0) instanceof CfMapChange);
        CfMapChange cfMapChange = (CfMapChange) cfChange.get(0);
        assertThat(cfMapChange.getChangedValues().size(), is(1));
        assertThat(cfMapChange.getValueChangesBy(ChangeType.REMOVED).size(), is(1));
        assertThat(cfMapChange.getPropertyName(), is("environmentVariables"));
        assertThat(cfMapChange.getPropertyName(), is(mapChange.getPropertyName()));

        CfMapValueChanged mapChangeValue = cfMapChange.getValueChangesBy(ChangeType.REMOVED).get(0);
        EntryRemoved entryRemoved = mapChange.getEntryRemovedChanges().get(0);
        assertThat(mapChangeValue.getKey(), is(entryRemoved.getKey()));
        assertThat(mapChangeValue.getChangeType(), is(ChangeType.REMOVED));
        assertThat(mapChangeValue.getValueBefore(), is(entryRemoved.getValue().toString()));
        assertThat(mapChangeValue.getValueAfter(), is(""));
    }

    @Test
    public void testParseMapChangeChangedEntry() {
        // given
        ApplicationManifestBean appManfiestLive = new ApplicationManifestBean();
        appManfiestLive.setEnvironmentVariables(Collections.singletonMap("key", "value"));
        ApplicationManifestBean appManifestDesired = new ApplicationManifestBean();
        appManifestDesired.setEnvironmentVariables(Collections.singletonMap("key", "newValue"));

        MapChange mapChange = createJaversChanges(appManfiestLive, appManifestDesired)
                .getChangesByType(MapChange.class)
                .get(0);

        // when
        List<CfChange> cfChange = changeParser.parse(mapChange);

        // then
        assertThat(cfChange.size(), is(1));
        assertTrue(cfChange.get(0) instanceof CfMapChange);
        CfMapChange cfMapChange = (CfMapChange) cfChange.get(0);
        assertThat(cfMapChange.getChangedValues().size(), is(1));
        assertThat(cfMapChange.getValueChangesBy(ChangeType.CHANGED).size(), is(1));
        assertThat(cfMapChange.getPropertyName(), is("environmentVariables"));
        assertThat(cfMapChange.getPropertyName(), is(mapChange.getPropertyName()));

        CfMapValueChanged mapChangeValue = cfMapChange.getValueChangesBy(ChangeType.CHANGED).get(0);
        EntryValueChange entryValueChange = mapChange.getEntryValueChanges().get(0);
        assertThat(mapChangeValue.getKey(), is(entryValueChange.getKey()));
        assertThat(mapChangeValue.getChangeType(), is(ChangeType.CHANGED));
        assertThat(mapChangeValue.getValueBefore(), is(entryValueChange.getLeftValue().toString()));
        assertThat(mapChangeValue.getValueAfter(), is(entryValueChange.getRightValue().toString()));
    }

    @Test
    public void testParseContainerChangeAddedEntry() {
        // given
        SpecBean specLive = new SpecBean();
        specLive.setSpaceDevelopers(Collections.emptyList());
        SpecBean specDesired = new SpecBean();
        specDesired.setSpaceDevelopers(Collections.singletonList("value"));
        ContainerChange containerChange = createJaversChanges(specLive, specDesired)
                .getChangesByType(ContainerChange.class)
                .get(0);

        // when
        List<CfChange> cfChange = changeParser.parse(containerChange);

        // then
        assertThat(cfChange.size(), is(1));
        assertTrue(cfChange.get(0) instanceof CfContainerChange);
        CfContainerChange cfContainerChange = (CfContainerChange) cfChange.get(0);
        assertThat(cfContainerChange.getPropertyName(), is("spaceDevelopers"));
        assertThat(cfContainerChange.getValueChangesBy(ChangeType.ADDED).size(), is(1));
        assertThat(cfContainerChange.getChangedValues().size(), is(1));

        CfContainerValueChanged containerChangeValue = cfContainerChange.getValueChangesBy(ChangeType.ADDED).get(0);
        ValueAdded valueAdded = containerChange.getValueAddedChanges().get(0);
        assertThat(containerChangeValue.getChangeType(), is(ChangeType.ADDED));
        assertThat(containerChangeValue.getValue(), is(valueAdded.getAddedValue().toString()));
    }

    @Test
    public void testParseContainerChangeRemovedEntry() {
        // given
        SpecBean specLive = new SpecBean();
        specLive.setSpaceDevelopers(Collections.singletonList("value"));
        SpecBean specDesired = new SpecBean();
        specDesired.setSpaceDevelopers(Collections.emptyList());
        ContainerChange containerChange = createJaversChanges(specLive, specDesired)
                .getChangesByType(ContainerChange.class)
                .get(0);

        // when
        List<CfChange> cfChange = changeParser.parse(containerChange);

        // then
        assertThat(cfChange.size(), is(1));
        assertTrue(cfChange.get(0) instanceof CfContainerChange);
        CfContainerChange cfContainerChange = (CfContainerChange) cfChange.get(0);
        assertThat(cfContainerChange.getPropertyName(), is("spaceDevelopers"));
        assertThat(cfContainerChange.getValueChangesBy(ChangeType.REMOVED).size(), is(1));
        assertThat(cfContainerChange.getChangedValues().size(), is(1));

        CfContainerValueChanged containerChangeValue = cfContainerChange.getValueChangesBy(ChangeType.REMOVED).get(0);
        ValueRemoved valueRemoved = containerChange.getValueRemovedChanges().get(0);
        assertThat(containerChangeValue.getChangeType(), is(ChangeType.REMOVED));
        assertThat(containerChangeValue.getValue(), is(valueRemoved.getRemovedValue().toString()));
    }

    @Test
    public void testParseReferenceChangeReturnsNull() {
        // given
        ConfigBean configLive = new ConfigBean();
        ConfigBean configDesired = new ConfigBean();
        TargetBean targetLive = new TargetBean();
        configLive.setTarget(targetLive);

        ReferenceChange referenceChange = createJaversChanges(configLive, configDesired)
                .getChangesByType(ReferenceChange.class)
                .get(0);
        assertThat(referenceChange, notNullValue());
        assertThat(referenceChange.getPropertyName(), is("target"));

        // when
        List<CfChange> cfChange = changeParser.parse(referenceChange);

        // then
        assertThat(cfChange.size(), is(0));
    }

    private static Changes createJaversChanges(Bean live, Bean desired) {
        return JaversBuilder
                .javers()
                .withListCompareAlgorithm(ListCompareAlgorithm.AS_SET)
                .registerValueObject(ApplicationManifestBean.class)
                .build()
                .compare(live, desired)
                .getChanges();
    }
}
