package com.ai.server.security;

import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

/**
 * 安全用户信息（实现Spring Security的UserDetails）
 */
@Getter
public class SecurityUser implements UserDetails {

    private final UUID uuidId;
    private final String email;
    private final String password;
    private final String firstName;
    private final String lastName;
    private final String phone;
    private final boolean enabled;
    private final boolean activated;
    private final Collection<GrantedAuthority> authorities;

    @Builder
    public SecurityUser(UUID uuidId, String email, String password, String firstName,
                        String lastName, String phone, boolean enabled, boolean activated,
                        String role) {
        this.uuidId = uuidId;
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.enabled = enabled;
        this.activated = activated;
        this.authorities = Collections.singletonList(new SimpleGrantedAuthority(role != null ? role : "ROLE_USER"));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
