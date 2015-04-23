package com.isroot.stash.plugin.jira;

import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.sal.api.net.ResponseException;

/**
 * Service object to interact with JIRA.
 *
 * @author Sean Ford
 * @since 2013-10-26
 */
public interface JiraService
{
    public boolean doesJiraApplicationLinkExist();
    public boolean doesIssueMatchJqlQuery(String jqlQuery, IssueKey issueKey) throws CredentialsRequiredException, ResponseException;
    public boolean doesIssueExist(IssueKey issueKey) throws CredentialsRequiredException, ResponseException;
    public boolean doesProjectExist(String projectKey) throws CredentialsRequiredException, ResponseException;
    public boolean isJqlQueryValid(String jqlQuery) throws CredentialsRequiredException, ResponseException;
}
