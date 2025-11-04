package com.metaforge.auth.service;

import com.metaforge.auth.dto.request.RoleCreateRequest;
import com.metaforge.auth.entity.Role;
import com.metaforge.auth.repository.RoleRepository;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class RoleService {
    @Resource
    private RoleRepository roleRepository;

    public Role getRoleById(Long id) {
        return roleRepository.findById(id).orElse(null);
    }

    public Role getRoleByName(String name) {
        return roleRepository.findByName(name).orElse(null);
    }

    public boolean existsByName(String name) {
        return roleRepository.existsByName(name);
    }

    public Set<Role> getRolesByNameIn(Set<String> roleNames) {
        return roleRepository.findByNameIn(roleNames);
    }

    public Set<Role> getRolesByIdIn(Set<Long> roleIds) {
        return roleRepository.findByIdIn(roleIds);
    }

    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    public List<Role> getAllRolesWithUserCount() {
        return roleRepository.findAll();
    }

    public void createRole(RoleCreateRequest request) {
    }
}
