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
    private Map<String, Object> environmentVariables;
    private String healthCheckHttpEndpoint;
    private ApplicationHealthCheck healthCheckType;
    private Integer instances;
    private Integer memory;
    private Boolean noRoute;
    private Boolean randomRoute;
    private List<String> routes;
    private List<String> services;
    private String stack;
    private Integer timeout;

    public ApplicationManifestBean(ApplicationManifest manifest) {
        this.buildpack = manifest.getBuildpack();
        this.command = manifest.getCommand();
        this.disk = manifest.getDisk();
        this.environmentVariables = manifest.getEnvironmentVariables() != null
                && manifest.getEnvironmentVariables().isEmpty()
                ? null
                : manifest.getEnvironmentVariables();
        this.healthCheckHttpEndpoint = manifest.getHealthCheckHttpEndpoint();
        this.healthCheckType = manifest.getHealthCheckType();
        this.instances = manifest.getInstances();
        this.memory = manifest.getMemory();
        this.noRoute = manifest.getNoRoute();
        this.randomRoute = manifest.getRandomRoute();
        this.routes = manifest.getRoutes() == null ? null : manifest.getRoutes()
                .stream()
                .map(Route::getRoute)
                .collect(Collectors.toList());
        this.services = manifest.getServices() != null
                && manifest.getServices().isEmpty()
                ? null
                : manifest.getServices();
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

    /**
     * Since declaring this bean as a {@link Value} object for the jaVers parser, it is necessary to provide a
     * equals method that evaluates the equality of two app manifest beans correctly.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApplicationManifestBean otherBean = (ApplicationManifestBean) o;
        return Objects.equals(buildpack, otherBean.buildpack) &&
                Objects.equals(command, otherBean.command) &&
                Objects.equals(disk, otherBean.disk) &&
                Objects.equals(environmentVariables, otherBean.environmentVariables) &&
                Objects.equals(healthCheckHttpEndpoint, otherBean.healthCheckHttpEndpoint) &&
                healthCheckType == otherBean.healthCheckType &&
                Objects.equals(instances, otherBean.instances) &&
                Objects.equals(memory, otherBean.memory) &&
                Objects.equals(noRoute, otherBean.noRoute) &&
                Objects.equals(randomRoute, otherBean.randomRoute) &&
                Objects.equals(routes, otherBean.routes) &&
                Objects.equals(services, otherBean.services) &&
                Objects.equals(stack, otherBean.stack) &&
                Objects.equals(timeout, otherBean.timeout);
    }

    /**
     * Overriding the equals method implies overriding the hashcode method
     */
    @Override
    public int hashCode() {
        return Objects.hash(buildpack,
                command,
                disk,
                environmentVariables,
                healthCheckHttpEndpoint,
                healthCheckType,
                instances,
                memory,
                noRoute,
                randomRoute,
                routes,
                services,
                stack,
                timeout);
    }

    @Override
    public String toString() {
        return "ApplicationManifestBean{" +
                "buildpack='" + buildpack + '\'' +
                ", command='" + command + '\'' +
                ", disk=" + disk +
                ", environmentVariables=" + StringUtils.join(environmentVariables) +
                ", healthCheckHttpEndpoint='" + healthCheckHttpEndpoint + '\'' +
                ", healthCheckType=" + healthCheckType +
                ", instances=" + instances +
                ", memory=" + memory +
                ", noRoute=" + noRoute +
                ", randomRoute=" + randomRoute +
                ", routes=" + StringUtils.join(routes) +
                ", services=" + StringUtils.join(services) +
                ", stack='" + stack + '\'' +
                ", timeout=" + timeout +
                '}';
    }

}
