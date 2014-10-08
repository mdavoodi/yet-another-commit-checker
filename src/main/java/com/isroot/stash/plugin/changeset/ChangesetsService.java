package com.isroot.stash.plugin.changeset;

import com.atlassian.stash.repository.RefChange;
import com.atlassian.stash.repository.Repository;

import java.util.Set;

/**
 * Service to get new commits from a {@link RefChange}.
 *
 * @author Sean Ford
 * @since 2013-10-26
 */
public interface ChangesetsService
{
    /**
     * Get new {@link YaccChangeset}s not already present in the repository for the given {@link RefChange}.
     */
    public Set<YaccChangeset> getNewChangesets(Repository repository, RefChange refChange);
}
