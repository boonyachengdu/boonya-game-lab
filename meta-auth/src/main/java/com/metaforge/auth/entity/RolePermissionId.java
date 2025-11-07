package com.metaforge.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

public /**
 * 角色权限关联主键
 */
@Embeddable
class RolePermissionId implements java.io.Serializable {

    @Column(name = "role_id")
    private Long roleId;

    @Column(name = "permission_id")
    private Long permissionId;

    // 构造方法
    public RolePermissionId() {
    }

    public RolePermissionId(Long roleId, Long permissionId) {
        this.roleId = roleId;
        this.permissionId = permissionId;
    }

    // Getter和Setter
    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public Long getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(Long permissionId) {
        this.permissionId = permissionId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RolePermissionId that = (RolePermissionId) o;

        if (!roleId.equals(that.roleId)) return false;
        return permissionId.equals(that.permissionId);
    }

    @Override
    public int hashCode() {
        int result = roleId.hashCode();
        result = 31 * result + permissionId.hashCode();
        return result;
    }
}
