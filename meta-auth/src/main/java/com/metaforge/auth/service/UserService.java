package com.metaforge.auth.service;

import com.metaforge.auth.component.ApplicationContextHelper;
import com.metaforge.auth.component.cache.UserCache;
import com.metaforge.auth.component.event.UserChangeEvent;
import com.metaforge.auth.entity.Role;
import com.metaforge.auth.entity.User;
import com.metaforge.auth.repository.RoleRepository;
import com.metaforge.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserCache userCache;

    /**
     * 根据用户ID获取用户信息
     */
    public User getUserById(Long userId) {
        User user = userCache.getCache().get(userId);
        if (user == null) {
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + userId));
            userCache.getCache().put(userId, user);
        }
        return user;
    }

    /**
     * 根据角色名称列表查找角色
     */
    private Set<Role> findRolesByNameIn(Set<String> roleNames) {
        return roleRepository.findByNameIn(roleNames);
    }

    /**
     * 根据角色名称列表查找角色
     */
    private Set<Role> findRolesByIdIn(Set<Long> roleIds) {
        return roleRepository.findByIdIn(roleIds);
    }

    /**
     * 创建新用户
     */
    @Transactional
    public User createUser(String username, String password, String email, Set<Long> roleIds) {
        log.info("开始创建用户: {}", username);

        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("用户名已存在: " + username);
        }
        if (email != null && userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("邮箱地址已被使用: " + email);
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setEnabled(true);
        user.setAccountNonExpired(true);
        user.setAccountNonLocked(true);
        user.setCredentialsNonExpired(true);

        // 分配角色
        if (roleIds != null && !roleIds.isEmpty()) {
            Set<Role> roles = findRolesByIdIn(roleIds);
            user.setRoles(roles);
            log.debug("为用户 {} 分配角色: {}", username, roles);
        } else {
            // 默认分配 USER 角色
            Role userRole = roleRepository.findByName("USER")
                    .orElseThrow(() -> new IllegalStateException("USER role not found"));
            user.getRoles().add(userRole);
            log.debug("为用户 {} 分配默认 USER 角色", username);
        }

        User savedUser = userRepository.save(user);
        log.info("用户创建成功: {}, 角色数量: {}", username, savedUser.getRoles().size());

        ApplicationContextHelper.getApplicationContext().publishEvent(
                new UserChangeEvent(new UserChangeEvent.UserChangeEventSource(savedUser.getId())));

        return savedUser;
    }

    /**
     * 更新用户信息
     */
    @Transactional
    public User updateUser(Long userId, String email, Boolean enabled, Set<String> roleNames) {
        log.info("开始更新用户: {}", userId);

        // 使用 JOIN FETCH 查询确保加载 roles
        User user = userRepository.findByIdWithRoles(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + userId));

        if (email != null && !email.equals(user.getEmail())) {
            if (userRepository.existsByEmail(email)) {
                throw new IllegalArgumentException("邮箱地址已被使用: " + email);
            }
            user.setEmail(email);
        }

        if (enabled != null) {
            user.setEnabled(enabled);
        }

        if (roleNames != null) {
            Set<Role> roles = findRolesByNameIn(roleNames);
            user.setRoles(roles);
            log.debug("更新用户 {} 的角色为: {}", user.getUsername(), roleNames);
        }

        user.setUpdatedAt(LocalDateTime.now());
        User updatedUser = userRepository.save(user);

        ApplicationContextHelper.getApplicationContext().publishEvent(
                new UserChangeEvent(new UserChangeEvent.UserChangeEventSource(
                        updatedUser.getId())));
        log.info("用户更新成功: {}, 角色数量: {}", user.getUsername(), updatedUser.getRoles().size());

        return updatedUser;
    }

    /**
     * 删除用户
     */
    @Transactional
    public void deleteUser(Long userId) {
        log.info("开始删除用户: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + userId));

        String username = user.getUsername();
        userRepository.delete(user);

        ApplicationContextHelper.getApplicationContext().publishEvent(
                new UserChangeEvent(new UserChangeEvent.UserChangeEventSource(user.getId())));
        log.info("用户删除成功: {}", username);
    }

    /**
     * 重置密码
     */
    @Transactional
    public void resetPassword(Long userId, String newPassword) {
        log.info("开始重置用户密码: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + userId));

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        log.info("用户密码重置成功: {}", user.getUsername());
    }

    /**
     * 用户自己修改密码
     */
    @Transactional
    public boolean changePassword(String username, String currentPassword, String newPassword) {
        log.info("用户 {} 请求修改密码", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + username));

        // 验证当前密码
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            log.warn("用户 {} 密码修改失败：当前密码不正确", username);
            return false;
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        log.info("用户 {} 密码修改成功", username);
        return true;
    }

    /**
     * 锁定/解锁用户
     */
    @Transactional
    public void setUserLockStatus(String username, boolean locked) {
        log.info("设置用户锁定状态: {} -> {}", username, locked ? "locked" : "unlocked");
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + username));
        userRepository.updateAccountLockStatus(username, !locked);

        ApplicationContextHelper.getApplicationContext().publishEvent(
                new UserChangeEvent(new UserChangeEvent.UserChangeEventSource(user.getId())));
    }

    /**
     * 启用/禁用用户
     */
    @Transactional
    public void setUserEnabledStatus(Long userId, boolean enabled) {
        log.info("设置用户启用状态: {} -> {}", userId, enabled ? "enabled" : "disabled");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + userId));

        user.setEnabled(enabled);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        ApplicationContextHelper.getApplicationContext().publishEvent(
                new UserChangeEvent(new UserChangeEvent.UserChangeEventSource(user.getId())));
    }

    /**
     * 记录登录成功
     */
    @Transactional
    public void recordSuccessfulLogin(String username) {
        log.debug("记录用户登录成功: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + username));

        user.recordLogin();
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        ApplicationContextHelper.getApplicationContext().publishEvent(
                new UserChangeEvent(new UserChangeEvent.UserChangeEventSource(user.getId())));
    }

    /**
     * 记录登录失败
     */
    @Transactional
    public void recordFailedLogin(String username) {
        log.debug("记录用户登录失败: {}", username);

        userRepository.incrementLoginAttempts(username);

        // 检查是否需要锁定账户（例如：连续5次失败）
        User user = userRepository.findByUsername(username).orElse(null);
        if (user != null && user.getLoginAttempts() >= 5) {
            userRepository.updateAccountLockStatus(username, false);
            log.warn("由于多次登录失败，账户已被锁定: {}", username);
        }

        ApplicationContextHelper.getApplicationContext().publishEvent(
                new UserChangeEvent(new UserChangeEvent.UserChangeEventSource(user.getId())));
    }

    /**
     * 解锁用户账户
     */
    @Transactional
    public void unlockUserAccount(String username) {
        log.info("解锁用户账户: {}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + username));
        userRepository.updateAccountLockStatus(username, true);
        userRepository.resetLoginAttempts(username);

        ApplicationContextHelper.getApplicationContext().publishEvent(
                new UserChangeEvent(new UserChangeEvent.UserChangeEventSource(user.getId())));
    }

    /**
     * 获取所有用户
     */
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        log.debug("获取所有用户列表");
        return userRepository.findAll();
    }

    /**
     * 分页获取用户
     */
    @Transactional(readOnly = true)
    public Page<User> getUsers(Pageable pageable) {
        log.debug("分页获取用户列表，页码: {}, 大小: {}", pageable.getPageNumber(), pageable.getPageSize());
        return userRepository.findAll(pageable);
    }

    /**
     * 根据用户名查找用户（不带角色）
     */
    @Transactional(readOnly = true)
    public Optional<User> findByUsernameBasic(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * 根据ID查找用户（带角色）
     */
    @Transactional(readOnly = true)
    public Optional<User> findByIdWithRoles(Long id) {
        return userRepository.findByIdWithRoles(id);
    }

    /**
     * 根据ID查找用户
     */
    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * 根据邮箱查找用户
     */
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * 检查用户名是否存在
     */
    public boolean usernameExists(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * 检查邮箱是否存在
     */
    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * 获取用户数量
     */
    @Transactional(readOnly = true)
    public long getUserCount() {
        return userRepository.count();
    }

    /**
     * 获取活跃用户数量（最近30天登录过）
     */
    @Transactional(readOnly = true)
    public long getActiveUserCount() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        // 这里需要添加相应的查询方法
        // return userRepository.countByLastLoginAfter(thirtyDaysAgo);
        return userRepository.count(); // 临时返回总用户数
    }

    /**
     * 为用户添加角色
     */
    @Transactional
    public void addRoleToUser(Long userId, String roleName) {
        log.info("为用户 {} 添加角色: {}", userId, roleName);

        User user = userRepository.findByIdWithRoles(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + userId));

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalArgumentException("角色不存在: " + roleName));

        if (!user.getRoles().contains(role)) {
            user.getRoles().add(role);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);

            ApplicationContextHelper.getApplicationContext().publishEvent(
                    new UserChangeEvent(new UserChangeEvent.UserChangeEventSource(user.getId())));

            log.info("成功为用户 {} 添加角色: {}", user.getUsername(), roleName);
        } else {
            log.debug("用户 {} 已拥有角色: {}", user.getUsername(), roleName);
        }
    }

    /**
     * 为用户移除角色
     */
    @Transactional
    public void removeRoleFromUser(Long userId, String roleName) {
        log.info("为用户 {} 移除角色: {}", userId, roleName);

        User user = userRepository.findByIdWithRoles(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + userId));

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalArgumentException("角色不存在: " + roleName));

        if (user.getRoles().contains(role)) {
            user.getRoles().remove(role);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);

            ApplicationContextHelper.getApplicationContext().publishEvent(
                    new UserChangeEvent(new UserChangeEvent.UserChangeEventSource(user.getId())));
            log.info("成功为用户 {} 移除角色: {}", user.getUsername(), roleName);
        } else {
            log.debug("用户 {} 不拥有角色: {}", user.getUsername(), roleName);
        }
    }

    /**
     * 检查用户是否拥有指定角色
     */
    @Transactional(readOnly = true)
    public boolean userHasRole(Long userId, String roleName) {
        User user = userRepository.findByIdWithRoles(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + userId));

        return user.getRoles().stream()
                .anyMatch(role -> role.getName().equals(roleName));
    }

    /**
     * 获取用户的角色列表
     */
    @Transactional(readOnly = true)
    public Set<String> getUserRoles(Long userId) {
        User user = userRepository.findByIdWithRoles(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + userId));

        return user.getRoleNames();
    }

    /**
     * 搜索用户（根据用户名或邮箱）
     */
    @Transactional(readOnly = true)
    public List<User> searchUsers(String keyword) {
        log.debug("搜索用户，关键词: {}", keyword);

        // 这里需要添加相应的查询方法
        // return userRepository.findByUsernameContainingOrEmailContaining(keyword, keyword);
        return userRepository.findAll(); // 临时返回所有用户
    }

    /**
     * 批量启用/禁用用户
     */
    @Transactional
    public void batchUpdateUserStatus(List<Long> userIds, boolean enabled) {
        log.info("批量更新用户状态: {} 个用户 -> {}", userIds.size(), enabled ? "enabled" : "disabled");

        for (Long userId : userIds) {
            try {
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + userId));

                user.setEnabled(enabled);
                user.setUpdatedAt(LocalDateTime.now());
                userRepository.save(user);

                ApplicationContextHelper.getApplicationContext().publishEvent(
                        new UserChangeEvent(new UserChangeEvent.UserChangeEventSource(user.getId())));

                log.debug("用户 {} 状态已更新为: {}", user.getUsername(), enabled ? "enabled" : "disabled");
            } catch (Exception e) {
                log.error("更新用户状态失败: {}", userId, e);
            }
        }
    }

    /**
     * 更新用户最后登录时间
     */
    @Transactional
    public void updateLastLogin(String username) {
        log.debug("更新用户最后登录时间: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + username));

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        ApplicationContextHelper.getApplicationContext().publishEvent(
                new UserChangeEvent(new UserChangeEvent.UserChangeEventSource(user.getId())));
    }

    /**
     * 验证用户凭据
     */
    public boolean validateUserCredentials(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            return passwordEncoder.matches(password, user.getPassword()) &&
                    user.getEnabled() &&
                    user.getAccountNonLocked() &&
                    user.getAccountNonExpired() &&
                    user.getCredentialsNonExpired();
        }
        return false;
    }

    /**
     * 清理过期用户（长时间未登录）
     */
    @Transactional
    public int cleanupInactiveUsers(int daysInactive) {
        log.info("清理 {} 天内未登录的非活跃用户", daysInactive);

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysInactive);
        // 这里需要添加相应的查询和删除方法
        // List<User> inactiveUsers = userRepository.findByLastLoginBeforeAndEnabledTrue(cutoffDate);
        // userRepository.deleteAll(inactiveUsers);
        // return inactiveUsers.size();

        return 0; // 临时返回0
    }
}