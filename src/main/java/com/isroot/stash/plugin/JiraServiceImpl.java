package com.isroot.stash.plugin;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.applinks.api.ApplicationLinkRequestFactory;
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.applinks.api.application.jira.JiraApplicationType;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.ResponseException;
import com.atlassian.sal.api.net.ResponseStatusException;
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sean Ford
 * @since 2013-10-20
 */
public class JiraServiceImpl implements JiraService
{
    private static final Logger log = LoggerFactory.getLogger(JiraServiceImpl.class);

    private final ApplicationLinkService applicationLinkService;

    public JiraServiceImpl(ApplicationLinkService applicationLinkService)
    {
        this.applicationLinkService = applicationLinkService;
    }

    private Iterable<ApplicationLink> getJiraApplicationLinks()
    {
        Iterable<ApplicationLink> applicationLinks = applicationLinkService.getApplicationLinks(JiraApplicationType.class);

        if (applicationLinks == null || Iterables.isEmpty(applicationLinks))
        {
            throw new IllegalStateException("JIRA application link does not exist!");
        }

        return applicationLinks;
    }

    @Override
    public boolean doesJiraApplicationLinkExist()
    {
        return applicationLinkService.getPrimaryApplicationLink(JiraApplicationType.class) != null;
    }

    @Override
    public boolean doesIssueExist(@Nonnull IssueKey issueKey) throws JiraLookupsException
    {
        checkNotNull(issueKey, "issueKey is null");

        Map<ApplicationLink, Exception> errors = new HashMap<ApplicationLink, Exception>();
        
        for (final ApplicationLink link : getJiraApplicationLinks())
        {
            try
            {
                final ApplicationLinkRequestFactory fac = link.createAuthenticatedRequestFactory();

                ApplicationLinkRequest req = fac.createRequest(Request.MethodType.GET, "/rest/api/2/issue/"+issueKey.getFullyQualifiedIssueKey()+"?fields=summary");
                
                req.executeAndReturn(new ErrorParsingReturningResponseHandler());
                
                // No error, so must exist
                return true;
            }
            catch (ResponseStatusException e)
            {
                if (e.getResponse().getStatusCode() == 404)
                {
                    /* Project is unknown */
                    continue;
                }

                errors.put(link, e);
            }
            catch (CredentialsRequiredException e)
            {
                errors.put(link, e);
            }
            catch (ResponseException e)
            {
                errors.put(link, e);
            }
            catch (Exception e)
            {
                log.error("Unknown error validating issue", e);
                errors.put(link, e);
            }
        }
        
        if (!errors.isEmpty()) {
            throw new JiraLookupsException(errors);
        }
        
        return false;
    }

    @Override
    public boolean doesProjectExist(@Nonnull String projectKey) throws JiraLookupsException
    {
        checkNotNull(projectKey, "projectKey is null");
        
        Map<ApplicationLink, Exception> errors = new HashMap<ApplicationLink, Exception>();
        
        for (final ApplicationLink link : getJiraApplicationLinks())
        {
            try
            {
                ApplicationLinkRequest req = link.createAuthenticatedRequestFactory().createRequest(Request.MethodType.GET, "/rest/api/2/project/" + projectKey);
                String jsonResponse = req.executeAndReturn(new ErrorParsingReturningResponseHandler());
                JsonObject response = new JsonParser().parse(jsonResponse).getAsJsonObject();
                
                if (projectKey.equals(response.get("key").getAsString())) {
                    return true;
                }
            }
            catch (ResponseStatusException e)
            {
                if (e.getResponse().getStatusCode() == 404)
                {
                    /* Project is unknown */
                    continue;
                }

                errors.put(link, e);
            }
            catch (CredentialsRequiredException e)
            {
                errors.put(link, e);
            }
            catch (ResponseException e)
            {
                // Don't log the full stack trace - connection timeouts/etc
                // are clear from the error message
                errors.put(link, e);
            }
            catch (Exception e)
            {
                log.error("Unknown error validating project", e);
                errors.put(link, e);
            }
        }
        
        if (!errors.isEmpty())
        {
            throw new JiraLookupsException(errors);
        }
        
        return false;
    }

    @Override
    public boolean doesIssueMatchJqlQuery(@Nonnull String jqlQuery, @Nonnull IssueKey issueKey) throws JiraLookupsException
    {
        checkNotNull(jqlQuery, "jqlQuery is null");
        checkNotNull(issueKey, "issueKey is null");

        // Combine the user's jql query with issueKey=<issueKey> to avoid paging. If a single result is returned,
        // then the issue key matches the jql query
        String jqlQueryWithIssueExpression = String.format("issueKey=%s and (%s)", issueKey.getFullyQualifiedIssueKey(),
                jqlQuery);

        Map<ApplicationLink, Exception> errors = new HashMap<ApplicationLink, Exception>();
        List<ApplicationLink> notFound = new ArrayList<ApplicationLink>();
        
        for (final ApplicationLink link : getJiraApplicationLinks())
        {
            try
            {
                String jsonResponse = executeJqlQuery(link, jqlQueryWithIssueExpression);

                JsonObject response = new JsonParser().parse(jsonResponse).getAsJsonObject();
                JsonArray issues = response.get("issues").getAsJsonArray();

                if (issues.size() > 0)
                {
                    return true;
                }
                else
                {
                    notFound.add(link);
                }
            }
            catch (JiraExecutionException e)
            {
                // Older versions of JIRA (<6.0.3) don't support the validateQuery param, so they
                // throw an error if the JIRA doesn't exist.
                // If Stash is linked to multiple JIRA versions, one new and one old
                // then we get an error from the old JIRA and no result from the new one.
                // This leads to reporting the error to the user, but not the no result,
                // which is confusing.
                // To manage that, swallow the errors.
                // The parsing doesn't have to be too flexible, because newer versions will support
                // this parameter...
                List<String> jiraErrors = e.getJiraErrors();
                if (jiraErrors.size() == 1 && jiraErrors.get(0).contains(issueKey.getFullyQualifiedIssueKey()))
                {
                    notFound.add(link);
                }
                else
                {
                    errors.put(link, e);
                }
            }
            catch (CredentialsRequiredException e)
            {
                errors.put(link, e);
            }
            catch (ResponseException e)
            {
                errors.put(link, e);
            }
            catch (Exception e)
            {
                log.error("Unknown error running JQL query", e);
                errors.put(link, e);
            }
        }

        if (!errors.isEmpty())
        {
            // Add all the JIRAs where the response wasn't matched,
            // so that the user can see the problem for all the linked JIRAs
            for (ApplicationLink link : notFound)
            {
                errors.put(link, new ResponseException(issueKey.getFullyQualifiedIssueKey() + ": JIRA issue does not match JQL query: " + jqlQuery));
            }
            throw new JiraLookupsException(errors);
        }
        
        return false;
    }

    @Override
    public List<String> checkJqlQuery(@Nonnull String jqlQuery)
    {
        checkNotNull(jqlQuery, "jqlQuery is null");
        
        // Note that we still need to iterate over all of the application links here,
        // because a query using a custom field may not be valid on all instances
        Map<ApplicationLink, Exception> errors = new HashMap<ApplicationLink, Exception>();
        
        for (final ApplicationLink link : getJiraApplicationLinks())
        {
            try
            {
                // This will throw an exception if the jql query is invalid.
                executeJqlQuery(link, jqlQuery);
                return ImmutableList.<String>of();
            }
            catch (CredentialsRequiredException e)
            {
                errors.put(link, e);
            }
            catch (ResponseException e)
            {
                errors.put(link, e);
            }
            catch (Exception e)
            {
                log.error("Unknown error validating JQL query", e);
                errors.put(link, e);
            }
        }
        
        if (!errors.isEmpty()) {
            // Eww....
            JiraLookupsException ex = new JiraLookupsException(errors);
            return ex.getPrintableErrors();
        }
        
        return ImmutableList.<String>of();
    }

    private String executeJqlQuery(ApplicationLink link, String jqlQuery) throws CredentialsRequiredException, ResponseException
    {
        checkNotNull(jqlQuery, "jqlQuery is null");

        ApplicationLinkRequest req = link.createAuthenticatedRequestFactory()
                .createRequest(Request.MethodType.POST, "/rest/api/2/search?fields=summary&validateQuery=false");
        req.setHeader("Content-Type", "application/json");

        log.debug("using jql: {}", jqlQuery);

        Map<String, String> request = new HashMap<String, String>();
        request.put("jql", jqlQuery);
        req.setEntity(new Gson().toJson(request));

        String response = req.executeAndReturn(new ErrorParsingReturningResponseHandler());

        log.debug("json response: {}", response);

        return response;
    }
}
