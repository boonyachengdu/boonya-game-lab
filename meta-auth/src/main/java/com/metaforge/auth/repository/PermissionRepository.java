package com.metaforge.auth.repository;

import com.metaforge.auth.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 权限数据访问接口
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    Optional<Permission> findByName(String name);

    boolean existsByName(String name);

    @Query("SELECT COUNT(rp.role.id) FROM RolePermission rp WHERE rp.permission.id = :permissionId")
    Long countRolesByPermissionId(@Param("permissionId") Long permissionId);

    @Query("SELECT p FROM Permission p ORDER BY p.createdAt DESC")
    List<Permission> findAllOrderByCreatedAtDesc();

    @Query("SELECT p FROM Permission p WHERE p.name LIKE %:keyword% OR p.description LIKE %:keyword%")
    List<Permission> findByNameContainingOrDescriptionContaining(@Param("keyword") String keyword);
}