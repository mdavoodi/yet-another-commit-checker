package com.isroot.stash.plugin.changeset;

/**
 * Minimal metadata required to verify a changeset.
 */
public class YaccChangeset {
    /**
     * Construct a new changeset instance.
     *
     * @param id Changeset ID (eg, Git hash).
     * @param committer The changeset committer.
     * @param message Git commit message.
     * @param parentCount The number of parent commits listed in this commit.
     */
    public YaccChangeset (String id, YaccPerson committer, String message, int parentCount) {
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
    private String removeTrailingNewLine(String str)
    {
        if(str.endsWith("\n"))
        {
            str = str.substring(0, str.length() - 1);
        }

        return str;
    }

    /**
     * Return the git changeset ID.
     *
     * @return Git changeset ID.
     */
    public String getId() {
        return id;
    }


    /**
     * Return the git committer identity associated with this changeset.
     *
     * @return Git committer identity.
     */
    public YaccPerson getCommitter() {
        return committer;
    }

    /**
     * Return the commit message associated with this changeset.
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
