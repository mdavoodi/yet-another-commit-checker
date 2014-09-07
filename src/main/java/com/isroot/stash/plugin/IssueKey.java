package com.isroot.stash.plugin;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A JIRA issue key.
 */
public class IssueKey
{
    /**
     * Parse any issue keys (i.e., strings that match the standard issue key format) found within the given input.
     *
     * @param input The input string to be parsed for issue keys.
     * @return The issue keys found
     */
    static public Set<IssueKey> parseIssueKeys (String input) {
        Set<IssueKey> issueKeys = new HashSet<IssueKey>();
        Matcher matcher = ISSUE_PATTERN.matcher(input);
        while (matcher.find())
        {
            issueKeys.add(new IssueKey(getMatchedProjectKey(matcher), getMatchedIssueId(matcher)));
        }

        return issueKeys;
    }

    /**
     * Parse the given issue key string.
     *
     * @param issueKey A valid JIRA issue key (eg, ABC-123).
     * @throws InvalidIssueKeyException if issueKey is not a correctly formatted JIRA issue key.
     */
    public IssueKey(String issueKey) throws InvalidIssueKeyException {
        Matcher matcher = ISSUE_PATTERN.matcher(issueKey);
        if (!matcher.find()) {
            throw new InvalidIssueKeyException(issueKey);
        }

        this.projectKey = getMatchedProjectKey(matcher);
        this.issueId = getMatchedIssueId(matcher);
    }

    /**
     * Construct a new issue key.
     *
     * @param projectKey The project key for this issue.
     * @param issueId The issue's project-relative issue identifier.
     */
    public IssueKey(String projectKey, String issueId) {
        this.projectKey = projectKey;
        this.issueId = issueId;
    }

    /**
     * Return the issue's project key.
     * @return JIRA project key.
     */
    public String getProjectKey() {
        return projectKey;
    }

    /**
     * Return the issue key's project-relative issue identifier.
     * @return JIRA issue identifier.
     */
    public String getIssueId() {
        return issueId;
    }

    /**
     * Return the fully qualified issue key (eg, "projectKey-issueId").
     * @return Fully qualified JIRA issue key.
     */
    public String getFullyQualifiedIssueKey() {
        return projectKey + '-' + issueId;
    }

    @Override
    public String toString() {
        return "IssueKey{" + getFullyQualifiedIssueKey() + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        IssueKey issueKey = (IssueKey) o;

        if (!issueId.equals(issueKey.issueId))
        {
            return false;
        }
        if (!projectKey.equals(issueKey.projectKey))
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = projectKey.hashCode();
        result = 31 * result + issueId.hashCode();
        return result;
    }

    /** JIRA project key */
    private final String projectKey;

    /** JIRA project-relative issue identifier */
    private final String issueId;

    /**
     * A regex pattern that matches on JIRA issue keys. The individual components of a valid match
     * should be extracted using the {@link #getMatchedProjectKey(java.util.regex.Matcher)} and {@link #getMatchedProjectKey(java.util.regex.Matcher)}
     * methods.
     */
    private static Pattern ISSUE_PATTERN = Pattern.compile("([A-Z][A-Z_0-9]+)-([0-9]+)");

    /**
     * Return the project key matched by {@link #ISSUE_PATTERN}.
     *
     * @param matcher A successfully matching {@link #ISSUE_PATTERN} matcher.
     * @return The matched project key.
     */
    private static String getMatchedProjectKey(Matcher matcher) {
        return matcher.group(1);
    }

    /**
     * Return the issue identifier matched by {@link #ISSUE_PATTERN}.
     *
     * @param matcher A successfully matching {@link #ISSUE_PATTERN} matcher.
     * @return The matched issue identifier.
     */
    private static String getMatchedIssueId(Matcher matcher) {
        return matcher.group(2);
    }
}
