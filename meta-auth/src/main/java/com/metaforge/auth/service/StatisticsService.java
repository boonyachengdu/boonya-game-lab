package com.metaforge.auth.service;

import com.metaforge.auth.repository.RoleRepository;
import com.metaforge.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public long getTotalUsers() {
        return userRepository.count();
    }

    public long getTotalRoles() {
        return roleRepository.count();
    }

    public long getTotalEnabledUsers() {
        return userRepository.countByEnabled(true);
    }

    public long getTotalDisabledUsers() {
        return userRepository.countByEnabled(false);
    }

    public long getTotalLockedUsers() {
        return userRepository.countByAccountNonLocked(false);
    }

    public long getTodayLogins() {
        return userRepository.countByLastLoginAfter(LocalDateTime.now().minusDays(1));
    }
}
