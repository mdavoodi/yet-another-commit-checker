package it.com.isroot.stash.plugin.checks;

import com.atlassian.pageobjects.TestedProductFactory;
import com.atlassian.webdriver.bitbucket.BitbucketTestedProduct;
import com.atlassian.webdriver.bitbucket.page.BitbucketLoginPage;
import com.atlassian.webdriver.testing.rule.WebDriverScreenshotRule;
import com.isroot.stash.test.LogTestStartRule;
import it.com.isroot.stash.plugin.YaccBranchCreationPage;
import it.com.isroot.stash.plugin.YaccGlobalSettingsPage;
import it.com.isroot.stash.plugin.YaccRepoSettingsPage;
import it.com.isroot.stash.plugin.YaccTestUtils;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sean Ford
 * @since 2015-09-21
 */
public class BranchNameRegexTest {
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
    public void testGlobalSetting() {
        YaccGlobalSettingsPage globalSettings = STASH.visit(BitbucketLoginPage.class)
                .loginAsSysAdmin(YaccGlobalSettingsPage.class);

        globalSettings.setBranchNameRegex("bugfix/[A-Z]+-[0-9]+.*");
        globalSettings.clickSubmit();

        YaccBranchCreationPage branchCreate = STASH.visit(YaccBranchCreationPage.class);
        branchCreate.setBranchName("invalid-branch-name");

        branchCreate.createBranchWithError();
        assertThat(branchCreate.getError()).isEqualTo("Invalid branch name. " +
                "bugfix/invalid-branch-name does not match regex bugfix/[A-Z]+-[0-9]+.*");

        branchCreate.setBranchName("ABC-123-good-name-" + System.currentTimeMillis());
        branchCreate.createBranch("PROJECT_1", "rep_1");
    }

    @Test
    public void testRepoSetting() {
        YaccRepoSettingsPage repoSettingsPage = STASH.visit(BitbucketLoginPage.class)
                .loginAsSysAdmin(YaccRepoSettingsPage.class);

        repoSettingsPage.clickEditYacc();

        repoSettingsPage.setBranchNameRegex("bugfix/repo-[A-Z]+-[0-9]+.*");
        repoSettingsPage.clickSubmit();

        YaccBranchCreationPage branchCreate = STASH.visit(YaccBranchCreationPage.class);
        branchCreate.setBranchName("invalid-branch-name");

        branchCreate.createBranchWithError();
        assertThat(branchCreate.getError()).isEqualTo("Invalid branch name. " +
                "bugfix/invalid-branch-name does not match regex bugfix/repo-[A-Z]+-[0-9]+.*");

        branchCreate.setBranchName("repo-ABC-123-good-name-" + System.currentTimeMillis());
        branchCreate.createBranch("PROJECT_1", "rep_1");
    }
}
