package com.isroot.stash.plugin;

import com.atlassian.stash.hook.HookResponse;
import com.atlassian.stash.hook.repository.PreReceiveRepositoryHook;
import com.atlassian.stash.hook.repository.RepositoryHookContext;
import com.atlassian.stash.repository.RefChange;
import com.atlassian.stash.repository.RefChangeType;
import com.google.common.collect.Lists;
import com.isroot.stash.plugin.localization.ResourceBundleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;

/**
 * @author Sean Ford
 * @since 2013-05-11
 */
public final class YaccHook implements PreReceiveRepositoryHook
{
    private static final Logger log = LoggerFactory.getLogger(YaccHook.class);

    public static final String ERROR_BEARS = "\n" +
            "  (c).-.(c)    (c).-.(c)    (c).-.(c)    (c).-.(c)    (c).-.(c) \n" +
            "   / ._. \\      / ._. \\      / ._. \\      / ._. \\      / ._. \\ \n" +
            " __\\( Y )/__  __\\( Y )/__  __\\( Y )/__  __\\( Y )/__  __\\( Y )/__\n" +
            "(_.-/'-'\\-._)(_.-/'-'\\-._)(_.-/'-'\\-._)(_.-/'-'\\-._)(_.-/'-'\\-._)\n" +
            "   || E ||      || R ||      || R ||      || O ||      || R ||\n" +
            " _.' `-' '._  _.' `-' '._  _.' `-' '._  _.' `-' '._  _.' `-' '.\n" +
            "(.-./`-'\\.-.)(.-./`-`\\.-.)(.-./`-`\\.-.)(.-./`-'\\.-.)(.-./`-`\\.-.)\n" +
            " `-'     `-'  `-'     `-'  `-'     `-'  `-'     `-'  `-'     `-' \n" +
            "\n" +
            "\n" +
            "%s.\n";

    private final YaccService yaccService;
    private final ResourceBundleService bundle;

    public YaccHook(YaccService yaccService, ResourceBundleService bundle)
    {
        this.yaccService = yaccService;
        this.bundle = bundle;
    }

    @Override
    public boolean onReceive(@Nonnull RepositoryHookContext repositoryHookContext,
                             @Nonnull Collection<RefChange> refChanges, @Nonnull HookResponse hookResponse)
    {
        List<String> errors = Lists.newArrayList();
        // refresh bundle configuration to apply language change
        bundle.refresh(repositoryHookContext.getSettings());

        for (RefChange rf : refChanges)
        {
            if (rf.getType() == RefChangeType.DELETE)
            {
                continue;
            }

            errors.addAll(yaccService.checkRefChange(repositoryHookContext.getRepository(), repositoryHookContext.getSettings(), rf));
        }

        if (errors.isEmpty())
        {
            log.debug("push allowed");

            return true;
        }
        else
        {
            String errorBearsMsg = bundle.getMessage("errorBearsMessage");
            hookResponse.err().println(String.format(ERROR_BEARS, errorBearsMsg));

            for (String error : errors)
            {
                log.debug("error: {}", error);

                hookResponse.err().println(error);
            }

            hookResponse.err().println();

            log.debug("push rejected");

            return false;
        }
    }
}
