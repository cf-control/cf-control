package cloud.foundry.cli.crosscutting.mapping;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import cloud.foundry.cli.crosscutting.mapping.beans.TargetBean;
import cloud.foundry.cli.services.LoginCommandOptions;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link CfOperationsCreator}
 */
public class CfOperationsCreatorTest {

    private static final String SOME_API = "api.local.pcf.dev.io";
    private static final String SOME_SPACE = "SOME_SPACE";

    private static final String SOME_ORGANIZATION = "SOME_ORGANIZATION";
    private static final String SOME_CREDENTIALS = "SOME_CREDENTIALS ;)";
    private static final String SOME_USER_NAME = "SOME_USER_NAME";

    @Test
    public void createCfOperationsShouldFailIfAtLeastOneOptionIsMissingInTargetBeanAndLoginOptions() {
        // given
        LoginCommandOptions loginCommandOptions = new LoginCommandOptions();
        loginCommandOptions.setUserName(SOME_USER_NAME);
        loginCommandOptions.setPassword(SOME_CREDENTIALS);

        loginCommandOptions.setApiHost(SOME_API);
        loginCommandOptions.setSpace(SOME_SPACE);

        TargetBean targetBean = new TargetBean();
        targetBean.setEndpoint(SOME_API);
        targetBean.setSpace(SOME_SPACE);

        // then + when
        assertThrows(IllegalArgumentException.class, () -> CfOperationsCreator.createCfOperations(
                targetBean,
                loginCommandOptions));
    }

    @Test
    public void createCfOperationsShouldFailIfAtLeastOneLoginOptionIsMissingAndTargetBeanIsNull() {
        // given
        LoginCommandOptions loginCommandOptions = new LoginCommandOptions();
        loginCommandOptions.setUserName(SOME_USER_NAME);
        loginCommandOptions.setPassword(SOME_CREDENTIALS);

        // then + when
        assertThrows(IllegalArgumentException.class, () -> CfOperationsCreator.createCfOperations(
                null,
                loginCommandOptions));
    }

    @Test
    public void createCfOperationsShouldCreateValidInstanceIfAllLoginOptionsAreAvailable() {
        // given
        LoginCommandOptions loginCommandOptions = new LoginCommandOptions();
        loginCommandOptions.setUserName(SOME_USER_NAME);
        loginCommandOptions.setPassword(SOME_CREDENTIALS);

        loginCommandOptions.setApiHost(SOME_API);
        loginCommandOptions.setSpace(SOME_SPACE);
        loginCommandOptions.setOrganization(SOME_ORGANIZATION);

        // when
        DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(
                null,
                loginCommandOptions);

        // then
        assertThat(cfOperations, is(not(nullValue())));
        assertThat(cfOperations.getSpace(), is(SOME_SPACE));
        assertThat(cfOperations.getOrganization(), is(SOME_ORGANIZATION));
    }

    @Test
    public void createCfOperationsShouldCreateValidInstanceIfAllTargetBeanOptionsAreAvailable() {
        // given
        LoginCommandOptions loginCommandOptions = new LoginCommandOptions();
        loginCommandOptions.setUserName(SOME_USER_NAME);
        loginCommandOptions.setPassword(SOME_CREDENTIALS);

        TargetBean targetBean = new TargetBean();
        targetBean.setEndpoint(SOME_API);
        targetBean.setSpace(SOME_SPACE);
        targetBean.setOrg(SOME_ORGANIZATION);

        // when
        DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(
                targetBean,
                loginCommandOptions);

        // then
        assertThat(cfOperations, is(not(nullValue())));
        assertThat(cfOperations.getSpace(), is(SOME_SPACE));
        assertThat(cfOperations.getOrganization(), is(SOME_ORGANIZATION));
    }

    @Test
    public void createCfOperationsShouldCreateValidInstanceIfAllRelevantTargetBeanAndLoginOptionsAreAvailable() {
        // given
        LoginCommandOptions loginCommandOptions = new LoginCommandOptions();
        loginCommandOptions.setUserName(SOME_USER_NAME);
        loginCommandOptions.setPassword(SOME_CREDENTIALS);

        loginCommandOptions.setOrganization(SOME_ORGANIZATION);

        TargetBean targetBean = new TargetBean();
        targetBean.setEndpoint(SOME_API);
        targetBean.setSpace(SOME_SPACE);

        // when
        DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(
                targetBean,
                loginCommandOptions);

        // then
        assertThat(cfOperations, is(not(nullValue())));
        assertThat(cfOperations.getSpace(), is(SOME_SPACE));
        assertThat(cfOperations.getOrganization(), is(SOME_ORGANIZATION));
    }

}
