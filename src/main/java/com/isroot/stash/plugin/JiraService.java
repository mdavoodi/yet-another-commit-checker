package com.isroot.stash.plugin;

import java.util.List;
import javax.annotation.Nonnull;

/**
 * Service object to interact with JIRA.
 *
 * @author Sean Ford
 * @since 2013-10-26
 */
public interface JiraService
{
    public boolean doesJiraApplicationLinkExist();
    public boolean doesIssueMatchJqlQuery(@Nonnull String jqlQuery, @Nonnull IssueKey issueKey) throws JiraLookupsException;
    public boolean doesIssueExist(@Nonnull IssueKey issueKey) throws JiraLookupsException;
    public boolean doesProjectExist(@Nonnull String projectKey) throws JiraLookupsException;
    public List<String> checkJqlQuery(@Nonnull String jqlQuery);
}
