<script setup>
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Star, StarFilled, Edit, Close, Delete, Document, Promotion } from '@element-plus/icons-vue'
import axios from 'axios'

// ── 状态 ──

const prompt = ref('')
const sqlOutput = ref('')
const generating = ref(false)
const selectedFormCode = ref('')
const availableForms = ref([])
const selectedIds = ref([])
const selectMode = ref(false)

// ── 系统表状态 ──

const systemTablesCount = ref(7)
const showSystemTables = ref(false)
const systemTables = ref([])
const expandedTable = ref('')

async function loadSystemTables() {
  try {
    const { data } = await axios.get('/api/knowledge/system-tables')
    systemTables.value = data
    systemTablesCount.value = data.length
  } catch (e) {
    console.error('加载系统表失败', e)
  }
}

function openSystemTables() {
  showSystemTables.value = true
}

function toggleTableExpand(tableName) {
  expandedTable.value = expandedTable.value === tableName ? '' : tableName
}

// ── 推荐问题 ──

const recommendedQuestions = [
  '按人员+月份统计表单数量',
  '按部门+年份统计表单数量',
  '按流程标题查审批记录',
  '查我发起的流程',
  '查待我审批的流程',
]

// ── 快捷示例 ──

const allQuickExamples = [
  '统计采购申请数量（按人员 + 月份）',
  '统计采购申请数量（按部门 + 年份）',
  '按流程标题查审批记录',
  '查我发起的流程',
  '查待我审批的采购申请',
  '查询挂账申请表本月数据',
  '统计各部门请假天数',
  '查询运输通知单接口数据',
]

const quickExamples = ref([])

function shuffleExamples() {
  const shuffled = [...allQuickExamples].sort(() => Math.random() - 0.5)
  quickExamples.value = shuffled.slice(0, 5)
}

function useQuickExample(text) {
  prompt.value = text
}

// ── 快捷模板 ──

const builtinTemplates = [
  '查询 formmain_1001 中本月所有请假单',
  '查询 OA 待办流程列表',
  '查询 EBS 订单数据关联客户信息',
  '查询 formmain_2003 中状态为审批中的记录',
  '统计各表单本月提交数量'
]

const quickTemplates = ref([])

function loadTemplates() {
  try {
    const saved = localStorage.getItem('sql_copilot_quick_templates')
    if (saved) {
      quickTemplates.value = JSON.parse(saved)
    } else {
      const defaultSaved = localStorage.getItem('sql_copilot_default_templates')
      quickTemplates.value = defaultSaved ? JSON.parse(defaultSaved) : [...builtinTemplates]
    }
  } catch (e) {
    quickTemplates.value = [...builtinTemplates]
  }
}

function saveTemplates() {
  localStorage.setItem('sql_copilot_quick_templates', JSON.stringify(quickTemplates.value))
}

function useTemplate(text) {
  prompt.value = text
}

function removeTemplate(index) {
  quickTemplates.value.splice(index, 1)
  saveTemplates()
}

async function editTemplate(index) {
  try {
    const { value } = await ElMessageBox.prompt('编辑模板内容', '编辑模板', {
      confirmButtonText: '保存',
      cancelButtonText: '取消',
      inputValue: quickTemplates.value[index],
      inputType: 'textarea',
      inputValidator: (val) => val.trim() ? true : '模板内容不能为空'
    })
    quickTemplates.value[index] = value.trim()
    saveTemplates()
    ElMessage.success('模板已更新')
  } catch (e) {}
}

async function addTemplate() {
  try {
    const { value } = await ElMessageBox.prompt('输入新的快捷模板', '新增模板', {
      confirmButtonText: '添加',
      cancelButtonText: '取消',
      inputType: 'textarea',
      inputValidator: (val) => val.trim() ? true : '模板内容不能为空'
    })
    quickTemplates.value.push(value.trim())
    saveTemplates()
    ElMessage.success('模板已添加')
  } catch (e) {}
}

function resetTemplates() {
  const defaultSaved = localStorage.getItem('sql_copilot_default_templates')
  quickTemplates.value = defaultSaved ? JSON.parse(defaultSaved) : [...builtinTemplates]
  saveTemplates()
  ElMessage.success('已恢复默认模板')
}

function setAsDefault() {
  localStorage.setItem('sql_copilot_default_templates', JSON.stringify(quickTemplates.value))
  ElMessage.success('已将当前模板设为默认值')
}

// ── 表单数据字典 ──

async function loadAvailableForms() {
  try {
    const { data } = await axios.get('/api/knowledge/forms/search', { params: { keyword: '' } })
    availableForms.value = data
  } catch (e) {
    console.error('加载表单列表失败', e)
  }
}

async function searchForms(keyword) {
  try {
    const { data } = await axios.get('/api/knowledge/forms/search', { params: { keyword } })
    availableForms.value = data
  } catch (e) {
    console.error('搜索表单失败', e)
  }
}

function onFormSearch(query) {
  searchForms(query)
}

function onFormChange(formCode) {
  selectedFormCode.value = formCode
}

// ── 历史记录 ──

const history = ref([])

function getFormName(formCode) {
  if (!formCode) return ''
  const form = availableForms.value.find(f => f.formCode === formCode)
  return form ? form.formName : formCode
}

async function loadHistory() {
  try {
    const { data } = await axios.get('/api/sql/history', { params: { limit: 20 } })
    history.value = data.map(item => ({
      id: item.id,
      prompt: item.prompt,
      sql: item.sqlResult,
      formCode: item.formCode,
      formName: getFormName(item.formCode),
      isPinned: item.pinned,
      time: formatTime(item.createTime)
    }))
  } catch (e) {
    console.error('加载历史记录失败', e)
  }
}

function formatTime(timeStr) {
  if (!timeStr) return ''
  const parts = timeStr.split('T')
  if (parts.length === 2) {
    return parts[1].substring(0, 5)
  }
  return timeStr
}

function timeAgo(timeStr) {
  if (!timeStr) return ''
  const now = new Date()
  const [h, m] = timeStr.split(':').map(Number)
  const created = new Date(now)
  created.setHours(h, m, 0, 0)
  const diff = Math.floor((now - created) / 60000)
  if (diff < 1) return '刚刚'
  if (diff < 60) return `${diff}分钟前`
  if (diff < 1440) return `${Math.floor(diff / 60)}小时前`
  return `${Math.floor(diff / 1440)}天前`
}

async function togglePin(item) {
  try {
    await axios.put(`/api/sql/history/${item.id}/pin`)
    await loadHistory()
    ElMessage.success(item.isPinned ? '已取消置顶' : '已置顶')
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

async function deleteHistory(item) {
  try {
    await ElMessageBox.confirm('确定删除该记录？', '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await axios.delete(`/api/sql/history/${item.id}`)
    await loadHistory()
    ElMessage.success('已删除')
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

function toggleSelect(id) {
  const idx = selectedIds.value.indexOf(id)
  if (idx >= 0) selectedIds.value.splice(idx, 1)
  else selectedIds.value.push(id)
}

function toggleSelectAll() {
  if (selectedIds.value.length === history.value.length) {
    selectedIds.value = []
  } else {
    selectedIds.value = history.value.map(h => h.id)
  }
}

async function batchDelete() {
  if (selectedIds.value.length === 0) return
  try {
    await ElMessageBox.confirm(
      `确定删除选中的 ${selectedIds.value.length} 条记录？`,
      '确认批量删除',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    await axios.delete('/api/sql/history/batch', { data: { ids: selectedIds.value } })
    selectedIds.value = []
    await loadHistory()
    ElMessage.success('批量删除完成')
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('批量删除失败')
  }
}

// ── SQL 语法高亮 ──

function highlightSql(sql) {
  if (!sql) return ''
  let result = sql
  // 关键字
  const keywords = ['SELECT', 'FROM', 'WHERE', 'AND', 'OR', 'JOIN', 'LEFT', 'RIGHT',
    'INNER', 'ON', 'AS', 'IN', 'NOT', 'NULL', 'IS', 'LIKE', 'BETWEEN', 'EXISTS',
    'COUNT', 'SUM', 'AVG', 'MIN', 'MAX', 'DISTINCT', 'GROUP', 'BY', 'ORDER',
    'ASC', 'DESC', 'HAVING', 'UNION', 'ALL', 'INSERT', 'INTO', 'VALUES',
    'UPDATE', 'SET', 'DELETE', 'CREATE', 'TABLE', 'DROP', 'ALTER',
    'CASE', 'WHEN', 'THEN', 'ELSE', 'END', 'TO_CHAR', 'TO_DATE', 'NVL',
    'DECODE', 'SUBSTR', 'TRIM', 'UPPER', 'LOWER', 'LENGTH', 'CONNECT', 'WITH']
  // 先转义 HTML
  result = result.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
  // 单引号字符串
  result = result.replace(/'([^']*)'/g, '<span class="sql-str">\'$1\'</span>')
  // 数字
  result = result.replace(/\b(\d+)\b/g, '<span class="sql-num">$1</span>')
  // 关键字
  keywords.forEach(kw => {
    const regex = new RegExp(`\\b(${kw})\\b`, 'gi')
    result = result.replace(regex, '<span class="sql-kw">$1</span>')
  })
  // 注释
  result = result.replace(/(--.*)/g, '<span class="sql-comment">$1</span>')
  return result
}

const sqlLines = computed(() => {
  if (!sqlOutput.value) return []
  return sqlOutput.value.split('\n')
})

// ── 生成逻辑 ──

async function generate() {
  if (!prompt.value.trim()) {
    ElMessage.warning('请输入查询需求描述')
    return
  }
  generating.value = true
  sqlOutput.value = ''
  try {
    const { data } = await axios.post('/api/sql/generate', {
      prompt: prompt.value,
      formCode: selectedFormCode.value || undefined
    })
    if (data.error) {
      ElMessage.error(data.error)
      return
    }
    sqlOutput.value = data.sql
    await loadHistory()
  } catch (e) {
    ElMessage.error('生成失败，请检查后端服务是否启动')
    console.error('生成 SQL 失败', e)
  } finally {
    generating.value = false
  }
}

function copySql() {
  if (!sqlOutput.value) return
  navigator.clipboard.writeText(sqlOutput.value)
  ElMessage.success('已复制到剪贴板')
}

function loadFromHistory(item) {
  prompt.value = item.prompt
  sqlOutput.value = item.sql
}

function clearAll() {
  prompt.value = ''
  sqlOutput.value = ''
}

function handleRecommendClick(q) {
  prompt.value = q
}

// ── 初始化 ──

onMounted(async () => {
  await loadAvailableForms()
  await loadSystemTables()
  loadHistory()
  loadTemplates()
  shuffleExamples()
})
</script>

<template>
  <div class="sql-copilot">
    <el-row :gutter="20" class="sql-copilot__row">
      <!-- ═══════════════════════════════════════════════
           中间：输入区 + 结果区
           ═══════════════════════════════════════════════ -->
      <el-col :xs="24" :lg="17">
        <!-- 智能生成 SQL -->
        <div class="panel panel--input animate-fade-in-up">
          <!-- 标题区 -->
          <div class="panel__header">
            <div class="panel__title">
              <div class="panel__title-icon panel__title-icon--ai">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
                  <path d="M12 2L9.5 8.5L3 12L9.5 15.5L12 22L14.5 15.5L21 12L14.5 8.5L12 2Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </div>
              <div>
                <h2 class="panel__title-text">智能生成 SQL</h2>
              </div>
            </div>
            <div class="panel__ai-badge" @click="openSystemTables" style="cursor: pointer">
              <span class="panel__ai-badge-dot"></span>
              {{ systemTablesCount }} 张系统表已就绪
              <el-icon :size="12"><ArrowRight /></el-icon>
            </div>
          </div>

          <!-- 快捷模板 -->
          <div class="quick-templates-section">
            <div class="quick-templates-header">
              <span class="quick-templates-label">快捷模板</span>
              <div class="quick-templates-actions">
                <button class="quick-template-btn" @click="addTemplate">新增</button>
                <button class="quick-template-btn" @click="setAsDefault">设为默认</button>
                <button class="quick-template-btn" @click="resetTemplates">恢复</button>
              </div>
            </div>
            <div class="quick-templates-list">
              <div
                v-for="(t, idx) in quickTemplates"
                :key="idx"
                class="quick-template-item"
                @click="useTemplate(t)"
              >
                <div class="quick-template-item__icon">
                  <svg width="12" height="12" viewBox="0 0 24 24" fill="none">
                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                    <polyline points="14,2 14,8 20,8" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  </svg>
                </div>
                <span class="quick-template-item__text">{{ t }}</span>
                <div class="quick-template-item__actions">
                  <button class="quick-template-action-btn" @click.stop="editTemplate(idx)" title="编辑">
                    <el-icon :size="10"><Edit /></el-icon>
                  </button>
                  <button class="quick-template-action-btn quick-template-action-btn--danger" @click.stop="removeTemplate(idx)" title="删除">
                    <el-icon :size="10"><Close /></el-icon>
                  </button>
                </div>
              </div>
            </div>
          </div>

          <!-- 输入区 -->
          <div class="input-area">
            <div class="input-wrapper">
              <el-input
                v-model="prompt"
                type="textarea"
                :rows="4"
                :autosize="{ minRows: 3, maxRows: 8 }"
                placeholder="请输入您的问题，比如：查王得童5月份采购申请数量"
                class="prompt-textarea"
                resize="none"
              />
              <div class="input-footer">
                <span class="input-count" :class="{ 'input-count--warn': prompt.length > 450 }">
                  {{ prompt.length }} / 500
                </span>
              </div>
            </div>

            <!-- 操作栏 -->
            <div class="input-actions">
              <el-select
                v-model="selectedFormCode"
                placeholder="选择表单（可选）"
                clearable
                filterable
                remote
                :remote-method="onFormSearch"
                @change="onFormChange"
                class="form-select"
                size="large"
              >
                <template #prefix>
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" style="color: var(--color-text-muted)">
                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                    <polyline points="14,2 14,8 20,8" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                    <line x1="16" y1="13" x2="8" y2="13" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                    <line x1="16" y1="17" x2="8" y2="17" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                  </svg>
                </template>
                <el-option
                  v-for="form in availableForms"
                  :key="form.formCode"
                  :label="form.formName + ' (' + form.tableName + ')'"
                  :value="form.formCode"
                />
              </el-select>

              <div class="input-actions__right">
                <el-button
                  size="large"
                  class="btn-clear"
                  @click="clearAll"
                >
                  <el-icon><Delete /></el-icon>
                  清空
                </el-button>
                <el-button
                  type="primary"
                  size="large"
                  :loading="generating"
                  @click="generate"
                  class="btn-generate"
                >
                  <template #loading>
                    <div class="btn-loading">
                      <span class="btn-loading__dot"></span>
                      <span class="btn-loading__dot"></span>
                      <span class="btn-loading__dot"></span>
                    </div>
                  </template>
                  <template v-if="!generating">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" style="margin-right:6px">
                      <path d="M12 2L9.5 8.5L3 12L9.5 15.5L12 22L14.5 15.5L21 12L14.5 8.5L12 2Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                    </svg>
                    生成 SQL
                  </template>
                  <template v-else>生成中...</template>
                </el-button>
              </div>
            </div>
          </div>
        </div>

        <!-- 生成结果 -->
        <div class="panel panel--output animate-fade-in-up" style="animation-delay: 0.08s">
          <!-- 结果头部 -->
          <div class="panel__header">
            <div class="panel__title">
              <div class="panel__title-icon panel__title-icon--result">
                <el-icon :size="18"><Document /></el-icon>
              </div>
              <div>
                <h2 class="panel__title-text">生成结果</h2>
              </div>
              <el-tag v-if="sqlOutput" type="info" size="small" effect="plain" class="result-tag">SQL</el-tag>
            </div>
            <div v-if="sqlOutput" class="result-actions">
              <button class="result-btn" @click="copySql" title="复制">
                <el-icon :size="15"><CopyDocument /></el-icon>
                复制
              </button>
            </div>
          </div>

          <!-- SQL 代码区域 -->
          <div class="code-editor">
            <!-- 生成中 skeleton -->
            <template v-if="generating">
              <div class="code-editor__header">
                <div class="code-editor__dots">
                  <span></span><span></span><span></span>
                </div>
                <span class="code-editor__filename">query.sql</span>
                <div class="code-editor__toolbar-right">
                  <span class="code-editor__ai-status">
                    <span class="code-editor__ai-spinner"></span>
                    AI 生成中...
                  </span>
                </div>
              </div>
              <div class="code-editor__body">
                <div class="code-editor__skeleton">
                  <div class="skeleton skeleton-line" style="width: 30%"></div>
                  <div class="skeleton skeleton-line" style="width: 80%"></div>
                  <div class="skeleton skeleton-line" style="width: 65%"></div>
                  <div class="skeleton skeleton-line" style="width: 90%"></div>
                  <div class="skeleton skeleton-line" style="width: 50%"></div>
                  <div class="skeleton skeleton-line" style="width: 75%"></div>
                  <div class="skeleton skeleton-line" style="width: 40%"></div>
                </div>
              </div>
              <div class="code-editor__footer">
                <div class="code-editor__status code-editor__status--loading">
                  <span class="code-editor__ai-spinner"></span>
                  正在生成 SQL，请稍候...
                </div>
              </div>
            </template>

            <!-- 生成完成 -->
            <template v-else-if="sqlOutput">
              <div class="code-editor__header">
                <div class="code-editor__dots">
                  <span></span><span></span><span></span>
                </div>
                <span class="code-editor__filename">query.sql</span>
                <div class="code-editor__toolbar-right">
                  <el-tag size="small" type="info" effect="plain" class="code-editor__lang-tag">SQL</el-tag>
                  <el-tag size="small" type="success" effect="plain" class="code-editor__lang-tag">{{ sqlLines.length }} 行</el-tag>
                </div>
              </div>
              <div class="code-editor__body">
                <div class="code-editor__lines">
                  <span v-for="(_, i) in sqlLines" :key="i" class="code-editor__line-num">{{ i + 1 }}</span>
                </div>
                <pre class="code-editor__code"><code v-html="highlightSql(sqlOutput)"></code></pre>
              </div>
              <div class="code-editor__footer">
                <div class="code-editor__status">
                  <span class="code-editor__status-dot"></span>
                  SQL 生成成功！共 {{ sqlLines.length }} 行
                </div>
              </div>
            </template>

            <!-- 空状态 -->
            <template v-else>
              <div class="code-editor__empty">
                <div class="code-editor__empty-icon">
                  <svg width="40" height="40" viewBox="0 0 24 24" fill="none">
                    <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
                  </svg>
                </div>
                <p class="code-editor__empty-text">描述你的查询需求</p>
                <p class="code-editor__empty-hint">AI 将基于数据字典自动生成 Oracle SQL</p>
              </div>
            </template>
          </div>
        </div>
      </el-col>

      <!-- ═══════════════════════════════════════════════
           右侧：历史记录 + 推荐问题 + 快捷模板
           ═══════════════════════════════════════════════ -->
      <el-col :xs="24" :lg="7">
        <!-- 历史记录 -->
        <div class="panel panel--side animate-fade-in-up" style="animation-delay: 0.16s">
          <div class="side-section">
            <div class="side-section__header">
              <h3 class="side-section__title">
                <el-icon :size="16"><Clock /></el-icon>
                历史记录
              </h3>
              <div class="side-section__actions">
                <template v-if="!selectMode">
                  <button v-if="history.length > 0" class="side-action-btn" @click="selectMode = true">选择</button>
                </template>
                <template v-else>
                  <button class="side-action-btn" @click="toggleSelectAll">
                    {{ selectedIds.length === history.length ? '取消全选' : '全选' }}
                  </button>
                  <button v-if="selectedIds.length > 0" class="side-action-btn side-action-btn--danger" @click="batchDelete">
                    删除 ({{ selectedIds.length }})
                  </button>
                  <button class="side-action-btn" @click="selectMode = false; selectedIds = []">取消</button>
                </template>
              </div>
            </div>

            <div v-if="history.length === 0" class="side-empty">
              <p>暂无历史记录</p>
            </div>

            <div v-else class="history-list">
              <div
                v-for="item in history"
                :key="item.id"
                class="history-item"
                :class="{
                  'history-item--pinned': item.isPinned,
                  'history-item--selected': selectedIds.includes(item.id)
                }"
              >
                <input
                  v-if="selectMode"
                  type="checkbox"
                  class="history-checkbox"
                  :checked="selectedIds.includes(item.id)"
                  @change="toggleSelect(item.id)"
                  @click.stop
                />
                <div class="history-item__content" @click="loadFromHistory(item)">
                  <div class="history-item__icon">
                    <el-icon :size="14"><Document /></el-icon>
                  </div>
                  <div class="history-item__info">
                    <p class="history-item__prompt">{{ item.prompt }}</p>
                    <div class="history-item__meta">
                      <span class="history-item__time">{{ timeAgo(item.time) }}</span>
                      <span v-if="item.formName" class="history-item__form">{{ item.formName }}</span>
                    </div>
                  </div>
                </div>
                <div v-if="!selectMode" class="history-item__actions">
                  <button
                    class="history-action-btn"
                    :class="{ 'history-action-btn--pinned': item.isPinned }"
                    @click.stop="togglePin(item)"
                    :title="item.isPinned ? '取消置顶' : '置顶'"
                  >
                    <el-icon :size="14"><StarFilled v-if="item.isPinned" /><Star v-else /></el-icon>
                  </button>
                  <button class="history-action-btn history-action-btn--danger" @click.stop="deleteHistory(item)">
                    <el-icon :size="14"><Delete /></el-icon>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 推荐问题 -->
        <div class="panel panel--side animate-fade-in-up" style="animation-delay: 0.2s; margin-top: 16px">
          <div class="side-section">
            <div class="side-section__header">
              <h3 class="side-section__title">
                <el-icon :size="16"><Promotion /></el-icon>
                推荐问题
              </h3>
              <button class="side-action-btn" @click="clearAll">
                <el-icon :size="14"><Delete /></el-icon>
                清空
              </button>
            </div>
            <div class="recommend-chips">
              <button
                v-for="(q, idx) in recommendedQuestions"
                :key="q"
                class="ai-chip"
                @click="handleRecommendClick(q)"
              >
                {{ q }}
              </button>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 系统表数据字典抽屉 -->
    <el-drawer
      v-model="showSystemTables"
      title="系统表数据字典"
      direction="rtl"
      size="480px"
    >
      <div class="system-tables-drawer">
        <p class="drawer-desc">OA 系统内置表，用于 ID 转名称、流程状态查询等</p>

        <div class="system-table-list">
          <div
            v-for="table in systemTables"
            :key="table.id"
            class="system-table-item"
            :class="{ 'system-table-item--expanded': expandedTable === table.id }"
          >
            <div class="system-table-header" @click="toggleTableExpand(table.id)">
              <div class="system-table-info">
                <span class="system-table-name">{{ table.tableName }}</span>
                <span class="system-table-desc">{{ table.description }}</span>
              </div>
              <el-icon class="system-table-arrow" :class="{ 'system-table-arrow--expanded': expandedTable === table.id }">
                <ArrowRight />
              </el-icon>
            </div>

            <div v-if="expandedTable === table.id" class="system-table-detail">
              <div v-if="table.joinRule" class="detail-section">
                <span class="detail-label">JOIN 规则：</span>
                <code class="detail-code">{{ table.joinRule }}</code>
              </div>

              <div class="detail-section">
                <span class="detail-label">字段列表：</span>
                <div class="field-list">
                  <div v-for="field in table.fields" :key="field.fieldName" class="field-item">
                    <span class="field-name">{{ field.fieldName }}</span>
                    <span class="field-display">{{ field.displayName }}</span>
                    <span v-if="field.isSpecial" class="field-special">特殊</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.sql-copilot {
  height: 100%;
}

.sql-copilot__row {
  height: 100%;
}

/* ═══════════════════════════════════════════════════════
   Panel Base
   ═══════════════════════════════════════════════════════ */

.panel {
  background: var(--color-bg-card);
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border-light);
  box-shadow: var(--shadow-card);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--color-border-light);
  flex-shrink: 0;
}

.panel__title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.panel__title-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: var(--color-primary-light);
  color: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.panel__title-icon--result {
  background: #f0fdf4;
  color: var(--color-success);
}

.panel__title-text {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-text-primary);
  line-height: 1.2;
}

.panel__title-desc {
  font-size: 12px;
  color: var(--color-text-muted);
  margin-top: 2px;
}

.panel__badge {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--color-success);
  font-weight: 500;
  background: #f0fdf4;
  padding: 6px 12px;
  border-radius: 20px;
}

.panel__badge-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-success);
}

/* AI Header */
.panel__header--ai {
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.04), rgba(139, 92, 246, 0.03));
}

.panel__title-icon--ai {
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  color: #fff;
}

.panel__ai-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 500;
  color: var(--color-success);
  background: #f0fdf4;
  border: 1px solid #d1fae5;
  padding: 6px 12px;
  border-radius: 20px;
}

.panel__ai-badge-dot {
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

/* ═══════════════════════════════════════════════════════
   Input Panel
   ═══════════════════════════════════════════════════════ */

.panel--input {
  margin-bottom: 16px;
}

/* Quick Templates Section */
.quick-templates-section {
  padding: 12px 20px;
  border-bottom: 1px solid var(--color-border-light);
  background: var(--color-bg-page);
}

.quick-templates-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.quick-templates-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-secondary);
}

.quick-templates-actions {
  display: flex;
  gap: 4px;
}

.quick-template-btn {
  font-size: 11px;
  color: var(--color-text-muted);
  background: none;
  border: none;
  cursor: pointer;
  padding: 2px 6px;
  border-radius: 4px;
  transition: all var(--transition-fast);
}

.quick-template-btn:hover {
  color: var(--color-primary);
  background: var(--color-primary-light);
}

.quick-templates-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.quick-template-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border-light);
  border-radius: 6px;
  cursor: pointer;
  transition: all var(--transition-fast);
  max-width: 200px;
}

.quick-template-item:hover {
  border-color: var(--color-primary);
  background: var(--color-primary-light);
}

.quick-template-item__icon {
  color: var(--color-primary);
  flex-shrink: 0;
  opacity: 0.7;
}

.quick-template-item:hover .quick-template-item__icon {
  opacity: 1;
}

.quick-template-item__text {
  font-size: 12px;
  color: var(--color-text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.quick-template-item:hover .quick-template-item__text {
  color: var(--color-primary);
}

.quick-template-item__actions {
  display: flex;
  gap: 2px;
  opacity: 0;
  transition: opacity var(--transition-fast);
}

.quick-template-item:hover .quick-template-item__actions {
  opacity: 1;
}

.quick-template-action-btn {
  width: 18px;
  height: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  border-radius: 4px;
  cursor: pointer;
  color: var(--color-text-muted);
  transition: all var(--transition-fast);
}

.quick-template-action-btn:hover {
  background: var(--color-bg-page);
  color: var(--color-text-secondary);
}

.quick-template-action-btn--danger:hover {
  background: #fef2f2;
  color: var(--color-danger);
}

/* Recommend Section */
.recommend-section {
  padding: 16px 20px;
  border-bottom: 1px solid var(--color-border-light);
}

.recommend-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.recommend-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
}

.recommend-clear {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--color-text-muted);
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast);
}

.recommend-clear:hover {
  color: var(--color-danger);
  background: #fef2f2;
}

.recommend-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

/* Input Area */
.input-area {
  padding: 16px 20px;
}

.input-wrapper {
  display: flex;
  flex-direction: column;
}

.prompt-textarea {
  flex: 1;
}

.prompt-textarea :deep(.el-textarea__inner) {
  border-radius: var(--radius-md) !important;
  border: 1.5px solid var(--color-border) !important;
  font-size: 14px;
  padding: 14px 16px;
  line-height: 1.7;
  transition: border-color var(--transition-fast), box-shadow var(--transition-fast) !important;
}

.prompt-textarea :deep(.el-textarea__inner:focus) {
  border-color: var(--color-primary) !important;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1) !important;
}

.input-footer {
  display: flex;
  justify-content: flex-end;
  padding: 6px 0 0;
}

.input-count {
  font-size: 11px;
  color: var(--color-text-muted);
}

.input-count--warn {
  color: var(--color-warning);
}

/* Actions */
.input-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--color-border-light);
}

.form-select {
  flex: 1;
}

.form-select :deep(.el-select__wrapper) {
  border-radius: var(--radius-md) !important;
  border: 1.5px solid var(--color-border) !important;
  box-shadow: none !important;
}

.form-select :deep(.el-select__wrapper:hover) {
  border-color: var(--color-primary) !important;
}

.form-select :deep(.el-select__wrapper.is-focused) {
  border-color: var(--color-primary) !important;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1) !important;
}

.input-actions__right {
  display: flex;
  gap: 8px;
}

.btn-clear {
  border-radius: var(--radius-md) !important;
  font-weight: 500 !important;
}

.btn-generate {
  border-radius: var(--radius-md) !important;
  font-weight: 600 !important;
  padding: 12px 28px !important;
  background: var(--color-primary) !important;
  border: none !important;
  box-shadow: 0 2px 4px rgba(59, 130, 246, 0.2) !important;
  transition: all var(--transition-fast) !important;
}

.btn-generate:hover {
  background: var(--color-primary-hover) !important;
  box-shadow: 0 4px 8px rgba(59, 130, 246, 0.3) !important;
}

.btn-generate:active {
  transform: translateY(0);
}

/* Loading dots */
.btn-loading {
  display: flex;
  gap: 4px;
  align-items: center;
}

.btn-loading__dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #fff;
  animation: dotPulse 1.2s ease-in-out infinite;
}

.btn-loading__dot:nth-child(2) { animation-delay: 0.2s; }
.btn-loading__dot:nth-child(3) { animation-delay: 0.4s; }

@keyframes dotPulse {
  0%, 80%, 100% { opacity: 0.3; transform: scale(0.8); }
  40% { opacity: 1; transform: scale(1); }
}

/* ═══════════════════════════════════════════════════════
   Output Panel (Code Editor)
   ═══════════════════════════════════════════════════════ */

.panel--output {
  height: 100%;
}

.result-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.result-tag {
  font-family: 'SF Mono', 'Consolas', monospace;
  font-weight: 600;
}

.result-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
  background: var(--color-bg-page);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.result-btn:hover {
  color: var(--color-primary);
  border-color: var(--color-primary);
  background: var(--color-primary-light);
}

/* Code Editor */
.code-editor {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fafbfc;
  overflow: hidden;
  min-height: 320px;
}

.code-editor__header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  background: #f1f3f5;
  border-bottom: 1px solid var(--color-border-light);
}

.code-editor__dots {
  display: flex;
  gap: 6px;
}

.code-editor__dots span {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.code-editor__dots span:nth-child(1) { background: #ff5f57; }
.code-editor__dots span:nth-child(2) { background: #febc2e; }
.code-editor__dots span:nth-child(3) { background: #28c840; }

.code-editor__filename {
  font-size: 12px;
  color: var(--color-text-muted);
  font-family: 'SF Mono', 'Consolas', monospace;
}

.code-editor__actions {
  margin-left: auto;
  display: flex;
  gap: 4px;
}

.code-editor__btn {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 4px 10px;
  font-size: 12px;
  color: var(--color-text-muted);
  background: transparent;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.code-editor__btn:hover {
  color: var(--color-text-primary);
  background: var(--color-bg-card);
  border-color: var(--color-border);
}

.code-editor__body {
  flex: 1;
  display: flex;
  overflow: auto;
  padding: 16px 0;
}

.code-editor__lines {
  display: flex;
  flex-direction: column;
  padding: 0 12px 0 16px;
  border-right: 1px solid var(--color-border-light);
  user-select: none;
  flex-shrink: 0;
}

.code-editor__line-num {
  font-size: 12px;
  color: var(--color-text-muted);
  font-family: 'SF Mono', 'Consolas', monospace;
  line-height: 22px;
  text-align: right;
  min-width: 28px;
}

.code-editor__code {
  flex: 1;
  margin: 0;
  padding: 0 16px;
  overflow-x: auto;
}

.code-editor__code code {
  font-family: 'SF Mono', 'Consolas', 'Courier New', monospace;
  font-size: 13px;
  line-height: 22px;
  color: var(--color-text-primary);
  white-space: pre;
  tab-size: 2;
}

/* SQL Syntax Colors */
:deep(.sql-kw) { color: #3b82f6; font-weight: 600; }
:deep(.sql-str) { color: #10b981; }
:deep(.sql-num) { color: #f59e0b; }
:deep(.sql-comment) { color: #94a3b8; font-style: italic; }

/* Empty State */
.code-editor__empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 40px;
}

.code-editor__empty-icon {
  width: 72px;
  height: 72px;
  border-radius: 20px;
  background: var(--color-bg-page);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-muted);
}

.code-editor__empty-text {
  font-size: 14px;
  color: var(--color-text-secondary);
  font-weight: 500;
}

.code-editor__empty-hint {
  font-size: 12px;
  color: var(--color-text-muted);
}

/* ═══════════════════════════════════════════════════════
   Right Side Panel
   ═══════════════════════════════════════════════════════ */

.panel--side {
  overflow-y: auto;
}

/* Side Section */
.side-section {
  padding: 16px;
}

.side-section__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.side-section__title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 700;
  color: var(--color-text-primary);
}

.side-section__actions {
  display: flex;
  gap: 4px;
}

.side-action-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--color-text-muted);
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast);
  font-weight: 500;
}

.side-action-btn:hover {
  color: var(--color-primary);
  background: var(--color-primary-light);
}

.side-action-btn--danger:hover {
  color: var(--color-danger);
  background: #fef2f2;
}

.side-empty {
  padding: 24px 0;
  text-align: center;
}

.side-empty p {
  font-size: 13px;
  color: var(--color-text-muted);
}

/* History List */
.history-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-height: 320px;
  overflow-y: auto;
}

.history-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: var(--radius-md);
  transition: all var(--transition-fast);
  cursor: default;
}

.history-item:hover {
  background: var(--color-bg-page);
}

.history-item--pinned {
  background: #fef3c7;
}

.history-item--pinned:hover {
  background: #fde68a;
}

.history-item--selected {
  background: var(--color-primary-light) !important;
}

.history-checkbox {
  width: 14px;
  height: 14px;
  accent-color: var(--color-primary);
  cursor: pointer;
  flex-shrink: 0;
}

.history-item__content {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  min-width: 0;
  padding-right: 4px;
}

.history-item__icon {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: var(--color-bg-page);
  color: var(--color-text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.history-item__info {
  min-width: 0;
  flex: 1;
}

.history-item__prompt {
  font-size: 13px;
  color: var(--color-text-primary);
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 1.4;
}

.history-item__meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 2px;
}

.history-item__time {
  font-size: 11px;
  color: var(--color-text-muted);
}

.history-item__form {
  font-size: 11px;
  color: var(--color-primary);
  background: var(--color-primary-light);
  padding: 1px 6px;
  border-radius: 4px;
}

.history-item__actions {
  display: flex;
  gap: 2px;
  opacity: 0;
  transition: opacity var(--transition-fast);
}

.history-item:hover .history-item__actions {
  opacity: 1;
}

.history-action-btn {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  border-radius: 6px;
  cursor: pointer;
  color: var(--color-text-muted);
  transition: all var(--transition-fast);
}

.history-action-btn:hover {
  background: var(--color-bg-page);
  color: var(--color-text-secondary);
}

.history-action-btn--pinned {
  color: var(--color-warning);
}

.history-action-btn--danger:hover {
  color: var(--color-danger);
  background: #fef2f2;
}

/* ═══════════════════════════════════════════════════════
   Responsive
   ═══════════════════════════════════════════════════════ */

@media (max-width: 1200px) {
  .sql-copilot__row > .el-col:last-child {
    display: none;
  }
}

@media (max-width: 768px) {
  .panel__header {
    padding: 14px 16px;
  }

  .input-area {
    padding: 14px 16px;
  }

  .recommend-section {
    padding: 14px 16px;
  }

  .input-actions {
    flex-direction: column;
  }

  .form-select {
    width: 100%;
  }

  .input-actions__right {
    width: 100%;
  }

  .btn-generate {
    flex: 1;
  }
}

/* ═══════════════════════════════════════════════════════
   Code Editor Enhancements
   ═══════════════════════════════════════════════════════ */

.code-editor__toolbar-right {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 8px;
}

.code-editor__lang-tag {
  font-family: 'SF Mono', 'Consolas', monospace;
  font-weight: 600;
  font-size: 11px;
}

.code-editor__ai-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--color-primary);
  font-weight: 500;
}

.code-editor__ai-spinner {
  width: 12px;
  height: 12px;
  border: 2px solid rgba(59, 130, 246, 0.2);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.code-editor__skeleton {
  padding: 16px;
  width: 100%;
}

.code-editor__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
  background: #f1f3f5;
  border-top: 1px solid var(--color-border-light);
}

.code-editor__footer-left,
.code-editor__footer-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.code-editor__status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--color-success);
  font-weight: 500;
}

.code-editor__status--loading {
  color: var(--color-primary);
}

.code-editor__status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-success);
}

.code-editor__encoding {
  font-size: 11px;
  color: var(--color-text-muted);
  font-family: 'SF Mono', 'Consolas', monospace;
}

.code-editor__ai-badge {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: var(--color-primary);
  font-weight: 500;
  opacity: 0.7;
}

/* ═══════════════════════════════════════════════════════
   System Tables Drawer
   ═══════════════════════════════════════════════════════ */

.system-tables-drawer {
  padding: 0 4px;
}

.drawer-desc {
  font-size: 13px;
  color: var(--color-text-muted);
  margin-bottom: 20px;
  line-height: 1.5;
}

.system-table-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.system-table-item {
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-md);
  overflow: hidden;
  transition: all var(--transition-fast);
}

.system-table-item:hover {
  border-color: var(--color-border);
}

.system-table-item--expanded {
  border-color: var(--color-primary);
  background: var(--color-primary-light);
}

.system-table-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  cursor: pointer;
  transition: background var(--transition-fast);
}

.system-table-header:hover {
  background: var(--color-bg-page);
}

.system-table-item--expanded .system-table-header {
  background: transparent;
}

.system-table-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.system-table-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
  font-family: 'SF Mono', 'Consolas', monospace;
}

.system-table-desc {
  font-size: 12px;
  color: var(--color-text-muted);
}

.system-table-arrow {
  color: var(--color-text-muted);
  transition: transform var(--transition-fast);
  flex-shrink: 0;
}

.system-table-arrow--expanded {
  transform: rotate(90deg);
  color: var(--color-primary);
}

.system-table-detail {
  padding: 0 14px 14px;
  border-top: 1px solid var(--color-border-light);
}

.detail-section {
  margin-top: 12px;
}

.detail-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-secondary);
  display: block;
  margin-bottom: 6px;
}

.detail-code {
  font-size: 12px;
  font-family: 'SF Mono', 'Consolas', monospace;
  background: var(--color-bg-page);
  padding: 6px 10px;
  border-radius: 6px;
  display: block;
  word-break: break-all;
  color: var(--color-primary);
}

.field-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-height: 200px;
  overflow-y: auto;
}

.field-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  background: var(--color-bg-page);
  border-radius: 6px;
  font-size: 12px;
}

.field-name {
  font-family: 'SF Mono', 'Consolas', monospace;
  font-weight: 600;
  color: var(--color-text-primary);
  min-width: 120px;
}

.field-display {
  color: var(--color-text-secondary);
  flex: 1;
}

.field-special {
  font-size: 10px;
  color: var(--color-warning);
  background: #fef3c7;
  padding: 2px 6px;
  border-radius: 4px;
  font-weight: 600;
}
</style>
