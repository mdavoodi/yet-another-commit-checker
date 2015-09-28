package it.com.isroot.stash.plugin;

import com.atlassian.pageobjects.TestedProductFactory;
import com.atlassian.webdriver.bitbucket.BitbucketTestedProduct;
import com.atlassian.webdriver.bitbucket.page.BitbucketLoginPage;
import com.atlassian.webdriver.testing.rule.WebDriverScreenshotRule;
import com.isroot.stash.test.LogTestStartRule;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test to verify that both the YACC global and repo settings page are in sync and both working.
 *
 * @author Sean Ford
 * @since 2015-08-30
 */
public class SettingsTest {
    private static final BitbucketTestedProduct STASH = TestedProductFactory.create(BitbucketTestedProduct.class);

    @Rule
    public WebDriverScreenshotRule webDriverScreenshotRule = new WebDriverScreenshotRule();

    @Rule public LogTestStartRule logTestStartRule = new LogTestStartRule();


    @BeforeClass
    public static void setup() {
        YaccTestUtils.waitForStashToBoot(STASH.getTester());
        YaccTestUtils.resetData(STASH);
    }

    @After
    public void cleanup() {
        STASH.getTester().getDriver().manage().deleteAllCookies();
    }

    @Test
    public void testGlobalSettings() {
        YaccGlobalSettingsPage globalSettings = STASH.visit(BitbucketLoginPage.class)
                .loginAsSysAdmin(YaccGlobalSettingsPage.class);

        verifyDefaults(globalSettings);

        setInvalidValues(globalSettings);
        globalSettings.clickSubmit();
        verifyValidationErrors(globalSettings);

        setValues(globalSettings);
        globalSettings.clickSubmit();

        globalSettings = STASH.visit(YaccGlobalSettingsPage.class);

        verifyValues(globalSettings);
    }

    @Test
    public void testRepositorySettings() {
        YaccRepoSettingsPage repoSettingsPage = STASH.visit(BitbucketLoginPage.class)
                .loginAsSysAdmin(YaccRepoSettingsPage.class)
                .clickEditYacc();

        verifyDefaults(repoSettingsPage);

        setInvalidValues(repoSettingsPage);
        repoSettingsPage.clickSubmit();
        verifyValidationErrors(repoSettingsPage);

        setValues(repoSettingsPage);
        repoSettingsPage.clickSubmit();

        repoSettingsPage = STASH.visit(YaccRepoSettingsPage.class)
                .clickEditYacc();

        verifyValues(repoSettingsPage);
    }

    private void verifyDefaults(YaccSettingsCommon yaccSettingsCommon) {
        yaccSettingsCommon.verifyRequireMatchingAuthorName(false)
                .verifyRequireMatchingAuthorEmail(false)
                .verifyCommitMessageRegex("")
                .verifyErrorMessageHeader("")
                .verifyErrorMessageCommitterEmail("")
                .verifyErrorMessageCommitterName("")
                .verifyErrorMessageCommitRegex("")
                .verifyErrorMessageIssueJql("")
                .verifyErrorMessageBranchName("")
                .verifyErrorMessageFooter("")
                .verifyExcludeMergeCommits(false)
                .verifyExcludeByRegex("")
                .verifyExcludeMergeCommits(false);
    }

    private void setInvalidValues(YaccSettingsCommon yaccSettingsCommon) {
        yaccSettingsCommon.setCommitMessageRegex("(invalid regex")
                .setExcludeByRegex("(invalid regex");
    }

    private void verifyValidationErrors(YaccSettingsCommon yaccSettingsCommon) {
        assertThat(yaccSettingsCommon.getFieldIdsWithErrors())
                .containsOnly("commitMessageRegex", "excludeByRegex");
    }

    private void setValues(YaccSettingsCommon yaccSettingsCommon) {
        yaccSettingsCommon.clickRequireMatchingAuthorEmail()
                .clickRequireMatchingAuthorName()
                .setCommitMessageRegex(".*")
                .setErrorMessageHeader("header")
                .setErrorMessageCommitterEmail("email")
                .setErrorMessageCommitterName("name")
                .setErrorMessageCommitRegex("commit regex")
                .setErrorMessageIssueJql("issue jql")
                .setErrorMessageBranchName("branch name")
                .setErrorMessageFooter("footer")
                .clickExcludeMergeCommits()
                .setExcludeByRegex(".*")
                .clickExcludeServiceUserCommits();
    }

    private void verifyValues(YaccSettingsCommon yaccSettingsCommon) {
        yaccSettingsCommon.verifyRequireMatchingAuthorEmail(true)
                .verifyRequireMatchingAuthorName(true)
                .verifyCommitMessageRegex(".*")
                .verifyErrorMessageHeader("header")
                .verifyErrorMessageCommitterEmail("email")
                .verifyErrorMessageCommitterName("name")
                .verifyErrorMessageCommitRegex("commit regex")
                .verifyErrorMessageIssueJql("issue jql")
                .verifyErrorMessageBranchName("branch name")
                .verifyErrorMessageFooter("footer")
                .verifyExcludeMergeCommits(true)
                .verifyExcludeByRegex(".*")
                .verifyExcludeMergeCommits(true);
    }
}
