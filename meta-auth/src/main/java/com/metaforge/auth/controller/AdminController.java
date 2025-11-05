package com.metaforge.auth.controller;

import com.metaforge.auth.dto.request.RoleCreateRequest;
import com.metaforge.auth.dto.request.UserCreateRequest;
import com.metaforge.auth.entity.Role;
import com.metaforge.auth.entity.User;
import com.metaforge.auth.service.RoleService;
import com.metaforge.auth.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;

    private final RoleService roleService;

    /**
     * 用户管理页面
     */
    @GetMapping("/users")
    public String userManagement(@RequestParam(defaultValue = "1") int page,
                                 @RequestParam(defaultValue = "10") int size,
                                 Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        model.addAttribute("username", authentication.getName());
        try {
            Pageable pageable = Pageable.ofSize(size).withPage(page - 1);
            Page<User> userPage = userService.getUsers(pageable);
            List<Role> roles = roleService.getAllRoles();

            model.addAttribute("users", userPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", userPage.getTotalPages());
            model.addAttribute("roles", roles);
        }catch (Exception e){
            // 处理异常，确保模型属性不为空
            model.addAttribute("users", Collections.emptyList());
            model.addAttribute("currentPage", 1);
            model.addAttribute("totalPages", 1);
            model.addAttribute("totalElements", 0L);
            model.addAttribute("roles", Collections.emptyList());
            model.addAttribute("error", "加载用户数据失败: " + e.getMessage());
        }

        return "admin/user";
    }

    /**
     * 角色管理页面
     */
    @GetMapping("/roles")
    public String roleManagement(Model model) {
        List<Role> roles = roleService.getAllRolesWithUserCount();
        model.addAttribute("roles", roles);
        return "admin/role";
    }

    /**
     * 添加用户
     */
    @PostMapping("/users")
    public String addUser(@Valid UserCreateRequest request,
                          BindingResult result,
                          RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "表单验证失败");
            return "redirect:/admin/users";
        }

        try {
            userService.createUser(request.getUsername(), request.getPassword(), request.getEmail(), request.getRoleIds());
            redirectAttributes.addFlashAttribute("success", "用户创建成功");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "用户创建失败: " + e.getMessage());
        }

        return "redirect:/admin/users";
    }

    /**
     * 添加角色
     */
    @PostMapping("/roles")
    public String addRole(@Valid RoleCreateRequest request,
                          BindingResult result,
                          RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "表单验证失败");
            return "redirect:/admin/roles";
        }

        try {
            roleService.createRole(request);
            redirectAttributes.addFlashAttribute("success", "角色创建成功");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "角色创建失败: " + e.getMessage());
        }

        return "redirect:/admin/roles";
    }

    /**
     * 启用/禁用用户
     */
    @PostMapping("/users/{id}/{action}")
    public String toggleUserStatus(@PathVariable Long id,
                                   @PathVariable String action,
                                   RedirectAttributes redirectAttributes) {
        try {
            if ("enable".equals(action)) {
                userService.setUserEnabledStatus(id, true);
                redirectAttributes.addFlashAttribute("success", "用户启用成功");
            } else if ("disable".equals(action)) {
                userService.setUserEnabledStatus(id, false);
                redirectAttributes.addFlashAttribute("success", "用户禁用成功");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "操作失败: " + e.getMessage());
        }

        return "redirect:/admin/users";
    }

    @GetMapping("/users/{id}")
    public User getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }
}