-- Seed data: default admin user and sample external contacts
-- Admin password hash: SHA-256("admin123")

INSERT INTO app_user(id, username, password_hash, display_name, role) VALUES
('u_admin', 'admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', '系统管理员', 'admin'),
('u_zhangsan', 'zhangsan', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', '张三', 'user'),
('u_lisi', 'lisi', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', '李四', 'user')
ON CONFLICT (id) DO NOTHING;

-- Seed external contacts
INSERT INTO external_contact(id, name, email, company, contact_type, phone, created_by) VALUES
(1, '王五(微信)', 'wangwu@example.com', '腾讯科技', 'wechat', '13800138001', 'u_admin'),
(2, '赵六(客户)', 'zhaoliu@client.com', '阿里巴巴', 'client', '13900139002', 'u_admin'),
(3, '孙七(合作方)', 'sunqi@partner.com', '华为技术', 'partner', '13700137003', 'u_admin')
ON CONFLICT (id) DO NOTHING;

COMMIT;