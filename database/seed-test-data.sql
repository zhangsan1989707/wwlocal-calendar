-- 测试数据脚本 - 真实的业务场景数据
-- 生成时间: 2026-06-06

BEGIN;

-- ========================================
-- 1. 外部联系人测试数据
-- ========================================
INSERT INTO external_contact (id, name, email, company, contact_type, phone, created_by) VALUES
(4, '陈小明', 'chenxiaoming@techcorp.com', '科技创新有限公司', 'client', '13900139004', 'user-001'),
(5, '周敏', 'zhoumin@startup.io', '创业加速器', 'partner', '13900139005', 'user-001'),
(6, '吴强', 'wuqiang@bigcompany.com', '大型集团企业', 'client', '13900139006', 'user-002'),
(7, '郑丽', 'zhengli@consulting.com', '咨询顾问公司', 'partner', '13900139007', 'user-003'),
(8, '钱伟', 'qianwei@finance.com', '金融服务公司', 'client', '13900139008', 'user-004'),
(9, '孙芳', 'sunfang@media.com', '媒体传播集团', 'partner', '13900139009', 'user-005'),
(10, '李娜', 'lina@retail.com', '零售连锁集团', 'client', '13900139010', 'user-001'),
(11, '张小刚', 'zhangxiaogang@logistics.com', '物流运输公司', 'partner', '13900139011', 'user-002'),
(12, '王美丽', 'wangmeili@education.com', '教育培训中心', 'wechat', '13900139012', 'user-003'),
(13, '刘洋', 'liuyang@healthcare.com', '医疗健康集团', 'client', '13900139013', 'user-004'),
(14, '陈静', 'chenjing@realestate.com', '房地产开发公司', 'wechat', '13900139014', 'user-005'),
(15, '黄志明', 'huangzhiming@manufacturing.com', '制造业龙头企业', 'partner', '13900139015', 'user-001')
ON CONFLICT (id) DO NOTHING;

-- ========================================
-- 2. 日程测试数据 - 包含各种类型的真实场景
-- ========================================
INSERT INTO event (id, calendar_id, organizer_user_id, title, description, location, start_at, end_at, all_day, timezone, status, created_at, updated_at) VALUES
-- 今天
(101, 'cal-001', 'user-001', '产品需求评审会议', '讨论Q3季度产品需求优先级和实现方案', '会议室A', NOW()::date + TIME '09:00', NOW()::date + TIME '10:30', false, 'Asia/Shanghai', 'ACTIVE', NOW(), NOW()),
(102, 'cal-001', 'user-001', '每日站会', '敏捷开发团队每日站会，同步进度和 blockers', '线上会议', NOW()::date + TIME '10:00', NOW()::date + TIME '10:15', false, 'Asia/Shanghai', 'ACTIVE', NOW(), NOW()),
(103, 'cal-001', 'user-001', '午餐约会', '与客户陈小明共进午餐，讨论合作细节', '公司楼下餐厅', NOW()::date + TIME '12:00', NOW()::date + TIME '13:30', false, 'Asia/Shanghai', 'ACTIVE', NOW(), NOW()),

-- 明天
(104, 'cal-001', 'user-001', '项目周会', '项目进度汇报会议，各模块负责人汇报', '会议室B', NOW()::date + INTERVAL '1 day' + TIME '14:00', NOW()::date + INTERVAL '1 day' + TIME '15:30', false, 'Asia/Shanghai', 'ACTIVE', NOW(), NOW()),
(105, 'cal-001', 'user-001', '一对一沟通', '与团队成员进行一对一绩效沟通', '办公室', NOW()::date + INTERVAL '1 day' + TIME '16:00', NOW()::date + INTERVAL '1 day' + TIME '17:00', false, 'Asia/Shanghai', 'ACTIVE', NOW(), NOW()),

-- 本周
(106, 'cal-001', 'user-001', '代码评审', '评审新功能的代码实现', '会议室C', NOW()::date + INTERVAL '2 day' + TIME '10:00', NOW()::date + INTERVAL '2 day' + TIME '11:00', false, 'Asia/Shanghai', 'ACTIVE', NOW(), NOW()),
(107, 'cal-001', 'user-001', '技术分享', '内部技术分享会，主题：微服务架构设计', '培训室', NOW()::date + INTERVAL '3 day' + TIME '15:00', NOW()::date + INTERVAL '3 day' + TIME '17:00', false, 'Asia/Shanghai', 'ACTIVE', NOW(), NOW()),
(108, 'cal-001', 'user-001', '客户拜访', '拜访重要客户周敏，确认项目交付时间', '客户办公室', NOW()::date + INTERVAL '3 day' + TIME '09:00', NOW()::date + INTERVAL '3 day' + TIME '12:00', false, 'Asia/Shanghai', 'ACTIVE', NOW(), NOW()),

-- 本月
(109, 'cal-001', 'user-001', '月度总结会议', '总结本月工作完成情况，制定下月计划', '会议室A', NOW()::date + INTERVAL '7 day' + TIME '14:00', NOW()::date + INTERVAL '7 day' + TIME '16:00', false, 'Asia/Shanghai', 'ACTIVE', NOW(), NOW()),
(110, 'cal-001', 'user-001', '培训学习', '参加外部培训：产品经理进阶培训', '外部培训机构', NOW()::date + INTERVAL '10 day' + TIME '09:00', NOW()::date + INTERVAL '10 day' + TIME '18:00', false, 'Asia/Shanghai', 'ACTIVE', NOW(), NOW()),

-- 重复日程
(111, 'cal-001', 'user-001', '周例会', '每周固定周例会，回顾本周工作和下周计划', '会议室B', NOW()::date + INTERVAL '4 day' + TIME '10:00', NOW()::date + INTERVAL '4 day' + TIME '11:30', false, 'Asia/Shanghai', 'ACTIVE', NOW(), NOW()),
(112, 'cal-001', 'user-001', '健身时间', '每周固定的健身时间，保持健康', '健身房', NOW()::date + INTERVAL '1 day' + TIME '19:00', NOW()::date + INTERVAL '1 day' + TIME '20:30', false, 'Asia/Shanghai', 'ACTIVE', NOW(), NOW()),

-- 全天事件（end_at需要比start_at晚）
(113, 'cal-001', 'user-001', '公司团建活动', '年度公司团建活动，全员参与', '户外拓展基地', NOW()::date + INTERVAL '14 day', NOW()::date + INTERVAL '15 day', true, 'Asia/Shanghai', 'ACTIVE', NOW(), NOW()),
(114, 'cal-001', 'user-001', '出差北京', '前往北京总部参加年度会议', '北京总部', NOW()::date + INTERVAL '20 day', NOW()::date + INTERVAL '22 day', true, 'Asia/Shanghai', 'ACTIVE', NOW(), NOW()),

-- 其他用户的日程
(115, 'cal-002', 'user-002', '销售周报汇报', '向领导汇报本周销售进展', '会议室A', NOW()::date + TIME '11:00', NOW()::date + TIME '12:00', false, 'Asia/Shanghai', 'ACTIVE', NOW(), NOW()),
(116, 'cal-003', 'user-003', '代码重构讨论', '与技术团队讨论代码重构方案', '会议室C', NOW()::date + INTERVAL '1 day' + TIME '14:00', NOW()::date + INTERVAL '1 day' + TIME '15:00', false, 'Asia/Shanghai', 'ACTIVE', NOW(), NOW()),
(117, 'cal-004', 'user-004', '质量审查会议', '审查本月代码质量指标', '会议室B', NOW()::date + INTERVAL '2 day' + TIME '10:00', NOW()::date + INTERVAL '2 day' + TIME '11:30', false, 'Asia/Shanghai', 'ACTIVE', NOW(), NOW()),
(118, 'cal-005', 'user-005', '客户需求沟通', '与客户沟通新的需求变更', '客户现场', NOW()::date + INTERVAL '3 day' + TIME '14:00', NOW()::date + INTERVAL '3 day' + TIME '16:00', false, 'Asia/Shanghai', 'ACTIVE', NOW(), NOW()),

-- 已过期的日程
(119, 'cal-001', 'user-001', '上月项目启动会', '项目启动会议，讨论项目计划和分工', '会议室A', NOW()::date - INTERVAL '30 day' + TIME '10:00', NOW()::date - INTERVAL '30 day' + TIME '12:00', false, 'Asia/Shanghai', 'ACTIVE', NOW(), NOW()),
(120, 'cal-001', 'user-001', '技术选型评审', '评估技术方案选型', '会议室B', NOW()::date - INTERVAL '25 day' + TIME '14:00', NOW()::date - INTERVAL '25 day' + TIME '15:30', false, 'Asia/Shanghai', 'ACTIVE', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- ========================================
-- 3. 日程参与人测试数据
-- ========================================
INSERT INTO event_participant (event_id, user_id, department_id, external_contact_id, response_status, role, created_at) VALUES
-- 会议101的参与者（只包含内部用户，不包含外部联系人）
(101, 'user-002', NULL, NULL, 'ACCEPTED', 'EDITOR', NOW()),
(101, 'user-003', NULL, NULL, 'ACCEPTED', 'VIEWER', NOW()),
(101, 'user-004', NULL, NULL, 'NEEDS_ACTION', 'VIEWER', NOW()),

-- 会议102的参与者
(102, 'user-002', NULL, NULL, 'ACCEPTED', 'VIEWER', NOW()),
(102, 'user-003', NULL, NULL, 'ACCEPTED', 'VIEWER', NOW()),
(102, 'user-004', NULL, NULL, 'ACCEPTED', 'VIEWER', NOW()),
(102, 'user-005', NULL, NULL, 'ACCEPTED', 'VIEWER', NOW()),

-- 会议103的参与者（外部联系人 - 单独一个event）
(103, NULL, NULL, 4, 'ACCEPTED', 'VIEWER', NOW()),  -- 陈小明

-- 会议104的参与者
(104, 'user-002', NULL, NULL, 'NEEDS_ACTION', 'VIEWER', NOW()),
(104, 'user-003', NULL, NULL, 'NEEDS_ACTION', 'VIEWER', NOW()),
(104, NULL, 'dept-001', NULL, 'NEEDS_ACTION', 'VIEWER', NOW()),  -- 产品部全员

-- 会议106的参与者
(106, 'user-002', NULL, NULL, 'ACCEPTED', 'VIEWER', NOW()),
(106, 'user-003', NULL, NULL, 'ACCEPTED', 'EDITOR', NOW()),

-- 会议107的参与者
(107, NULL, 'dept-002', NULL, 'NEEDS_ACTION', 'VIEWER', NOW()),  -- 研发部全员

-- 会议108的参与者（外部联系人 - 单独一个event）
(108, NULL, NULL, 5, 'ACCEPTED', 'VIEWER', NOW()),  -- 周敏

-- 会议111的参与者（重复日程）
(111, 'user-002', NULL, NULL, 'ACCEPTED', 'VIEWER', NOW()),
(111, 'user-003', NULL, NULL, 'ACCEPTED', 'VIEWER', NOW()),

-- 其他会议的参与者
(115, 'user-001', NULL, NULL, 'ACCEPTED', 'VIEWER', NOW()),
(115, 'user-003', NULL, NULL, 'ACCEPTED', 'VIEWER', NOW()),
(116, 'user-001', NULL, NULL, 'ACCEPTED', 'VIEWER', NOW()),
(116, 'user-002', NULL, NULL, 'ACCEPTED', 'VIEWER', NOW()),
(117, 'user-001', NULL, NULL, 'ACCEPTED', 'VIEWER', NOW()),
(117, 'user-002', NULL, NULL, 'ACCEPTED', 'VIEWER', NOW()),
(117, 'user-003', NULL, NULL, 'ACCEPTED', 'VIEWER', NOW()),
(118, NULL, NULL, 8, 'ACCEPTED', 'VIEWER', NOW())  -- 钱伟
ON CONFLICT DO NOTHING;

-- ========================================
-- 4. 日程提醒测试数据
-- ========================================
INSERT INTO event_reminder (event_id, minutes_before, method, created_at) VALUES
(101, 30, 'SYSTEM', NOW()),
(101, 1440, 'SYSTEM', NOW()),
(102, 5, 'SYSTEM', NOW()),
(103, 60, 'SYSTEM', NOW()),
(104, 60, 'SYSTEM', NOW()),
(106, 30, 'SYSTEM', NOW()),
(107, 1440, 'SYSTEM', NOW()),
(108, 1440, 'SYSTEM', NOW()),
(111, 60, 'SYSTEM', NOW()),
(111, 1440, 'SYSTEM', NOW()),
(113, 4320, 'SYSTEM', NOW()),
(114, 1440, 'SYSTEM', NOW())
ON CONFLICT DO NOTHING;

-- ========================================
-- 5. 日程待办事项测试数据
-- ========================================
INSERT INTO event_todo (event_id, title, priority, completed, assignee_user_id, due_at, created_at) VALUES
(101, '准备产品需求文档', 'HIGH', false, 'user-001', NOW()::date, NOW()),
(101, '整理竞品分析报告', 'MEDIUM', false, 'user-002', NOW()::date + INTERVAL '1 day', NOW()),
(101, '预约会议室', 'LOW', true, 'user-003', NOW()::date - INTERVAL '1 day', NOW()),

(104, '准备周报PPT', 'HIGH', false, 'user-001', NOW()::date + INTERVAL '1 day', NOW()),
(104, '收集各模块进度', 'MEDIUM', false, 'user-002', NOW()::date + INTERVAL '1 day', NOW()),

(106, '审查代码实现', 'HIGH', false, 'user-003', NOW()::date + INTERVAL '2 day', NOW()),
(106, '准备评审意见', 'MEDIUM', false, 'user-002', NOW()::date + INTERVAL '2 day', NOW()),

(107, '准备演示材料', 'HIGH', false, 'user-001', NOW()::date + INTERVAL '3 day', NOW()),
(107, '测试演示环境', 'MEDIUM', false, 'user-002', NOW()::date + INTERVAL '3 day', NOW()),

(108, '准备客户资料', 'HIGH', false, 'user-001', NOW()::date + INTERVAL '2 day', NOW()),
(108, '确认出行安排', 'MEDIUM', true, 'user-001', NOW()::date + INTERVAL '1 day', NOW()),

(113, '准备户外装备', 'MEDIUM', false, NULL, NOW()::date + INTERVAL '14 day', NOW()),
(113, '请假申请', 'HIGH', true, NULL, NOW()::date + INTERVAL '7 day', NOW()),

(114, '预订机票酒店', 'HIGH', false, NULL, NOW()::date + INTERVAL '18 day', NOW()),
(114, '准备会议材料', 'MEDIUM', false, NULL, NOW()::date + INTERVAL '19 day', NOW()),
(114, '通知相关部门', 'LOW', false, NULL, NOW()::date + INTERVAL '19 day', NOW())
ON CONFLICT DO NOTHING;

-- ========================================
-- 6. 日历共享成员测试数据
-- ========================================
INSERT INTO calendar_shared_member (calendar_id, user_id, role, created_at) VALUES
('cal-009', 'user-001', 'EDITOR', NOW()),
('cal-009', 'user-002', 'EDITOR', NOW()),
('cal-009', 'user-003', 'EDITOR', NOW()),
('cal-009', 'user-004', 'EDITOR', NOW()),
('cal-009', 'user-005', 'EDITOR', NOW()),
('cal-010', 'user-001', 'MANAGER', NOW()),
('cal-010', 'user-002', 'EDITOR', NOW()),
('cal-010', 'user-003', 'EDITOR', NOW()),
('cal-010', 'user-004', 'EDITOR', NOW()),
('cal-010', 'user-005', 'EDITOR', NOW()),
('cal-011', 'user-001', 'EDITOR', NOW()),
('cal-011', 'user-002', 'EDITOR', NOW()),
('cal-011', 'user-003', 'EDITOR', NOW()),
('cal-011', 'user-004', 'EDITOR', NOW()),
('cal-011', 'user-005', 'EDITOR', NOW())
ON CONFLICT DO NOTHING;

-- ========================================
-- 7. 重复日程规则测试数据
-- ========================================
INSERT INTO event_recurrence (event_id, rrule, end_at, occurrence_count, created_at) VALUES
(102, 'FREQ=DAILY;INTERVAL=1', NULL, NULL, NOW()),
(111, 'FREQ=WEEKLY;INTERVAL=1;BYDAY=FR', NULL, NULL, NOW()),
(112, 'FREQ=WEEKLY;INTERVAL=1;BYDAY=TU,TH', NULL, NULL, NOW())
ON CONFLICT DO NOTHING;

-- ========================================
-- 8. 站内通知测试数据
-- ========================================
INSERT INTO notification (user_id, event_id, title, message, notification_type, is_read, created_at) VALUES
('user-001', 101, '会议邀请：产品需求评审会议', '您被邀请参加产品需求评审会议，时间：今天 09:00', 'event_invite', false, NOW() - INTERVAL '1 day'),
('user-001', 102, '会议邀请：每日站会', '您被邀请参加每日站会，时间：今天 10:00', 'event_invite', true, NOW() - INTERVAL '2 day'),
('user-001', 104, '会议邀请：项目周会', '您被邀请参加项目周会，时间：明天 14:00', 'event_invite', false, NOW() - INTERVAL '3 day'),
('user-001', NULL, '系统通知：日程更新', '您的日程「产品需求评审会议」已更新', 'event_update', true, NOW() - INTERVAL '1 day'),
('user-002', 101, '会议邀请：产品需求评审会议', '您被邀请参加产品需求评审会议，时间：今天 09:00', 'event_invite', true, NOW() - INTERVAL '1 day'),
('user-002', 115, '会议邀请：销售周报汇报', '您被邀请参加销售周报汇报，时间：今天 11:00', 'event_invite', false, NOW() - INTERVAL '2 day'),
('user-003', 101, '会议邀请：产品需求评审会议', '您被邀请参加产品需求评审会议，时间：今天 09:00', 'event_invite', false, NOW() - INTERVAL '1 day'),
('user-003', 116, '会议邀请：代码重构讨论', '您被邀请参加代码重构讨论，时间：明天 14:00', 'event_invite', true, NOW() - INTERVAL '3 day'),
('user-004', 101, '会议邀请：产品需求评审会议', '您被邀请参加产品需求评审会议，时间：今天 09:00', 'event_invite', true, NOW() - INTERVAL '1 day'),
('user-005', 102, '会议邀请：每日站会', '您被邀请参加每日站会，时间：今天 10:00', 'event_invite', false, NOW() - INTERVAL '2 day')
ON CONFLICT DO NOTHING;

COMMIT;
