package com.isroot.stash.plugin;

import com.atlassian.bitbucket.hook.repository.CommitAddedDetails;
import com.atlassian.bitbucket.hook.repository.PreRepositoryHookCommitCallback;
import com.atlassian.bitbucket.hook.repository.RepositoryHookResult;
import com.atlassian.bitbucket.setting.Settings;
import com.google.common.collect.Lists;
import com.isroot.stash.plugin.errors.YaccError;
import com.isroot.stash.plugin.errors.YaccErrorBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Sean Ford
 * @since 2017-05-06
 */
class YaccHookCommitCallback implements PreRepositoryHookCommitCallback {
    private static final Logger log = LoggerFactory.getLogger(YaccHookCommitCallback.class);

    private final Settings settings;
    private final YaccService yaccService;
    private final List<YaccError> errors;
    private final boolean showDefaultMessageHeader;

    private RepositoryHookResult result = RepositoryHookResult.accepted();

    public YaccHookCommitCallback(YaccService yaccService, Settings settings,
                                  boolean showDefaultMessageHeader) {
        this.settings = settings;
        this.yaccService = yaccService;
        this.errors = new ArrayList<>();
        this.showDefaultMessageHeader = showDefaultMessageHeader;
    }

    @Override
    public boolean onCommitAdded(@Nonnull CommitAddedDetails commitDetails) {
        log.debug("yacc commit callback, ref={} commit={}", commitDetails.getRef().getId(),
                commitDetails.getCommit().getId());

        if(commitDetails.getRef().getId().startsWith("refs/notes/")) {
            log.debug("git notes ref, skipping");

            return true;
        }

        YaccCommit yaccCommit = new YaccCommit(commitDetails.getCommit());

        String branchName = commitDetails.getRef().getDisplayId();

        List<YaccError> commitErrors;
        try {
            commitErrors = yaccService.checkCommit(settings, yaccCommit, branchName);
        } catch (TimeLimitedMatcherFactory.RegExpTimeoutException e) {
            log.error("Regex timeout for {} / {}", commitDetails.getCommit().getRepository().getProject().getName(), commitDetails.getCommit().getRepository().getName());
            log.error("Regex timeout exceeded", e);
            commitErrors = Lists.newArrayList();
            commitErrors.add(new YaccError(YaccError.Type.OTHER, "The timeout for evaluating regular expression has been exceeded"));
        }

        for (YaccError e : commitErrors) {
            String refAndCommitId = String.format("%s: %s",
                    commitDetails.getRef().getId(), commitDetails.getCommit().getId());

            errors.add(e.prependText(refAndCommitId));
        }

        return true;
    }

    @Override
    public void onEnd() {
        log.debug("callback onEnd");

        if (!errors.isEmpty()) {
            YaccErrorBuilder errorBuilder = new YaccErrorBuilder(settings,
                    showDefaultMessageHeader);
            String message = errorBuilder.getErrorMessage(errors);

            result = RepositoryHookResult.rejected("Push rejected by YACC", message);
        }
    }

    @Nonnull
    @Override
    public RepositoryHookResult getResult() {
        return result;
    }
}
