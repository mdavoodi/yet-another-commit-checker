package it.com.isroot.stash.plugin;

import com.atlassian.pageobjects.TestedProductFactory;
import com.atlassian.webdriver.bitbucket.BitbucketTestedProduct;
import com.atlassian.webdriver.bitbucket.page.BitbucketLoginPage;
import com.atlassian.webdriver.testing.rule.WebDriverScreenshotRule;
import it.com.isroot.stash.plugin.pageobjects.YaccGlobalSettingsPage;
import it.com.isroot.stash.plugin.pageobjects.YaccRepoSettingsPage;
import it.com.isroot.stash.plugin.pageobjects.YaccSettingsCommon;
import it.com.isroot.stash.plugin.util.YaccTestUtils;
import org.junit.After;
import org.junit.AfterClass;
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


    @BeforeClass
    public static void setup() {
        YaccTestUtils.waitForStashToBoot(STASH.getTester());
        YaccTestUtils.resetData(STASH);
    }

    @AfterClass
    public static void resetSettings() {
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
        verifyDefaultGlobalSettings(globalSettings);

        setInvalidValues(globalSettings);
        setInvalidGlobalValues(globalSettings);
        globalSettings.clickSubmit();
        verifyGlobalValidationErrors(globalSettings);

        setValues(globalSettings);
        setGlobalValues(globalSettings);
        globalSettings.clickSubmit();

        globalSettings = STASH.visit(YaccGlobalSettingsPage.class);

        verifyValues(globalSettings);
        verifyGlobalValues(globalSettings);
    }

    @Test
    public void testRepositorySettings() {
        YaccRepoSettingsPage repoSettingsPage = STASH.visit(BitbucketLoginPage.class)
                .loginAsSysAdmin(YaccRepoSettingsPage.class)
                .clickEnableYacc();

        verifyDefaults(repoSettingsPage);

        setInvalidValues(repoSettingsPage);
        repoSettingsPage.clickSubmit();
        verifyValidationErrors(repoSettingsPage);

        setValues(repoSettingsPage);
        repoSettingsPage.clickSubmit();

        repoSettingsPage = STASH.visit(YaccRepoSettingsPage.class)
                .clickEnableYacc();

        verifyValues(repoSettingsPage);
    }

    private void verifyDefaultGlobalSettings(YaccGlobalSettingsPage yaccGlobalSettingsPage) {
        yaccGlobalSettingsPage.verifyOverrideJiraUserEnabled(false)
            .verifyOverrideJiraUserText("");
    }

    private void verifyDefaults(YaccSettingsCommon yaccSettingsCommon) {
        yaccSettingsCommon.verifyRequireMatchingAuthorName(false)
                .verifyRequireMatchingAuthorEmail(false)
                .verifyCommitterEmailRegex("")
                .verifyCommitMessageRegex("")
                .verifyErrorMessageHeader("")
                .verifyErrorMessageCommitterEmail("")
                .verifyErrorMessageCommitterName("")
                .verifyErrorMessageCommitRegex("")
                .verifyErrorMessageIssueJql("")
                .verifyErrorMessageNoJiraIssue("")
                .verifyErrorMessageBranchName("")
                .verifyErrorMessageFooter("")
                .verifyExcludeMergeCommits(false)
                .verifyExcludeByRegex("")
                .verifyExcludeBranchRegex("")
                .verifyExcludeMergeCommits(false);
    }

    private void setInvalidGlobalValues(YaccGlobalSettingsPage yaccGlobalSettingsPage) {
        yaccGlobalSettingsPage.setOverrideJiraUserText("nonexistantuser");
    }

    private void setInvalidValues(YaccSettingsCommon yaccSettingsCommon) {
        yaccSettingsCommon.setCommitMessageRegex("(invalid regex")
                .setExcludeByRegex("(invalid regex")
                .setExcludeBranchRegex("(invalid regex")
                .setCommitterEmailRegex("(invalid regex");
    }

    private void verifyValidationErrors(YaccSettingsCommon yaccSettingsCommon) {
        assertThat(yaccSettingsCommon.getFieldIdsWithErrors())
                .containsOnly("commitMessageRegex", "excludeByRegex", "excludeBranchRegex", "committerEmailRegex");
    }

    private void verifyGlobalValidationErrors(YaccSettingsCommon yaccSettingsCommon) {
        assertThat(yaccSettingsCommon.getFieldIdsWithErrors())
                .containsOnly("overrideJiraUser", "commitMessageRegex", "excludeByRegex", "excludeBranchRegex", "committerEmailRegex");
    }

    private void setValues(YaccSettingsCommon yaccSettingsCommon) {
        yaccSettingsCommon.clickRequireMatchingAuthorEmail()
                .clickRequireMatchingAuthorName()
                .setCommitterEmailRegex(".*")
                .setCommitMessageRegex(".*")
                .setErrorMessageHeader("header")
                .setErrorMessageCommitterEmail("email")
                .setErrorMessageCommitterEmailRegex("committer email regex")
                .setErrorMessageCommitterName("name")
                .setErrorMessageCommitRegex("commit regex")
                .setErrorMessageIssueJql("issue jql")
                .setErrorMessageNoJiraIssue("no jira issue")
                .setErrorMessageBranchName("branch name")
                .setErrorMessageFooter("footer")
                .clickExcludeMergeCommits()
                .setExcludeByRegex(".*")
                .setExcludeBranchRegex(".*")
                .clickExcludeServiceUserCommits();
    }

    private void setGlobalValues(YaccGlobalSettingsPage yaccGlobalSettingsPage) {
        yaccGlobalSettingsPage.clickOverrideJiraUserEnabled()
            .setOverrideJiraUserText("admin");
    }

    private void verifyValues(YaccSettingsCommon yaccSettingsCommon) {
        yaccSettingsCommon.verifyRequireMatchingAuthorEmail(true)
                .verifyRequireMatchingAuthorName(true)
                .verifyCommitterEmailRegex(".*")
                .verifyCommitMessageRegex(".*")
                .verifyErrorMessageHeader("header")
                .verifyErrorMessageCommitterEmail("email")
                .verifyErrorMessageCommitterEmailRegex("committer email regex")
                .verifyErrorMessageCommitterName("name")
                .verifyErrorMessageCommitRegex("commit regex")
                .verifyErrorMessageIssueJql("issue jql")
                .verifyErrorMessageNoJiraIssue("no jira issue")
                .verifyErrorMessageBranchName("branch name")
                .verifyErrorMessageFooter("footer")
                .verifyExcludeMergeCommits(true)
                .verifyExcludeByRegex(".*")
                .verifyExcludeBranchRegex(".*")
                .verifyExcludeMergeCommits(true);
    }

    private void verifyGlobalValues(YaccGlobalSettingsPage yaccGlobalSettingsPage) {
        yaccGlobalSettingsPage.verifyOverrideJiraUserEnabled(true)
            .verifyOverrideJiraUserText("admin");
    }
}
