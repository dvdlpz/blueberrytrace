package com.keraune.vlvblueberrysystem.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Persisted permission assigned to one of the fixed corporate roles.
 * The role catalog is controlled by RolePermissionPolicy; this entity only stores
 * the selections approved by an administrator.
 */
@Entity
@Table(name = "role_permissions", uniqueConstraints = @UniqueConstraint(
        name = "uk_role_permissions_role_module_action",
        columnNames = {"role_id", "module_key", "action_key"}
))
public class RolePermission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(name = "module_key", nullable = false, length = 80)
    private String moduleKey;

    @Column(name = "action_key", nullable = false, length = 80)
    private String actionKey;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public String getModuleKey() { return moduleKey; }
    public void setModuleKey(String moduleKey) { this.moduleKey = moduleKey; }
    public String getActionKey() { return actionKey; }
    public void setActionKey(String actionKey) { this.actionKey = actionKey; }
}
