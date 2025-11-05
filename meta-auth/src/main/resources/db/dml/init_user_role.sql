-- 插入角色
INSERT INTO roles (name,code, description, status) VALUES
                                          ('USER','USER', '普通用户', 'ACTIVE'),
                                          ('ADMIN','ADMIN', '管理员', 'ACTIVE');

-- 插入初始用户 (密码使用BCrypt加密) 初始密码：123456
INSERT INTO users (username, password, email, phone, avatar, status) VALUES
                                                  ('user', '$2a$10$WP4mzYJh2Q6yvN5UPQC8n.z.RJ1k4ov4ybe0.aFsKrNDdEaHfKGba', 'user@example.com', '12345678901', 'https://www.gravatar.com/avatar/00000000000000000000000000000000?d=mp&f=y', 'ACTIVE'),
                                                  ('admin', '$2a$10$WP4mzYJh2Q6yvN5UPQC8n.z.RJ1k4ov4ybe0.aFsKrNDdEaHfKGba', 'admin@example.com', '12345678901', 'https://www.gravatar.com/avatar/00000000000000000000000000000000?d=mp&f=y', 'ACTIVE');

-- 分配角色
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.username = 'user' AND r.name = 'USER';

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.username = 'admin' AND r.name IN ('ADMIN', 'USER');