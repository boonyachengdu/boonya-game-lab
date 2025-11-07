package com.metaforge.auth.controller;

import com.google.common.collect.Lists;
import com.metaforge.auth.dto.request.PermissionCreateRequest;
import com.metaforge.auth.entity.Permission;
import com.metaforge.auth.service.PermissionService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

/**
 * 权限管理控制器
 */
@Slf4j
@Controller
@RequestMapping("/admin/permissions")
public class PermissionController {

    @Autowired
    private PermissionService permissionService;

    /**
     * 权限管理页面
     */
    @GetMapping
    public String permissionManagement(Model model) {
        try {
            List<Permission> permissions = permissionService.getAllPermissionsWithRoleCount();
            model.addAttribute("permissions", Optional.ofNullable(permissions).orElse(Lists.newArrayList()));
        } catch (Exception e) {
            log.error("加载权限列表失败", e);
            model.addAttribute("error", "加载权限列表失败: " + e.getMessage());
        }

        return "admin/permission";
    }

    /**
     * 创建权限
     */
    @PostMapping
    public String createPermission(@Valid PermissionCreateRequest request,
                                   BindingResult result,
                                   RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "表单验证失败");
            return "redirect:/admin/permissions";
        }

        try {
            permissionService.createPermission(request);
            redirectAttributes.addFlashAttribute("success", "权限创建成功");
        } catch (Exception e) {
            log.error("创建权限失败", e);
            redirectAttributes.addFlashAttribute("error", "权限创建失败: " + e.getMessage());
        }

        return "redirect:/admin/permissions";
    }

    /**
     * 更新权限
     */
    @PostMapping("/{id}")
    public String updatePermission(@PathVariable Long id,
                                   @Valid PermissionCreateRequest request,
                                   BindingResult result,
                                   RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "表单验证失败");
            return "redirect:/admin/permissions";
        }

        try {
            Permission permission = new Permission();
            permission.setName(request.getName());
            permission.setDescription(request.getDescription());

            permissionService.updatePermission(id, permission);
            redirectAttributes.addFlashAttribute("success", "权限更新成功");
        } catch (Exception e) {
            log.error("更新权限失败", e);
            redirectAttributes.addFlashAttribute("error", "权限更新失败: " + e.getMessage());
        }

        return "redirect:/admin/permissions";
    }

    /**
     * 删除权限
     */
    @PostMapping("/{id}/delete")
    public String deletePermission(@PathVariable Long id,
                                   RedirectAttributes redirectAttributes) {
        try {
            permissionService.deletePermission(id);
            redirectAttributes.addFlashAttribute("success", "权限删除成功");
        } catch (Exception e) {
            log.error("删除权限失败", e);
            redirectAttributes.addFlashAttribute("error", "权限删除失败: " + e.getMessage());
        }

        return "redirect:/admin/permissions";
    }

    /**
     * 搜索权限
     */
    @GetMapping("/search")
    public String searchPermissions(@RequestParam String keyword,
                                    Model model) {
        try {
            List<Permission> permissions = permissionService.searchPermissions(keyword);
            model.addAttribute("permissions", permissions);
            model.addAttribute("searchKeyword", keyword);
        } catch (Exception e) {
            log.error("搜索权限失败", e);
            model.addAttribute("error", "搜索失败: " + e.getMessage());
        }

        return "admin/permission";
    }

    /**
     * 获取权限详情（API接口）
     */
    @GetMapping("/{id}/detail")
    @ResponseBody
    public Permission getPermissionDetail(@PathVariable Long id) {
        return permissionService.getPermissionById(id)
                .orElseThrow(() -> new RuntimeException("权限不存在: " + id));
    }
}