<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Delete, Download, Search } from '@element-plus/icons-vue'
import axios from 'axios'

// ── 状态 ──

const logFiles = ref([])
const currentFile = ref('app.log')
const logLines = ref([])
const totalLines = ref(0)
const currentPage = ref(1)
const pageSize = ref(100)
const loading = ref(false)
const filterLevel = ref('')
const filterKeyword = ref('')
const autoRefresh = ref(false)
let refreshTimer = null

// 日志统计
const logStats = ref({ total: 0, errors: 0, warns: 0, infos: 0, size: '0 B' })

// ── 加载日志文件列表 ──

async function loadLogFiles() {
  try {
    const { data } = await axios.get('/api/logs/files')
    logFiles.value = data
    if (data.length > 0 && !currentFile.value) {
      currentFile.value = data[0].name
    }
  } catch (e) {
    console.error('加载日志文件列表失败', e)
  }
}

// ── 加载日志统计 ──

async function loadStats() {
  try {
    const { data } = await axios.get(`/api/logs/stats/${currentFile.value}`)
    logStats.value = data
  } catch (e) {
    console.error('加载统计失败', e)
  }
}

// ── 加载日志内容 ──

async function loadLogs() {
  loading.value = true
  try {
    const params = {
      fileName: currentFile.value,
      page: currentPage.value,
      pageSize: pageSize.value
    }
    if (filterLevel.value) params.level = filterLevel.value
    if (filterKeyword.value) params.keyword = filterKeyword.value

    const { data } = await axios.get('/api/logs/read', { params })
    logLines.value = data.lines || []
    totalLines.value = data.total || 0

    // 同时刷新统计
    loadStats()
  } catch (e) {
    console.error('加载日志失败', e)
    ElMessage.error('加载日志失败')
  } finally {
    loading.value = false
  }
}

// ── 刷新 ──

function refresh() {
  currentPage.value = 1
  loadLogs()
}

// ── 切换文件 ──

function switchFile(fileName) {
  currentFile.value = fileName
  currentPage.value = 1
  loadLogs()
}

// ── 清除日志 ──

async function clearLog() {
  try {
    await ElMessageBox.confirm('确定要清除当前日志文件吗？', '确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await axios.delete(`/api/logs/clear/${currentFile.value}`)
    ElMessage.success('日志已清除')
    loadLogs()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('清除失败')
    }
  }
}

// ── 删除日志文件 ──

async function deleteLog() {
  try {
    await ElMessageBox.confirm('确定要删除当前日志文件吗？此操作不可恢复！', '确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await axios.delete(`/api/logs/delete/${currentFile.value}`)
    ElMessage.success('日志文件已删除')
    currentFile.value = 'app.log'
    await loadLogFiles()
    loadLogs()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// ── 导出日志 ──

function exportLog() {
  const url = `/api/logs/export/${currentFile.value}`
  window.open(url, '_blank')
  ElMessage.success('正在导出...')
}

function exportFilteredLog() {
  let url = `/api/logs/export-filtered/${currentFile.value}?`
  if (filterLevel.value) url += `level=${filterLevel.value}&`
  if (filterKeyword.value) url += `keyword=${filterKeyword.value}`
  window.open(url, '_blank')
  ElMessage.success('正在导出筛选后的日志...')
}

// ── 自动刷新 ──

function toggleAutoRefresh() {
  if (autoRefresh.value) {
    refreshTimer = setInterval(() => {
      loadLogs()
    }, 5000)
  } else {
    if (refreshTimer) {
      clearInterval(refreshTimer)
      refreshTimer = null
    }
  }
}

// ── 获取日志级别样式 ──

function getLevelClass(line) {
  if (line.includes('ERROR')) return 'level-error'
  if (line.includes('WARN')) return 'level-warn'
  if (line.includes('INFO')) return 'level-info'
  if (line.includes('DEBUG')) return 'level-debug'
  return ''
}

// ── 分页 ──

const totalPages = computed(() => Math.ceil(totalLines.value / pageSize.value))

function prevPage() {
  if (currentPage.value > 1) {
    currentPage.value--
    loadLogs()
  }
}

function nextPage() {
  if (currentPage.value < totalPages.value) {
    currentPage.value++
    loadLogs()
  }
}

// ── 初始化 ──

onMounted(async () => {
  await loadLogFiles()
  loadLogs()
})

onUnmounted(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
  }
})
</script>

<template>
  <div class="system-log">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="page-title">
        <span>系统运行日志</span>
      </div>
      <div class="page-desc">查看应用运行日志，支持筛选和导出</div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-cards">
      <div class="stat-card">
        <span class="stat-value">{{ logStats.total }}</span>
        <span class="stat-label">总日志数</span>
      </div>
      <div class="stat-card stat-error">
        <span class="stat-value">{{ logStats.errors }}</span>
        <span class="stat-label">ERROR</span>
      </div>
      <div class="stat-card stat-warn">
        <span class="stat-value">{{ logStats.warns }}</span>
        <span class="stat-label">WARN</span>
      </div>
      <div class="stat-card stat-info">
        <span class="stat-value">{{ logStats.infos }}</span>
        <span class="stat-label">INFO</span>
      </div>
      <div class="stat-card">
        <span class="stat-value">{{ logStats.size }}</span>
        <span class="stat-label">文件大小</span>
      </div>
    </div>

    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <!-- 日志文件选择 -->
        <div class="file-selector">
          <button
            v-for="file in logFiles"
            :key="file.name"
            :class="['file-btn', { active: currentFile === file.name }]"
            @click="switchFile(file.name)"
          >
            <span class="file-name">{{ file.name }}</span>
            <span class="file-size">{{ file.size }}</span>
          </button>
        </div>
      </div>

      <div class="toolbar-right">
        <!-- 筛选 -->
        <el-select v-model="filterLevel" placeholder="日志级别" clearable style="width: 120px" @change="refresh">
          <el-option label="ERROR" value="ERROR" />
          <el-option label="WARN" value="WARN" />
          <el-option label="INFO" value="INFO" />
          <el-option label="DEBUG" value="DEBUG" />
        </el-select>

        <el-input
          v-model="filterKeyword"
          placeholder="搜索关键词"
          clearable
          style="width: 200px"
          @keyup.enter="refresh"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>

        <!-- 操作按钮 -->
        <el-button :icon="Refresh" @click="refresh" :loading="loading">刷新</el-button>
        <el-button @click="toggleAutoRefresh" :type="autoRefresh ? 'success' : 'default'">
          {{ autoRefresh ? '停止' : '自动刷新' }}
        </el-button>
        <el-button :icon="Download" @click="exportLog">导出</el-button>
        <el-button v-if="filterLevel || filterKeyword" :icon="Download" @click="exportFilteredLog">
          导出筛选
        </el-button>
        <el-button type="danger" :icon="Delete" @click="clearLog">清除</el-button>
      </div>
    </div>

    <!-- 日志统计 -->
    <div class="log-stats">
      <span>共 {{ totalLines }} 条日志</span>
      <span v-if="filterLevel"> | 级别: {{ filterLevel }}</span>
      <span v-if="filterKeyword"> | 搜索: {{ filterKeyword }}</span>
      <span> | 第 {{ currentPage }} / {{ totalPages }} 页</span>
    </div>

    <!-- 日志内容 -->
    <div class="log-content" v-loading="loading">
      <div v-if="logLines.length === 0" class="empty-state">
        暂无日志数据
      </div>
      <div v-else class="log-list">
        <div
          v-for="(line, index) in logLines"
          :key="index"
          :class="['log-line', getLevelClass(line)]"
        >
          <pre>{{ line }}</pre>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div class="pagination" v-if="totalPages > 1">
      <el-button :disabled="currentPage <= 1" @click="prevPage">上一页</el-button>
      <span class="page-info">{{ currentPage }} / {{ totalPages }}</span>
      <el-button :disabled="currentPage >= totalPages" @click="nextPage">下一页</el-button>
    </div>
  </div>
</template>

<style scoped>
.system-log {
  padding: 24px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.page-header {
  margin-bottom: 20px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 8px;
}

.page-desc {
  font-size: 14px;
  color: var(--color-text-secondary);
}

/* 统计卡片 */
.stats-cards {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.stat-card {
  flex: 1;
  min-width: 100px;
  background: white;
  border: 1px solid var(--color-border-light);
  border-radius: 12px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--color-text-primary);
}

.stat-label {
  font-size: 12px;
  color: var(--color-text-secondary);
}

.stat-error .stat-value {
  color: #f44747;
}

.stat-warn .stat-value {
  color: #cca700;
}

.stat-info .stat-value {
  color: #4ec9b0;
}

/* 工具栏 */
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.file-selector {
  display: flex;
  gap: 8px;
}

.file-btn {
  padding: 8px 16px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: white;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  transition: all 0.2s;
}

.file-btn:hover {
  border-color: var(--color-primary);
}

.file-btn.active {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: white;
}

.file-name {
  font-size: 13px;
  font-weight: 500;
}

.file-size {
  font-size: 11px;
  opacity: 0.7;
}

.log-stats {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin-bottom: 12px;
}

/* 日志内容 */
.log-content {
  flex: 1;
  background: #1e1e1e;
  border-radius: 8px;
  padding: 16px;
  overflow: auto;
  min-height: 400px;
  max-height: calc(100vh - 380px);
}

.empty-state {
  text-align: center;
  color: #666;
  padding: 40px;
}

.log-list {
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 12px;
  line-height: 1.6;
}

.log-line {
  padding: 2px 8px;
  border-radius: 2px;
}

.log-line:hover {
  background: rgba(255, 255, 255, 0.1);
}

.log-line pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
  color: #d4d4d4;
}

.log-line.level-error pre {
  color: #f44747;
}

.log-line.level-warn pre {
  color: #cca700;
}

.log-line.level-info pre {
  color: #4ec9b0;
}

.log-line.level-debug pre {
  color: #808080;
}

/* 分页 */
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  margin-top: 16px;
  padding: 12px;
}

.page-info {
  font-size: 14px;
  color: var(--color-text-secondary);
}
</style>
