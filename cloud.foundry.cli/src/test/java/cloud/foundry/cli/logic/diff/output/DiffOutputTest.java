package cloud.foundry.cli.logic.diff.output;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import cloud.foundry.cli.crosscutting.mapping.beans.TargetBean;
import cloud.foundry.cli.logic.diff.DiffNode;
import cloud.foundry.cli.logic.diff.change.ChangeType;
import cloud.foundry.cli.logic.diff.change.container.CfContainerChange;
import cloud.foundry.cli.logic.diff.change.container.CfContainerValueChanged;
import cloud.foundry.cli.logic.diff.change.map.CfMapChange;
import cloud.foundry.cli.logic.diff.change.map.CfMapValueChanged;
import cloud.foundry.cli.logic.diff.change.object.CfNewObject;
import cloud.foundry.cli.logic.diff.change.object.CfRemovedObject;
import cloud.foundry.cli.logic.diff.change.object.CfObjectValueChanged;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class DiffOutputTest {

    DiffOutput diffOutput;

    @BeforeEach
    public void setUp() {
        this.diffOutput = new DiffOutput();
    }

    @Test
    public void testConstructorWithInvalidIncrementValue() {
        assertThrows(IllegalArgumentException.class, () -> new DiffOutput(0));
        assertThrows(IllegalArgumentException.class, () -> new DiffOutput(-1));
    }

    @Test
    public void testConstructorWithValidIncrementValue() {
        new DiffOutput(1);
        new DiffOutput(5);
    }

    @Test
    public void testFromOnlyRootNode() {
        // given
        DiffNode config = new DiffNode("config");

        // when
        String diffString = diffOutput.from(config);

        //then
        assertTrue(diffString.isEmpty());
    }

    @Test
    public void testFromWithMap() {
        // given
        DiffNode config = new DiffNode("config");
        CfMapChange cfMapChange = new CfMapChange(mock(Object.class),
                "mapName",
                Arrays.asList("root"),
                Arrays.asList(new CfMapValueChanged("key1",
                        "value1Before",
                        "value1After",
                        ChangeType.CHANGED),
                new CfMapValueChanged("key2",
                        "",
                        "value2",
                        ChangeType.ADDED),
                new CfMapValueChanged("key3",
                        "value3",
                        "",
                        ChangeType.REMOVED)));
        config.addChange(cfMapChange);

        // when
        String diffString = diffOutput.from(config);

        //then
        assertThat(diffString, is(" mapName:\n" +
                "+  key1: value1After\n" +
                "-  key1: value1Before\n" +
                "+  key2: value2\n" +
                "-  key3: value3"));
    }

    @Test
    public void testFromWithList() {
        // given
        DiffNode config = new DiffNode("config");
        CfContainerChange cfContainerChange = new CfContainerChange(mock(Object.class),
                "listName",
                Arrays.asList("root"),
                Arrays.asList(new CfContainerValueChanged("value1", ChangeType.ADDED),
                        new CfContainerValueChanged("value2", ChangeType.REMOVED)));
        config.addChange(cfContainerChange);

        // when
        String diffString = diffOutput.from(config);

        //then
        assertThat(diffString, is(" listName:\n" +
                "+- value1\n" +
                "-- value2"));
    }

    @Test
    public void testFromWithObjectValueChanged() {
        // given
        DiffNode config = new DiffNode("config");
        CfObjectValueChanged cfObjectValueChanged = new CfObjectValueChanged(mock(Object.class),
                "key",
                Arrays.asList("root"),
                "valueBefore",
                "valueAfter");
        config.addChange(cfObjectValueChanged);

        // when
        String diffString = diffOutput.from(config);

        //then
        assertThat(diffString, is("+key: valueAfter\n" +
                "-key: valueBefore"));
    }

    @Test
    public void testFromWithNewObject() {
        // given
        DiffNode config = new DiffNode("config");
        DiffNode target = new DiffNode("target");
        config.addChild(target);
        TargetBean targetBean = new TargetBean();
        targetBean.setOrg("orgName");

        CfNewObject cfNewObject = new CfNewObject(targetBean,
                "target",
                Arrays.asList("config"));
        target.addChange(cfNewObject);

        // when
        String diffString = diffOutput.from(config);

        //then
        assertThat(diffString, is("+target:\n" +
                "+  org: orgName"));
    }


    @Test
    public void testFromWithRemovedObject() {
        // given
        DiffNode config = new DiffNode("config");
        DiffNode target = new DiffNode("target");
        config.addChild(target);
        TargetBean targetBean = new TargetBean();
        targetBean.setOrg("orgName");

        CfRemovedObject cfRemovedObject = new CfRemovedObject(targetBean,
                "target",
                Arrays.asList("config"));
        target.addChange(cfRemovedObject);

        // when
        String diffString = diffOutput.from(config);

        //then
        assertThat(diffString, is("-target:\n" +
                "-  org: orgName"));
    }


    @Test
    public void testFromWithObjectValueChangedDeep() {
        // given
        DiffNode config = new DiffNode("config");
        DiffNode target = new DiffNode("target");
        config.addChild(target);

        CfObjectValueChanged cfObjectValueChanged = new CfObjectValueChanged(new TargetBean(),
                "org",
                Arrays.asList("config", "target"),
                "orgNameBefore",
                "orgNameAfter");
        target.addChange(cfObjectValueChanged);

        // when
        String diffString = diffOutput.from(config);

        //then
        assertThat(diffString, is(" target:\n" +
                "+  org: orgNameAfter\n" +
                "-  org: orgNameBefore"));
    }

    @Test
    public void testFromWithObjectValueChangedDeepAndSetIndentationIncrement() {
        // given
        DiffOutput diffOutput = new DiffOutput(4);
        DiffNode config = new DiffNode("config");
        DiffNode target = new DiffNode("target");
        config.addChild(target);

        CfObjectValueChanged cfObjectValueChanged = new CfObjectValueChanged(new TargetBean(),
                "org",
                Arrays.asList("config", "target"),
                "orgNameBefore",
                "orgNameAfter");
        target.addChange(cfObjectValueChanged);

        // when
        String diffString = diffOutput.from(config);

        //then
        assertThat(diffString, is(" target:\n" +
                "+    org: orgNameAfter\n" +
                "-    org: orgNameBefore"));
    }
}
