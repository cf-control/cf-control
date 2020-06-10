package cloud.foundry.cli.logic.diff.change;

/**
 * A change can be any of these types.
 * ADDED if a new container entry, map entry or object field value was added
 * REMOVED if a container entry, map entry or object field value was removed
 * CHANGED if map entry or a object field value was changed
 */
public enum ChangeType {
    ADDED,
    REMOVED,
    CHANGED
}
