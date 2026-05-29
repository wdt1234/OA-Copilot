import { createRouter, createWebHistory } from 'vue-router'
import AdminLayout from '../layout/AdminLayout.vue'

const routes = [
  {
    path: '/',
    component: AdminLayout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/Dashboard.vue'),
        meta: { title: 'Dashboard', icon: 'DataBoard' }
      },
      {
        path: 'sql-copilot',
        name: 'SqlCopilot',
        component: () => import('../views/SqlCopilot.vue'),
        meta: { title: 'SQL Copilot', icon: 'Document' }
      },
      {
        path: 'dee-copilot',
        name: 'DeeCopilot',
        component: () => import('../views/DeeCopilot.vue'),
        meta: { title: 'DEE 模板生成', icon: 'Connection' }
      },
      {
        path: 'field-mapper',
        name: 'FieldMapper',
        component: () => import('../views/FieldMapper.vue'),
        meta: { title: '字段映射助手', icon: 'Switch' }
      },
      {
        path: 'data-dictionary',
        name: 'DataDictionary',
        component: () => import('../views/DataDictionary.vue'),
        meta: { title: '数据字典管理', icon: 'Notebook' }
      },
      {
        path: 'api-doc',
        name: 'ApiDocGenerator',
        component: () => import('../views/ApiDocGenerator.vue'),
        meta: { title: '接口文档生成', icon: 'Document' }
      },
      {
        path: 'prompt-manager',
        name: 'PromptManager',
        component: () => import('../views/PromptManager.vue'),
        meta: { title: 'AI Prompt 管理', icon: 'Setting' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
