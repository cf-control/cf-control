package cloud.foundry.cli.crosscutting.mapping.beans;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * Bean holding all data about the space developers.
 */
public class SpaceDevelopersBean implements Bean {

    private List<String> spaceDevelopers;

    public SpaceDevelopersBean() {
    }

    public SpaceDevelopersBean(List<String> spaceDevelopers) {
        this.spaceDevelopers = spaceDevelopers;
    }

    public List<String> getSpaceDevelopers() {
        return spaceDevelopers;
    }

    public void setSpaceDevelopers(List<String> spaceDevelopers) {
        this.spaceDevelopers = spaceDevelopers;
    }
  
    @Override
    public String toString() {
        return "SpaceDevelopersBean{" +
                "spaceDevelopers=" + StringUtils.join(spaceDevelopers) +
                '}';
    }
}
