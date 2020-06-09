package cloud.foundry.cli.logic.diff;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cloud.foundry.cli.crosscutting.mapping.YamlMapper;
import cloud.foundry.cli.crosscutting.mapping.beans.ConfigBean;
import cloud.foundry.cli.crosscutting.mapping.beans.SpecBean;
import cloud.foundry.cli.logic.diff.change.ChangeType;
import cloud.foundry.cli.logic.diff.change.container.CfContainerChange;
import cloud.foundry.cli.logic.diff.change.map.CfMapChange;
import cloud.foundry.cli.logic.diff.change.object.CfNewObject;
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
        assertTrue(tree.getChildNodes().containsKey("spec"));

        // only spec, since there are not changes at target
        DiffNode specNode = tree.getChildNodes().get("spec");
        assertThat(specNode.getChildNodes().size(), is(2));
        assertTrue(specNode.getChildNodes().containsKey("services"));
        assertTrue(specNode.getChildNodes().containsKey("apps"));
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
        DiffNode services = specNode.getChildNodes().get("services");
        assertThat(services.getChanges().size(), is(0));
        //only one since the service (web-service-name) that is not in the desired config gets skipped
        assertThat(services.getChildNodes().size(), is(1));
        assertTrue(services.getChildNodes().containsKey("sql-service-name"));
        DiffNode service = services.getChildNodes().get("sql-service-name");
        assertThat(service.getChildNodes().size(), is(0));
        assertThat(service.getChanges().size(), is(1));
        assertThat(service.getChanges().get(0).getPropertyName(), is("plan"));
        assertThat(((CfObjectValueChanged) service.getChanges().get(0)).getValueBefore(), is("unsecure"));
        assertThat(((CfObjectValueChanged) service.getChanges().get(0)).getValueAfter(), is("secure"));

        //apps
        DiffNode apps = specNode.getChildNodes().get("apps");
        assertThat(apps.getChanges().size(), is(0));
        //only two since the app (app2) that is not in the desired config gets skipped
        assertThat(apps.getChildNodes().size(), is(2));
        assertTrue(apps.getChildNodes().containsKey("app1"));
        assertTrue(apps.getChildNodes().containsKey("app3"));

        //app1
        DiffNode app1 = apps.getChildNodes().get("app1");
        assertThat(app1.getChanges().size(), is(0));
        assertThat(app1.getChildNodes().size(), is(1));
        assertTrue(app1.getChildNodes().containsKey("manifest"));
        DiffNode app1Manifest = app1.getChildNodes().get("manifest");
        assertThat(app1Manifest.getChildNodes().size(), is(0));
        assertThat(app1Manifest.getChanges().size(), is(1));
        assertThat(app1Manifest.getChanges(), contains(instanceOf(CfMapChange.class)));
        CfMapChange environmentVariablesChange = (CfMapChange) app1Manifest.getChanges().get(0);
        assertThat(environmentVariablesChange.getChangedValues(), hasItems(
                hasProperty("changeType", is(ChangeType.ADDED)),
                hasProperty("changeType", is(ChangeType.REMOVED)),
                hasProperty("changeType", is(ChangeType.CHANGED))));
        assertThat(environmentVariablesChange.getPropertyName(), is("environmentVariables"));

        //app3
        DiffNode app3 = apps.getChildNodes().get("app3");
        assertThat(app3.getChanges().size(), is(1));
        assertTrue(app3.getChanges().get(0) instanceof CfNewObject);
        assertThat(app3.getChildNodes().size(), is(1));
        assertTrue(app3.getChildNodes().containsKey("manifest"));
        //new app manifest object also
        DiffNode app3Manifest = app3.getChildNodes().get("manifest");
        assertThat(app3Manifest.getChildNodes().size(), is(0));
        assertThat(app3Manifest.getChanges().size(), is(1));
        assertTrue(app3Manifest.getChanges().get(0) instanceof CfNewObject);
    }
}
