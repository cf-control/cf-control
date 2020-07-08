package cloud.foundry.cli.logic.diff;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cloud.foundry.cli.crosscutting.mapping.YamlMapper;
import cloud.foundry.cli.crosscutting.mapping.beans.ConfigBean;
import cloud.foundry.cli.crosscutting.mapping.beans.ServiceBean;
import cloud.foundry.cli.crosscutting.mapping.beans.SpecBean;
import cloud.foundry.cli.crosscutting.mapping.beans.TargetBean;
import cloud.foundry.cli.logic.diff.change.ChangeType;
import cloud.foundry.cli.logic.diff.change.container.CfContainerChange;
import cloud.foundry.cli.logic.diff.change.map.CfMapChange;
import cloud.foundry.cli.logic.diff.change.object.CfObjectValueChanged;
import cloud.foundry.cli.logic.diff.change.object.CfRemovedObject;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;

/**
 * Test for {@link Differ}
 */
public class DifferTest {

    @Test
    public void testCreateDiffTreeFromNullThrowsException() {
        assertThrows(NullPointerException.class, () -> new Differ().createDiffTree(null, new ConfigBean()));
        assertThrows(NullPointerException.class, () -> new Differ().createDiffTree(new ConfigBean(), null));
    }

    @Test
    public void testCreateDiffTreeOfDifferentBeanTypesThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new Differ().createDiffTree(new SpecBean(), new ConfigBean()));
    }

    @Test
    public void testIgnoreRemovedObjects() {
        // given
        ConfigBean configLive = new ConfigBean();
        ConfigBean configDesired = new ConfigBean();
        configLive.setSpec(new SpecBean());
        configLive.setTarget(new TargetBean());

        Differ differ = new Differ();

        // when
        DiffNode treeWithRemovedObjects = differ.createDiffTree(configLive, configDesired);
        differ.ignoreRemovedObjects();
        DiffNode treeWithoutRemovedObjects = differ.createDiffTree(configLive, configDesired);

        // then
        assertThat(treeWithRemovedObjects.getChildNodes().size(), is(2));
        assertThat(treeWithRemovedObjects.getChanges().size(), is(0));

        DiffNode specNode  = treeWithRemovedObjects.getChild("spec");
        assertThat(specNode.getChanges().size(), is(1));
        assertThat(specNode.getChanges().get(0), is(instanceOf(CfRemovedObject.class)));

        DiffNode targetNode  = treeWithRemovedObjects.getChild("target");
        assertThat(targetNode.getChanges().size(), is(1));
        assertThat(targetNode.getChanges().get(0), is(instanceOf(CfRemovedObject.class)));

        assertThat(treeWithoutRemovedObjects.getChildNodes().size(), is(0));
        assertThat(treeWithoutRemovedObjects.getChanges().size(), is(0));
    }

    @Test
    public void testIgnoreSpecBeanMapChange() {
        // given
        SpecBean specLive = new SpecBean();
        SpecBean specDesired = new SpecBean();
        specLive.setServices(Collections.singletonMap("someservice", new ServiceBean()));

        Differ differ = new Differ();

        // when
        DiffNode specTreeWithMapChange = differ.createDiffTree(specLive, specDesired);
        differ.ignoreSpecBeanMapChange();
        DiffNode specTreeWithoutMapChange = differ.createDiffTree(specLive, specDesired);

        // then
        assertThat(specTreeWithMapChange.getChildNodes().size(), is(1));
        assertThat(specTreeWithMapChange.getChanges().size(), is(1));
        assertThat(specTreeWithMapChange.getChanges().get(0), is(instanceOf(CfMapChange.class)));

        assertThat(specTreeWithoutMapChange.getChildNodes().size(), is(1));
        assertThat(specTreeWithoutMapChange.getChanges().size(), is(0));
        DiffNode serviceNode = specTreeWithoutMapChange.getChild("services").getChild("someservice");
        assertThat(serviceNode.getChanges().size(), is(1));
        assertThat(serviceNode.getChanges(), hasItem(instanceOf(CfRemovedObject.class)));
    }

    @Test
    public void testCreateDiffTreeSucceedsWithoutSpecBeanMapChangeAndWithoutRemovedObjects() throws IOException {
        // given
        String filePath = "./src/test/resources/basic/";
        ConfigBean configLive = YamlMapper.loadBeanFromFile(filePath + "configLive.yml", ConfigBean.class);
        ConfigBean configDesired = YamlMapper.loadBeanFromFile(filePath + "configDesired.yml", ConfigBean.class);

        Differ differ = new Differ();
        differ.ignoreSpecBeanMapChange();
        differ.ignoreRemovedObjects();

        // when
        DiffNode tree = differ.createDiffTree(configLive, configDesired);

        // then
        //no changes at root
        assertThat(tree.getChanges().size(), is(0));
        assertThat(tree.getChildNodes().size(), is(1));
        assertThat(tree.getChild("spec"), notNullValue());
        assertTrue(tree.isRoot());

        // only spec, since there are no changes at target
        DiffNode specNode = tree.getChild("spec");
        assertThat(specNode.getChildNodes().size(), is(2));
        assertThat(specNode.getChild("services"), notNullValue());
        assertThat(specNode.getChild("apps"), notNullValue());
        assertThat(specNode.getChanges().size(), is(1));

        //spaceDevelopers
        assertTrue(specNode.getChanges().get(0) instanceof CfContainerChange);
        CfContainerChange spaceDeveloperChanges = (CfContainerChange) specNode.getChanges().get(0);
        assertThat(spaceDeveloperChanges.getPropertyName(), is("spaceDevelopers"));
        assertThat(spaceDeveloperChanges.getChangedValues().size(), is(2));
        assertThat(spaceDeveloperChanges.getChangedValues(), hasItems(
                hasProperty("changeType", is(ChangeType.ADDED)),
                hasProperty("changeType", is(ChangeType.REMOVED))));

        //services
        DiffNode services = specNode.getChild("services");
        assertThat(services.getChanges().size(), is(0));
        //only one since the service (web-service-name) that is not in the desired config gets skipped
        assertThat(services.getChildNodes().size(), is(1));
        assertThat(services.getChild("sql-service-name"), notNullValue());
        DiffNode service = services.getChild("sql-service-name");
        assertTrue(service.isLeaf());
        assertThat(service.getChanges().size(), is(1));
        assertThat(service.getChanges().get(0).getPropertyName(), is("plan"));
        assertThat(((CfObjectValueChanged) service.getChanges().get(0)).getValueBefore(), is("unsecure"));
        assertThat(((CfObjectValueChanged) service.getChanges().get(0)).getValueAfter(), is("secure"));

        //apps
        DiffNode apps = specNode.getChild("apps");
        assertThat(apps.getChanges().size(), is(0));
        //only two since the app (app2) that is not in the desired config gets skipped
        assertThat(apps.getChildNodes().size(), is(2));
        assertThat(apps.getChild("app1"), notNullValue());
        assertThat(apps.getChild("app3"), notNullValue());

        //app1
        DiffNode app1 = apps.getChild("app1");
        assertThat(app1.getChanges().size(), is(0));
        assertThat(app1.getChildNodes().size(), is(1));
        assertThat(app1.getChild("manifest"), notNullValue());

        DiffNode app1Manifest = app1.getChild("manifest");
        assertTrue(app1Manifest.isLeaf());
        assertThat(app1Manifest.getChanges().size(), is(1));
        assertThat(app1Manifest.getChanges(), contains(instanceOf(CfMapChange.class)));
        CfMapChange environmentVariablesChange = (CfMapChange) app1Manifest.getChanges().get(0);
        assertThat(environmentVariablesChange.getChangedValues(), hasItems(
                hasProperty("changeType", is(ChangeType.ADDED)),
                hasProperty("changeType", is(ChangeType.REMOVED)),
                hasProperty("changeType", is(ChangeType.CHANGED))));
        assertThat(environmentVariablesChange.getPropertyName(), is("environmentVariables"));

        //app3
        DiffNode app3 = apps.getChild("app3");
        assertTrue(app3.isNewObject());
        assertThat(app3.getChildNodes().size(), is(0));
        assertTrue(app3.isLeaf());

    }
}
