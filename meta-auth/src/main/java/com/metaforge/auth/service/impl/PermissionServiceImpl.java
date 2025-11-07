package com.metaforge.auth.service.impl;

import com.metaforge.auth.dto.request.PermissionCreateRequest;
import com.metaforge.auth.entity.Permission;
import com.metaforge.auth.entity.Role;
import com.metaforge.auth.entity.RolePermission;
import com.metaforge.auth.repository.PermissionRepository;
import com.metaforge.auth.repository.RolePermissionRepository;
import com.metaforge.auth.repository.RoleRepository;
import com.metaforge.auth.service.PermissionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 权限服务实现
 */
@Slf4j
@Service
@Transactional
public class PermissionServiceImpl implements PermissionService {

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RolePermissionRepository rolePermissionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Permission> getAllPermissionsWithRoleCount() {
        List<Permission> permissions = permissionRepository.findAllOrderByCreatedAtDesc();

        // 为每个权限设置角色数量
        for (Permission permission : permissions) {
            Long roleCount = permissionRepository.countRolesByPermissionId(permission.getId());
            permission.setRoleCount(roleCount);
        }

        return permissions;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Permission> getAllPermissions() {
        return permissionRepository.findAllOrderByCreatedAtDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Permission> getPermissionById(Long id) {
        return permissionRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Permission> getPermissionByName(String name) {
        return permissionRepository.findByName(name);
    }

    @Override
    public Permission createPermission(PermissionCreateRequest request) {
        // 检查权限名称是否已存在
        if (existsByName(request.getName())) {
            throw new RuntimeException("权限名称已存在: " + request.getName());
        }

        Permission permission = new Permission();
        permission.setName(request.getName());
        permission.setDescription(request.getDescription());

        Permission savedPermission = permissionRepository.save(permission);
        log.info("创建权限成功: {}", savedPermission.getName());

        return savedPermission;
    }

    @Override
    public Permission updatePermission(Long id, Permission permissionDetails) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("权限不存在: " + id));

        // 检查名称是否与其他权限冲突
        if (!permission.getName().equals(permissionDetails.getName()) &&
                existsByName(permissionDetails.getName())) {
            throw new RuntimeException("权限名称已存在: " + permissionDetails.getName());
        }

        permission.setName(permissionDetails.getName());
        permission.setDescription(permissionDetails.getDescription());

        Permission updatedPermission = permissionRepository.save(permission);
        log.info("更新权限成功: {}", updatedPermission.getName());

        return updatedPermission;
    }

    @Override
    public void deletePermission(Long id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("权限不存在: " + id));

        // 检查权限是否被使用
        if (isPermissionInUse(id)) {
            throw new RuntimeException("权限正在被角色使用，无法删除");
        }

        permissionRepository.delete(permission);
        log.info("删除权限成功: {}", permission.getName());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByName(String name) {
        return permissionRepository.existsByName(name);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isPermissionInUse(Long permissionId) {
        Long roleCount = permissionRepository.countRolesByPermissionId(permissionId);
        return roleCount != null && roleCount > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Permission> searchPermissions(String keyword) {
        return permissionRepository.findByNameContainingOrDescriptionContaining(keyword);
    }

    @Override
    public void assignPermissionsToRole(Long roleId, List<Long> permissionIds) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("角色不存在: " + roleId));

        // 清除现有权限
        rolePermissionRepository.deleteByRoleId(roleId);

        // 添加新权限
        for (Long permissionId : permissionIds) {
            Permission permission = permissionRepository.findById(permissionId)
                    .orElseThrow(() -> new RuntimeException("权限不存在: " + permissionId));

            RolePermission rolePermission = new RolePermission(role, permission);
            rolePermissionRepository.save(rolePermission);
        }

        log.info("为角色 {} 分配了 {} 个权限", role.getName(), permissionIds.size());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Permission> getPermissionsByRoleId(Long roleId) {
        List<RolePermission> rolePermissions = rolePermissionRepository.findByRoleId(roleId);
        return rolePermissions.stream()
                .map(RolePermission::getPermission)
                .collect(Collectors.toList());
    }

    @Override
    public void removePermissionFromRole(Long roleId, Long permissionId) {
        rolePermissionRepository.deleteByRoleIdAndPermissionId(roleId, permissionId);
        log.info("从角色 {} 移除了权限 {}", roleId, permissionId);
    }
}