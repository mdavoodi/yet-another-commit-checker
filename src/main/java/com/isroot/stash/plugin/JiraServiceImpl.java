package com.isroot.stash.plugin;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.applinks.api.ApplicationLinkRequestFactory;
import com.atlassian.applinks.api.ApplicationLinkResponseHandler;
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.applinks.api.application.jira.JiraApplicationType;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.Response;
import com.atlassian.sal.api.net.ResponseException;
import com.atlassian.sal.api.net.ResponseStatusException;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Sean Ford
 * @since 2013-10-20
 */
public class JiraServiceImpl implements JiraService {
    private static final Logger log = LoggerFactory.getLogger(JiraServiceImpl.class);

    private final ApplicationLinkService applicationLinkService;

    public JiraServiceImpl(ApplicationLinkService applicationLinkService) {
        this.applicationLinkService = applicationLinkService;
    }

    private ApplicationLink getJiraApplicationLink() {
        ApplicationLink applicationLink = applicationLinkService.getPrimaryApplicationLink(JiraApplicationType.class);

        if (applicationLink == null) {
            throw new IllegalStateException("Primary JIRA application link does not exist!");
        }

        return applicationLink;
    }

    @Override
    public boolean doesJiraApplicationLinkExist() {
        return applicationLinkService.getPrimaryApplicationLink(JiraApplicationType.class) != null;
    }

    @Override
    public boolean doesIssueExist(IssueKey issueKey) throws CredentialsRequiredException, ResponseException {
        checkNotNull(issueKey, "issueKey is null");


        final ApplicationLinkRequestFactory fac = getJiraApplicationLink().createAuthenticatedRequestFactory();

        ApplicationLinkRequest req = fac.createRequest(Request.MethodType.GET, "/rest/api/2/issue/"+issueKey.getFullyQualifiedIssueKey());

        return req.execute(new ApplicationLinkResponseHandler<Boolean>() {
            @Override
            public Boolean credentialsRequired(Response response) throws ResponseException {
                throw new ResponseException(new CredentialsRequiredException(fac, "Token is invalid"));
            }

            @Override
            public Boolean handle(Response response) throws ResponseException {
                return response.isSuccessful();
            }
        });
    }

    @Override
    public boolean doesProjectExist(String projectKey) throws CredentialsRequiredException, ResponseException {
        checkNotNull(projectKey, "projectKey is null");

        ApplicationLinkRequest req = getJiraApplicationLink().createAuthenticatedRequestFactory().createRequest(Request.MethodType.GET, "/rest/api/2/project/" + projectKey);

        try {
            String jsonResponse = req.execute();
            JsonObject response = new JsonParser().parse(jsonResponse).getAsJsonObject();
            return (projectKey.equals(response.get("key").getAsString()));
        }
        catch (ResponseStatusException e) {
            if (e.getResponse().getStatusCode() == 404) {
                /* Project is unknown */
                return false;
            } else {
                throw new ResponseException("Request failed", e);
            }
        }
    }

    @Override
    public boolean doesIssueMatchJqlQuery(String jqlQuery, IssueKey issueKey) throws CredentialsRequiredException,
            ResponseException {
        checkNotNull(jqlQuery, "jqlQuery is null");
        checkNotNull(issueKey, "issueKey is null");

        // Combine the user's jql query with issueKey=<issueKey> to avoid paging. If a single result is returned,
        // then the issue key matches the jql query
        String jqlQueryWithIssueExpression = String.format("issueKey=%s and (%s)", issueKey.getFullyQualifiedIssueKey(),
                jqlQuery);

        String jsonResponse = executeJqlQuery(jqlQueryWithIssueExpression);

        JsonObject response = new JsonParser().parse(jsonResponse).getAsJsonObject();
        JsonArray issues = response.get("issues").getAsJsonArray();

        return issues.size() == 1;
    }

    @Override
    public boolean isJqlQueryValid(String jqlQuery) throws CredentialsRequiredException, ResponseException {
        try {
            // This will throw an exception if the jql query is invalid.
            executeJqlQuery(jqlQuery);
            return true;
        } catch(ResponseStatusException e) {
            // if the jql query is invalid, a 400 error is returned
            if(e.getResponse().getStatusCode() == 400) {
                return false;
            } else {
                // Not a 400 response... just re-throw it to avoid hiding potential problems
                throw e;
            }

        }
    }

    private String executeJqlQuery(String jqlQuery) throws CredentialsRequiredException, ResponseException {
        checkNotNull(jqlQuery, "jqlQuery is null");

        ApplicationLinkRequest req = getJiraApplicationLink().createAuthenticatedRequestFactory()
                .createRequest(Request.MethodType.POST, "/rest/api/2/search");
        req.setHeader("Content-Type", "application/json");

        log.debug("using jql: {}", jqlQuery);

        Map<String, String> request = new HashMap<>();
        request.put("jql", jqlQuery);
        req.setEntity(new Gson().toJson(request));

        String response = req.execute();

        log.debug("json response: {}", response);

        return response;
    }
}
