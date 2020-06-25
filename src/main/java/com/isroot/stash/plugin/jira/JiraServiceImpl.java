package com.isroot.stash.plugin.jira;

import org.apache.http.conn.HttpHostConnectException;
import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.applinks.api.ReadOnlyApplicationLink;
import com.atlassian.applinks.api.application.jira.JiraApplicationType;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.ResponseException;
import com.atlassian.sal.api.net.ResponseStatusException;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.isroot.stash.plugin.IssueKey;
import com.isroot.stash.plugin.JiraService;
import com.isroot.stash.plugin.errors.YaccError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Sean Ford
 * @since 2013-10-20
 */
public class JiraServiceImpl implements JiraService {
    private static final Logger log = LoggerFactory.getLogger(JiraServiceImpl.class);

    private static final String ISSUE_NOT_FOUND = "%s: JIRA Issue does not exist";
    private static final String JQL_NO_MATCH = "%s: JIRA Issue does not match JQL Query: %s";

    private final ApplicationLinkService applicationLinkService;

    public JiraServiceImpl(ApplicationLinkService applicationLinkService) {
        this.applicationLinkService = applicationLinkService;
    }

    private Iterable<ReadOnlyApplicationLink> getJiraApplicationLinks() {
        List<ReadOnlyApplicationLink> links = new ArrayList<>();

        for (ApplicationLink link : applicationLinkService.getApplicationLinks(JiraApplicationType.class)) {
            links.add(link);
        }

        if (links.isEmpty()) {
            throw new IllegalStateException("No JIRA application links exist.");
        }

        log.debug("number of JIRA application links: {}", links.size());

        return links;
    }

    @Override
    public boolean doesJiraApplicationLinkExist() {
        return applicationLinkService.getPrimaryApplicationLink(JiraApplicationType.class) != null;
    }

    @Override
    public List<YaccError> doesIssueExist(IssueKey issueKey, boolean ignoreJiraConnectionFailures) {
        checkNotNull(issueKey, "issueKey is null");

        List<YaccError> errors = new ArrayList<>();

        try {
            // JIRA response to this query can be different depending on how the issue key is
            // invalid.
            // 1) If project key does not exist, an 200 with zero result size is returned
            // 2) If project key exists but issue number does not exist, a 400 response due to
            //    invalid JQL is returned
            if (!execute("issueKey='" + issueKey.getFullyQualifiedIssueKey() + "'",
                    SUCCESS_ON.NON_ZERO_RESULT, false, ignoreJiraConnectionFailures)) {
                errors.add(new YaccError(YaccError.Type.ISSUE_JQL, "%s: JIRA Issue does not exist",
                        issueKey.getFullyQualifiedIssueKey()));
            }
        } catch (JiraLookupsException e) {
            e.addMessageForApplicationLinksNotPresent(getJiraApplicationLinks(),
                    String.format(ISSUE_NOT_FOUND, issueKey.getFullyQualifiedIssueKey()));

            errors.addAll(e.getYaccErrors());
        }

        return errors;
    }

    @Override
    public boolean doesProjectExist(IssueKey issueKey, boolean ignoreJiraConnectionFailures) {
        checkNotNull(issueKey, "issueKey is null");

        try {
            String jql = String.format("project = '%s'", issueKey.getProjectKey());

            return execute(jql, SUCCESS_ON.STATUS_200, false, ignoreJiraConnectionFailures);
        } catch (JiraLookupsException e) {

            // Assume project exists if there is any sort of error. If there
            // is an error, it is probably going to be an credentials issue
            // with JIRA. If this is the case, it will be handled better when
            // JIRA issue or JQL checkers run.
            return true;
        }
    }

    @Override
    public List<YaccError> doesIssueMatchJqlQuery(String jqlQuery, IssueKey issueKey, boolean ignoreJiraConnectionFailures) {
        checkNotNull(jqlQuery, "jqlQuery is null");
        checkNotNull(issueKey, "issueKey is null");

        List<YaccError> errors = new ArrayList<>();

        String jqlQueryWithIssueExpression = String.format("issueKey=%s and (%s)",
                issueKey.getFullyQualifiedIssueKey(), jqlQuery);

        try {
            if (!execute(jqlQueryWithIssueExpression, SUCCESS_ON.NON_ZERO_RESULT, true, ignoreJiraConnectionFailures)) {
                errors.add(new YaccError(YaccError.Type.ISSUE_JQL, "%s: JIRA Issue does not match JQL Query: %s",
                        issueKey.getFullyQualifiedIssueKey(), jqlQuery));
            }
        } catch (JiraLookupsException e) {
            e.addMessageForApplicationLinksNotPresent(getJiraApplicationLinks(),
                    String.format(JQL_NO_MATCH, issueKey.getFullyQualifiedIssueKey(), jqlQuery));

            errors.addAll(e.getYaccErrors());
        }

        return errors;
    }

    @Override
    public List<String> checkJqlQuery(@Nonnull String jqlQuery) {
        checkNotNull(jqlQuery, "jqlQuery is null");

        try {
            // This will throw an exception if the jql query is invalid.
            if(execute(jqlQuery, SUCCESS_ON.STATUS_200, false, false)) {
                return ImmutableList.<String>of();
            } else {
                return ImmutableList.of("JQL Query is invalid.");
            }

        } catch (JiraLookupsException e) {
            e.addMessageForApplicationLinksNotPresent(getJiraApplicationLinks(), "JQL Query is invalid.");

            return e.getYaccErrors().stream()
                    .map(YaccError::getMessage)
                    .collect(Collectors.toList());
        }
    }

    private boolean execute(String jqlQuery, SUCCESS_ON successOn, boolean trackInvalidJqlAsError, boolean ignoreJiraConnectionFailures)
            throws JiraLookupsException {
        checkNotNull(jqlQuery, "jqlQuery is null");

        JiraLookupsException ex = new JiraLookupsException();

        for (final ReadOnlyApplicationLink link : getJiraApplicationLinks()) {
            try {
                log.debug("executing JQL query on JIRA application link '{}': {}", link.getName(),
                        jqlQuery);

                ApplicationLinkRequest req = link.createAuthenticatedRequestFactory()
                        .createRequest(Request.MethodType.POST, "/rest/api/2/search");

                req.setHeader("Content-Type", "application/json");

                Map<String, Object> request = new HashMap<>();
                request.put("jql", jqlQuery);

                List<String> requestedFields = new ArrayList<>();
                requestedFields.add("summary");
                request.put("fields", requestedFields);

                req.setEntity(new Gson().toJson(request));

                String jsonResponse = req.execute();

                log.debug("json response: {}", jsonResponse);

                JsonObject response = new JsonParser().parse(jsonResponse).getAsJsonObject();
                JsonArray issues = response.get("issues").getAsJsonArray();

                if (successOn == SUCCESS_ON.NON_ZERO_RESULT && issues.size() > 0) {
                    return true;
                }
                else if (successOn == SUCCESS_ON.STATUS_200) {
                    return true;
                }
            } catch (CredentialsRequiredException e) {
                log.error("credentials", e);

                ex.addError(link, e);
            } catch (ResponseException e) {
                if (e instanceof ResponseStatusException) {
                    ResponseStatusException statusException = (ResponseStatusException) e;

                    log.debug("status code {}", statusException.getResponse().getStatusCode(), e);

                    try {
                        log.debug("response entity: {}", statusException.getResponse().getResponseBodyAsString());
                    } catch (ResponseException e1) {
                        log.error("error getting response body", e);
                    }

                    if (statusException.getResponse().getStatusCode() == 400) {
                        log.debug("400 check, {}", trackInvalidJqlAsError);

                        if(trackInvalidJqlAsError) {
                            ex.addError(link, "Query is not valid for JIRA instance: " + jqlQuery);
                        }

                        continue;
                    }
                }

                if (ignoreJiraConnectionFailures && e.getCause() instanceof HttpHostConnectException) {
                    log.error("can not connect to JIRA instance, Ignore JIRA Issue Requirements ignored", e);
                    return true;
                }

                log.error("response", e);

                ex.addError(link, e);
            }
        }

        if (ex.hasErrors()) {
            throw ex;
        }

        return false;
    }

    private enum SUCCESS_ON {STATUS_200, NON_ZERO_RESULT}
}
