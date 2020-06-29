package cloud.foundry.cli.logic.diff;

import cloud.foundry.cli.logic.diff.change.CfChange;

public interface FilterCriteria {

    boolean isMet(CfChange change);

}
