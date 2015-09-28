package it.com.isroot.stash.plugin;

import com.atlassian.bitbucket.async.AsyncTestUtils;
import com.atlassian.bitbucket.async.WaitCondition;
import com.atlassian.webdriver.bitbucket.BitbucketTestedProduct;
import com.atlassian.webdriver.bitbucket.page.BitbucketLoginPage;
import com.atlassian.webdriver.pageobjects.WebDriverTester;
import org.hamcrest.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sean Ford
 * @since 2015-09-21
 */
public class YaccTestUtils {
    private static final Logger log = LoggerFactory.getLogger(YaccTestUtils.class);

    /**
     * Reset YACC settings data so that tests can start at a clean slate.
     * This is sort of hackish... maybe the Atlassian integration test
     * API has a way to reset all the data?
     */
    public static void resetData(BitbucketTestedProduct stash) {
        log.info("resetting test generated data");

        YaccRepoSettingsPage settingsPage = stash.visit(BitbucketLoginPage.class)
                .loginAsSysAdmin(YaccRepoSettingsPage.class);

        settingsPage.clickEditYacc()
                .clearSettings();
        settingsPage.clickSubmit();
        settingsPage.clickDisable();

        YaccGlobalSettingsPage globalSettingsPage = stash.visit(YaccGlobalSettingsPage.class);
        globalSettingsPage.clearSettings();
        globalSettingsPage.clickSubmit();

        stash.getTester().getDriver().manage().deleteAllCookies();

        log.info("done resetting data");
    }

    public static void waitForStashToBoot(final WebDriverTester tester) {
        AsyncTestUtils.waitFor(new WaitCondition() {
            @Override
            public boolean test() throws Exception {
                tester.gotoUrl(System.getProperty("http.bitbucket.url") + "/status");
                boolean isRunning = tester.getDriver().getPageSource().contains("RUNNING");

                log.info("bitbucket is running: {}\n{}", isRunning, tester.getDriver().getPageSource());

                return isRunning;
            }

            @Override
            public void describeFailure(Description description) throws Exception {

            }
        }, 60000, 5000);
    }

}
