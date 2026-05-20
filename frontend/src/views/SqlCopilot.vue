<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Star, StarFilled, Edit, Close, Delete } from '@element-plus/icons-vue'
import axios from 'axios'

// ── 状态 ──

const prompt = ref('')
const sqlOutput = ref('')
const generating = ref(false)
const selectedFormCode = ref('')
const availableForms = ref([])
const selectedIds = ref([])
const selectMode = ref(false)

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
  } catch (e) {
    // 取消
  }
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
  } catch (e) {
    // 取消
  }
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

// ── 表单数据字典（模糊搜索） ──

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

async function loadHistory() {
  try {
    const { data } = await axios.get('/api/sql/history', { params: { limit: 20 } })
    history.value = data.map(item => ({
      id: item.id,
      prompt: item.prompt,
      sql: item.sqlResult,
      isPinned: item.pinned,
      time: formatTime(item.createTime)
    }))
  } catch (e) {
    console.error('加载历史记录失败', e)
  }
}

function formatTime(timeStr) {
  if (!timeStr) return ''
  // "2026-05-13T10:32:00" -> "10:32"
  const parts = timeStr.split('T')
  if (parts.length === 2) {
    return parts[1].substring(0, 5)
  }
  return timeStr
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
    await ElMessageBox.confirm(
      '确定删除该记录？',
      '确认删除',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    await axios.delete(`/api/sql/history/${item.id}`)
    await loadHistory()
    ElMessage.success('已删除')
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

function toggleSelect(id) {
  const idx = selectedIds.value.indexOf(id)
  if (idx >= 0) {
    selectedIds.value.splice(idx, 1)
  } else {
    selectedIds.value.push(id)
  }
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
    if (e !== 'cancel') {
      ElMessage.error('批量删除失败')
    }
  }
}

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
    // 自动刷新历史列表
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
  ElMessage.success('SQL 已复制到剪贴板')
}

function loadFromHistory(item) {
  prompt.value = item.prompt
  sqlOutput.value = item.sql
}

function clearAll() {
  prompt.value = ''
  sqlOutput.value = ''
}

// ── 初始化 ──

onMounted(() => {
  loadHistory()
  loadAvailableForms()
  loadTemplates()
})
</script>

<template>
  <div class="sql-copilot">
    <el-row :gutter="16">
      <!-- 左侧：主区域 -->
      <el-col :xs="24" :lg="17">
        <!-- 输入区 -->
        <el-card shadow="never">
          <template #header>
            <span class="card-title">SQL 生成</span>
          </template>

          <div class="templates">
            <div class="templates-header">
              <span class="templates-label">快捷模板：</span>
              <div class="templates-actions">
                <el-button size="small" text type="primary" @click="addTemplate">新增</el-button>
                <el-button size="small" text type="success" @click="setAsDefault">设为默认</el-button>
                <el-button size="small" text type="info" @click="resetTemplates">恢复默认</el-button>
              </div>
            </div>
            <div class="templates-list">
              <div
                v-for="(t, idx) in quickTemplates"
                :key="idx"
                class="template-item"
              >
                <el-tag
                  size="small"
                  class="template-tag"
                  @click="useTemplate(t)"
                >{{ t }}</el-tag>
                <el-button
                  size="small"
                  text
                  type="primary"
                  class="template-edit-btn"
                  @click.stop="editTemplate(idx)"
                >
                  <el-icon><Edit /></el-icon>
                </el-button>
                <el-button
                  size="small"
                  text
                  type="danger"
                  class="template-delete-btn"
                  @click.stop="removeTemplate(idx)"
                >
                  <el-icon><Close /></el-icon>
                </el-button>
              </div>
            </div>
          </div>

          <div class="form-select">
            <span class="form-select-label">数据字典：</span>
            <el-select
              v-model="selectedFormCode"
              placeholder="输入关键词搜索表单（可选）"
              clearable
              filterable
              remote
              :remote-method="onFormSearch"
              @change="onFormChange"
              size="default"
              style="width: 320px"
            >
              <el-option
                v-for="form in availableForms"
                :key="form.formCode"
                :label="form.formName + ' (' + form.tableName + ')'"
                :value="form.formCode"
              />
            </el-select>
            <span class="form-select-hint">输入关键词模糊搜索，如"采购"、"付款"</span>
          </div>

          <el-input
            v-model="prompt"
            type="textarea"
            :rows="4"
            placeholder="描述你需要的 SQL 查询，例如：查询 formmain_1001 中本月所有请假单"
            class="prompt-input"
          />

          <div class="actions">
            <el-button
              type="primary"
              :loading="generating"
              @click="generate"
            >
              <el-icon v-if="!generating"><Promotion /></el-icon>
              {{ generating ? '生成中...' : '生成 SQL' }}
            </el-button>
            <el-button @click="clearAll">清空</el-button>
          </div>
        </el-card>

        <!-- 输出区 -->
        <el-card shadow="never" class="output-card">
          <template #header>
            <div class="output-header">
              <span class="card-title">生成结果</span>
              <el-button
                v-if="sqlOutput"
                size="small"
                type="primary"
                plain
                @click="copySql"
              >
                <el-icon><CopyDocument /></el-icon>
                复制
              </el-button>
            </div>
          </template>

          <div v-if="sqlOutput" class="sql-output">
            <pre><code>{{ sqlOutput }}</code></pre>
          </div>
          <el-empty v-else description="输入需求后点击「生成 SQL」" :image-size="80" />
        </el-card>
      </el-col>

      <!-- 右侧：历史记录 -->
      <el-col :xs="24" :lg="7">
        <el-card shadow="never" class="history-card">
          <template #header>
            <div class="history-header">
              <span class="card-title">历史记录</span>
              <div class="history-header-actions">
                <el-button
                  v-if="history.length > 0 && !selectMode"
                  size="small"
                  text
                  @click="selectMode = true"
                >
                  选择
                </el-button>
                <template v-if="selectMode">
                  <el-button size="small" text @click="toggleSelectAll">
                    {{ selectedIds.length === history.length ? '取消全选' : '全选' }}
                  </el-button>
                  <el-button
                    v-if="selectedIds.length > 0"
                    type="danger"
                    size="small"
                    text
                    @click="batchDelete"
                  >
                    删除 ({{ selectedIds.length }})
                  </el-button>
                  <el-button size="small" text @click="selectMode = false; selectedIds = []">取消</el-button>
                </template>
              </div>
            </div>
          </template>

          <div v-if="history.length === 0" class="history-empty">
            <el-empty description="暂无记录" :image-size="60" />
          </div>

          <div v-else class="history-list">
            <div
              v-for="item in history"
              :key="item.id"
              class="history-item"
              :class="{ 'history-item-pinned': item.isPinned, 'history-item-selected': selectedIds.includes(item.id) }"
            >
              <el-checkbox
                v-if="selectMode"
                :model-value="selectedIds.includes(item.id)"
                @change="toggleSelect(item.id)"
                @click.stop
                class="history-checkbox"
              />
              <el-tooltip :content="item.prompt" placement="left" :show-after="300">
                <div class="history-content" @click="loadFromHistory(item)">
                  <div class="history-prompt">
                    <el-icon v-if="item.isPinned" class="pin-icon"><StarFilled /></el-icon>
                    {{ item.prompt }}
                  </div>
                  <div class="history-time">{{ item.time }}</div>
                </div>
              </el-tooltip>
              <div v-if="!selectMode" class="history-actions">
                <el-button
                  :type="item.isPinned ? 'warning' : 'info'"
                  size="small"
                  text
                  @click.stop="togglePin(item)"
                  :title="item.isPinned ? '取消置顶' : '置顶'"
                >
                  <el-icon><StarFilled v-if="item.isPinned" /><Star v-else /></el-icon>
                </el-button>
                <el-button
                  type="danger"
                  size="small"
                  text
                  @click.stop="deleteHistory(item)"
                >
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.sql-copilot {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.card-title {
  font-size: 15px;
  font-weight: 500;
  color: #262626;
}

/* 快捷模板 */
.templates {
  margin-bottom: 12px;
}

.templates-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.templates-actions {
  display: flex;
  gap: 4px;
}

.templates-label {
  font-size: 13px;
  color: #8c8c8c;
  flex-shrink: 0;
}

.templates-list {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
}

.template-item {
  display: inline-flex;
  align-items: center;
  gap: 1px;
}

.template-tag {
  cursor: pointer;
  font-weight: 400;
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.template-tag:hover {
  color: #409eff;
  border-color: #409eff;
}

.template-edit-btn,
.template-delete-btn {
  padding: 2px !important;
  height: auto !important;
  opacity: 0;
  transition: opacity 0.15s;
}

.template-item:hover .template-edit-btn,
.template-item:hover .template-delete-btn {
  opacity: 1;
}

.template-edit-btn .el-icon,
.template-delete-btn .el-icon {
  font-size: 12px;
}

/* 表单选择 */
.form-select {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.form-select-label {
  font-size: 13px;
  color: #8c8c8c;
  flex-shrink: 0;
}

.form-select-hint {
  font-size: 12px;
  color: #bfbfbf;
}

/* 输入 */
.prompt-input {
  margin-bottom: 12px;
}

:deep(.el-textarea__inner) {
  font-size: 14px;
  font-family: 'Consolas', 'Monaco', monospace;
}

/* 按钮 */
.actions {
  display: flex;
  gap: 8px;
}

/* 输出区 */
.output-card {
  margin-top: 16px;
}

.output-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.sql-output {
  background: #1e1e1e;
  border-radius: 6px;
  padding: 16px;
  overflow-x: auto;
}

.sql-output pre {
  margin: 0;
}

.sql-output code {
  color: #d4d4d4;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre;
}

/* 历史记录 */
.history-card {
  height: 100%;
}

.history-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.history-header-actions {
  display: flex;
  gap: 4px;
  align-items: center;
}

.history-checkbox {
  margin-right: 8px;
  flex-shrink: 0;
}

.history-item-selected {
  background-color: #e6f7ff !important;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.history-item {
  padding: 10px 12px;
  border-radius: 4px;
  transition: background-color 0.15s;
  display: flex;
  align-items: center;
  position: relative;
}

.history-item:hover {
  background-color: #f5f7fa;
}

.history-item:hover .history-actions {
  opacity: 1;
}

.history-item-pinned {
  background-color: #fffbe6;
}

.history-item-pinned:hover {
  background-color: #fff7cc;
}

.history-content {
  flex: 1;
  cursor: pointer;
  min-width: 0;
  padding-right: 60px;
}

.history-prompt {
  font-size: 13px;
  color: #262626;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pin-icon {
  color: #faad14;
  margin-right: 4px;
  font-size: 12px;
}

.history-time {
  font-size: 12px;
  color: #bfbfbf;
  margin-top: 4px;
}

.history-actions {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  gap: 2px;
  opacity: 0;
  transition: opacity 0.15s;
  background: inherit;
  padding-left: 8px;
}

.history-empty {
  padding: 20px 0;
}

:deep(.el-card__header) {
  padding: 14px 20px;
  border-bottom: 1px solid #f0f0f0;
}

:deep(.el-card__body) {
  padding: 16px 20px;
}
</style>
