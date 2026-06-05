import { createRouter, createWebHistory } from 'vue-router';
import CalendarHome from '@/views/calendar/CalendarHome.vue';
import CalendarSearch from '@/views/calendar/CalendarSearch.vue';
import AdminLayout from '@/views/admin/AdminLayout.vue';
import AdminHome from '@/views/admin/AdminHome.vue';
import AdminTablePage from '@/views/admin/AdminTablePage.vue';
import AllCalendars from '@/views/admin/AllCalendars.vue';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/calendar' },
    { path: '/calendar', component: CalendarHome },
    { path: '/calendar/search', component: CalendarSearch },
    {
      path: '/admin',
      component: AdminLayout,
      children: [
        { path: '', component: AdminHome },
        { path: 'users', component: AdminTablePage, props: { resource: 'users', title: '系统用户管理' } },
        { path: 'departments', component: AdminTablePage, props: { resource: 'departments', title: '部门管理' } },
        { path: 'calendars', component: AdminTablePage, props: { resource: 'calendars', title: '日历管理' } },
        { path: 'all-calendars', component: AllCalendars },
        { path: 'events', component: AdminTablePage, props: { resource: 'events', title: '日程管理' } },
        { path: 'attachments', component: AdminTablePage, props: { resource: 'attachments', title: '附件管理' } },
        { path: 'exports', component: AdminTablePage, props: { resource: 'exports', title: '导出任务管理' } },
        { path: 'audit-logs', component: AdminTablePage, props: { resource: 'audit-logs', title: '审计日志' } },
        { path: 'backup', component: AdminTablePage, props: { resource: 'backup', title: '备份恢复' } },
        { path: 'settings', component: AdminTablePage, props: { resource: 'settings', title: '系统配置' } }
      ]
    }
  ]
});

export default router;
