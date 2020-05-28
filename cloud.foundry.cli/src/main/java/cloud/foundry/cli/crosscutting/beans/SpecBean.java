package cloud.foundry.cli.crosscutting.beans;

import java.util.List;
import java.util.Map;

public class SpecBean {

    List<String> spaceDevelopers;
    Map<String, ServiceBean> services;
    Map<String, ApplicationBean> apps;

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
