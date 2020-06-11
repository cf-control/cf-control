package cloud.foundry.cli.logic.apply.beanchanges;

import cloud.foundry.cli.logic.diff.DiffNode;
import cloud.foundry.cli.logic.diff.change.CfChange;

import java.util.LinkedList;
import java.util.List;

public abstract class BeanChange {

    protected DiffNode node;

    public BeanChange(DiffNode node) {
        this.node = node;
    }

    public List<CfChange> getAllChanges() {
        List<CfChange> result = new LinkedList<>();
        doGetAllChanges(this.node, result);
        return result;
    }

    private void doGetAllChanges(DiffNode node, List<CfChange> list) {
        list.addAll(node.getChanges());
        for (DiffNode child : node.getChildNodes().values()) {
            doGetAllChanges(child, list);
        }
    }

}
