package cloud.foundry.cli.operations;

import static com.google.common.base.Preconditions.checkNotNull;

import cloud.foundry.cli.crosscutting.beans.ApplicationBean;
import cloud.foundry.cli.crosscutting.beans.ApplicationManifestBean;
import cloud.foundry.cli.crosscutting.exceptions.CreationException;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.applications.*;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


/**
 * Handles the operations for manipulating applications on a cloud foundry instance.
 */
public class ApplicationOperations extends AbstractOperations<DefaultCloudFoundryOperations> {

    public ApplicationOperations(DefaultCloudFoundryOperations cloudFoundryOperations) {
        super(cloudFoundryOperations);
    }

    public List<ApplicationBean> getAll() {
        List<ApplicationSummary> applications = this.cloudFoundryOperations
                .applications()
                .list()
                .collectList()
                .block();

        // create a list of special bean data objects, as the summaries cannot be serialized directly
        List<ApplicationBean> beans = new ArrayList<>();
        for (ApplicationSummary summary : applications) {
            ApplicationManifest manifest = getApplicationManifest(summary);

            beans.add(new ApplicationBean(manifest));
        }

        return beans;
    }

    private ApplicationManifest getApplicationManifest(ApplicationSummary applicationSummary) {
        return this.cloudFoundryOperations
                .applications()
                .getApplicationManifest(GetApplicationManifestRequest
                        .builder()
                        .name(applicationSummary.getName())
                        .build())
                .block();
    }

    /**
     * TODO: Clarification with project owner necessary:
     * TODO: how to proceed when push fails to apply some settings?
     * TODO: remove on fail ?
     * TODO: keep on fail and only print errors as warnings ?
     *
     * for now keep on fail with error messages
     */
    /**
     *
     *  Pushes the app to the cloud foundry instance specified within the cloud foundry operations instance
     *
     * @param name  name of the app
     * @param bean  application bean
     * @param noStart   if the app should not start after being created
     * @throws CreationException
     */
    public void create(String name, ApplicationBean bean, boolean noStart) throws CreationException {
        checkNotNull(name);
        checkNotNull(bean);
        checkNotEmpty(name);
        // this check is important, otherwise an app could get overwritten
        checkAppNotExists(name);

        doCreate(name, bean, noStart);
    }

    private void doCreate(String name, ApplicationBean bean, boolean noStart) throws CreationException {
        try{
            //the app manifest might not have been given, for example user didn't specify anything in the yaml file
            if(bean.getManifest() == null){
                pushApp(name, bean, noStart);
            }else {
                //path is not injected from yaml processor so setting it manually
                bean.getManifest().setPath(bean.getPath());

                //override any name set in bean so that there are no conflicts
                bean.getManifest().setName(name);

                pushAppManifest(bean.getManifest(), noStart);
            }

        }catch (Exception e) {
            throw new CreationException("FAILED: " + e.getMessage());
        }
    }

    private void pushApp(String name, ApplicationBean bean, boolean noStart) {
        List<Throwable> errors = new LinkedList<>();

        this.cloudFoundryOperations
                .applications()
                .push(PushApplicationRequest
                        .builder()
                        .name(name)
                        .path(Paths.get(bean.getPath()))
                        .noStart(noStart)
                        .build())
                .onErrorContinue((throwable, o) -> errors.add(throwable))
                .block();

        //TODO: temporary error printing, will be replaced at a future date
        errors.forEach(throwable -> System.out.println(throwable.getMessage()));
    }


    private void pushAppManifest(ApplicationManifestBean bean, boolean noStart) {
        List<Throwable> errors = new LinkedList<>();

        this.cloudFoundryOperations
                .applications()
                .pushManifest(PushApplicationManifestRequest
                        .builder()
                        .manifest(bean.asApplicationManifest())
                        .noStart(noStart)
                        .build())
                .onErrorContinue((throwable, o) -> errors.add(throwable))
                .block();

        //TODO: temporary error printing, will be replaced at a future date
        errors.forEach(throwable -> System.out.println(throwable.getMessage()));
    }

    /**
     * assertion method
     */
    private void checkAppExists(String name) {
        // if app does not exists an IllegalArgumentException will be thrown
        this.cloudFoundryOperations
                .applications()
                .get(GetApplicationRequest
                        .builder()
                        .name(name)
                        .build())
                .block();
    }

    /**
     * assertion method
     */
    private void checkAppNotExists(String name) throws CreationException {

        // if an app does not exist it will throw an IllegalArgumentException so return without fail
        try{
            checkAppExists(name);
        } catch ( IllegalArgumentException e) {
            return;
        }

        // if an app does exist it doesn't throw an error, so throw an error
        throw new CreationException("FAILED: app exists already");

    }

    /**
     * assertion method
     */
    private void checkNotEmpty(String value) {
        if(value.isEmpty()){
            throw new IllegalArgumentException("empty string");
        }
    }

}
