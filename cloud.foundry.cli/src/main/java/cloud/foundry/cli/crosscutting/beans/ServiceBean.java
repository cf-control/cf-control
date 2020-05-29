package cloud.foundry.cli.crosscutting.beans;

import org.cloudfoundry.operations.services.ServiceInstance;
import org.cloudfoundry.operations.services.ServiceInstanceType;

import java.util.List;

/**
 * Bean holding all data that is related to an instance of a service.
 */
public class ServiceBean implements Bean {

    private String service;
    private String lastOperation;
    private String plan;
    private List<String> tags;
    private ServiceInstanceType type;

    public ServiceBean(ServiceInstance serviceInstance) {
        this.service = serviceInstance.getService();
        this.lastOperation = serviceInstance.getLastOperation() == null ? null
                            : serviceInstance.getLastOperation() + " " + serviceInstance.getStatus();
        this.plan = serviceInstance.getPlan();
        this.tags = serviceInstance.getTags() == null || serviceInstance.getTags().isEmpty()
                ? null
                : serviceInstance.getTags();
        this.type = serviceInstance.getType();

    }

    public ServiceBean() {
    }

    public String getLastOperation() {
        return lastOperation;
    }

    public void setLastOperation(String lastOperation) {
        this.lastOperation = lastOperation;
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
        if (tags == null || tags.isEmpty()) {
            this.tags = null;
        }

        this.tags = tags;
    }

    public ServiceInstanceType getType() {
        return type;
    }

    public void setType(ServiceInstanceType type) {
        this.type = type;
    }
}