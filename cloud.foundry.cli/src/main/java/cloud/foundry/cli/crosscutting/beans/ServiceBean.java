package cloud.foundry.cli.crosscutting.beans;

import org.cloudfoundry.operations.services.ServiceInstanceSummary;

import java.util.List;

/**
 * Bean holding all data that is related to an instance of a service.
 */
public class ServiceBean implements Bean {

    private String service;
    private String plan;
    private List<String> tags;

    public ServiceBean(ServiceInstanceSummary serviceInstanceSummary) {
        this.service = serviceInstanceSummary.getService();
        this.plan = serviceInstanceSummary.getPlan();
        this.tags = serviceInstanceSummary.getTags();
    }

    public ServiceBean() {
    }

    public String getPlan() {
        return plan;
    }

    public void setPlan(String plan) {
        this.plan = plan;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}
