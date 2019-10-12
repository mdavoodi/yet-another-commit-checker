package com.isroot.stash.plugin;

import com.atlassian.bitbucket.hook.repository.PreRepositoryHookContext;
import com.atlassian.bitbucket.hook.repository.RepositoryHookRequest;
import com.atlassian.bitbucket.hook.repository.RepositoryHookResult;
import com.atlassian.bitbucket.repository.RefChange;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.setting.Settings;
import com.isroot.stash.plugin.errors.YaccError;

import java.util.List;

/**
 * @author Sean Ford
 * @since 2014-01-14
 */
public interface YaccService {
    RepositoryHookResult check(PreRepositoryHookContext context,
                               RepositoryHookRequest repositoryPushHookRequest, Settings settings);

    List<YaccError> checkRefChange(Repository repository, Settings settings, RefChange refChange);

    List<YaccError> checkCommit(Settings settings, YaccCommit commit, String branchName);
}
