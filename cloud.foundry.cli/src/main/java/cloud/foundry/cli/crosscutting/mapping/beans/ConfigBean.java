package cloud.foundry.cli.crosscutting.mapping.beans;

import java.util.Map;

/**
 * Bean holding all data of the state of a live system.
 */
public class ConfigBean implements Bean {

    String apiVersion;
    TargetBean target;
    SpecBean spec;

    public ConfigBean() {

    }

    public ConfigBean(Map<String, ApplicationBean> apps) {
        spec = new SpecBean();
        spec.setApps(apps);
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public TargetBean getTarget() {
        return target;
    }

    public void setTarget(TargetBean target) {
        this.target = target;
    }

    public SpecBean getSpec() {
        return spec;
    }

    public void setSpec(SpecBean spec) {
        this.spec = spec;
    }

    @Override
    public String toString() {
        return "ConfigBean{" +
                "apiVersion='" + apiVersion + '\'' +
                ", target=" + String.valueOf(target) +
                ", spec=" + String.valueOf(spec) +
                '}';
    }
}
