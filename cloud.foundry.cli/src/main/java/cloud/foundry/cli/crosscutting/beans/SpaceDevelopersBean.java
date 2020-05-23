package cloud.foundry.cli.crosscutting.beans;

import java.util.List;

/**
 * Bean holding all data about the space developers.
 */
public class SpaceDevelopersBean implements Bean {

    private List<String> spaceDevelopers;

    public SpaceDevelopersBean() {
    }

    public List<String> getSpaceDevelopers() {
        return spaceDevelopers;
    }

    public void setSpaceDevelopers(List<String> spaceDevelopers) {
        this.spaceDevelopers = spaceDevelopers;
    }

    /**
     * TODO doc
     */
    @Override
    public void visit(BeanVisitor visitor) {
        visitor.visit(this);
    }
}
