package com.metaforge.auth.repository;

import com.metaforge.auth.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(String name);

    Boolean existsByName(String name);

    @Query("SELECT r FROM Role r WHERE r.name IN :roleNames")
    Set<Role> findByNameIn(@Param("roleNames") Set<String> roleNames);

    @Query("SELECT r FROM Role r WHERE r.id IN :roleIds")
    Set<Role> findByIdIn(@Param("roleIds") Set<Long> roleIds);

    // 新增：查找用户的所有角色
    @Query("SELECT r FROM Role r JOIN r.users u WHERE u.id = :userId")
    Set<Role> findByUserId(@Param("userId") Long userId);
}