<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const isCollapse = ref(false)

const menuItems = [
  { path: '/dashboard', title: 'Dashboard', icon: 'DataBoard' },
  { path: '/sql-copilot', title: 'SQL Copilot', icon: 'Document' },
  { path: '/dee-copilot', title: 'DEE 模板生成', icon: 'Connection' },
  { path: '/field-mapper', title: '字段映射助手', icon: 'Switch' },
  { path: '/data-dictionary', title: '数据字典管理', icon: 'Notebook' }
]

const activeMenu = computed(() => route.path)
const pageTitle = computed(() => route.meta?.title || 'OA Integration Copilot')

function handleMenuSelect(path) {
  router.push(path)
}

function toggleSidebar() {
  isCollapse.value = !isCollapse.value
}
</script>

<template>
  <el-container class="admin-layout">
    <el-aside :width="isCollapse ? '64px' : '220px'" class="sidebar">
      <div class="logo" @click="router.push('/dashboard')">
        <el-icon :size="24"><Monitor /></el-icon>
        <span v-show="!isCollapse" class="logo-text">OA Copilot</span>
      </div>

      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        background-color="transparent"
        text-color="rgba(255, 255, 255, 0.75)"
        active-text-color="#ffffff"
        :collapse-transition="false"
        @select="handleMenuSelect"
      >
        <el-menu-item
          v-for="item in menuItems"
          :key="item.path"
          :index="item.path"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <template #title>{{ item.title }}</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-icon
            class="collapse-btn"
            :size="20"
            @click="toggleSidebar"
          >
            <Fold v-if="!isCollapse" />
            <Expand v-else />
          </el-icon>
          <div class="page-title">
            <h1>{{ pageTitle }}</h1>
          </div>
        </div>

        <div class="header-right">
          <el-dropdown>
            <span class="user-info">
              <el-avatar :size="32" class="user-avatar">
                <el-icon><UserFilled /></el-icon>
              </el-avatar>
              <span class="username">Admin</span>
              <el-icon class="arrow-icon"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item>个人设置</el-dropdown-item>
                <el-dropdown-item divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.admin-layout {
  height: 100vh;
  overflow: hidden;
}

.sidebar {
  background: linear-gradient(180deg, #1a1a2e 0%, #16213e 100%);
  overflow-y: auto;
  overflow-x: hidden;
  transition: width 0.3s ease;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.15);
}

.sidebar::-webkit-scrollbar {
  width: 0;
}

.logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.05);
}

.logo-text {
  white-space: nowrap;
  letter-spacing: 0.5px;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  padding: 0 24px;
  height: 64px !important;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  cursor: pointer;
  color: #8c8c8c;
  transition: color 0.2s;
  padding: 4px;
  border-radius: 4px;
}

.collapse-btn:hover {
  color: #1890ff;
  background: rgba(24, 144, 255, 0.06);
}

.page-title h1 {
  font-size: 16px;
  font-weight: 600;
  color: #262626;
  margin: 0;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: #595959;
  padding: 4px 8px;
  border-radius: 6px;
  transition: background 0.2s;
}

.user-info:hover {
  background: #f5f5f5;
}

.user-avatar {
  background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
}

.username {
  font-size: 14px;
  font-weight: 500;
}

.arrow-icon {
  font-size: 12px;
  color: #bfbfbf;
}

.main-content {
  background-color: #f5f7fa;
  overflow-y: auto;
  padding: 20px;
}

:deep(.el-menu) {
  border-right: none;
  padding: 8px;
}

:deep(.el-menu-item) {
  border-radius: 6px;
  margin-bottom: 4px;
  height: 44px;
  line-height: 44px;
}

:deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.08) !important;
}

:deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, #1890ff 0%, #40a9ff 100%) !important;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.3);
}
</style>
