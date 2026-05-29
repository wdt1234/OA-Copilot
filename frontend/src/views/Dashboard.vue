<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { Promotion } from '@element-plus/icons-vue'
import axios from 'axios'

const router = useRouter()
const heroInput = ref('')
const heroInputRef = ref(null)
const recentRecords = ref([])

// ── 三个对话窗状态 ──

const chatWindows = ref([
  {
    id: 'oa',
    title: 'OA 开发助手',
    icon: '📋',
    color: '#3b82f6',
    messages: [],
    input: '',
    loading: false,
    placeholder: '问 OA/SQL/流程/审批 相关问题...'
  },
  {
    id: 'dee',
    title: 'DEE 开发助手',
    icon: '⚙️',
    color: '#8b5cf6',
    messages: [],
    input: '',
    loading: false,
    placeholder: '问 Workflow/Token/JSON 映射 相关问题...'
  },
  {
    id: 'general',
    title: '通用助手',
    icon: '💬',
    color: '#10b981',
    messages: [],
    input: '',
    loading: false,
    placeholder: '随便问，像豆包一样...'
  }
])

// ── 快捷能力 ──

const capabilities = [
  {
    path: '/sql-copilot',
    title: 'SQL Copilot',
    desc: '自然语言转 Oracle 查询',
    icon: 'sql',
    gradient: 'linear-gradient(135deg, #3b82f6, #6366f1)'
  },
  {
    path: '/api-doc',
    title: '接口文档',
    desc: '一键生成 6-Sheet Excel',
    icon: 'doc',
    gradient: 'linear-gradient(135deg, #10b981, #059669)'
  },
  {
    path: '/dee-copilot',
    title: 'DEE 代码',
    desc: '模板化生成 DEE 配置',
    icon: 'code',
    gradient: 'linear-gradient(135deg, #8b5cf6, #7c3aed)'
  },
  {
    path: '/data-dictionary',
    title: '数据字典',
    desc: 'OA 表单结构管理',
    icon: 'dict',
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
  { text: 'formmain_0433 字段映射到 JSON', target: '/data-dictionary' },
]

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

// ── 对话功能 ──

async function sendMessage(win) {
  const text = win.input.trim()
  if (!text || win.loading) return

  win.messages.push({ role: 'user', content: text })
  win.input = ''
  win.loading = true

  try {
    const history = win.messages.slice(-20).map(m => ({
      role: m.role,
      content: m.content
    }))

    const { data } = await axios.post('/api/chat', {
      messages: history,
      promptType: win.id === 'general' ? '' : win.id
    })

    if (data.success) {
      win.messages.push({ role: 'assistant', content: data.reply })
    } else {
      win.messages.push({ role: 'assistant', content: '回答出错：' + data.message })
    }
  } catch (e) {
    win.messages.push({ role: 'assistant', content: '网络错误，请稍后重试' })
  } finally {
    win.loading = false
    scrollChatToBottom(win.id)
  }
}

function scrollChatToBottom(winId) {
  nextTick(() => {
    const container = document.querySelector(`[data-chat="${winId}"] .chat-window__messages`)
    if (container) {
      container.scrollTop = container.scrollHeight
    }
  })
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
  // 初始化欢迎消息
  chatWindows.value.forEach(win => {
    const welcome = {
      oa: '你好！我是 OA 开发助手，可以帮你解答 SQL 查询、流程审批、表单设计等问题。',
      dee: '你好！我是 DEE 开发助手，可以帮你解答 Workflow、Token 认证、JSON 映射等问题。',
      general: '你好！我是通用助手，有什么问题尽管问。'
    }
    win.messages.push({ role: 'assistant', content: welcome[win.id] })
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
         三个 AI 对话窗
         ═══════════════════════════════════════════════ -->
    <div class="chat-section animate-fade-in-up" style="animation-delay: 0.16s">
      <div class="chat-section__label">AI 助手</div>
      <div class="chat-grid">
        <div
          v-for="win in chatWindows"
          :key="win.id"
          class="chat-window"
          :data-chat="win.id"
          :style="{ '--win-color': win.color }"
        >
          <div class="chat-window__header">
            <span class="chat-window__icon">{{ win.icon }}</span>
            <span class="chat-window__title">{{ win.title }}</span>
          </div>

          <div class="chat-window__messages">
            <div
              v-for="(msg, idx) in win.messages"
              :key="idx"
              class="chat-window__msg"
              :class="{ 'chat-window__msg--user': msg.role === 'user' }"
            >
              <div v-if="msg.role === 'assistant'" class="chat-window__avatar">{{ win.icon }}</div>
              <div class="chat-window__bubble">{{ msg.content }}</div>
            </div>
            <div v-if="win.loading" class="chat-window__msg">
              <div class="chat-window__avatar">{{ win.icon }}</div>
              <div class="chat-window__bubble chat-window__bubble--loading">
                <span class="chat-window__typing"></span>
              </div>
            </div>
          </div>

          <div class="chat-window__input-area">
            <div class="chat-window__input-box">
              <input
                v-model="win.input"
                class="chat-window__input"
                :placeholder="win.placeholder"
                @keydown.enter="sendMessage(win)"
                :disabled="win.loading"
              />
              <button
                class="chat-window__send"
                @click="sendMessage(win)"
                :disabled="!win.input.trim() || win.loading"
              >
                <el-icon :size="16"><Promotion /></el-icon>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ═══════════════════════════════════════════════
         快捷能力卡片
         ═══════════════════════════════════════════════ -->
    <div class="capabilities animate-fade-in-up" style="animation-delay: 0.24s">
      <div
        v-for="(cap, idx) in capabilities"
        :key="cap.path"
        class="capability-card"
        @click="navigateTo(cap.path)"
        :style="{ '--cap-gradient': cap.gradient }"
      >
        <div class="capability-card__icon">
          <svg v-if="cap.icon === 'sql'" width="24" height="24" viewBox="0 0 24 24" fill="none">
            <ellipse cx="12" cy="6" rx="8" ry="3" stroke="currentColor" stroke-width="2"/>
            <path d="M4 6v6c0 1.66 3.58 3 8 3s8-1.34 8-3V6" stroke="currentColor" stroke-width="2"/>
            <path d="M4 12v6c0 1.66 3.58 3 8 3s8-1.34 8-3v-6" stroke="currentColor" stroke-width="2"/>
          </svg>
          <svg v-else-if="cap.icon === 'doc'" width="24" height="24" viewBox="0 0 24 24" fill="none">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" stroke="currentColor" stroke-width="2"/>
            <polyline points="14 2 14 8 20 8" stroke="currentColor" stroke-width="2"/>
            <line x1="16" y1="13" x2="8" y2="13" stroke="currentColor" stroke-width="2"/>
            <line x1="16" y1="17" x2="8" y2="17" stroke="currentColor" stroke-width="2"/>
          </svg>
          <svg v-else-if="cap.icon === 'code'" width="24" height="24" viewBox="0 0 24 24" fill="none">
            <polyline points="16 18 22 12 16 6" stroke="currentColor" stroke-width="2"/>
            <polyline points="8 6 2 12 8 18" stroke="currentColor" stroke-width="2"/>
          </svg>
          <svg v-else-if="cap.icon === 'dict'" width="24" height="24" viewBox="0 0 24 24" fill="none">
            <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" stroke="currentColor" stroke-width="2"/>
            <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" stroke="currentColor" stroke-width="2"/>
          </svg>
        </div>
        <div class="capability-card__info">
          <h3>{{ cap.title }}</h3>
          <p>{{ cap.desc }}</p>
        </div>
        <svg class="capability-card__arrow" width="16" height="16" viewBox="0 0 24 24" fill="none">
          <path d="M9 18l6-6-6-6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </div>
    </div>

    <!-- ═══════════════════════════════════════════════
         最近记录
         ═══════════════════════════════════════════════ -->
    <div v-if="recentRecords.length" class="recent animate-fade-in-up" style="animation-delay: 0.32s">
      <div class="recent__label">最近生成</div>
      <div class="recent__list">
        <div
          v-for="item in recentRecords"
          :key="item.id"
          class="recent__item"
          @click="router.push({ path: '/sql-copilot', query: { q: item.prompt } })"
        >
          <div class="recent__item-prompt">{{ item.prompt }}</div>
          <div class="recent__item-meta">
            <span v-if="item.formCode">{{ item.formCode }}</span>
            <span>{{ item.time }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dashboard {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 0 40px;
}

/* ═══════════════════════════════════════════════
   Hero
   ═══════════════════════════════════════════════ */

.hero {
  text-align: center;
  padding: 48px 24px 40px;
}

.hero__badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 16px;
  background: rgba(16, 185, 129, 0.08);
  border: 1px solid rgba(16, 185, 129, 0.2);
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
  color: #10b981;
  margin-bottom: 24px;
}

.hero__badge-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #10b981;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.hero__title {
  margin: 0 0 12px;
  font-size: 42px;
  font-weight: 800;
  letter-spacing: -1px;
  line-height: 1.1;
}

.hero__title-main {
  color: var(--color-text-primary);
}

.hero__title-accent {
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero__subtitle {
  margin: 0 0 32px;
  font-size: 16px;
  color: var(--color-text-muted);
}

.hero__input-wrapper {
  max-width: 560px;
  margin: 0 auto;
}

.hero__input-box {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 8px 8px 20px;
  background: var(--color-bg-card);
  border: 2px solid var(--color-border-light);
  border-radius: 16px;
  transition: border-color var(--transition-fast);
}

.hero__input-box:focus-within {
  border-color: var(--color-primary);
}

.hero__input-icon {
  color: var(--color-text-muted);
  flex-shrink: 0;
}

.hero__input {
  flex: 1;
  border: none;
  background: transparent;
  font-size: 15px;
  color: var(--color-text-primary);
  outline: none;
}

.hero__input::placeholder {
  color: var(--color-text-muted);
}

.hero__submit {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  border: none;
  background: var(--color-primary);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all var(--transition-fast);
  flex-shrink: 0;
}

.hero__submit:hover:not(:disabled) {
  transform: scale(1.05);
}

.hero__submit:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.hero__input-hint {
  margin: 12px 0 0;
  font-size: 13px;
  color: var(--color-text-muted);
}

/* ═══════════════════════════════════════════════
   Suggestions
   ═══════════════════════════════════════════════ */

.suggestions {
  margin-bottom: 32px;
}

.suggestions__label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-muted);
  margin-bottom: 12px;
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
  padding: 12px 16px;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border-light);
  border-radius: 10px;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.suggestion-card:hover {
  border-color: var(--color-primary);
  background: var(--color-bg-page);
}

.suggestion-card__text {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.suggestion-card__arrow {
  color: var(--color-text-muted);
  flex-shrink: 0;
}

/* ═══════════════════════════════════════════════
   Chat Section
   ═══════════════════════════════════════════════ */

.chat-section {
  margin-bottom: 32px;
}

.chat-section__label {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-muted);
  margin-bottom: 12px;
}

.chat-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.chat-window {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border-light);
  border-radius: 12px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  height: 320px;
}

.chat-window__header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border-bottom: 1px solid var(--color-border-light);
  flex-shrink: 0;
}

.chat-window__icon {
  font-size: 16px;
}

.chat-window__title {
  font-size: 13px;
  font-weight: 600;
  color: var(--win-color);
}

.chat-window__messages {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.chat-window__msg {
  display: flex;
  gap: 6px;
  max-width: 90%;
}

.chat-window__msg--user {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.chat-window__avatar {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  background: var(--win-color);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  flex-shrink: 0;
}

.chat-window__bubble {
  padding: 8px 12px;
  border-radius: 10px;
  font-size: 13px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}

.chat-window__msg--user .chat-window__bubble {
  background: var(--win-color);
  color: white;
  border-bottom-right-radius: 4px;
}

.chat-window__msg:not(.chat-window__msg--user) .chat-window__bubble {
  background: var(--color-bg-page);
  color: var(--color-text-primary);
  border-bottom-left-radius: 4px;
}

.chat-window__bubble--loading {
  padding: 10px 16px;
}

.chat-window__typing {
  display: inline-block;
  width: 30px;
  height: 6px;
  position: relative;
}

.chat-window__typing::before,
.chat-window__typing::after,
.chat-window__typing span {
  content: '';
  position: absolute;
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--color-text-muted);
  animation: typing 1.4s infinite ease-in-out;
}

.chat-window__typing::before { left: 0; animation-delay: 0s; }
.chat-window__typing span { left: 12px; animation-delay: 0.2s; }
.chat-window__typing::after { left: 24px; animation-delay: 0.4s; }

@keyframes typing {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }
  30% { transform: translateY(-4px); opacity: 1; }
}

.chat-window__input-area {
  padding: 10px 12px;
  border-top: 1px solid var(--color-border-light);
  flex-shrink: 0;
}

.chat-window__input-box {
  display: flex;
  gap: 6px;
  align-items: center;
}

.chat-window__input {
  flex: 1;
  height: 34px;
  padding: 0 10px;
  border: 1px solid var(--color-border-light);
  border-radius: 8px;
  font-size: 13px;
  background: var(--color-bg-page);
  color: var(--color-text-primary);
  outline: none;
  transition: border-color var(--transition-fast);
}

.chat-window__input:focus {
  border-color: var(--win-color);
}

.chat-window__input::placeholder {
  color: var(--color-text-muted);
  font-size: 12px;
}

.chat-window__send {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  border: none;
  background: var(--win-color);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all var(--transition-fast);
  flex-shrink: 0;
}

.chat-window__send:hover:not(:disabled) {
  opacity: 0.9;
}

.chat-window__send:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* ═══════════════════════════════════════════════
   Capabilities
   ═══════════════════════════════════════════════ */

.capabilities {
  margin-bottom: 32px;
}

.capability-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border-light);
  border-radius: 12px;
  margin-bottom: 10px;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.capability-card:hover {
  border-color: var(--color-primary);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.capability-card__icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: var(--cap-gradient);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.capability-card__info {
  flex: 1;
  min-width: 0;
}

.capability-card__info h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.capability-card__info p {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--color-text-muted);
}

.capability-card__arrow {
  color: var(--color-text-muted);
  flex-shrink: 0;
}

/* ═══════════════════════════════════════════════
   Recent
   ═══════════════════════════════════════════════ */

.recent__label {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-muted);
  margin-bottom: 12px;
}

.recent__list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.recent__item {
  padding: 12px 16px;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border-light);
  border-radius: 10px;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.recent__item:hover {
  border-color: var(--color-primary);
}

.recent__item-prompt {
  font-size: 13px;
  color: var(--color-text-primary);
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recent__item-meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: var(--color-text-muted);
}
</style>
