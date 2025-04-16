import { createRouter, createWebHistory } from 'vue-router'
import Dashboard from '../views/Dashboard.vue'

const routes = [
  {
    path: '/',
    name: 'Dashboard',
    component: Dashboard
  },
  {
    path: '/devices',
    name: 'Devices',
    component: () => import('../views/Devices.vue')
  },
  {
    path: '/rooms',
    name: 'Rooms',
    component: () => import('../views/Rooms.vue')
  },
  {
    path: '/scenarios',
    name: 'Scenarios',
    component: () => import('../views/Scenarios.vue')
  },
  {
    path: '/history',
    name: 'History',
    component: () => import('../views/History.vue')
  },
  {
    path: '/settings',
    name: 'Settings',
    component: () => import('../views/Settings.vue')
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('../views/Profile.vue')
  },
  {
    path: '/notifications',
    name: 'Notifications',
    component: () => import('../views/Notifications.vue')
  },
  // Вложенные маршруты для безопасности
  {
    path: '/security',
    name: 'Security',
    component: () => import('../views/Security.vue'),
    children: [
      {
        path: 'cameras',
        name: 'Cameras',
        component: () => import('../views/security/Cameras.vue')
      },
      {
        path: 'locks',
        name: 'Locks',
        component: () => import('../views/security/Locks.vue')
      }
    ]
  },
  // Маршрут для 404 страницы
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('../views/NotFound.vue')
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

// Защита маршрутов (если требуется авторизация)
router.beforeEach((to, from, next) => {
  // Здесь можно добавить логику для проверки авторизации
  // и перенаправления на страницу входа при необходимости
  next()
})

export default router 