package cloud.foundry.cli.crosscutting.beans;

import org.cloudfoundry.operations.applications.ApplicationHealthCheck;
import org.cloudfoundry.operations.applications.ApplicationManifest;
import org.cloudfoundry.operations.applications.Docker;
import org.cloudfoundry.operations.applications.Route;

import java.util.List;
import java.util.Map;

/**
 * Immutable data type used to generate nice YAML output for applications.
 */
public class ApplicationManifestBean {

    // list of all attributes the manifest supports, except for path
    private String buildpack;
    private String command;
    private Integer disk;
    private Docker docker;
    private List<String> domains;
    private Map<String, Object> environmentVariables;
    private String healthCheckHttpEndpoint;
    private ApplicationHealthCheck healthCheckType;
    private List<String> hosts;
    private Integer instances;
    private Integer memory;
    private String name;
    private Boolean noHostname;
    private Boolean noRoute;
    private Boolean randomRoute;
    private String routePath;
    private List<Route> routes;
    private List<String> services;
    private String stack;
    private Integer timeout;

    public ApplicationManifestBean(ApplicationManifest manifest) {
        this.buildpack = manifest.getBuildpack();
        this.command = manifest.getCommand();
        this.disk = manifest.getDisk();
        this.docker = manifest.getDocker();
        this.domains = manifest.getDomains();
        this.environmentVariables = manifest.getEnvironmentVariables();
        this.healthCheckHttpEndpoint = manifest.getHealthCheckHttpEndpoint();
        this.healthCheckType = manifest.getHealthCheckType();
        this.hosts = manifest.getHosts();
        this.instances = manifest.getInstances();
        this.memory = manifest.getMemory();
        this.name = manifest.getName();
        this.noHostname = manifest.getNoHostname();
        this.noRoute = manifest.getNoRoute();
        this.randomRoute = manifest.getRandomRoute();
        this.routePath = manifest.getRoutePath();
        this.routes = manifest.getRoutes();
        this.services = manifest.getServices();
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

    public Docker getDocker() {
        return docker;
    }

    public void setDocker(Docker docker) {
        this.docker = docker;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public List<Route> getRoutes() {
        return routes;
    }

    public void setRoutes(List<Route> routes) {
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

}