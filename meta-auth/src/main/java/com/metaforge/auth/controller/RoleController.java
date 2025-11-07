package com.metaforge.auth.controller;

import com.metaforge.auth.dto.request.RoleCreateRequest;
import com.metaforge.auth.entity.Role;
import com.metaforge.auth.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    /**
     * 角色管理页面
     */
    @GetMapping
    public String roleManagement(Model model) {
        List<Role> roles = roleService.getAllRolesWithUserCount();
        model.addAttribute("roles", roles);
        return "admin/role";
    }

    /**
     * 添加角色
     */
    @PostMapping
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
}