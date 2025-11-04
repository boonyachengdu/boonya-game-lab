/**
 * MetaAuth 管理后台 JavaScript
 */

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', function() {
    // 初始化工具提示
    const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    const tooltipList = tooltipTriggerList.map(function (tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl);
    });

    // 初始化搜索功能
    initSearch();

    // 初始化筛选功能
    initFilters();

    // 初始化表单验证
    initFormValidation();
});

/**
 * 初始化搜索功能
 */
function initSearch() {
    const searchBtn = document.getElementById('searchBtn');
    const searchInput = document.getElementById('searchInput');

    if (searchBtn && searchInput) {
        searchBtn.addEventListener('click', performSearch);
        searchInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                performSearch();
            }
        });
    }
}

/**
 * 执行搜索
 */
function performSearch() {
    const searchInput = document.getElementById('searchInput');
    const searchTerm = searchInput.value.trim();

    if (searchTerm) {
        // 在实际应用中，这里应该发送AJAX请求到后端
        // 这里简化为在前端过滤
        filterTable(searchTerm);
    } else {
        // 显示所有行
        const rows = document.querySelectorAll('#userTable tbody tr, #roleTable tbody tr');
        rows.forEach(row => row.style.display = '');
    }
}

/**
 * 初始化筛选功能
 */
function initFilters() {
    const statusFilter = document.getElementById('statusFilter');
    const roleFilter = document.getElementById('roleFilter');

    if (statusFilter) {
        statusFilter.addEventListener('change', applyFilters);
    }

    if (roleFilter) {
        roleFilter.addEventListener('change', applyFilters);
    }
}

/**
 * 应用筛选条件
 */
function applyFilters() {
    // 在实际应用中，这里应该发送AJAX请求到后端
    console.log('应用筛选条件');
}

/**
 * 初始化表单验证
 */
function initFormValidation() {
    const addUserForm = document.getElementById('addUserForm');
    const addRoleForm = document.getElementById('addRoleForm');

    if (addUserForm) {
        addUserForm.addEventListener('submit', validateUserForm);
    }

    if (addRoleForm) {
        addRoleForm.addEventListener('submit', validateRoleForm);
    }
}

/**
 * 验证用户表单
 */
function validateUserForm(event) {
    const password = document.getElementById('password').value;
    const confirmPassword = document.getElementById('confirmPassword').value;

    if (password !== confirmPassword) {
        event.preventDefault();
        alert('密码和确认密码不匹配！');
        return false;
    }

    if (password.length < 6) {
        event.preventDefault();
        alert('密码长度至少6位！');
        return false;
    }

    return true;
}

/**
 * 验证角色表单
 */
function validateRoleForm(event) {
    const roleCode = document.getElementById('roleCode').value;

    if (!/^[A-Z][A-Z0-9_]*$/.test(roleCode)) {
        event.preventDefault();
        alert('角色代码必须以大写字母开头，只能包含大写字母、数字和下划线！');
        return false;
    }

    return true;
}

/**
 * 用户管理相关函数
 */

function editUser(userId) {
    console.log('编辑用户:', userId);
    // 在实际应用中，这里应该打开编辑模态框或跳转到编辑页面
    alert('编辑用户功能: ' + userId);
}

function viewUser(userId) {
    console.log('查看用户:', userId);
    // 跳转到用户详情页面
    window.location.href = `/admin/users/${userId}`;
}

function disableUser(userId) {
    if (confirm('确定要禁用这个用户吗？')) {
        console.log('禁用用户:', userId);
        // 发送禁用请求
        fetch(`/admin/users/${userId}/disable`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-CSRF-TOKEN': getCsrfToken()
            }
        }).then(response => {
            if (response.ok) {
                location.reload();
            } else {
                alert('操作失败！');
            }
        });
    }
}

function enableUser(userId) {
    if (confirm('确定要启用这个用户吗？')) {
        console.log('启用用户:', userId);
        // 发送启用请求
        fetch(`/admin/users/${userId}/enable`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-CSRF-TOKEN': getCsrfToken()
            }
        }).then(response => {
            if (response.ok) {
                location.reload();
            } else {
                alert('操作失败！');
            }
        });
    }
}

function deleteUser(userId, username) {
    if (confirm(`确定要删除用户 "${username}" 吗？此操作不可恢复！`)) {
        console.log('删除用户:', userId);
        // 发送删除请求
        fetch(`/admin/users/${userId}`, {
            method: 'DELETE',
            headers: {
                'Content-Type': 'application/json',
                'X-CSRF-TOKEN': getCsrfToken()
            }
        }).then(response => {
            if (response.ok) {
                location.reload();
            } else {
                alert('删除失败！');
            }
        });
    }
}

/**
 * 角色管理相关函数
 */

function editRole(roleId) {
    console.log('编辑角色:', roleId);
    // 在实际应用中，这里应该打开编辑模态框
    alert('编辑角色功能: ' + roleId);
}

function viewRole(roleId) {
    console.log('查看角色:', roleId);
    // 跳转到角色详情页面
    window.location.href = `/admin/roles/${roleId}`;
}

function managePermissions(roleId) {
    console.log('管理角色权限:', roleId);

    // 显示权限管理模态框
    const permissionModal = new bootstrap.Modal(document.getElementById('permissionModal'));

    // 加载权限数据
    loadRolePermissions(roleId);

    // 设置当前角色ID
    document.getElementById('permissionModal').dataset.roleId = roleId;

    permissionModal.show();
}

function disableRole(roleId) {
    if (confirm('确定要禁用这个角色吗？')) {
        console.log('禁用角色:', roleId);
        // 发送禁用请求
        fetch(`/admin/roles/${roleId}/disable`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-CSRF-TOKEN': getCsrfToken()
            }
        }).then(response => {
            if (response.ok) {
                location.reload();
            } else {
                alert('操作失败！');
            }
        });
    }
}

function enableRole(roleId) {
    if (confirm('确定要启用这个角色吗？')) {
        console.log('启用角色:', roleId);
        // 发送启用请求
        fetch(`/admin/roles/${roleId}/enable`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-CSRF-TOKEN': getCsrfToken()
            }
        }).then(response => {
            if (response.ok) {
                location.reload();
            } else {
                alert('操作失败！');
            }
        });
    }
}

function deleteRole(roleId, roleName) {
    if (confirm(`确定要删除角色 "${roleName}" 吗？此操作不可恢复！`)) {
        console.log('删除角色:', roleId);
        // 发送删除请求
        fetch(`/admin/roles/${roleId}`, {
            method: 'DELETE',
            headers: {
                'Content-Type': 'application/json',
                'X-CSRF-TOKEN': getCsrfToken()
            }
        }).then(response => {
            if (response.ok) {
                location.reload();
            } else {
                alert('删除失败！');
            }
        });
    }
}

/**
 * 加载角色权限
 */
function loadRolePermissions(roleId) {
    const permissionTree = document.getElementById('permissionTree');

    // 显示加载中
    permissionTree.innerHTML = `
        <div class="text-center">
            <div class="spinner-border" role="status">
                <span class="visually-hidden">加载中...</span>
            </div>
        </div>
    `;

    // 模拟加载权限数据
    setTimeout(() => {
        // 在实际应用中，这里应该从后端获取权限数据
        const permissions = [
            {
                category: '用户管理',
                permissions: [
                    { id: 1, name: '查看用户', code: 'USER_READ' },
                    { id: 2, name: '创建用户', code: 'USER_CREATE' },
                    { id: 3, name: '编辑用户', code: 'USER_UPDATE' },
                    { id: 4, name: '删除用户', code: 'USER_DELETE' }
                ]
            },
            {
                category: '角色管理',
                permissions: [
                    { id: 5, name: '查看角色', code: 'ROLE_READ' },
                    { id: 6, name: '创建角色', code: 'ROLE_CREATE' },
                    { id: 7, name: '编辑角色', code: 'ROLE_UPDATE' },
                    { id: 8, name: '删除角色', code: 'ROLE_DELETE' }
                ]
            },
            {
                category: '系统设置',
                permissions: [
                    { id: 9, name: '系统配置', code: 'SYSTEM_CONFIG' },
                    { id: 10, name: '日志查看', code: 'LOG_READ' }
                ]
            }
        ];

        renderPermissionTree(permissions);
    }, 1000);
}

/**
 * 渲染权限树
 */
function renderPermissionTree(permissions) {
    const permissionTree = document.getElementById('permissionTree');
    let html = '';

    permissions.forEach(category => {
        html += `
            <div class="permission-category">
                ${category.category}
            </div>
        `;

        category.permissions.forEach(permission => {
            html += `
                <div class="permission-item">
                    <div class="form-check">
                        <input class="form-check-input permission-checkbox" 
                               type="checkbox" 
                               value="${permission.id}" 
                               id="perm_${permission.id}">
                        <label class="form-check-label" for="perm_${permission.id}">
                            ${permission.name} 
                            <small class="text-muted">(${permission.code})</small>
                        </label>
                    </div>
                </div>
            `;
        });
    });

    permissionTree.innerHTML = html;
}

/**
 * 保存权限
 */
function savePermissions() {
    const roleId = document.getElementById('permissionModal').dataset.roleId;
    const selectedPermissions = [];

    document.querySelectorAll('.permission-checkbox:checked').forEach(checkbox => {
        selectedPermissions.push(checkbox.value);
    });

    console.log('保存权限:', roleId, selectedPermissions);

    // 在实际应用中，这里应该发送权限保存请求
    alert('权限保存功能: 角色ID=' + roleId + ', 权限=' + selectedPermissions.join(','));

    // 关闭模态框
    const permissionModal = bootstrap.Modal.getInstance(document.getElementById('permissionModal'));
    permissionModal.hide();
}

/**
 * 表格过滤功能
 */
function filterTable(searchTerm) {
    const table = document.querySelector('#userTable tbody') || document.querySelector('#roleTable tbody');
    if (!table) return;

    const rows = table.querySelectorAll('tr');
    const searchLower = searchTerm.toLowerCase();

    rows.forEach(row => {
        const text = row.textContent.toLowerCase();
        if (text.includes(searchLower)) {
            row.style.display = '';
        } else {
            row.style.display = 'none';
        }
    });
}

/**
 * 获取CSRF令牌
 */
function getCsrfToken() {
    return document.querySelector('meta[name="_csrf"]')?.getAttribute('content') || '';
}

/**
 * 显示成功消息
 */
function showSuccess(message) {
    // 在实际应用中，可以使用Toast通知
    alert('成功: ' + message);
}

/**
 * 显示错误消息
 */
function showError(message) {
    // 在实际应用中，可以使用Toast通知
    alert('错误: ' + message);
}