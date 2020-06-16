package cloud.foundry.cli.logic.diff;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cloud.foundry.cli.crosscutting.mapping.YamlMapper;
import cloud.foundry.cli.crosscutting.mapping.beans.ConfigBean;
import cloud.foundry.cli.crosscutting.mapping.beans.SpecBean;
import cloud.foundry.cli.logic.diff.change.ChangeType;
import cloud.foundry.cli.logic.diff.change.container.CfContainerChange;
import cloud.foundry.cli.logic.diff.change.map.CfMapChange;
import cloud.foundry.cli.logic.diff.change.object.CfObjectValueChanged;
import org.junit.jupiter.api.Test;

import java.io.IOException;

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
    public void testCreateDiffTreeSucceeds() throws IOException {
        // given
        String filePath = "./src/test/resources/basic/";
        ConfigBean configLive = YamlMapper.loadBean(filePath + "configLive.yml", ConfigBean.class);
        ConfigBean configDesired = YamlMapper.loadBean(filePath + "configDesired.yml", ConfigBean.class);

        // when
        DiffNode tree = new Differ().createDiffTree(configLive, configDesired);

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
        assertThat(app3.getChildNodes().size(), is(1));
        assertThat(app3.getChild("manifest"), notNullValue());
        //new app manifest object also
        DiffNode app3Manifest = app3.getChild("manifest");
        assertTrue(app3Manifest.isNewObject());
        assertTrue(app3Manifest.isLeaf());
    }
}
