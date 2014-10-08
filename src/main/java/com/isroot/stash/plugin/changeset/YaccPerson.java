package com.isroot.stash.plugin.changeset;

import javax.annotation.Nonnull;

/**
 * A git person identity.
 */
public class YaccPerson {

    /** Name of person. */
    private final String name;

    /** E-mail address of person */
    private final String emailAddress;

    /**
     * Construct a new person value.
     *
     * @param name The name of the person.
     * @param emailAddress The e-mail address of the person.
     */
    public YaccPerson(@Nonnull String name, @Nonnull String emailAddress) {
        this.name = name;
        this.emailAddress = emailAddress;
    }

    /**
     * Return the name associated with this identity.
     *
     * @return Name of person.
     */
    @Nonnull
    public String getName() {
        return name;
    }

    /**
     * Return the e-mail address associated with this identity.
     *
     * @return E-mail of person.
     */
    @Nonnull
    public String getEmailAddress() {
        return emailAddress;
    }
}
