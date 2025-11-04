-- 插入角色
INSERT INTO roles (name, description) VALUES
                                          ('USER', '普通用户'),
                                          ('ADMIN', '管理员'),
                                          ('MODERATOR', ' moderators');

-- 插入初始用户 (密码使用BCrypt加密) 初始密码：123456
INSERT INTO users (username, password, email) VALUES
                                                  ('user', '$2a$10$WP4mzYJh2Q6yvN5UPQC8n.z.RJ1k4ov4ybe0.aFsKrNDdEaHfKGba', 'user@example.com'),
                                                  ('admin', '$2a$10$WP4mzYJh2Q6yvN5UPQC8n.z.RJ1k4ov4ybe0.aFsKrNDdEaHfKGba', 'admin@example.com');

-- 分配角色
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.username = 'user' AND r.name = 'USER';

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.username = 'admin' AND r.name IN ('ADMIN', 'USER');