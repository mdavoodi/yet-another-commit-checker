package com.isroot.stash.plugin;

import com.atlassian.bitbucket.event.branch.BranchCreationHookRequest;
import com.atlassian.bitbucket.hook.repository.PreRepositoryHook;
import com.atlassian.bitbucket.hook.repository.PreRepositoryHookContext;
import com.atlassian.bitbucket.hook.repository.RepositoryHook;
import com.atlassian.bitbucket.hook.repository.RepositoryHookRequest;
import com.atlassian.bitbucket.hook.repository.RepositoryHookResult;
import com.atlassian.bitbucket.hook.repository.RepositoryHookService;
import com.atlassian.bitbucket.hook.repository.RepositoryHookTrigger;
import com.atlassian.bitbucket.hook.repository.StandardRepositoryHookTrigger;
import com.atlassian.bitbucket.permission.Permission;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.scope.Scopes;
import com.atlassian.bitbucket.setting.Settings;
import com.atlassian.bitbucket.user.SecurityService;
import com.atlassian.bitbucket.util.UncheckedOperation;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * System-wide pre-receive hook.  Will defer to the local repository YACC hook configuration if present.
 *
 * @author Uldis Ansmits
 * @author Jim Bethancourt
 */
public class YaccGlobalHook implements PreRepositoryHook {

    private static final Logger log = LoggerFactory.getLogger(YaccGlobalHook.class);

    private final YaccService yaccService;
    private final PluginSettingsFactory pluginSettingsFactory;
    private final SecurityService securityService;
    private final RepositoryHookService repositoryHookService;

    public YaccGlobalHook(YaccService yaccService,
                              PluginSettingsFactory pluginSettingsFactory,
                              SecurityService securityService,
                              RepositoryHookService repositoryHookService) {
        this.yaccService = yaccService;
        this.pluginSettingsFactory = pluginSettingsFactory;
        this.securityService = securityService;
        this.repositoryHookService = repositoryHookService;
    }

    @Nonnull
    @Override
    public RepositoryHookResult preUpdate(@Nonnull PreRepositoryHookContext context,
            @Nonnull RepositoryHookRequest repositoryHookRequest) {
        log.debug("global hook, trigger={}", repositoryHookRequest.getTrigger());

        final Repository repository = repositoryHookRequest.getRepository();

        RepositoryHook hook = securityService.withPermission(Permission.REPO_ADMIN, "Get plugin configuration").call(new UncheckedOperation<RepositoryHook>() {
            public RepositoryHook perform() {
                return repositoryHookService.getByKey(Scopes.repository(repository),
                        "com.isroot.stash.plugin.yacc:yaccHook");
            }
        });

        log.debug("yacc repo hook, enabled={} scope={}", hook.isEnabled(), hook.getScope());

        if(!hook.isEnabled()) {
            log.debug("yacc repository hook disabled");

            Settings storedConfig = YaccUtils.buildYaccConfig(pluginSettingsFactory, repositoryHookService);

            log.debug("global settings: {}", storedConfig.asMap());

            if(areThereEnabledSettings(storedConfig.asMap())) {
                return handleGlobalHook(context, repositoryHookRequest, storedConfig);
            } else {
                log.debug("no need to run yacc because no global settings configured");
            }
        } else {
            log.debug("yacc repository hook enabled");
        }

        // Will be accepted unless commit callback rejects a commit
        return RepositoryHookResult.accepted();
    }

    private RepositoryHookResult handleGlobalHook(PreRepositoryHookContext context,
                                                  RepositoryHookRequest request, Settings config) {
        final RepositoryHookTrigger trigger = request.getTrigger();

        if (trigger == StandardRepositoryHookTrigger.REPO_PUSH) {
            return yaccService.check(context, request, config);
        } else if (trigger == StandardRepositoryHookTrigger.BRANCH_CREATE
                && request instanceof BranchCreationHookRequest) {

            // sford: Note: Both trigger BRANCH_CREATE and class BranchCreationHookRequest both
            // need to be checked because BBS has a bug where BranchDeletionHookRequest is marked
            // as BRANCH_CREATE.
            //
            // See: https://jira.atlassian.com/browse/BSERV-11458

            return YaccHook.handleBranchCreation(config, (BranchCreationHookRequest)request);
        }

        log.debug("trigger does not needed handling, accepting request");

        return RepositoryHookResult.accepted();
    }

    /**
     * Return true if there are enabled settings, else false. This allows us to only run
     * {@link YaccHook} if there something is enabled. YACC can take a while to run on
     * large repositories, and we don't want to run it globally unless it is actually
     * configured to do something.
     */
    private boolean areThereEnabledSettings(Map<String, Object> settings) {
        for(Map.Entry<String, Object> setting : settings.entrySet()) {
            if(setting.getKey().startsWith("errorMessage")) {
                continue;
            }

            if(setting.getValue() == null) {
                continue;
            }

            String val = setting.getValue().toString();

            if(val.equals("true")) {
                return true;
            }

            // 'false' strings are assumed to be disabled boolean settings, so they are
            // not considered enabled settings.
            if(!val.isEmpty() && !val.equalsIgnoreCase("false")) {
                return true;
            }
        }

        return false;
    }
}
