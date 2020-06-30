package cloud.foundry.cli.logic;

import static com.google.common.base.Preconditions.checkNotNull;

import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationBean;
import cloud.foundry.cli.crosscutting.mapping.beans.ConfigBean;
import cloud.foundry.cli.crosscutting.mapping.beans.ServiceBean;
import cloud.foundry.cli.crosscutting.mapping.beans.SpecBean;
import cloud.foundry.cli.crosscutting.exceptions.ApplyException;
import cloud.foundry.cli.logic.apply.ApplicationRequestsPlaner;
import cloud.foundry.cli.logic.apply.ServiceRequestsPlaner;
import cloud.foundry.cli.logic.diff.DiffResult;
import cloud.foundry.cli.logic.diff.change.CfChange;
import cloud.foundry.cli.operations.ApplicationsOperations;
import cloud.foundry.cli.operations.ServicesOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import reactor.core.publisher.Flux;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;

/**
 * This class takes care of applying desired cloud foundry configurations to a live system.
 */
public class ApplyLogic {

    private static final Log log = Log.getLog(ApplyLogic.class);

    private DefaultCloudFoundryOperations cfOperations;

    private ServicesOperations servicesOperations;

    private DiffLogic diffLogic = new DiffLogic();

    /**
     * Creates a new instance that will use the provided cf operations internally.
     * @param cfOperations the cf operations that should be used to communicate with the cf instance
     * @throws NullPointerException if the argument is null
     */
    public ApplyLogic(@Nonnull DefaultCloudFoundryOperations cfOperations) {
        checkNotNull(cfOperations);

        this.cfOperations = cfOperations;
    }

    /**
     * Creates a new instance that will use the provided cf operations internally.
     * @param servicesOperations the operations object that should be used to communicate with the cf instance
     * @throws NullPointerException if the argument is null
     */
    public ApplyLogic(@Nonnull ServicesOperations servicesOperations) {
        this.servicesOperations = servicesOperations;
    }


    //TODO update the documentation as soon as the method does more than just creating applications
    /**
     * Creates all provided applications that are not present in the live system.
     * @param desiredApplications the applications that should all be present in the live system after the procedure
     * @throws ApplyException if an error occurs during the procedure
     * @throws NullPointerException if the argument is null
     */
    public void applyApplications(@Nonnull Map<String, ApplicationBean> desiredApplications) {
        checkNotNull(desiredApplications);

        ApplicationsOperations applicationsOperations = new ApplicationsOperations(cfOperations);
        log.info("Fetching information about applications...");
        Map<String, ApplicationBean> liveApplications = applicationsOperations.getAll().block();
        log.info("Information fetched.");

        // that way only the applications of the live system are compared in the diff
        ConfigBean desiredApplicationsConfig = createConfigFromApplications(desiredApplications);
        ConfigBean liveApplicationsConfig = createConfigFromApplications(liveApplications);

        // compare entire configs as the diff wrapper is only suited for diff trees of these
        log.info("Comparing the applications...");
        DiffResult diffResult = this.diffLogic.createDiffResult(liveApplicationsConfig, desiredApplicationsConfig);
        log.info("Applications compared.");

        Map<String, List<CfChange>> allApplicationChanges = diffResult.getApplicationChanges();

        Flux<Void> applicationRequests = Flux.fromIterable(allApplicationChanges.entrySet())
                .flatMap( appChangeEntry -> ApplicationRequestsPlaner.create(applicationsOperations,
                        appChangeEntry.getKey(),
                        appChangeEntry.getValue()))
                .onErrorContinue(log::error);
        applicationRequests.blockLast();

        log.info("Applying changes to applications...");
    }

    //TODO update the documentation as soon as the method does more than just creating/removing services
    /**
     * Creates/removes all provided services.
     * @param desiredServices the applications that should all be present in the live system after the procedure
     * @throws ApplyException if an error occurs during the procedure
     * @throws NullPointerException if the argument is null
     */
    public void applyServices(@Nonnull Map<String, ServiceBean> desiredServices) {
        checkNotNull(desiredServices);

        ServicesOperations servicesOperations = new ServicesOperations(cfOperations);
        GetLogic getLogic = new GetLogic();
        log.info("Fetching information about services...");
        Map<String, ServiceBean> liveServices = getLogic.getServices(servicesOperations);
        log.info("Information fetched.");

        // that way only the applications of the live system are compared in the diff
        ConfigBean desiredServicesConfig = createConfigFromServices(desiredServices);
        ConfigBean liveServicesConfig = createConfigFromServices(liveServices);

        // compare entire configs as the diff wrapper is only suited for diff trees of these
        log.info("Comparing the services...");
        DiffResult diffResult = this.diffLogic.createDiffResult(liveServicesConfig, desiredServicesConfig);
        log.info("Services compared.");

        Map<String, List<CfChange>> allServicesChanges = diffResult.getServiceChanges();

        Flux<Void> serviceRequests = Flux.fromIterable(allServicesChanges.entrySet())
                .flatMap( serviceChangeEntry ->
                        ServiceRequestsPlaner.create(servicesOperations,
                                serviceChangeEntry.getKey(),
                                serviceChangeEntry.getValue()))
                .onErrorContinue(log::error);
        serviceRequests.blockLast();

        log.info("Applying changes to services...");
    }


    /**
     * @param applicationBeans the application beans that should be contained in the resulting config bean
     * @return a config bean only containing the entered application beans
     */
    private ConfigBean createConfigFromApplications(Map<String, ApplicationBean> applicationBeans) {
        SpecBean applicationsSpecBean = new SpecBean();
        applicationsSpecBean.setApps(applicationBeans);
        ConfigBean applicationsConfigBean = new ConfigBean();
        applicationsConfigBean.setSpec(applicationsSpecBean);
        return applicationsConfigBean;
    }

    /**
     * @param serviceBeans the service beans that should be contained in the resulting config bean
     * @return a config bean only containing the entered application beans
     */
    private ConfigBean createConfigFromServices(Map<String, ServiceBean> serviceBeans) {
        SpecBean servicesSpecBean = new SpecBean();
        servicesSpecBean.setServices(serviceBeans);
        ConfigBean servicesConfigBean = new ConfigBean();
        servicesConfigBean.setSpec(servicesSpecBean);
        return servicesConfigBean;
    }

    public void setDiffLogic(DiffLogic diffLogic) {
        this.diffLogic = diffLogic;
    }
}
