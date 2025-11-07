package com.metaforge.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 权限创建请求DTO
 */
public class PermissionCreateRequest {

    @NotBlank(message = "权限名称不能为空")
    @Size(min = 2, max = 100, message = "权限名称长度必须在2-100个字符之间")
    private String name;

    @Size(max = 255, message = "权限描述长度不能超过255个字符")
    private String description;

    // 构造方法
    public PermissionCreateRequest() {}

    public PermissionCreateRequest(String name) {
        this.name = name;
    }

    public PermissionCreateRequest(String name, String description) {
        this.name = name;
        this.description = description;
    }

    // Getter和Setter
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "PermissionCreateRequest{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}