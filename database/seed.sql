INSERT INTO sys_department (id, name, sort_order) VALUES
  (1, '产品部', 10),
  (2, '研发部', 20),
  (3, '质量部', 30)
ON CONFLICT (id) DO NOTHING;

INSERT INTO sys_user (id, name, department_id, email, phone, avatar_color) VALUES
  (1, '李宇航', 1, 'liyuhang@example.com', '13800000001', '#2563eb'),
  (2, '张三', 1, 'zhangsan@example.com', '13800000002', '#16a34a'),
  (3, '李四', 2, 'lisi@example.com', '13800000003', '#dc2626'),
  (4, '王五', 2, 'wangwu@example.com', '13800000004', '#9333ea'),
  (5, '赵六', 3, 'zhaoliu@example.com', '13800000005', '#ea580c'),
  (6, '产品经理', 1, 'pm@example.com', '13800000006', '#0891b2'),
  (7, '研发负责人', 2, 'rd@example.com', '13800000007', '#4f46e5'),
  (8, '质量人员', 3, 'qa@example.com', '13800000008', '#be123c')
ON CONFLICT (id) DO NOTHING;

INSERT INTO calendar (id, name, description, type, color, owner_user_id, visibility) VALUES
  (1, '李宇航个人日历', '个人工作安排', 'PERSONAL', '#2563eb', 1, 'PRIVATE'),
  (2, '张三个人日历', '个人工作安排', 'PERSONAL', '#16a34a', 2, 'PRIVATE'),
  (3, '李四个人日历', '个人工作安排', 'PERSONAL', '#dc2626', 3, 'PRIVATE'),
  (4, '王五个人日历', '个人工作安排', 'PERSONAL', '#9333ea', 4, 'PRIVATE'),
  (5, '赵六个人日历', '个人工作安排', 'PERSONAL', '#ea580c', 5, 'PRIVATE'),
  (6, '产品经理个人日历', '个人工作安排', 'PERSONAL', '#0891b2', 6, 'PRIVATE'),
  (7, '研发负责人个人日历', '个人工作安排', 'PERSONAL', '#4f46e5', 7, 'PRIVATE'),
  (8, '质量人员个人日历', '个人工作安排', 'PERSONAL', '#be123c', 8, 'PRIVATE'),
  (9, '公共日历', '跨部门公开事项', 'PUBLIC', '#0f766e', NULL, 'PUBLIC'),
  (10, '项目共享日历', '项目协同事项', 'SHARED', '#7c3aed', NULL, 'SHARED'),
  (11, '公司全员日历', '公司级活动与跨部门事项', 'ALL_MEMBER', '#f59e0b', NULL, 'ALL')
ON CONFLICT (id) DO NOTHING;

INSERT INTO calendar_auto_subscribe_scope (calendar_id, scope_type) VALUES
  (11, 'ALL_COMPANY')
ON CONFLICT DO NOTHING;

INSERT INTO calendar_shared_member (calendar_id, user_id) VALUES
  (11, 6),
  (11, 7)
ON CONFLICT DO NOTHING;

INSERT INTO tag_color (id, name, color, sort_order) VALUES
  (1, '会议', '#2563eb', 10),
  (2, '出差', '#ea580c', 20),
  (3, '私事', '#16a34a', 30),
  (4, '值班', '#9333ea', 40),
  (5, '培训', '#f59e0b', 50)
ON CONFLICT (id) DO NOTHING;

INSERT INTO event (id, calendar_id, organizer_user_id, title, location, description, tag, tag_color, start_time, end_time, all_day, recurrence_rule, allow_join) VALUES
  (1, 1, 1, '产品方案评审', '会议室 A', '确认首页与管理端范围', '会议', '#2563eb', '2026-06-08 09:00:00+08', '2026-06-08 10:30:00+08', false, NULL, false),
  (2, 10, 6, '研发排期沟通', '线上会议', '确认接口与数据库排期', '会议', '#2563eb', '2026-06-08 14:00:00+08', '2026-06-08 15:00:00+08', false, NULL, false),
  (3, 11, 6, '全员月度沟通会', '大会议室', '公司经营与项目同步', '会议', '#2563eb', '2026-06-09 10:00:00+08', '2026-06-09 11:30:00+08', false, NULL, true),
  (4, 9, 7, '研发值班安排', '研发区', '本周值班说明', '值班', '#9333ea', '2026-06-10 00:00:00+08', '2026-06-11 00:00:00+08', true, NULL, false),
  (5, 3, 3, '接口联调', '研发区', '日程接口联调', '会议', '#2563eb', '2026-06-10 13:30:00+08', '2026-06-10 16:00:00+08', false, NULL, false),
  (6, 8, 8, '验收用例梳理', '质量部', '整理功能验收项', '会议', '#2563eb', '2026-06-11 09:30:00+08', '2026-06-11 11:00:00+08', false, NULL, false),
  (7, 11, 7, '安全规范宣导', '线上会议', '代码与部署安全要求', '培训', '#f59e0b', '2026-06-12 15:00:00+08', '2026-06-12 16:00:00+08', false, NULL, true),
  (8, 10, 1, '跨部门进度同步', '会议室 B', '周进度同步', '会议', '#2563eb', '2026-06-15 10:00:00+08', '2026-06-15 11:00:00+08', false, 'FREQ=WEEKLY;INTERVAL=1', false),
  (9, 6, 6, '需求答疑窗口', '线上会议', '收集业务问题', '会议', '#2563eb', '2026-06-16 16:00:00+08', '2026-06-16 17:00:00+08', false, NULL, true),
  (10, 7, 7, '架构复盘', '研发区', '复盘服务边界', '会议', '#2563eb', '2026-06-17 09:00:00+08', '2026-06-17 10:00:00+08', false, NULL, false),
  (11, 9, 5, '质量巡检', '办公区', '日常巡检事项', '值班', '#9333ea', '2026-06-18 09:00:00+08', '2026-06-18 18:00:00+08', false, NULL, false),
  (12, 4, 4, '出差拜访', '上海', '客户现场沟通', '出差', '#ea580c', '2026-06-18 00:00:00+08', '2026-06-20 00:00:00+08', true, NULL, false),
  (13, 1, 1, '个人事务', '办公室', '资料整理', '私事', '#16a34a', '2026-06-19 18:30:00+08', '2026-06-19 19:00:00+08', false, NULL, false),
  (14, 11, 6, '新人培训', '培训室', '制度与工具说明', '培训', '#f59e0b', '2026-06-22 09:00:00+08', '2026-06-22 12:00:00+08', false, NULL, true),
  (15, 10, 7, '发布准备会', '会议室 C', '确认发布清单', '会议', '#2563eb', '2026-06-23 14:30:00+08', '2026-06-23 15:30:00+08', false, NULL, false),
  (16, 3, 3, '每日站会', '研发区', '研发进展同步', '会议', '#2563eb', '2026-06-24 09:30:00+08', '2026-06-24 09:45:00+08', false, 'FREQ=DAILY;INTERVAL=1', false),
  (17, 8, 8, '回归检查', '质量部', '核心流程核验', '会议', '#2563eb', '2026-06-24 16:00:00+08', '2026-06-24 18:00:00+08', false, NULL, false),
  (18, 11, 6, '公司开放日', '公司园区', '企业文化活动', '培训', '#f59e0b', '2026-06-26 13:00:00+08', '2026-06-26 17:00:00+08', false, NULL, true),
  (19, 2, 2, '资料归档', '办公室', '项目资料整理', '私事', '#16a34a', '2026-06-29 10:00:00+08', '2026-06-29 12:00:00+08', false, NULL, false),
  (20, 9, 1, '月末总结', '会议室 A', '月度结果回顾', '会议', '#2563eb', '2026-06-30 15:00:00+08', '2026-06-30 16:30:00+08', false, NULL, false)
ON CONFLICT (id) DO NOTHING;

INSERT INTO event_participant (event_id, user_id, response_status) VALUES
  (1, 6, 'ACCEPTED'), (1, 7, 'PENDING'), (2, 3, 'ACCEPTED'), (2, 7, 'TENTATIVE'),
  (3, 1, 'PENDING'), (3, 2, 'PENDING'), (3, 3, 'PENDING'), (3, 8, 'PENDING'),
  (5, 7, 'ACCEPTED'), (6, 6, 'ACCEPTED'), (7, 1, 'PENDING'), (8, 6, 'ACCEPTED'),
  (14, 2, 'PENDING'), (15, 8, 'PENDING'), (20, 6, 'ACCEPTED')
ON CONFLICT DO NOTHING;

INSERT INTO event_todo (event_id, title, assignee_user_id, priority, completed) VALUES
  (1, '整理评审材料', 6, 'HIGH', false),
  (2, '确认接口字段', 7, 'HIGH', false),
  (6, '汇总验收项', 8, 'MEDIUM', true),
  (15, '发布清单确认', 7, 'HIGH', false)
ON CONFLICT DO NOTHING;

INSERT INTO event_attachment (event_id, file_name, stored_name, file_size, content_type, storage_path, uploaded_by) VALUES
  (1, '产品方案.docx', 'product-plan.docx', 204800, 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', '/uploads/product-plan.docx', 6),
  (6, '验收清单.xlsx', 'acceptance-list.xlsx', 102400, 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', '/uploads/acceptance-list.xlsx', 8),
  (15, '发布清单.pdf', 'release-list.pdf', 153600, 'application/pdf', '/uploads/release-list.pdf', 7)
ON CONFLICT DO NOTHING;

INSERT INTO system_config (config_key, config_value, description) VALUES
  ('systemName', '企业协同日历 H5 系统', '顶部展示名称'),
  ('timezone', 'Asia/Shanghai', '默认时区'),
  ('workStartTime', '08:00', '工作开始时间'),
  ('workEndTime', '18:00', '工作结束时间'),
  ('maxUploadSizeMb', '20', '单文件大小限制'),
  ('allowedFileTypes', 'pdf,doc,docx,xls,xlsx,png,jpg,jpeg,zip', '允许上传的文件类型'),
  ('maxExportRows', '5000', '单次导出最大记录数'),
  ('showLunarCalendar', 'true', '是否展示农历'),
  ('showHoliday', 'true', '是否展示节假日')
ON CONFLICT (config_key) DO NOTHING;

INSERT INTO audit_log (operator_user_id, module, action, object_type, object_id, change_summary) VALUES
  (6, 'SYSTEM', 'INIT', 'SYSTEM', 'calendar', '系统基础数据已初始化')
ON CONFLICT DO NOTHING;

SELECT setval('sys_department_id_seq', 3, true);
SELECT setval('sys_user_id_seq', 8, true);
SELECT setval('calendar_id_seq', 11, true);
SELECT setval('event_id_seq', 20, true);
