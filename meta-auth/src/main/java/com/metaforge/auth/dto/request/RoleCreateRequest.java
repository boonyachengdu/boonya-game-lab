package com.metaforge.auth.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 角色创建请求DTOss
 */
public class RoleCreateRequest {

    @NotBlank(message = "角色名称不能为空")
    @Size(min = 2, max = 50, message = "角色名称长度必须在2-50个字符之间")
    private String name;

    @NotBlank(message = "角色代码不能为空")
    @Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "角色代码必须以大写字母开头，只能包含大写字母、数字和下划线")
    @Size(max = 50, message = "角色代码长度不能超过50个字符")
    private String code;

    @Size(max = 200, message = "角色描述长度不能超过200个字符")
    private String description;

    private String status = "ACTIVE"; // 默认状态为启用

    // 构造方法
    public RoleCreateRequest() {}

    public RoleCreateRequest(String name, String code) {
        this.name = name;
        this.code = code;
    }

    public RoleCreateRequest(String name, String code, String description) {
        this.name = name;
        this.code = code;
        this.description = description;
    }

    // Getter和Setter
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "RoleCreateRequest{" +
                "name='" + name + '\'' +
                ", code='" + code + '\'' +
                ", description='" + description + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
