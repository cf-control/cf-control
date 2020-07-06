package cloud.foundry.cli.crosscutting.mapping.beans;

import org.apache.commons.lang3.StringUtils;
import org.cloudfoundry.operations.services.ServiceInstance;

import java.util.List;
import java.util.Map;

/**
 * Bean holding all data that is related to an instance of a service.
 */
public class ServiceBean implements Bean {

    private String service;
    private String plan;
    private List<String> tags;
    private Map<String, Object> params;
    

    public ServiceBean(ServiceInstance serviceInstance) {
        this.service = serviceInstance.getService();
        this.plan = serviceInstance.getPlan();
        this.tags = serviceInstance.getTags();
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

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }
    
    @Override
    public String toString() {
        return "ServiceBean{" +
                "service='" + service + '\'' +
                ", plan='" + plan + '\'' +
                ", tags=" + tags + '\'' +
                ", params=" + StringUtils.join(params) +
                '}';
    }
}
