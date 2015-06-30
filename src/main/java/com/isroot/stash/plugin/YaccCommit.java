package com.isroot.stash.plugin;

/**
 * Minimal metadata required to verify a commit.
 */
public class YaccCommit {
    /**
     * Construct a new commit instance.
     *
     * @param id Commit ID (eg, Git hash).
     * @param committer The committer.
     * @param message Git commit message.
     * @param parentCount The number of parent commits listed in this commit.
     */
    public YaccCommit (String id, YaccPerson committer, String message, int parentCount) {
        this.id = id;
        this.committer = committer;
        this.message = removeTrailingNewLine(message);
        this.parentCount = parentCount;
    }

    /**
     * sford: Removing the trailing newline is necessary after changing to JGit to get commit information to fix the
     * stash author name linking bug (see commit 3b5e8e0). The commit message returned by JGit has a trailing newline
     * which wasn't present when using the Stash API to get the message. This broke the commit message regex, so, this
     * was added to maintain the previous behavior.
     */
    private String removeTrailingNewLine(String str) {
        if(str.endsWith("\n")) {
            str = str.substring(0, str.length() - 1);
        }

        return str;
    }

    /**
     * Return the git commit ID.
     *
     * @return Git commit ID.
     */
    public String getId() {
        return id;
    }


    /**
     * Return the git committer identity associated with this commit.
     *
     * @return Git committer identity.
     */
    public YaccPerson getCommitter() {
        return committer;
    }

    /**
     * Return the commit message associated with this commit.
     *
     * @return Commit message.
     */
    public String getMessage() {
        return message;
    }

    /**
     * Return the number of parent commits listed in this commit.
     *
     * @return Number of parents.
     */
    public int getParentCount() {
        return parentCount;
    }

    private final String id;
    private final YaccPerson committer;
    private final String message;
    private final int parentCount;
}
