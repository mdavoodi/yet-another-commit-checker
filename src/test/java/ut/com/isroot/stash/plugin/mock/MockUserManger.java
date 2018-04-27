package ut.com.isroot.stash.plugin.mock;

import com.atlassian.sal.api.user.UserKey;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import com.atlassian.sal.api.user.UserResolutionException;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.security.Principal;

/**
 * @author Benjamin Evenson
 * @since 2018-04-27
 */
public class MockUserManger implements UserManager {

    public class MockUserProfile implements  UserProfile {

        @Override
        public UserKey getUserKey() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getUsername() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getFullName() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getEmail() {
            throw new UnsupportedOperationException();
        }

        @Override
        public URI getProfilePictureUri(int i, int i1) {
            throw new UnsupportedOperationException();
        }

        @Override
        public URI getProfilePictureUri() {
            throw new UnsupportedOperationException();
        }

        @Override
        public URI getProfilePageUri() {
            throw new UnsupportedOperationException();
        }
    }

    @Nullable
    @Override
    public String getRemoteUsername() {
        throw new UnsupportedOperationException();
    }

    @Nullable
    @Override
    public UserProfile getRemoteUser() {
        return new MockUserProfile();
    }

    @Nullable
    @Override
    public UserKey getRemoteUserKey() {
        return new UserKey("mockKey");
    }

    @Nullable
    @Override
    public String getRemoteUsername(HttpServletRequest httpServletRequest) {
        throw new UnsupportedOperationException();
    }

    @Nullable
    @Override
    public UserProfile getRemoteUser(HttpServletRequest httpServletRequest) {
        return new MockUserProfile();
    }

    @Nullable
    @Override
    public UserKey getRemoteUserKey(HttpServletRequest httpServletRequest) {
        throw new UnsupportedOperationException();
    }

    @Nullable
    @Override
    public UserProfile getUserProfile(@Nullable String s) {
        throw new UnsupportedOperationException();
    }

    @Nullable
    @Override
    public UserProfile getUserProfile(@Nullable UserKey userKey) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isUserInGroup(@Nullable String s, @Nullable String s1) {
        return false;
    }

    @Override
    public boolean isUserInGroup(@Nullable UserKey userKey, @Nullable String s) {
        return false;
    }

    @Override
    public boolean isSystemAdmin(@Nullable String s) {
        return false;
    }

    @Override
    public boolean isSystemAdmin(@Nullable UserKey userKey) {
        return true;
    }

    @Override
    public boolean isAdmin(@Nullable String s) {
        return false;
    }

    @Override
    public boolean isAdmin(@Nullable UserKey userKey) {
        return false;
    }

    @Override
    public boolean authenticate(String s, String s1) {
        return false;
    }

    @Nullable
    @Override
    public Principal resolve(String s) throws UserResolutionException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterable<String> findGroupNamesByPrefix(String s, int i, int i1) {
        throw new UnsupportedOperationException();
    }
}
