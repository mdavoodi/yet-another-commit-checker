package com.isroot.stash.plugin.changeset;

import com.atlassian.stash.commit.CommitService;
import com.atlassian.stash.content.Changeset;
import com.atlassian.stash.content.ChangesetsBetweenRequest;
import com.atlassian.stash.repository.RefChange;
import com.atlassian.stash.repository.RefChangeType;
import com.atlassian.stash.repository.Repository;
import com.atlassian.stash.scm.git.GitRefPattern;
import com.atlassian.stash.server.ApplicationPropertiesService;
import com.atlassian.stash.util.Page;
import com.atlassian.stash.util.PageProvider;
import com.atlassian.stash.util.PageRequest;
import com.atlassian.stash.util.PagedIterable;
import com.google.common.collect.Sets;
import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

/**
 * @author Sean Ford
 * @since 2013-10-26
 */
public class ChangesetsServiceImpl implements ChangesetsService
{
    private final CommitService commitService;
    private final ApplicationPropertiesService applicationPropertiesService;

    public ChangesetsServiceImpl(CommitService commitService, ApplicationPropertiesService applicationPropertiesService)
    {
        this.commitService = commitService;
        this.applicationPropertiesService = applicationPropertiesService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<YaccChangeset> getNewChangesets(Repository repository, RefChange refChange)
    {
        try
        {
            org.eclipse.jgit.lib.Repository jGitRepo = getJGitRepo(repository);

            RevWalk walk = new RevWalk(jGitRepo);

            /* Tags are different to regular commits - they're just pointers.
             * The only relevent commitId is the destination one (and even then only for
             * ADD and UPDATE).
             * We need to work out whether or not the tag is lightweight (in which case
             * its commitid is an already-existing commit that we don't want to check - 
             * it may have been made by someone else) or annotated (in which case we do
             * care.
             *
             * Stash's API to work out the tag type doesn't work (see STASH-4993)
             * and since we're using JGit anyway, just use it for the whole lot.
             */
            Set<YaccChangeset> changesets = Sets.newHashSet();

            if (refChange.getRefId().startsWith(GitRefPattern.TAGS.getPath()))
            {
                if (refChange.getType() == RefChangeType.DELETE)
                {
                    // Deletes don't leave anything to check
                    return changesets;
                }

                RevObject obj = walk.parseAny(ObjectId.fromString(refChange.getToHash()));
                if (!(obj instanceof RevTag))
                {
                    // Just a lightweight tag - nothing to check
                    return changesets;
                }

                RevTag tag = (RevTag) obj;

                PersonIdent ident = tag.getTaggerIdent();
                final String message = tag.getFullMessage();
                final YaccPerson committer = new YaccPerson(ident.getName(), ident.getEmailAddress());
                final YaccChangeset yaccChangeset = new YaccChangeset(refChange.getToHash(), committer, message, 1);

                changesets.add(yaccChangeset);
            }
            else
            {
                final ChangesetsBetweenRequest request = new ChangesetsBetweenRequest.Builder(repository)
                        .exclude(getBranches(repository))
                        .include(refChange.getToHash())
                        .build();

                // Make sure to get all of the changes
                Iterable<Changeset> changes = new PagedIterable<Changeset>(new PageProvider<Changeset>()
                {
                    @Override
                    public Page<Changeset> get(PageRequest pr)
                    {
                        return commitService.getChangesetsBetween(request, pr);
                    }
                }, 100);

                for (Changeset changeset : changes)
                {
                    final RevCommit commit = walk.parseCommit(ObjectId.fromString(changeset.getId()));

                    /* Note that we use committer, instead of author -- for most commits, these will be identical. Where
                     * this differs is if a patch *author* submits a patch (eg, consider an external contribution), and
                     * the *committer* actually applies the patch.
                     *
                     * By validating the committer here, we can allow surrogate commits on behalf of patch submitters,
                     * while still ensuring that the authenticated user is either the author *or* the committer.
                     */
                    final PersonIdent ident = commit.getCommitterIdent();
                    final String message = commit.getFullMessage();
                    final YaccPerson committer = new YaccPerson(ident.getName(), ident.getEmailAddress());
                    final YaccChangeset yaccChangeset = new YaccChangeset(changeset.getId(), committer, message, commit.getParentCount());

                    changesets.add(yaccChangeset);
                }
            }

            return changesets;
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private Set<String> getBranches(Repository repository)
    {
        try
        {
            org.eclipse.jgit.lib.Repository jGitRepo = getJGitRepo(repository);

            Set<String> refHeads = Sets.newHashSet();

            for (String ref : jGitRepo.getAllRefs().keySet())
            {
                if (ref.startsWith("refs/heads/"))
                {
                    refHeads.add(ref);
                }
            }

            return refHeads;
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private org.eclipse.jgit.lib.Repository getJGitRepo (Repository repository) throws IOException {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        File repoDir = applicationPropertiesService.getRepositoryDir(repository);
        return builder.setGitDir(repoDir).build();
    }
}
