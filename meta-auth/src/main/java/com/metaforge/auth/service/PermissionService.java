package com.metaforge.auth.service;

import com.metaforge.auth.dto.request.PermissionCreateRequest;
import com.metaforge.auth.entity.Permission;

import java.util.List;
import java.util.Optional;

/**
 * 权限服务接口
 */
public interface PermissionService {

    /**
     * 获取所有权限（包含角色数量）
     */
    List<Permission> getAllPermissionsWithRoleCount();

    /**
     * 获取所有权限
     */
    List<Permission> getAllPermissions();

    /**
     * 根据ID获取权限
     */
    Optional<Permission> getPermissionById(Long id);

    /**
     * 根据名称获取权限
     */
    Optional<Permission> getPermissionByName(String name);

    /**
     * 创建权限
     */
    Permission createPermission(PermissionCreateRequest request);

    /**
     * 更新权限
     */
    Permission updatePermission(Long id, Permission permission);

    /**
     * 删除权限
     */
    void deletePermission(Long id);

    /**
     * 检查权限名称是否存在
     */
    boolean existsByName(String name);

    /**
     * 检查权限是否被角色使用
     */
    boolean isPermissionInUse(Long permissionId);

    /**
     * 搜索权限
     */
    List<Permission> searchPermissions(String keyword);

    /**
     * 为角色分配权限
     */
    void assignPermissionsToRole(Long roleId, List<Long> permissionIds);

    /**
     * 获取角色的权限列表
     */
    List<Permission> getPermissionsByRoleId(Long roleId);

    /**
     * 从角色移除权限
     */
    void removePermissionFromRole(Long roleId, Long permissionId);
}