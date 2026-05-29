<script setup>
import { ref, onMounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ChatDotRound, Promotion, Loading } from '@element-plus/icons-vue'
import axios from 'axios'

const router = useRouter()
const heroInputRef = ref(null)
const recentRecords = ref([])

// ── 对话状态 ──

const chatMessages = ref([])
const chatInput = ref('')
const chatLoading = ref(false)
const promptType = ref('oa') // oa / dee / 空（通用）

// ── Prompt 选项 ──

const promptOptions = [
  { value: 'oa', label: 'OA 开发助手', hint: 'SQL/流程/审批' },
  { value: 'dee', label: 'DEE 开发助手', hint: 'Workflow/Token/JSON' },
  { value: '', label: '通用聊天', hint: '无限制' },
]

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
]

// ── 对话功能 ──

async function sendMessage() {
  const text = chatInput.value.trim()
  if (!text || chatLoading.value) return

  // 添加用户消息
  chatMessages.value.push({ role: 'user', content: text })
  chatInput.value = ''
  chatLoading.value = true

  try {
    // 构建消息历史（只保留最近10轮）
    const history = chatMessages.value.slice(-20).map(m => ({
      role: m.role,
      content: m.content
    }))

    const { data } = await axios.post('/api/chat', {
      messages: history,
      promptType: promptType.value
    })

    if (data.success) {
      chatMessages.value.push({ role: 'assistant', content: data.reply })
    } else {
      chatMessages.value.push({ role: 'assistant', content: '抱歉，回答出错了：' + data.message })
    }
  } catch (e) {
    chatMessages.value.push({ role: 'assistant', content: '网络错误，请稍后重试' })
  } finally {
    chatLoading.value = false
    scrollChatToBottom()
  }
}

function scrollChatToBottom() {
  nextTick(() => {
    const container = document.querySelector('.chat__messages')
    if (container) {
      container.scrollTop = container.scrollHeight
    }
  })
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
  // 初始欢迎消息
  chatMessages.value.push({
    role: 'assistant',
    content: '你好！我是 OA Copilot 助手。选择一个场景开始对话，或者直接问我任何问题。'
  })
})
</script>

<template>
  <div class="dashboard">
    <!-- ═══════════════════════════════════════════════
         AI 对话区
         ═══════════════════════════════════════════════ -->
    <div class="chat-section animate-fade-in-up">
      <div class="chat">
        <!-- 顶部：Prompt 选择器 -->
        <div class="chat__header">
          <div class="chat__header-left">
            <el-icon :size="18" color="var(--color-primary)"><ChatDotRound /></el-icon>
            <span class="chat__title">AI 助手</span>
          </div>
          <div class="chat__prompts">
            <button
              v-for="opt in promptOptions"
              :key="opt.value"
              class="chat__prompt-btn"
              :class="{ 'chat__prompt-btn--active': promptType === opt.value }"
              @click="promptType = opt.value"
            >
              {{ opt.label }}
            </button>
          </div>
        </div>

        <!-- 消息列表 -->
        <div class="chat__messages">
          <div
            v-for="(msg, idx) in chatMessages"
            :key="idx"
            class="chat__message"
            :class="{ 'chat__message--user': msg.role === 'user', 'chat__message--assistant': msg.role === 'assistant' }"
          >
            <div v-if="msg.role === 'assistant'" class="chat__avatar">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                <rect x="2" y="3" width="20" height="14" rx="3" stroke="currentColor" stroke-width="1.8"/>
                <path d="M8 21h8M12 17v4" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
              </svg>
            </div>
            <div class="chat__bubble">{{ msg.content }}</div>
          </div>
          <!-- 加载中 -->
          <div v-if="chatLoading" class="chat__message chat__message--assistant">
            <div class="chat__avatar">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                <rect x="2" y="3" width="20" height="14" rx="3" stroke="currentColor" stroke-width="1.8"/>
                <path d="M8 21h8M12 17v4" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
              </svg>
            </div>
            <div class="chat__bubble chat__bubble--loading">
              <span class="chat__typing"></span>
            </div>
          </div>
        </div>

        <!-- 输入区 -->
        <div class="chat__input-area">
          <div class="chat__input-box">
            <input
              v-model="chatInput"
              class="chat__input"
              placeholder="输入消息..."
              @keydown.enter="sendMessage"
              :disabled="chatLoading"
            />
            <button
              class="chat__send"
              @click="sendMessage"
              :disabled="!chatInput.trim() || chatLoading"
            >
              <el-icon :size="18"><Promotion /></el-icon>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- ═══════════════════════════════════════════════
         推荐问题
         ═══════════════════════════════════════════════ -->
    <div class="suggestions animate-fade-in-up" style="animation-delay: 0.08s">
      <div class="suggestions__label">试试这些问题</div>
      <div class="suggestions__grid">
        <button
          v-for="(item, idx) in suggestions"
          :key="idx"
          class="suggestion-card"
          @click="handleSuggestion(item)"
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
        class="capability-card"
        @click="navigateTo(cap.path)"
        :style="{ '--cap-gradient': cap.gradient, animationDelay: `${0.2 + idx * 0.06}s` }"
      >
        <div class="capability-card__icon">
          <!-- SQL -->
          <svg v-if="cap.icon === 'sql'" width="24" height="24" viewBox="0 0 24 24" fill="none">
            <ellipse cx="12" cy="6" rx="8" ry="3" stroke="currentColor" stroke-width="2"/>
            <path d="M4 6v6c0 1.66 3.58 3 8 3s8-1.34 8-3V6" stroke="currentColor" stroke-width="2"/>
            <path d="M4 12v6c0 1.66 3.58 3 8 3s8-1.34 8-3v-6" stroke="currentColor" stroke-width="2"/>
          </svg>
          <!-- Doc -->
          <svg v-else-if="cap.icon === 'doc'" width="24" height="24" viewBox="0 0 24 24" fill="none">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" stroke="currentColor" stroke-width="2"/>
            <polyline points="14 2 14 8 20 8" stroke="currentColor" stroke-width="2"/>
            <line x1="16" y1="13" x2="8" y2="13" stroke="currentColor" stroke-width="2"/>
            <line x1="16" y1="17" x2="8" y2="17" stroke="currentColor" stroke-width="2"/>
          </svg>
          <!-- Code -->
          <svg v-else-if="cap.icon === 'code'" width="24" height="24" viewBox="0 0 24 24" fill="none">
            <polyline points="16 18 22 12 16 6" stroke="currentColor" stroke-width="2"/>
            <polyline points="8 6 2 12 8 18" stroke="currentColor" stroke-width="2"/>
          </svg>
          <!-- Dict -->
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
    <div v-if="recentRecords.length" class="recent animate-fade-in-up" style="animation-delay: 0.24s">
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
  max-width: 1100px;
  margin: 0 auto;
  padding: 0 0 40px;
}

/* ═══════════════════════════════════════════════
   Chat Section
   ═══════════════════════════════════════════════ */

.chat-section {
  margin-bottom: 32px;
}

.chat {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border-light);
  border-radius: 16px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  height: 420px;
}

.chat__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid var(--color-border-light);
  flex-shrink: 0;
}

.chat__header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.chat__title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.chat__prompts {
  display: flex;
  gap: 6px;
}

.chat__prompt-btn {
  padding: 4px 12px;
  border-radius: 16px;
  border: 1px solid var(--color-border-light);
  background: transparent;
  font-size: 12px;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.chat__prompt-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.chat__prompt-btn--active {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: white;
}

/* Messages */
.chat__messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.chat__message {
  display: flex;
  gap: 8px;
  max-width: 80%;
}

.chat__message--user {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.chat__message--assistant {
  align-self: flex-start;
}

.chat__avatar {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: var(--color-primary);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.chat__bubble {
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}

.chat__message--user .chat__bubble {
  background: var(--color-primary);
  color: white;
  border-bottom-right-radius: 4px;
}

.chat__message--assistant .chat__bubble {
  background: var(--color-bg-page);
  color: var(--color-text-primary);
  border-bottom-left-radius: 4px;
}

.chat__bubble--loading {
  padding: 12px 20px;
}

/* Typing indicator */
.chat__typing {
  display: inline-block;
  width: 40px;
  height: 8px;
  position: relative;
}

.chat__typing::before,
.chat__typing::after,
.chat__typing span {
  content: '';
  position: absolute;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-text-muted);
  animation: typing 1.4s infinite ease-in-out;
}

.chat__typing::before { left: 0; animation-delay: 0s; }
.chat__typing span { left: 17px; animation-delay: 0.2s; }
.chat__typing::after { left: 34px; animation-delay: 0.4s; }

@keyframes typing {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }
  30% { transform: translateY(-6px); opacity: 1; }
}

/* Input */
.chat__input-area {
  padding: 12px 16px;
  border-top: 1px solid var(--color-border-light);
  flex-shrink: 0;
}

.chat__input-box {
  display: flex;
  gap: 8px;
  align-items: center;
}

.chat__input {
  flex: 1;
  height: 40px;
  padding: 0 14px;
  border: 1px solid var(--color-border-light);
  border-radius: 10px;
  font-size: 14px;
  background: var(--color-bg-page);
  color: var(--color-text-primary);
  outline: none;
  transition: border-color var(--transition-fast);
}

.chat__input:focus {
  border-color: var(--color-primary);
}

.chat__input::placeholder {
  color: var(--color-text-muted);
}

.chat__send {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  border: none;
  background: var(--color-primary);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.chat__send:hover:not(:disabled) {
  opacity: 0.9;
  transform: scale(1.05);
}

.chat__send:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* ═══════════════════════════════════════════════
   Suggestions
   ═══════════════════════════════════════════════ */

.suggestions {
  margin-bottom: 32px;
}

.suggestions__label {
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
