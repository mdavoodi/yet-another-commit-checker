package com.isroot.stash.plugin;

import com.atlassian.bitbucket.hook.repository.RepositoryHookService;
import com.atlassian.bitbucket.setting.Settings;
import com.atlassian.bitbucket.user.SecurityService;
import com.atlassian.bitbucket.user.UserService;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;
import java.util.function.Supplier;

public class ImpersonationService {
    private final UserService userService;
    private final SecurityService securityService;
    private final PluginSettingsFactory pluginSettingsFactory;
    private final RepositoryHookService repositoryHookService;

    public ImpersonationService(
        UserService userService,
        SecurityService securityService,
        PluginSettingsFactory pluginSettingsFactory,
        RepositoryHookService repositoryHookService
    ) {
        this.userService = userService;
        this.securityService = securityService;
        this.pluginSettingsFactory = pluginSettingsFactory;
        this.repositoryHookService = repositoryHookService;
    }

    public  <T> T runImpersonating(Supplier<T> handler) {
        Settings settings = YaccUtils.buildYaccConfig(pluginSettingsFactory, repositoryHookService);
        if (settings.getBoolean("overrideJiraUserEnabled", false) && StringUtils.isNotEmpty(settings.getString("overrideJiraUser"))) {
            return Optional.ofNullable(userService.getUserByName(settings.getString("overrideJiraUser"))).map(applicationUser ->
                securityService.impersonating(applicationUser, "YACC Hook").call(handler::get)
            ).orElseGet(handler);
        }
        return handler.get();
    }
}
