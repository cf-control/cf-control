package cloud.foundry.cli.crosscutting.beans;

public class ConfigBean implements Bean {

    private String apiVersion;
    private TargetBean target;
    private SpecBean spec;

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
    public void visit(BeanVisitor visitor) {

    }
}
