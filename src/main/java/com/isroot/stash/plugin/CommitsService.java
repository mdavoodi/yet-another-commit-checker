package com.isroot.stash.plugin;

import com.atlassian.stash.repository.RefChange;
import com.atlassian.stash.repository.Repository;

import java.util.Set;

/**
 * Service to get new commits from a {@link RefChange}.
 *
 * @author Sean Ford
 * @since 2013-10-26
 */
public interface CommitsService {
    /**
     * Get new {@link YaccCommit}s not already present in the repository for the given {@link RefChange}.
     * @param repository {@link Repository} to look at
     * @param refChange {@link RefChange} to start from
     * @return A {@link Set} of {@link YaccCommit}s
     */
    public Set<YaccCommit> getNewCommits(Repository repository, RefChange refChange);
}
