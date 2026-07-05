package com.keraune.vlvblueberrysystem.security;

import com.keraune.vlvblueberrysystem.service.RolePermissionService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Evaluates stored role permissions at request time. */
@Component("permissionGuard")
public class PermissionGuard {
    private final RolePermissionService permissions;

    public PermissionGuard(RolePermissionService permissions) {
        this.permissions = permissions;
    }

    public boolean allows(String module, String action) {
        return permissions.allows(authentication(), module, action);
    }

    public boolean allowsAny(String module, String... actions) {
        return permissions.allowsAny(authentication(), module, actions);
    }

    private Authentication authentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}
