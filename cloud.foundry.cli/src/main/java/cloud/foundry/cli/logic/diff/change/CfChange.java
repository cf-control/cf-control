package cloud.foundry.cli.logic.diff.change;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

/**
 * Base class for all change classes.
 */
public abstract class CfChange {

    protected Object affectedObject;
    protected String propertyName;
    protected List<String> path;

    /**
     * @param affectedObject the object that holds the changed content as a field
     * @param propertyName the field name of the content
     * @param path the field names of the object graph that lead to the content (with the compared object as root)
     * @throws NullPointerException if any of the arguments is null
     * @throws IllegalArgumentException if the path does not contain a root (i.e. if the path is empty)
     */
    public CfChange(Object affectedObject, String propertyName, List<String> path) {
        checkNotNull(affectedObject);
        checkNotNull(propertyName);
        checkNotNull(path);
        checkArgument(!path.isEmpty(), "The path does not contain a root");

        this.affectedObject = affectedObject;
        this.propertyName = propertyName;
        this.path = path;
    }

    public Object getAffectedObject() {
        return affectedObject;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public List<String> getPath() {
        return path;
    }

}
