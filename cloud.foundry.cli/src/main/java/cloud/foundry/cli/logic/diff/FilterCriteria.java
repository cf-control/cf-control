package cloud.foundry.cli.logic.diff;

import cloud.foundry.cli.logic.diff.change.CfChange;

/**
 * Can be used as a lambda interface
 * Is used for filtering the parsed changes in {@link cloud.foundry.cli.logic.diff.Differ}
 */
public interface FilterCriteria {

    boolean isMet(CfChange change);

}
