<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const router = useRouter()
const heroInput = ref('')
const heroInputRef = ref(null)
const recentRecords = ref([])

// ── 快捷能力 ──

const capabilities = [
  {
    path: '/sql-copilot',
    title: 'SQL Copilot',
    desc: '自然语言转 Oracle 查询',
    icon: 'sql',
    color: '#3b82f6',
    gradient: 'linear-gradient(135deg, #3b82f6, #6366f1)'
  },
  {
    path: '/api-doc',
    title: '接口文档',
    desc: '一键生成 6-Sheet Excel',
    icon: 'doc',
    color: '#10b981',
    gradient: 'linear-gradient(135deg, #10b981, #059669)'
  },
  {
    path: '/dee-copilot',
    title: 'DEE 代码',
    desc: '模板化生成 DEE 配置',
    icon: 'code',
    color: '#8b5cf6',
    gradient: 'linear-gradient(135deg, #8b5cf6, #7c3aed)'
  },
  {
    path: '/field-mapper',
    title: '字段映射',
    desc: '表单字段自动映射',
    icon: 'map',
    color: '#f59e0b',
    gradient: 'linear-gradient(135deg, #f59e0b, #d97706)'
  },
  {
    path: '/data-dictionary',
    title: '数据字典',
    desc: 'OA 表单结构管理',
    icon: 'dict',
    color: '#ec4899',
    gradient: 'linear-gradient(135deg, #ec4899, #db2777)'
  }
]

// ── 推荐问题 ──

const suggestions = [
  { text: '查王得童 5 月份采购申请数量', target: '/sql-copilot' },
  { text: '生成运输通知单接口文档', target: '/api-doc' },
  { text: '生成请假审批 workflow 模板', target: '/dee-copilot' },
  { text: '查各部门本月发起流程统计', target: '/sql-copilot' },
  { text: '生成 token 获取接口配置', target: '/dee-copilot' },
  { text: 'formmain_0433 字段映射到 JSON', target: '/field-mapper' },
]

// ── 统计 ──

const stats = ref([
  { label: 'SQL 生成', value: 128, icon: '⚡' },
  { label: 'DEE 模板', value: 56, icon: '🔧' },
  { label: '接口文档', value: 23, icon: '📄' },
  { label: '节省时间', value: '42h', icon: '⏱' },
])

// ── 交互 ──

function handleHeroSubmit() {
  if (!heroInput.value.trim()) return
  router.push({ path: '/sql-copilot', query: { q: heroInput.value } })
  heroInput.value = ''
}

function handleSuggestion(item) {
  router.push({ path: item.target, query: { q: item.text } })
}

function navigateTo(path) {
  router.push(path)
}

// ── 最近记录 ──

async function loadRecent() {
  try {
    const { data } = await axios.get('/api/sql/history', { params: { limit: 5 } })
    recentRecords.value = data.map(item => ({
      id: item.id,
      prompt: item.prompt,
      time: formatTime(item.createTime),
      formCode: item.formCode
    }))
  } catch (e) {
    // 静默失败
  }
}

function formatTime(timeStr) {
  if (!timeStr) return ''
  const parts = timeStr.split('T')
  if (parts.length === 2) {
    const date = parts[0]
    const time = parts[1].substring(0, 5)
    const today = new Date().toISOString().split('T')[0]
    return date === today ? `今天 ${time}` : `${date.substring(5)} ${time}`
  }
  return timeStr
}

onMounted(() => {
  loadRecent()
  nextTick(() => {
    heroInputRef.value?.focus()
  })
})
</script>

<template>
  <div class="dashboard">
    <!-- ═══════════════════════════════════════════════
         Hero 区
         ═══════════════════════════════════════════════ -->
    <div class="hero animate-fade-in-up">
      <div class="hero__badge">
        <span class="hero__badge-dot"></span>
        AI Copilot 就绪
      </div>
      <h1 class="hero__title">
        <span class="hero__title-main">OA Integration</span>
        <span class="hero__title-accent">Copilot</span>
      </h1>
      <p class="hero__subtitle">企业级 AI 集成开发辅助平台 &middot; 自然语言驱动 OA 开发</p>

      <!-- 大输入框 -->
      <div class="hero__input-wrapper">
        <div class="hero__input-box">
          <svg class="hero__input-icon" width="20" height="20" viewBox="0 0 24 24" fill="none">
            <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          <input
            ref="heroInputRef"
            v-model="heroInput"
            class="hero__input"
            placeholder="描述你想查询的数据，例如：查王得童 5 月份采购申请数量"
            @keydown.enter="handleHeroSubmit"
          />
          <button class="hero__submit" @click="handleHeroSubmit" :disabled="!heroInput.trim()">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
              <path d="M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </button>
        </div>
        <p class="hero__input-hint">按 Enter 发送到 SQL Copilot &middot; 支持自然语言描述查询需求</p>
      </div>
    </div>

    <!-- ═══════════════════════════════════════════════
         推荐问题
         ═══════════════════════════════════════════════ -->
    <div class="suggestions animate-fade-in-up" style="animation-delay: 0.08s">
      <div class="suggestions__label">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
          <path d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
        试试这些问题
      </div>
      <div class="suggestions__grid">
        <button
          v-for="(item, idx) in suggestions"
          :key="idx"
          class="suggestion-card"
          @click="handleSuggestion(item)"
          :style="{ animationDelay: `${0.1 + idx * 0.04}s` }"
        >
          <span class="suggestion-card__text">{{ item.text }}</span>
          <svg class="suggestion-card__arrow" width="14" height="14" viewBox="0 0 24 24" fill="none">
            <path d="M5 12h14M12 5l7 7-7 7" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </button>
      </div>
    </div>

    <!-- ═══════════════════════════════════════════════
         快捷能力卡片
         ═══════════════════════════════════════════════ -->
    <div class="capabilities animate-fade-in-up" style="animation-delay: 0.16s">
      <div
        v-for="(cap, idx) in capabilities"
        :key="cap.path"
        class="cap-card"
        @click="navigateTo(cap.path)"
        :style="{ animationDelay: `${0.18 + idx * 0.05}s` }"
      >
        <div class="cap-card__icon" :style="{ background: cap.gradient }">
          <!-- SQL -->
          <svg v-if="cap.icon === 'sql'" width="20" height="20" viewBox="0 0 24 24" fill="none">
            <ellipse cx="12" cy="5" rx="9" ry="3" stroke="currentColor" stroke-width="2"/>
            <path d="M21 12c0 1.66-4.03 3-9 3s-9-1.34-9-3" stroke="currentColor" stroke-width="2"/>
            <path d="M3 5v14c0 1.66 4.03 3 9 3s9-1.34 9-3V5" stroke="currentColor" stroke-width="2"/>
          </svg>
          <!-- Doc -->
          <svg v-else-if="cap.icon === 'doc'" width="20" height="20" viewBox="0 0 24 24" fill="none">
            <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            <polyline points="14,2 14,8 20,8" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            <line x1="16" y1="13" x2="8" y2="13" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            <line x1="16" y1="17" x2="8" y2="17" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          </svg>
          <!-- Code -->
          <svg v-else-if="cap.icon === 'code'" width="20" height="20" viewBox="0 0 24 24" fill="none">
            <polyline points="16,18 22,12 16,6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            <polyline points="8,6 2,12 8,18" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          <!-- Map -->
          <svg v-else-if="cap.icon === 'map'" width="20" height="20" viewBox="0 0 24 24" fill="none">
            <path d="M8 3v3a2 2 0 01-2 2H3m18 0h-3a2 2 0 01-2-2V3m0 18v-3a2 2 0 012-2h3M3 16h3a2 2 0 012 2v3" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          <!-- Dict -->
          <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="none">
            <path d="M4 19.5A2.5 2.5 0 016.5 17H20" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M6.5 2H20v20H6.5A2.5 2.5 0 014 19.5v-15A2.5 2.5 0 016.5 2z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
        <div class="cap-card__info">
          <span class="cap-card__title">{{ cap.title }}</span>
          <span class="cap-card__desc">{{ cap.desc }}</span>
        </div>
        <svg class="cap-card__arrow" width="16" height="16" viewBox="0 0 24 24" fill="none">
          <path d="M9 18l6-6-6-6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </div>
    </div>

    <!-- ═══════════════════════════════════════════════
         底部：统计 + 最近使用
         ═══════════════════════════════════════════════ -->
    <div class="bottom-row animate-fade-in-up" style="animation-delay: 0.3s">
      <!-- 迷你统计 -->
      <div class="mini-stats">
        <div v-for="item in stats" :key="item.label" class="mini-stat">
          <span class="mini-stat__icon">{{ item.icon }}</span>
          <span class="mini-stat__value">{{ item.value }}</span>
          <span class="mini-stat__label">{{ item.label }}</span>
        </div>
      </div>

      <!-- 最近使用 -->
      <div class="recent-panel" v-if="recentRecords.length > 0">
        <div class="recent-panel__header">
          <span class="recent-panel__title">最近使用</span>
          <button class="recent-panel__more" @click="navigateTo('/sql-copilot')">查看全部</button>
        </div>
        <div class="recent-list">
          <div
            v-for="item in recentRecords"
            :key="item.id"
            class="recent-item"
            @click="navigateTo('/sql-copilot')"
          >
            <div class="recent-item__icon">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                <polyline points="16,18 22,12 16,6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <polyline points="8,6 2,12 8,18" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </div>
            <span class="recent-item__text">{{ item.prompt }}</span>
            <span class="recent-item__time">{{ item.time }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dashboard {
  max-width: 900px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 32px;
  padding: 20px 0 60px;
}

/* ═══════════════════════════════════════════════════════
   Hero
   ═══════════════════════════════════════════════════════ */

.hero {
  text-align: center;
  padding: 40px 0 0;
}

.hero__badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 16px;
  font-size: 12px;
  font-weight: 600;
  color: var(--color-success);
  background: #f0fdf4;
  border: 1px solid #d1fae5;
  border-radius: 20px;
  margin-bottom: 20px;
}

.hero__badge-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-success);
  animation: pulse-dot 2s ease-in-out infinite;
}

@keyframes pulse-dot {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

.hero__title {
  font-size: 42px;
  font-weight: 800;
  line-height: 1.15;
  letter-spacing: -1px;
  margin-bottom: 12px;
}

.hero__title-main {
  color: var(--color-text-primary);
}

.hero__title-accent {
  color: var(--color-primary);
}

.hero__subtitle {
  font-size: 16px;
  color: var(--color-text-muted);
  font-weight: 400;
  margin-bottom: 32px;
}

/* Hero Input */
.hero__input-wrapper {
  max-width: 640px;
  margin: 0 auto;
}

.hero__input-box {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 6px 6px 6px 18px;
  background: var(--color-bg-card);
  border: 1.5px solid var(--color-border);
  border-radius: 12px;
  box-shadow: var(--shadow-card);
  transition: all var(--transition-fast);
}

.hero__input-box:focus-within {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.hero__input-icon {
  color: var(--color-primary);
  flex-shrink: 0;
  opacity: 0.6;
}

.hero__input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: 15px;
  color: var(--color-text-primary);
  font-family: inherit;
  padding: 12px 0;
  line-height: 1.5;
}

.hero__input::placeholder {
  color: var(--color-text-muted);
}

.hero__submit {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  border: none;
  background: var(--color-primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all var(--transition-fast);
  flex-shrink: 0;
}

.hero__submit:hover:not(:disabled) {
  background: var(--color-primary-hover);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.hero__submit:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.hero__input-hint {
  font-size: 12px;
  color: var(--color-text-muted);
  margin-top: 10px;
  text-align: center;
}

/* ═══════════════════════════════════════════════════════
   Suggestions
   ═══════════════════════════════════════════════════════ */

.suggestions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.suggestions__label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
}

.suggestions__grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.suggestion-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border-light);
  border-radius: 10px;
  cursor: pointer;
  transition: all var(--transition-fast);
  text-align: left;
  box-shadow: var(--shadow-sm);
}

.suggestion-card:hover {
  border-color: var(--color-primary);
  box-shadow: var(--shadow-md);
  transform: translateX(2px);
}

.suggestion-card__text {
  font-size: 13px;
  color: var(--color-text-secondary);
  font-weight: 500;
  line-height: 1.4;
}

.suggestion-card:hover .suggestion-card__text {
  color: var(--color-primary);
}

.suggestion-card__arrow {
  color: var(--color-text-muted);
  flex-shrink: 0;
  opacity: 0;
  transform: translateX(-4px);
  transition: all var(--transition-fast);
}

.suggestion-card:hover .suggestion-card__arrow {
  opacity: 1;
  transform: translateX(0);
}

/* ═══════════════════════════════════════════════════════
   Capabilities
   ═══════════════════════════════════════════════════════ */

.capabilities {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
}

.cap-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 24px 16px;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border-light);
  border-radius: 12px;
  cursor: pointer;
  transition: all var(--transition-fast);
  text-align: center;
  box-shadow: var(--shadow-sm);
}

.cap-card:hover {
  border-color: var(--color-primary);
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}

.cap-card__icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.cap-card__info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.cap-card__title {
  font-size: 14px;
  font-weight: 700;
  color: var(--color-text-primary);
}

.cap-card__desc {
  font-size: 11px;
  color: var(--color-text-muted);
  line-height: 1.4;
}

.cap-card__arrow {
  color: var(--color-text-muted);
  position: absolute;
  top: 12px;
  right: 12px;
  opacity: 0;
  transition: all var(--transition-fast);
}

.cap-card:hover .cap-card__arrow {
  opacity: 0.6;
}

/* ═══════════════════════════════════════════════════════
   Bottom Row
   ═══════════════════════════════════════════════════════ */

.bottom-row {
  display: flex;
  gap: 16px;
}

/* Mini Stats */
.mini-stats {
  display: flex;
  gap: 0;
  flex-shrink: 0;
}

.mini-stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 16px 20px;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border-light);
  min-width: 90px;
}

.mini-stat:first-child {
  border-radius: 10px 0 0 10px;
}

.mini-stat:last-child {
  border-radius: 0 10px 10px 0;
}

.mini-stat:not(:last-child) {
  border-right: none;
}

.mini-stat__icon {
  font-size: 18px;
  line-height: 1;
}

.mini-stat__value {
  font-size: 20px;
  font-weight: 800;
  color: var(--color-text-primary);
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.5px;
}

.mini-stat__label {
  font-size: 11px;
  color: var(--color-text-muted);
  font-weight: 500;
}

/* Recent Panel */
.recent-panel {
  flex: 1;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border-light);
  border-radius: 10px;
  overflow: hidden;
  box-shadow: var(--shadow-sm);
}

.recent-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--color-border-light);
}

.recent-panel__title {
  font-size: 13px;
  font-weight: 700;
  color: var(--color-text-primary);
}

.recent-panel__more {
  font-size: 12px;
  color: var(--color-primary);
  background: none;
  border: none;
  cursor: pointer;
  font-weight: 500;
  padding: 2px 6px;
  border-radius: 6px;
  transition: all var(--transition-fast);
}

.recent-panel__more:hover {
  background: var(--color-primary-light);
}

.recent-list {
  display: flex;
  flex-direction: column;
}

.recent-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  cursor: pointer;
  transition: background var(--transition-fast);
}

.recent-item:hover {
  background: var(--color-bg-page);
}

.recent-item:not(:last-child) {
  border-bottom: 1px solid var(--color-border-light);
}

.recent-item__icon {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  background: var(--color-primary-light);
  color: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.recent-item__text {
  flex: 1;
  font-size: 13px;
  color: var(--color-text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recent-item__time {
  font-size: 11px;
  color: var(--color-text-muted);
  flex-shrink: 0;
}

/* ═══════════════════════════════════════════════════════
   Responsive
   ═══════════════════════════════════════════════════════ */

@media (max-width: 900px) {
  .capabilities {
    grid-template-columns: repeat(3, 1fr);
  }

  .suggestions__grid {
    grid-template-columns: 1fr;
  }

  .bottom-row {
    flex-direction: column;
  }

  .mini-stats {
    width: 100%;
  }

  .mini-stat {
    flex: 1;
  }
}

@media (max-width: 640px) {
  .hero__title {
    font-size: 28px;
  }

  .capabilities {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
