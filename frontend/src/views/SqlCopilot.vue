<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

// ── 状态 ──

const prompt = ref('')
const sqlOutput = ref('')
const generating = ref(false)
const selectedFormCode = ref('')
const availableForms = ref([])

// ── 快捷模板 ──

const templates = [
  '查询 formmain_1001 中本月所有请假单',
  '查询 OA 待办流程列表',
  '查询 EBS 订单数据关联客户信息',
  '查询 formmain_2003 中状态为审批中的记录',
  '统计各表单本月提交数量'
]

function useTemplate(text) {
  prompt.value = text
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
            <span class="templates-label">快捷模板：</span>
            <el-tag
              v-for="t in templates"
              :key="t"
              size="small"
              class="template-tag"
              @click="useTemplate(t)"
            >{{ t }}</el-tag>
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
            <span class="card-title">历史记录</span>
          </template>

          <div v-if="history.length === 0" class="history-empty">
            <el-empty description="暂无记录" :image-size="60" />
          </div>

          <div v-else class="history-list">
            <div
              v-for="item in history"
              :key="item.id"
              class="history-item"
              @click="loadFromHistory(item)"
            >
              <div class="history-prompt">{{ item.prompt }}</div>
              <div class="history-time">{{ item.time }}</div>
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
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.templates-label {
  font-size: 13px;
  color: #8c8c8c;
  flex-shrink: 0;
}

.template-tag {
  cursor: pointer;
  font-weight: 400;
}

.template-tag:hover {
  color: #409eff;
  border-color: #409eff;
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

.history-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.history-item {
  padding: 10px 12px;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.15s;
}

.history-item:hover {
  background-color: #f5f7fa;
}

.history-prompt {
  font-size: 13px;
  color: #262626;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.history-time {
  font-size: 12px;
  color: #bfbfbf;
  margin-top: 4px;
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
