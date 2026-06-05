import { createRouter, createWebHistory } from 'vue-router';
import CalendarLayout from '@/layouts/CalendarLayout.vue';
import AdminLayout from '@/layouts/AdminLayout.vue';
import CalendarPage from '@/pages/calendar/CalendarPage.vue';
import CalendarSearchPage from '@/pages/calendar/CalendarSearchPage.vue';
import AdminHomePage from '@/pages/admin/AdminHomePage.vue';
import AdminResourcePage from '@/pages/admin/AdminResourcePage.vue';
import AllCalendarsPage from '@/pages/admin/AllCalendarsPage.vue';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/calendar' },
    {
      path: '/calendar',
      component: CalendarLayout,
      children: [
        { path: '', component: CalendarPage },
        { path: 'search', component: CalendarSearchPage }
      ]
    },
    {
      path: '/admin',
      component: AdminLayout,
      children: [
        { path: '', component: AdminHomePage },
        { path: 'users', component: AdminResourcePage, props: { resource: 'users' } },
        { path: 'departments', component: AdminResourcePage, props: { resource: 'departments' } },
        { path: 'calendars', component: AdminResourcePage, props: { resource: 'calendars' } },
        { path: 'all-calendars', component: AllCalendarsPage },
        { path: 'events', component: AdminResourcePage, props: { resource: 'events' } },
        { path: 'attachments', component: AdminResourcePage, props: { resource: 'attachments' } },
        { path: 'exports', component: AdminResourcePage, props: { resource: 'exports' } },
        { path: 'audit-logs', component: AdminResourcePage, props: { resource: 'auditLogs' } },
        { path: 'backup', component: AdminResourcePage, props: { resource: 'backup' } },
        { path: 'settings', component: AdminResourcePage, props: { resource: 'settings' } }
      ]
    }
  ]
});

export default router;
