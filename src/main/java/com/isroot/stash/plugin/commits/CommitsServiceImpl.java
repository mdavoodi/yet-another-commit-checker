package com.isroot.stash.plugin.commits;

import com.atlassian.bitbucket.repository.RefChange;
import com.atlassian.bitbucket.repository.RefChangeType;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.repository.StandardRefType;
import com.atlassian.bitbucket.scm.ScmService;
import com.atlassian.bitbucket.scm.git.GitScm;
import com.atlassian.bitbucket.scm.git.command.GitScmCommandBuilder;
import com.atlassian.bitbucket.scm.git.command.revlist.GitRevListBuilder;
import com.google.common.collect.Sets;
import com.isroot.stash.plugin.YaccCommit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

/**
 * @author Sean Ford
 * @since 2013-10-26
 */
public class CommitsServiceImpl implements CommitsService {
    private final Logger log = LoggerFactory.getLogger(CommitsServiceImpl.class);

    private final ScmService scmService;

    public CommitsServiceImpl(ScmService scmService) {
        this.scmService = scmService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<YaccCommit> getNewCommits(Repository repository, RefChange refChange) {
        log.debug("getNewCommits, scmId={} refType={} refId={} toHash={} changeType={}",
                repository.getScmId(), refChange.getRef().getType(), refChange.getRef().getId(),
                refChange.getToHash(), refChange.getType());

        Set<YaccCommit> yaccCommits = Sets.newHashSet();

        if (!GitScm.ID.equals(repository.getScmId())) {
            log.warn("scmId={} not supported", repository.getScmId());

            return yaccCommits;
        }

        if (refChange.getRef().getType().equals(StandardRefType.TAG)) {
            if (refChange.getType() == RefChangeType.DELETE) {
                // Deletes don't leave anything to check
                return yaccCommits;
            }

            String hash = refChange.getToHash();
            YaccCommit commit = getGitScmCommandBuilder(repository).catFile()
                    .pretty()
                    .object(hash)
                    .build(new AnnotatedTagOutputHandler(hash)).call();

            if (commit != null) {
                log.debug("found annotated tag");
                yaccCommits.add(commit);
            }
        } else {
            GitRevListBuilder currentRevListBuilder = getGitScmCommandBuilder(repository).revList()
                    .format(RevListOutputHandler.FORMAT)
                    .rev(refChange.getToHash());

            List<YaccCommit> currentFound = currentRevListBuilder.build(new RevListOutputHandler())
                    .call();

            GitRevListBuilder allRevListBuilder = getGitScmCommandBuilder(repository).revList()
                    .format(RevListOutputHandler.FORMAT)
                    .all(true);

            List<YaccCommit> allFound = allRevListBuilder.build(new RevListOutputHandler())
                    .call();

            if (currentFound != null) {
                yaccCommits.addAll(currentFound);
            }

            if (allFound != null) {
                yaccCommits.removeAll(allFound);
            }
        }

        log.debug("found {} commits that need checking", yaccCommits.size());

        return yaccCommits;
    }

    private GitScmCommandBuilder getGitScmCommandBuilder(Repository repository) {
        return (GitScmCommandBuilder) scmService.createBuilder(repository);
    }
}
