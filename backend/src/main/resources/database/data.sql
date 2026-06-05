insert into departments(id, name, parent_id, sort_order)
values ('dept-root', '总部', null, 1)
on conflict (id) do nothing;

insert into users(id, name, email, mobile, department_id, status)
values ('user-admin', '系统管理员', 'admin@wwlocal.local', '13800000000', 'dept-root', 'active')
on conflict (id) do nothing;

insert into calendars(id, name, owner_user_id, type, color, visible)
values ('cal-admin', '工作日历', 'user-admin', 'personal', '#2563eb', true)
on conflict (id) do nothing;

insert into organization_calendars(id, name, scope_type, color, created_by)
values ('org-cal-default', '全员日历', 'organization', '#16a34a', 'user-admin')
on conflict (id) do nothing;

insert into schedules(id, title, calendar_id, start_at, end_at, location, description, created_by)
values (
    'schedule-welcome',
    '团队周会',
    'cal-admin',
    '2026-06-08T09:00:00+08:00',
    '2026-06-08T10:00:00+08:00',
    '第一会议室',
    '同步本周重点事项',
    'user-admin'
)
on conflict (id) do nothing;

insert into attachments(id, schedule_id, file_name, file_url, file_size, uploaded_by)
values ('attachment-agenda', 'schedule-welcome', 'agenda.pdf', 'https://files.wwlocal.local/agenda.pdf', 20480, 'user-admin')
on conflict (id) do nothing;

insert into export_tasks(id, task_type, status, file_url, requested_by)
values ('export-default', 'schedule', 'ready', 'https://files.wwlocal.local/export/schedules.xlsx', 'user-admin')
on conflict (id) do nothing;

insert into audit_logs(id, actor_user_id, action, target_type, target_id, detail)
values ('audit-boot', 'user-admin', 'initialize', 'system', 'wwlocal-calendar', '初始化基础数据')
on conflict (id) do nothing;

insert into backup_restores(id, operation_type, status, file_url, created_by)
values ('backup-default', 'backup', 'ready', 'https://files.wwlocal.local/backups/latest.dump', 'user-admin')
on conflict (id) do nothing;

insert into system_configs(id, config_key, config_value, description, updated_by)
values ('config-timezone', 'calendar.timezone', 'Asia/Shanghai', '系统时区', 'user-admin')
on conflict (id) do nothing;
