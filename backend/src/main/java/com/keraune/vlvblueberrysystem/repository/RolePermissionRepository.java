package com.keraune.vlvblueberrysystem.repository;

import com.keraune.vlvblueberrysystem.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {
    List<RolePermission> findByRoleIdOrderByModuleKeyAscActionKeyAsc(Long roleId);
    void deleteByRoleId(Long roleId);
}
