package cloud.foundry.cli.crosscutting.beans;

public class TargetBean implements Bean {

    private String endpoint;
    private String org;
    private String space;

    public TargetBean() {}

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getOrg() {
        return org;
    }

    public void setOrg(String org) {
        this.org = org;
    }

    public String getSpace() {
        return space;
    }

    public void setSpace(String space) {
        this.space = space;
    }

    @Override
    public void visit(BeanVisitor visitor) {

    }
}
