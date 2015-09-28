package it.com.isroot.stash.plugin;

import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.webdriver.bitbucket.page.BitbucketPage;
import com.atlassian.webdriver.bitbucket.util.ElementUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sean Ford
 * @since 2015-09-13
 */
abstract class YaccSettingsCommon extends BitbucketPage {
    private static final Logger log = LoggerFactory.getLogger(YaccSettingsCommon.class);

    @ElementBy(className = "prevent-double-submit")
    private PageElement form;

    @ElementBy(id = "requireMatchingAuthorEmail")
    private PageElement requireMatchingAuthorEmailCheckbox;

    @ElementBy(id = "requireMatchingAuthorName")
    private PageElement requireMatchingAuthorNameCheckbox;

    @ElementBy(id = "commitMessageRegex")
    private PageElement commitMessageRegex;

    @ElementBy(id = "branchNameRegex")
    private PageElement branchNameRegex;

    @ElementBy(id = "errorMessageHeader")
    private PageElement errorMessageHeader;

    @ElementBy(id = "errorMessage.COMMITTER_EMAIL")
    private PageElement errorMessageCommitterEmail;

    @ElementBy(id = "errorMessage.COMMITTER_NAME")
    private PageElement errorMessageCommitterName;

    @ElementBy(id = "errorMessage.COMMIT_REGEX")
    private PageElement errorMessageCommitRegex;

    @ElementBy(id = "errorMessage.ISSUE_JQL")
    private PageElement errorMessageIssueJql;

    @ElementBy(id = "errorMessage.BRANCH_NAME")
    private PageElement errorMessageBranchName;

    @ElementBy(id = "errorMessageFooter")
    private PageElement errorMessageFooter;

    @ElementBy(id = "excludeMergeCommits")
    private PageElement excludeMergeCommits;

    @ElementBy(id = "excludeByRegex")
    private PageElement excludeByRegex;

    @ElementBy(id = "excludeServiceUserCommits")
    private PageElement excludeServiceUserCommits;

    public YaccSettingsCommon clickRequireMatchingAuthorEmail() {
        requireMatchingAuthorEmailCheckbox.click();
        return this;
    }

    public YaccSettingsCommon verifyRequireMatchingAuthorEmail(boolean isSelected) {
        assertThat(requireMatchingAuthorEmailCheckbox.isSelected()).isEqualTo(isSelected);
        return this;
    }

    public YaccSettingsCommon clickRequireMatchingAuthorName() {
        requireMatchingAuthorNameCheckbox.click();
        return this;
    }

    public YaccSettingsCommon verifyRequireMatchingAuthorName(boolean isSelected) {
        assertThat(requireMatchingAuthorNameCheckbox.isSelected()).isEqualTo(isSelected);
        return this;
    }

    public YaccSettingsCommon setCommitMessageRegex(String regex) {
        commitMessageRegex.clear();
        commitMessageRegex.type(regex);
        return this;
    }

    public YaccSettingsCommon verifyCommitMessageRegex(String regex) {
        assertThat(commitMessageRegex.getValue()).isEqualTo(regex);
        return this;
    }

    public YaccSettingsCommon setBranchNameRegex(String regex) {
        branchNameRegex.clear();
        branchNameRegex.type(regex);
        return this;
    }

    public YaccSettingsCommon verifyBranchNameRegex(String regex) {
        assertThat(branchNameRegex.getValue())
                .isEqualTo(regex);
        return this;
    }

    public YaccSettingsCommon setErrorMessageHeader(String value) {
        errorMessageHeader.clear();
        errorMessageHeader.type(value);
        return this;
    }

    public YaccSettingsCommon verifyErrorMessageHeader(String value) {
        assertThat(errorMessageHeader.getValue()).isEqualTo(value);
        return this;
    }

    public YaccSettingsCommon setErrorMessageCommitterEmail(String value) {
        errorMessageCommitterEmail.clear();
        errorMessageCommitterEmail.type(value);
        return this;
    }

    public YaccSettingsCommon verifyErrorMessageCommitterEmail(String value) {
        assertThat(errorMessageCommitterEmail.getValue()).isEqualTo(value);
        return this;
    }

    public YaccSettingsCommon setErrorMessageCommitterName(String value) {
        errorMessageCommitterName.clear();
        errorMessageCommitterName.type(value);
        return this;
    }

    public YaccSettingsCommon verifyErrorMessageCommitterName(String value) {
        assertThat(errorMessageCommitterName.getValue()).isEqualTo(value);
        return this;
    }

    public YaccSettingsCommon setErrorMessageCommitRegex(String value) {
        errorMessageCommitRegex.clear();
        errorMessageCommitRegex.type(value);
        return this;
    }

    public YaccSettingsCommon verifyErrorMessageCommitRegex(String value) {
        assertThat(errorMessageCommitRegex.getValue()).isEqualTo(value);
        return this;
    }

    public YaccSettingsCommon setErrorMessageIssueJql(String value) {
        errorMessageIssueJql.clear();
        errorMessageIssueJql.type(value);
        return this;
    }

    public YaccSettingsCommon verifyErrorMessageIssueJql(String value) {
        assertThat(errorMessageIssueJql.getValue()).isEqualTo(value);
        return this;
    }

    public YaccSettingsCommon setErrorMessageBranchName(String value) {
        errorMessageBranchName.clear();
        errorMessageBranchName.type(value);
        return this;
    }

    public YaccSettingsCommon verifyErrorMessageBranchName(String value) {
        assertThat(errorMessageBranchName.getValue()).isEqualTo(value);
        return this;
    }

    public YaccSettingsCommon setErrorMessageFooter(String value) {
        errorMessageFooter.clear();
        errorMessageFooter.type(value);
        return this;
    }

    public YaccSettingsCommon verifyErrorMessageFooter(String value) {
        assertThat(errorMessageFooter.getValue()).isEqualTo(value);
        return this;
    }

    public YaccSettingsCommon clickExcludeServiceUserCommits() {
        excludeServiceUserCommits.click();
        return this;
    }

    public YaccSettingsCommon verifyExcludeServiceUserCommits(boolean isSelected) {
        assertThat(excludeServiceUserCommits.isSelected()).isEqualTo(isSelected);
        return this;
    }

    public YaccSettingsCommon clickExcludeMergeCommits() {
        excludeMergeCommits.click();
        return this;
    }

    public YaccSettingsCommon verifyExcludeMergeCommits(boolean isSelected) {
        assertThat(excludeMergeCommits.isSelected()).isEqualTo(isSelected);
        return this;
    }

    public YaccSettingsCommon setExcludeByRegex(String value) {
        excludeByRegex.clear();
        excludeByRegex.type(value);
        return this;
    }

    public YaccSettingsCommon verifyExcludeByRegex(String value) {
        assertThat(excludeByRegex.getValue()).isEqualTo(value);
        return this;
    }

    public Iterable<String> getFieldIdsWithErrors() {
        return ElementUtils.getFieldsWithErrors(form);
    }

    public YaccSettingsCommon clearSettings() {
        log.info("clearing settings");

        Poller.waitUntilTrue(excludeMergeCommits.timed().isVisible());

        if(excludeMergeCommits.isSelected()) {
            excludeMergeCommits.click();
        }

        if(excludeServiceUserCommits.isSelected()) {
            excludeServiceUserCommits.click();
        }

        if(requireMatchingAuthorEmailCheckbox.isSelected()) {
            requireMatchingAuthorEmailCheckbox.click();
        }

        if(requireMatchingAuthorNameCheckbox.isSelected()) {
            requireMatchingAuthorNameCheckbox.click();
        }

        setBranchNameRegex("");
        setExcludeByRegex("");
        setCommitMessageRegex("");
        setErrorMessageCommitterEmail("");
        setErrorMessageBranchName("");
        setErrorMessageCommitRegex("");
        setErrorMessageCommitterName("");
        setErrorMessageFooter("");
        setErrorMessageHeader("");
        setErrorMessageIssueJql("");

        return this;
    }

    /**
     * Sleep to wait for the page load. There have been some intermittent test failures related to
     * validation errors where I think we need to wait a bit after submit. Sleep's are hackish but
     * easy to do at the moment.
     */
    void waitABitForPageLoad() {
        try {
            Thread.sleep(5000);
        }
        catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
