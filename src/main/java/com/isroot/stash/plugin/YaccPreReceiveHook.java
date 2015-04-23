package com.isroot.stash.plugin;

import com.atlassian.stash.repository.Repository;
import com.atlassian.stash.hook.HookResponse;
import com.atlassian.stash.hook.PreReceiveHook;
import com.atlassian.stash.hook.repository.RepositoryHook;
import com.atlassian.stash.hook.repository.RepositoryHookService;
import com.atlassian.stash.hook.repository.RepositoryHookContext;
import com.atlassian.stash.repository.RefChange;
import com.atlassian.stash.repository.RefChangeType;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.stash.user.Permission;
import com.atlassian.stash.user.SecurityService;
import com.atlassian.stash.util.UncheckedOperation;
import com.atlassian.stash.setting.Settings;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * @author Sean Ford
 * @since 2013-05-11
 */
public final class YaccPreReceiveHook implements PreReceiveHook
{
    private static final Logger log = LoggerFactory.getLogger(YaccPreReceiveHook.class);

    private final  YaccHook yaccHook;
    private final PluginSettingsFactory pluginSettingsFactory;
    private final SecurityService securityService;
    private final RepositoryHookService repositoryHookService;

    public YaccPreReceiveHook(YaccService yaccService,
        PluginSettingsFactory pluginSettingsFactory,
        SecurityService securityService,
        RepositoryHookService repositoryHookService
    )
    {
        yaccHook=new YaccHook(yaccService);
        this.pluginSettingsFactory = pluginSettingsFactory;
        this.securityService = securityService;
        this.repositoryHookService = repositoryHookService;
    }

    void addStringFieldValue(HashMap<String, Object> config,HashMap<String, String> settingsMap,String fieldName) {
        String o;
        o=settingsMap.get(fieldName);
        if(o!=null && !o.isEmpty())config.put(fieldName,o);
    }
    void addBoolFieldValue(HashMap<String, Object> config,HashMap<String, String> settingsMap,String fieldName) {
        String o;
        o=settingsMap.get(fieldName);
        if(o!=null && (o.equals("on") || o.equals("true")))config.put(fieldName,true);
    }
    @Override
    public boolean onReceive(final Repository repository,
                             @Nonnull Collection<RefChange> refChanges, @Nonnull HookResponse hookResponse)
    {
        RepositoryHook hook= securityService.withPermission(Permission.REPO_ADMIN,"Get plugin configuration").call(new UncheckedOperation<RepositoryHook>(){
            public RepositoryHook perform() {
                return repositoryHookService.getByKey(repository, "com.isroot.stash.plugin.yacc:yaccHook");
            }
        });
        Settings settings;
        if(hook.isEnabled() && hook.isConfigured()) {
            // Repository hook is configured and enabled. 
            // Repository hook overrides default pre-receive hook configuration
            log.debug("PreReceiveRepositoryHook configured. Skip PreReceiveHook");
            return true;    
            //settings = repositoryHookService.getSettings(repository, "com.teslamotors.stash.hook.jira-issue-enforcer:commit-message-issue-enforcer");
        } else {
            // Repository hook not configured
            log.debug("PreReceiveRepositoryHook not configured. Run PreReceiveHook");
            PluginSettings pluginSettings;
            pluginSettings = pluginSettingsFactory.createGlobalSettings();
            
            @SuppressWarnings("unchecked")
            HashMap<String, Object> config =  new HashMap<String, Object>();
            HashMap<String, String> settingsMap = (HashMap<String, String>) pluginSettings.get(ConfigServlet.SETTINGS_MAP);
            if(settingsMap != null) {
                // Convert plugin setting to expected datatypes
                addBoolFieldValue(config,settingsMap,"requireMatchingAuthorEmail");
                addBoolFieldValue(config,settingsMap,"requireMatchingAuthorName");
                addStringFieldValue(config,settingsMap,"commitMessageRegex");
                addBoolFieldValue(config,settingsMap,"requireJiraIssue");
                addBoolFieldValue(config,settingsMap,"ignoreUnknownIssueProjectKeys");
                addStringFieldValue(config,settingsMap,"issueJqlMatcher");
                addBoolFieldValue(config,settingsMap,"excludeMergeCommits");
                addStringFieldValue(config,settingsMap,"excludeByRegex");
                addBoolFieldValue(config,settingsMap,"excludeServiceUserCommits");
            }
            for (Map.Entry<String, Object> entry : config.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                log.debug("got plugin config "+key+"="+value+" "+value.getClass().getName());
            }
            /* Watershed commits are not relevant in global hook configuration context */
            settings=repositoryHookService.createSettingsBuilder()
                    .addAll(config)
                    .build();
        }

        return yaccHook.onReceive(new RepositoryHookContext(repository,settings),refChanges,hookResponse);

    }
}
