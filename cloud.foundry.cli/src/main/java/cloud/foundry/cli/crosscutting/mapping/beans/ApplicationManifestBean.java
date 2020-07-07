package cloud.foundry.cli.crosscutting.mapping.beans;

import org.apache.commons.lang3.StringUtils;
import org.cloudfoundry.operations.applications.ApplicationHealthCheck;
import org.cloudfoundry.operations.applications.ApplicationManifest;
import org.cloudfoundry.operations.applications.Route;
import org.javers.core.metamodel.annotation.Value;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Bean holding all data of the manifest file from an application.
 */
@Value
public class ApplicationManifestBean implements Bean {

    // list of all attributes the manifest supports, except for path
    private String buildpack;
    private String command;
    private Integer disk;
    private String dockerImage;
    private String dockerUsername;
    private Map<String, Object> environmentVariables;
    private String healthCheckHttpEndpoint;
    private ApplicationHealthCheck healthCheckType;
    private Integer instances;
    private Integer memory;
    private Boolean noRoute;
    private Boolean randomRoute;
    private String routePath;
    private List<String> routes;
    private List<String> services;
    private String stack;
    private Integer timeout;


    /**
     * TODO
     * these attributes are deprecated and have been replaced by the attribute 'routes'
     * https://docs.cloudfoundry.org/devguide/deploy-apps/manifest-attributes.html#deprecated
     *
     * leaving them here for now, further clarification necessary
     */
    private List<String> domains;
    private List<String> hosts;
    private Boolean noHostname;

    public ApplicationManifestBean(ApplicationManifest manifest) {
        this.buildpack = manifest.getBuildpack();
        this.command = manifest.getCommand();
        this.disk = manifest.getDisk();
        this.dockerImage =  manifest.getDocker() == null ? null : manifest.getDocker().getImage();
        this.dockerUsername =  manifest.getDocker() == null ? null :  manifest.getDocker().getUsername();
        this.domains = manifest.getDomains();
        this.environmentVariables = manifest.getEnvironmentVariables() != null && manifest.getEnvironmentVariables().isEmpty() ? null : manifest.getEnvironmentVariables();
        this.healthCheckHttpEndpoint = manifest.getHealthCheckHttpEndpoint();
        this.healthCheckType = manifest.getHealthCheckType();
        this.hosts = manifest.getHosts();
        this.instances = manifest.getInstances();
        this.memory = manifest.getMemory();
        this.noHostname = manifest.getNoHostname();
        this.noRoute = manifest.getNoRoute();
        this.randomRoute = manifest.getRandomRoute();
        this.routePath = manifest.getRoutePath();
        this.routes = manifest.getRoutes() == null ? null : manifest.getRoutes()
                .stream()
                .map(Route::getRoute)
                .collect(Collectors.toList());
        this.services = manifest.getServices() != null && manifest.getServices().isEmpty() ? null : manifest.getServices();
        this.stack = manifest.getStack();
        this.timeout = manifest.getTimeout();
    }

    public ApplicationManifestBean() {
    }


    public String getBuildpack() {
        return buildpack;
    }

    public void setBuildpack(String buildpack) {
        this.buildpack = buildpack;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public Integer getDisk() {
        return disk;
    }

    public void setDisk(Integer disk) {
        this.disk = disk;
    }

    public String getDockerImage() {
        return dockerImage;
    }

    public void setDockerImage(String dockerImage) {
        this.dockerImage = dockerImage;
    }

    public String getDockerUsername() {
        return dockerUsername;
    }

    public void setDockerUsername(String dockerUsername) {
        this.dockerUsername = dockerUsername;
    }

    public List<String> getDomains() {
        return domains;
    }

    public void setDomains(List<String> domains) {
        this.domains = domains;
    }

    public Map<String, Object> getEnvironmentVariables() {
        return environmentVariables;
    }

    public void setEnvironmentVariables(Map<String, Object> environmentVariables) {
        this.environmentVariables = environmentVariables;
    }

    public String getHealthCheckHttpEndpoint() {
        return healthCheckHttpEndpoint;
    }

    public void setHealthCheckHttpEndpoint(String healthCheckHttpEndpoint) {
        this.healthCheckHttpEndpoint = healthCheckHttpEndpoint;
    }

    public ApplicationHealthCheck getHealthCheckType() {
        return healthCheckType;
    }

    public void setHealthCheckType(ApplicationHealthCheck healthCheckType) {
        this.healthCheckType = healthCheckType;
    }

    public List<String> getHosts() {
        return hosts;
    }

    public void setHosts(List<String> hosts) {
        this.hosts = hosts;
    }

    public Integer getInstances() {
        return instances;
    }

    public void setInstances(Integer instances) {
        this.instances = instances;
    }

    public Integer getMemory() {
        return memory;
    }

    public void setMemory(Integer memory) {
        this.memory = memory;
    }

    public Boolean getNoHostname() {
        return noHostname;
    }

    public void setNoHostname(Boolean noHostname) {
        this.noHostname = noHostname;
    }

    public Boolean getNoRoute() {
        return noRoute;
    }

    public void setNoRoute(Boolean noRoute) {
        this.noRoute = noRoute;
    }

    public Boolean getRandomRoute() {
        return randomRoute;
    }

    public void setRandomRoute(Boolean randomRoute) {
        this.randomRoute = randomRoute;
    }

    public String getRoutePath() {
        return routePath;
    }

    public void setRoutePath(String routePath) {
        this.routePath = routePath;
    }

    public List<String> getRoutes() {
        return routes;
    }

    public void setRoutes(List<String> routes) {
        this.routes = routes;
    }

    public List<String> getServices() {
        return services;
    }

    public void setServices(List<String> services) {
        this.services = services;
    }

    public String getStack() {
        return stack;
    }

    public void setStack(String stack) {
        this.stack = stack;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApplicationManifestBean that = (ApplicationManifestBean) o;
        return Objects.equals(buildpack, that.buildpack) &&
                Objects.equals(command, that.command) &&
                Objects.equals(disk, that.disk) &&
                Objects.equals(dockerImage, that.dockerImage) &&
                Objects.equals(dockerUsername, that.dockerUsername) &&
                Objects.equals(environmentVariables, that.environmentVariables) &&
                Objects.equals(healthCheckHttpEndpoint, that.healthCheckHttpEndpoint) &&
                healthCheckType == that.healthCheckType &&
                Objects.equals(instances, that.instances) &&
                Objects.equals(memory, that.memory) &&
                Objects.equals(noRoute, that.noRoute) &&
                Objects.equals(randomRoute, that.randomRoute) &&
                Objects.equals(routePath, that.routePath) &&
                Objects.equals(routes, that.routes) &&
                Objects.equals(services, that.services) &&
                Objects.equals(stack, that.stack) &&
                Objects.equals(timeout, that.timeout) &&
                Objects.equals(domains, that.domains) &&
                Objects.equals(hosts, that.hosts) &&
                Objects.equals(noHostname, that.noHostname);
    }

    @Override
    public int hashCode() {
        return Objects.hash(buildpack, command, disk, dockerImage, dockerUsername, environmentVariables, healthCheckHttpEndpoint, healthCheckType, instances, memory, noRoute, randomRoute, routePath, routes, services, stack, timeout, domains, hosts, noHostname);
    }

    @Override
    public String toString() {
        return "ApplicationManifestBean{" +
                "buildpack='" + buildpack + '\'' +
                ", command='" + command + '\'' +
                ", disk=" + disk +
                ", dockerImage='" + dockerImage + '\'' +
                ", dockerUsername='" + dockerUsername + '\'' +
                ", environmentVariables=" + StringUtils.join(environmentVariables) +
                ", healthCheckHttpEndpoint='" + healthCheckHttpEndpoint + '\'' +
                ", healthCheckType=" + healthCheckType +
                ", instances=" + instances +
                ", memory=" + memory +
                ", noRoute=" + noRoute +
                ", randomRoute=" + randomRoute +
                ", routePath='" + routePath + '\'' +
                ", routes=" + StringUtils.join(routes) +
                ", services=" + StringUtils.join(services) +
                ", stack='" + stack + '\'' +
                ", timeout=" + timeout +
                ", domains=" + StringUtils.join(domains) +
                ", hosts=" + StringUtils.join(hosts) +
                ", noHostname=" + noHostname +
                '}';
    }

}
