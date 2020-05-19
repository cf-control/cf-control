package cloud.foundry.cli.crosscutting.beans;

import java.util.List;
import java.util.Map;

/**
 * Bean holding all data of the current state of the live system.
 */
public class GetAllBean implements Bean {

    private String apiVersion;
    private Map<String, String> target;
    private Map<String, Object> spec;

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public Map<String, String> getTarget() {
        return target;
    }

    public void setTarget( Map<String, String> target) {
        this.target = target;
    }

    public  Map<String, Object> getSpec() {
        return spec;
    }

    public void setSpec( Map<String, Object> spec) {
        this.spec = spec;

    }
}