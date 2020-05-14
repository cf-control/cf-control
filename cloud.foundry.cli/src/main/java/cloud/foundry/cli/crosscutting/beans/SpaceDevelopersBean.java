package cloud.foundry.cli.crosscutting.beans;

import java.util.List;

public class SpaceDevelopersBean implements Bean {

    private List<String> spaceDevelopers;

    public List<String> getSpaceDevelopers() {
        return spaceDevelopers;
    }

    public void setSpaceDevelopers(List<String> spaceDevelopers) {
        this.spaceDevelopers = spaceDevelopers;
    }

    public SpaceDevelopersBean() {

    }
}
