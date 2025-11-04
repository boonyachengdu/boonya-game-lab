package com.metaforge.auth.repository;

import com.metaforge.auth.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // 基本查询（可能不会加载roles）
    Optional<User> findByUsername(String username);

    // 新增：使用 EntityGraph 强制加载 roles
    @EntityGraph(attributePaths = "roles")
    @Query("SELECT u FROM User u WHERE u.username = :username")
    Optional<User> findByUsernameWithRoles(@Param("username") String username);

    // 新增：使用 EntityGraph 强制加载 roles（通过ID）
    @EntityGraph(attributePaths = "roles")
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdWithRoles(@Param("id") Long id);

    Optional<User> findByEmail(String email);

    Boolean existsByUsername(String username);

    Boolean existsByEmail(String email);

    @Modifying
    @Query("UPDATE User u SET u.lastLogin = :lastLogin WHERE u.id = :userId")
    void updateLastLogin(@Param("userId") Long userId, @Param("lastLogin") LocalDateTime lastLogin);

    @Modifying
    @Query("UPDATE User u SET u.loginAttempts = u.loginAttempts + 1 WHERE u.username = :username")
    void incrementLoginAttempts(@Param("username") String username);

    @Modifying
    @Query("UPDATE User u SET u.loginAttempts = 0 WHERE u.username = :username")
    void resetLoginAttempts(@Param("username") String username);

    @Modifying
    @Query("UPDATE User u SET u.accountNonLocked = :locked WHERE u.username = :username")
    void updateAccountLockStatus(@Param("username") String username, @Param("locked") Boolean locked);

    /**
     * 根据最后登录时间查找用户
     */
    @Query("SELECT u FROM User u WHERE u.lastLogin < :cutoffDate AND u.enabled = true")
    List<User> findByLastLoginBeforeAndEnabledTrue(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * 统计启用状态的用户数量
     */
    long countByEnabled(boolean enabled);

    /**
     * 统计锁定状态的用户数量
     */
    long countByAccountNonLocked(boolean accountNonLocked);

    /**
     * 根据用户名或邮箱搜索用户
     */
    @Query("SELECT u FROM User u WHERE u.username LIKE %:keyword% OR u.email LIKE %:keyword%")
    List<User> findByUsernameContainingOrEmailContaining(@Param("keyword") String keyword);

    /**
     * 统计最近活跃用户数量
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.lastLogin > :cutoffDate")
    long countByLastLoginAfter(@Param("cutoffDate") LocalDateTime cutoffDate);
}