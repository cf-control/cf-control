package cloud.foundry.cli.logic.diff;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import cloud.foundry.cli.logic.diff.change.CfChange;
import cloud.foundry.cli.logic.diff.change.container.CfContainerChange;
import cloud.foundry.cli.logic.diff.change.map.CfMapChange;
import cloud.foundry.cli.logic.diff.change.object.CfNewObject;
import cloud.foundry.cli.logic.diff.change.object.CfObjectValueChanged;
import cloud.foundry.cli.logic.diff.change.object.CfRemovedObject;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

public class DiffResultTest {

    @Test
    public void testConstructorOnNullNodeThrowsException() {
        // when and then
        assertThrows(NullPointerException.class, () -> new DiffResult(null));
    }


    @Test
    public void testGetApplicationChangesWithDataReturnsCorrectMapOfChanges() {
        // given
        DiffResult diffResult = new DiffResult(createTreeStructure());

        // when
        Map<String, List<CfChange>> applicationChanges = diffResult.getApplicationChanges();

        // then
        assertThat(applicationChanges, notNullValue());
        assertThat(applicationChanges.size(), Matchers.is(2));
        assertThat(applicationChanges, hasKey("newApp"));
        assertThat(applicationChanges.get("newApp"), notNullValue());
        assertThat(applicationChanges.get("newApp").size(), is(1));
        assertThat(applicationChanges.get("newApp"), contains(instanceOf(CfNewObject.class)));
        assertThat(applicationChanges, hasKey("someApp"));
        assertThat(applicationChanges.get("someApp"), notNullValue());
        assertThat(applicationChanges.get("someApp").size(), is(2));
        assertThat(applicationChanges.get("someApp"),hasItems(
                instanceOf(CfObjectValueChanged.class),
                instanceOf(CfMapChange.class)));

    }

    @Test
    public void testGetApplicationChangesWithEmptyDataReturnsEmptyMap() {
        // given
        DiffResult diffResult = new DiffResult(new DiffNode("config"));

        // when
        Map<String, List<CfChange>> applicationChanges = diffResult.getApplicationChanges();

        // then
        assertThat(applicationChanges, notNullValue());
        assertThat(applicationChanges.size(), is(0));
    }

    @Test
    public void testGetServicesChangesReturnsCorrectMapOfChanges() {
        // given
        DiffResult diffResult = new DiffResult(createTreeStructure());

        // when
        Map<String, List<CfChange>> serviceChanges = diffResult.getServiceChanges();

        // then
        assertThat(serviceChanges, notNullValue());
        assertThat(serviceChanges.size(), Matchers.is(2));
        assertThat(serviceChanges, hasKey("removedService"));
        assertThat(serviceChanges.get("removedService"), notNullValue());
        assertThat(serviceChanges.get("removedService").size(), is(1));
        assertThat(serviceChanges.get("removedService"), contains(instanceOf(CfRemovedObject.class)));
        assertThat(serviceChanges, hasKey("someService"));
        assertThat(serviceChanges.get("someService"), notNullValue());
        assertThat(serviceChanges.get("someService").size(), is(1));
        assertThat(serviceChanges.get("someService"),hasItems(
                instanceOf(CfContainerChange.class)));
    }

    @Test
    public void testGetServiceChangesWithEmptyDataReturnsEmptyMap() {
        // given
        DiffResult diffResult = new DiffResult(new DiffNode("config"));

        // when
        Map<String, List<CfChange>> serviceChanges = diffResult.getServiceChanges();

        // then
        assertThat(serviceChanges, notNullValue());
        assertThat(serviceChanges.size(), is(0));
    }

    @Test
    public void testGetApiVersionChangeWithDataReturnsCorrectChange() {
        // given
        DiffResult diffResult = new DiffResult(createTreeStructure());

        // when
        CfChange apiVersionChange = diffResult.getApiVersionChange();

        // then
        assertThat(apiVersionChange, notNullValue());
        assertThat(apiVersionChange, instanceOf(CfObjectValueChanged.class));
        assertThat(apiVersionChange.getPropertyName(), is("apiVersion"));
    }

    @Test
    public void testGetApiVersionChangeWithEmptyDataReturnsNull() {
        // given
        DiffResult diffResult = new DiffResult(new DiffNode("config"));

        // when
        CfChange apiVersionChange = diffResult.getApiVersionChange();

        // then
        assertThat(apiVersionChange, nullValue());
    }

    @Test
    public void testGetSpaceDevelopersChangeWithDataReturnsCorrectChange() {
        // given
        DiffResult diffResult = new DiffResult(createTreeStructure());

        // when
        CfChange spaceDevelopersChange = diffResult.getSpaceDevelopersChange();

        // then
        assertThat(spaceDevelopersChange, notNullValue());
        assertThat(spaceDevelopersChange, instanceOf(CfContainerChange.class));
        assertThat(spaceDevelopersChange.getPropertyName(), is("spaceDevelopers"));
    }

    @Test
    public void testGetSpaceDevelopersChangeWithEmptyDataReturnsNull() {
        // given
        DiffResult diffResult = new DiffResult(new DiffNode("config"));

        // when
        CfChange spaceDevelopersChange = diffResult.getSpaceDevelopersChange();

        // then
        assertThat(spaceDevelopersChange, nullValue());
    }

    @Test
    public void testGetTargetChangesWithDataReturnsListOfChanges() {
        // given
        DiffResult diffResult = new DiffResult(createTreeStructure());

        // when
        List<CfChange> targetChanges = diffResult.getTargetChanges();

        // then
        assertThat(targetChanges, notNullValue());
        assertThat(targetChanges.size(), Matchers.is(1));
        assertThat(targetChanges, hasItems(instanceOf(CfObjectValueChanged.class)));
        assertThat(targetChanges.get(0).getPropertyName(), is("space"));
    }

    @Test
    public void testGetTargetChangesWithNoDataReturnsEmptyList() {
        // given
        DiffResult diffResult = new DiffResult(new DiffNode("config"));

        // when
        List<CfChange> targetChanges = diffResult.getTargetChanges();

        // then
        assertThat(targetChanges, notNullValue());
        assertThat(targetChanges.size(), is(0));
    }

    private DiffNode createTreeStructure() {
        DiffNode configNode = new DiffNode("config");
        DiffNode targetNode = new DiffNode("target");
        DiffNode specNode = new DiffNode("spec");
        DiffNode appsNode = new DiffNode("apps");
        DiffNode servicesNode = new DiffNode("services");
        DiffNode newAppNode = new DiffNode("newApp");
        DiffNode removedServiceNode = new DiffNode("removedService");
        DiffNode someAppNode = new DiffNode("someApp");
        DiffNode someAppManifestNode = new DiffNode("manifest");
        DiffNode someAppEnvVarNode = new DiffNode("environmentVariables");
        DiffNode someServiceNode = new DiffNode("someService");
        DiffNode someServiceTagsNode = new DiffNode("tags");

        configNode.addChild(targetNode);
        configNode.addChild(specNode);
        specNode.addChild(appsNode);
        specNode.addChild(servicesNode);
        appsNode.addChild(newAppNode);
        appsNode.addChild(someAppNode);
        servicesNode.addChild(removedServiceNode);
        servicesNode.addChild(someServiceNode);
        someAppNode.addChild(someAppManifestNode);
        someAppManifestNode.addChild(someAppEnvVarNode);
        someServiceNode.addChild(someServiceTagsNode);

        CfObjectValueChanged apiVersionChange = Mockito.mock(CfObjectValueChanged.class);
        when(apiVersionChange.getPropertyName()).thenReturn("apiVersion");

        CfObjectValueChanged spaceChange = Mockito.mock(CfObjectValueChanged.class);
        when(spaceChange.getPropertyName()).thenReturn("space");

        CfContainerChange spaceDevelopersChange = Mockito.mock(CfContainerChange.class);
        when(spaceDevelopersChange.getPropertyName()).thenReturn("spaceDevelopers");

        CfNewObject newAppChange = Mockito.mock(CfNewObject.class);
        when(newAppChange.getPropertyName()).thenReturn("newApp");

        CfRemovedObject removedServiceChange = Mockito.mock(CfRemovedObject.class);
        when(newAppChange.getPropertyName()).thenReturn("removedService");

        CfMapChange mapChange = Mockito.mock(CfMapChange.class);
        when(newAppChange.getPropertyName()).thenReturn("environmentVariables");

        CfObjectValueChanged pathChange = Mockito.mock(CfObjectValueChanged.class);
        when(newAppChange.getPropertyName()).thenReturn("path");

        CfContainerChange containerChange = Mockito.mock(CfContainerChange.class);
        when(newAppChange.getPropertyName()).thenReturn("tags");

        configNode.addChange(apiVersionChange);
        targetNode.addChange(spaceChange);
        specNode.addChange(spaceDevelopersChange);
        newAppNode.addChange(newAppChange);
        removedServiceNode.addChange(removedServiceChange);
        someAppNode.addChange(pathChange);
        someAppManifestNode.addChange(mapChange);
        someServiceNode.addChange(containerChange);

        return configNode;
    }
}
