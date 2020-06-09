package cloud.foundry.cli.crosscutting.mapping.beans;

/**
 * Bean holding all data of the state of a live system.
 */
public class ConfigBean {

    String apiVersion;
    TargetBean target;
    SpecBean spec;

    public ConfigBean() {

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
