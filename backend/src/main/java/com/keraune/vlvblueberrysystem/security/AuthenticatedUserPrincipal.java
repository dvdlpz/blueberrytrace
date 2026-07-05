package com.keraune.vlvblueberrysystem.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/** Carries a session revision so modified accounts are invalidated on the next request. */
public final class AuthenticatedUserPrincipal implements UserDetails {
    private final String username;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean enabled;
    private final long sessionVersion;

    public AuthenticatedUserPrincipal(String username, String password, Collection<? extends GrantedAuthority> authorities, boolean enabled, long sessionVersion) {
        this.username = username;
        this.password = password;
        this.authorities = authorities;
        this.enabled = enabled;
        this.sessionVersion = sessionVersion;
    }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword() { return password; }
    @Override public String getUsername() { return username; }
    @Override public boolean isEnabled() { return enabled; }
    public long getSessionVersion() { return sessionVersion; }
}
