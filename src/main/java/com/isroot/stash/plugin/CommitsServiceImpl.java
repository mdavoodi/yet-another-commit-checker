package com.isroot.stash.plugin;

import com.atlassian.stash.commit.Commit;
import com.atlassian.stash.commit.CommitService;
import com.atlassian.stash.commit.CommitsBetweenRequest;
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
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 * @author Sean Ford
 * @since 2013-10-26
 */
public class CommitsServiceImpl implements CommitsService {
    private final CommitService commitService;
    private final ApplicationPropertiesService applicationPropertiesService;

    public CommitsServiceImpl(CommitService commitService, ApplicationPropertiesService applicationPropertiesService) {
        this.commitService = commitService;
        this.applicationPropertiesService = applicationPropertiesService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<YaccCommit> getNewCommits(Repository repository, RefChange refChange) {
        try {
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
            Set<YaccCommit> yaccCommits = Sets.newHashSet();

            if (refChange.getRefId().startsWith(GitRefPattern.TAGS.getPath())) {
                if (refChange.getType() == RefChangeType.DELETE) {
                    // Deletes don't leave anything to check
                    return yaccCommits;
                }

                RevObject obj = walk.parseAny(ObjectId.fromString(refChange.getToHash()));
                if (!(obj instanceof RevTag)) {
                    // Just a lightweight tag - nothing to check
                    return yaccCommits;
                }

                RevTag tag = (RevTag) obj;

                PersonIdent ident = tag.getTaggerIdent();
                final String message = tag.getFullMessage();
                final YaccPerson committer = new YaccPerson(ident.getName(), ident.getEmailAddress());
                final YaccCommit yaccCommit = new YaccCommit(refChange.getToHash(), committer, message, 1);

                yaccCommits.add(yaccCommit);
            } else {
                final CommitsBetweenRequest request = new CommitsBetweenRequest.Builder(repository)
                        .exclude(getBranches(repository))
                        .include(refChange.getToHash())
                        .build();

                // Make sure to get all of the changes
                Iterable<Commit> commits = new PagedIterable<>(new PageProvider<Commit>() {
                    @Override
                    public Page<Commit> get(PageRequest pr) {
                        return commitService.getCommitsBetween(request, pr);
                    }
                }, 100);

                for (Commit commit : commits) {
                    final RevCommit revCommit = walk.parseCommit(ObjectId.fromString(commit.getId()));

                    /* Note that we use committer, instead of author -- for most commits, these will be identical. Where
                     * this differs is if a patch *author* submits a patch (eg, consider an external contribution), and
                     * the *committer* actually applies the patch.
                     *
                     * By validating the committer here, we can allow surrogate commits on behalf of patch submitters,
                     * while still ensuring that the authenticated user is either the author *or* the committer.
                     */
                    final PersonIdent ident = revCommit.getCommitterIdent();
                    final String message = revCommit.getFullMessage();
                    final YaccPerson committer = new YaccPerson(ident.getName(), ident.getEmailAddress());
                    final YaccCommit yaccCommit = new YaccCommit(commit.getId(), committer, message, revCommit.getParentCount());

                    yaccCommits.add(yaccCommit);
                }
            }

            return yaccCommits;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Set<String> getBranches(Repository repository) {
        try {
            org.eclipse.jgit.lib.Repository jGitRepo = getJGitRepo(repository);

            Set<String> refHeads = Sets.newHashSet();

            for (String ref : jGitRepo.getAllRefs().keySet()) {
                if (ref.startsWith("refs/heads/")) {
                    refHeads.add(ref);
                }
            }

            return refHeads;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private org.eclipse.jgit.lib.Repository getJGitRepo (Repository repository) throws IOException {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        File repoDir = applicationPropertiesService.getRepositoryDir(repository);
        return builder.setGitDir(repoDir).build();
    }
}
