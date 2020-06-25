package ut.com.isroot.stash.plugin.mock;

/**
 * @author Sean Ford
 * @since 2017-09-04
 */
public class MutableYaccSettings extends MapBackedSettings {
    public MutableYaccSettings setRequireJiraIssue(Boolean value) {
        set("requireJiraIssue", value);
        return this;
    }

    public MutableYaccSettings setIgnoreJiraConnectionFailures(Boolean value) {
        set("ignoreJiraConnectionFailures", value);
        return this;
    }

    public MutableYaccSettings setIgnoreUnknownIssueProjectKeys(Boolean value) {
        set("ignoreUnknownIssueProjectKeys", value);
        return this;
    }

    public MutableYaccSettings setRequireMatchingAuthorEmail(Boolean value) {
        set("requireMatchingAuthorEmail", value);
        return this;
    }

    public MutableYaccSettings setRequireMatchingAuthorName(Boolean value) {
        set("requireMatchingAuthorName", value);
        return this;
    }

    public MutableYaccSettings setCommitterEmailRegex(String value) {
        set("committerEmailRegex", value);
        return this;
    }

    public MutableYaccSettings setCommitMessageRegex(String value) {
        set("commitMessageRegex", value);
        return this;
    }

    public MutableYaccSettings setExcludeByRegex(String value) {
        set("excludeByRegex", value);
        return this;
    }

    public MutableYaccSettings setExcludeBranchRegex(String value) {
        set("excludeBranchRegex", value);
        return this;
    }

    public MutableYaccSettings setExcludeMergeCommits(Boolean value) {
        set("excludeMergeCommits", value);
        return this;
    }

    public MutableYaccSettings setBranchNameRegex(String value) {
        set("branchNameRegex", value);
        return this;
    }

    public MutableYaccSettings setExcludeUsers(String value) {
        set("excludeUsers", value);
        return this;
    }

    public MutableYaccSettings setExcludeServiceUserCommits(Boolean value) {
        set("excludeServiceUserCommits", value);
        return this;
    }
}
