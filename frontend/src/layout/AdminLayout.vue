<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'

const route = useRoute()
const router = useRouter()
const isCollapse = ref(false)

// ── AI 模型状态 ──

const aiModel = ref('加载中...')
const aiEnabled = ref(false)

async function loadAiStatus() {
  try {
    const { data } = await axios.get('/api/health')
    if (data.ai) {
      aiModel.value = data.ai.model || '未配置'
      aiEnabled.value = data.ai.enabled || false
    }
  } catch (e) {
    aiModel.value = '连接失败'
  }
}

onMounted(() => {
  loadAiStatus()
})

const menuGroups = [
  {
    label: '',
    items: [
      { path: '/dashboard', title: 'Dashboard', icon: 'Odometer' },
    ]
  },
  {
    label: 'AI COPILOT',
    items: [
      { path: '/sql-copilot', title: 'SQL Copilot', icon: 'Monitor' },
      { path: '/api-doc', title: '接口文档生成', icon: 'Tickets' },
      { path: '/dee-copilot', title: 'DEE 代码生成', icon: 'Connection' },
    ]
  },
  {
    label: '知识资产',
    items: [
      { path: '/data-dictionary', title: '系统知识库', icon: 'Notebook' },
      { path: '/field-mapper', title: 'AI Prompt 管理', icon: 'Switch' },
    ]
  },
  {
    label: '系统管理',
    items: [
      { path: '/field-mapper', title: '系统运行日志', icon: 'Document' },
      { path: '/data-dictionary', title: '系统设置', icon: 'Setting' },
    ]
  }
]

const activeMenu = computed(() => route.path)
const pageTitle = computed(() => {
  for (const group of menuGroups) {
    const found = group.items.find(i => i.path === route.path)
    if (found) return found.title
  }
  return 'OA Copilot'
})

function handleMenuSelect(path) {
  router.push(path)
}

function toggleSidebar() {
  isCollapse.value = !isCollapse.value
}
</script>

<template>
  <div class="admin-layout">
    <!-- 侧边栏 -->
    <aside class="sidebar" :class="{ 'sidebar--collapsed': isCollapse }">
      <!-- Logo -->
      <div class="sidebar__logo" @click="router.push('/dashboard')">
        <div class="sidebar__logo-icon">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
            <rect x="2" y="3" width="20" height="14" rx="3" stroke="currentColor" stroke-width="1.8"/>
            <path d="M8 21h8M12 17v4" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
            <circle cx="12" cy="10" r="2.5" stroke="currentColor" stroke-width="1.5"/>
            <path d="M7 10h-1M18 10h-1" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
          </svg>
        </div>
        <transition name="fade">
          <span v-show="!isCollapse" class="sidebar__logo-text">OA Copilot</span>
        </transition>
      </div>

      <!-- 菜单 -->
      <nav class="sidebar__menu">
        <div v-for="group in menuGroups" :key="group.label" class="sidebar__group">
          <div v-if="group.label && !isCollapse" class="sidebar__group-label">{{ group.label }}</div>
          <div v-for="item in group.items" :key="item.path"
            class="sidebar__item"
            :class="{ 'sidebar__item--active': activeMenu === item.path }"
            @click="handleMenuSelect(item.path)"
            :title="isCollapse ? item.title : ''"
          >
            <el-icon class="sidebar__item-icon" :size="18">
              <component :is="item.icon" />
            </el-icon>
            <transition name="fade">
              <span v-show="!isCollapse" class="sidebar__item-text">{{ item.title }}</span>
            </transition>
            <div v-if="activeMenu === item.path" class="sidebar__item-indicator"></div>
          </div>
        </div>
      </nav>

      <!-- 底部模型状态 -->
      <div class="sidebar__footer" v-show="!isCollapse">
        <div class="sidebar__model">
          <div class="sidebar__model-dot" :class="{ 'sidebar__model-dot--disabled': !aiEnabled }"></div>
          <div class="sidebar__model-info">
            <span class="sidebar__model-label">当前模型</span>
            <span class="sidebar__model-name">{{ aiModel }}</span>
          </div>
        </div>
      </div>
    </aside>

    <!-- 主区域 -->
    <div class="main-area">
      <!-- 顶栏 -->
      <header class="header">
        <div class="header__left">
          <button class="header__toggle" @click="toggleSidebar">
            <el-icon :size="18">
              <Fold v-if="!isCollapse" />
              <Expand v-else />
            </el-icon>
          </button>
          <div class="header__title">
            <h1>{{ pageTitle }}</h1>
            <span class="header__subtitle">AI Copilot</span>
          </div>
        </div>

        <div class="header__right">
          <div class="header__status">
            <span class="header__status-dot"></span>
            <span>在线</span>
          </div>
          <el-dropdown trigger="click">
            <button class="header__avatar">
              <el-avatar :size="32" class="header__avatar-img">
                <el-icon :size="16"><UserFilled /></el-icon>
              </el-avatar>
              <span class="header__username">Admin</span>
              <el-icon class="header__arrow" :size="12"><ArrowDown /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item>个人设置</el-dropdown-item>
                <el-dropdown-item divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <!-- 内容区 -->
      <main class="content">
        <router-view v-slot="{ Component }">
          <transition name="page" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<style scoped>
.admin-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

/* ═══════════════════════════════════════════════════════
   Sidebar
   ═══════════════════════════════════════════════════════ */

.sidebar {
  width: var(--sidebar-width);
  height: 100vh;
  background: var(--color-bg-sidebar);
  border-right: 1px solid var(--color-border-light);
  display: flex;
  flex-direction: column;
  transition: width var(--transition-normal);
  flex-shrink: 0;
  overflow: hidden;
}

.sidebar--collapsed {
  width: var(--sidebar-collapsed);
}

/* Logo */
.sidebar__logo {
  height: var(--header-height);
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 20px;
  cursor: pointer;
  border-bottom: 1px solid var(--color-border-light);
  flex-shrink: 0;
}

.sidebar__logo-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.sidebar__logo-text {
  color: var(--color-text-primary);
  font-size: 16px;
  font-weight: 700;
  letter-spacing: -0.3px;
  white-space: nowrap;
}

/* Menu */
.sidebar__menu {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 12px 8px;
}

.sidebar__menu::-webkit-scrollbar {
  width: 0;
}

.sidebar__group {
  margin-bottom: 8px;
}

.sidebar__group-label {
  padding: 8px 12px 6px;
  font-size: 11px;
  font-weight: 600;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.sidebar__item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  border-radius: 8px;
  cursor: pointer;
  color: var(--color-text-secondary);
  transition: all var(--transition-fast);
  position: relative;
  margin-bottom: 2px;
}

.sidebar__item:hover {
  background: var(--color-bg-sidebar-hover);
  color: var(--color-text-primary);
}

.sidebar__item--active {
  background: var(--color-bg-sidebar-active);
  color: var(--color-primary);
  font-weight: 600;
}

.sidebar__item-icon {
  flex-shrink: 0;
}

.sidebar__item-text {
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
}

.sidebar__item-indicator {
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 20px;
  background: var(--color-primary);
  border-radius: 0 4px 4px 0;
}

/* Footer - Model Status */
.sidebar__footer {
  padding: 12px 14px;
  border-top: 1px solid var(--color-border-light);
  flex-shrink: 0;
}

.sidebar__model {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  background: var(--color-bg-page);
  border: 1px solid var(--color-border-light);
  border-radius: 10px;
}

.sidebar__model-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--color-success);
  flex-shrink: 0;
  position: relative;
}

.sidebar__model-dot--disabled {
  background: var(--color-text-muted);
}

.sidebar__model-dot--disabled::after {
  animation: none;
}

.sidebar__model-dot::after {
  content: '';
  position: absolute;
  inset: -3px;
  border-radius: 50%;
  border: 1.5px solid var(--color-success);
  animation: pulse-ring 2s ease-out infinite;
}

.sidebar__model-info {
  display: flex;
  flex-direction: column;
  gap: 1px;
  min-width: 0;
}

.sidebar__model-label {
  font-size: 11px;
  color: var(--color-text-muted);
}

.sidebar__model-name {
  font-size: 12px;
  color: var(--color-text-primary);
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ═══════════════════════════════════════════════════════
   Main Area
   ═══════════════════════════════════════════════════════ */

.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
}

/* Header */
.header {
  height: var(--header-height);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--space-lg);
  background: var(--color-bg-card);
  border-bottom: 1px solid var(--color-border-light);
  flex-shrink: 0;
}

.header__left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header__toggle {
  width: 34px;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  border-radius: var(--radius-sm);
  cursor: pointer;
  color: var(--color-text-secondary);
  transition: all var(--transition-fast);
}

.header__toggle:hover {
  background: var(--color-bg-page);
  color: var(--color-text-primary);
}

.header__title h1 {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-text-primary);
  line-height: 1.2;
  letter-spacing: -0.3px;
}

.header__subtitle {
  font-size: 12px;
  color: var(--color-text-muted);
}

.header__right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header__status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--color-success);
  font-weight: 500;
}

.header__status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-success);
}

.header__avatar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 8px 4px 4px;
  border: none;
  background: transparent;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background var(--transition-fast);
}

.header__avatar:hover {
  background: var(--color-bg-page);
}

.header__avatar-img {
  background: var(--color-primary);
}

.header__username {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.header__arrow {
  color: var(--color-text-muted);
}

/* Content */
.content {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-lg);
  background: var(--color-bg-page);
}

/* ═══════════════════════════════════════════════════════
   Transitions
   ═══════════════════════════════════════════════════════ */

.fade-enter-active,
.fade-leave-active {
  transition: opacity var(--transition-fast);
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.page-enter-active {
  animation: fadeInUp 0.3s ease;
}
.page-leave-active {
  animation: fadeIn 0.15s ease reverse;
}

/* ═══════════════════════════════════════════════════════
   Responsive
   ═══════════════════════════════════════════════════════ */

@media (max-width: 768px) {
  .sidebar {
    position: fixed;
    z-index: 100;
    transform: translateX(-100%);
  }

  .sidebar:not(.sidebar--collapsed) {
    transform: translateX(0);
    box-shadow: 4px 0 24px rgba(0, 0, 0, 0.1);
  }

  .header__subtitle {
    display: none;
  }

  .header__status {
    display: none;
  }
}
</style>
