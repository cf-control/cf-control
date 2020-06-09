package cloud.foundry.cli.crosscutting.mapping.beans;

import java.util.List;
import java.util.Map;

/**
 * Bean holding all data that is related to cloud foundry specification.
 */
public class SpecBean implements Bean {

    private List<String> spaceDevelopers;
    private Map<String, ServiceBean> services;
    private Map<String, ApplicationBean> apps;

    public SpecBean() {

    }

    public List<String> getSpaceDevelopers() {
        return spaceDevelopers;
    }

    public void setSpaceDevelopers(List<String> spaceDevelopers) {
        this.spaceDevelopers = spaceDevelopers;
    }

    public Map<String, ServiceBean> getServices() {
        return services;
    }

    public void setServices(Map<String, ServiceBean> services) {
        this.services = services;
    }

    public Map<String, ApplicationBean> getApps() {
        return apps;
    }

    public void setApps(Map<String, ApplicationBean> apps) {
        this.apps = apps;
    }
}
