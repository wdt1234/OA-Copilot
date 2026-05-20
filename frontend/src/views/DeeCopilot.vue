<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Star, StarFilled, Delete } from '@element-plus/icons-vue'
import axios from 'axios'

// ── 模板类型 ──

const templateTypes = [
  { key: 'workflow', label: 'Workflow 模板', icon: 'Share', desc: '流程节点定义' },
  { key: 'token', label: 'Token 调用', icon: 'Key', desc: '认证接口配置' },
  { key: 'json', label: 'JSON 映射', icon: 'DocumentCopy', desc: '字段映射模板' },
  { key: 'java', label: 'Java 脚本', icon: 'Coffee', desc: 'DEE Handler 代码' }
]

// ── 状态 ──

const selectedType = ref('workflow')
const description = ref('')
const resultJson = ref('')
const generating = ref(false)
const selectedIds = ref([])
const selectMode = ref(false)

// ── 快捷模板 ──

const quickDescs = {
  workflow: '生成请假审批流程模板，包含发起、部门审批、人事备案三个节点',
  token: '生成获取 OA 认证 token 的接口调用配置',
  json: '生成 formmain_2003 采购申请表单的字段映射模板',
  java: '生成 DEE 工作流回调 Handler，调用 ERP 下单接口'
}

function useQuickDesc() {
  description.value = quickDescs[selectedType.value] || ''
}

// ── 历史记录 ──

const history = ref([])

async function loadHistory() {
  try {
    const { data } = await axios.get('/api/dee/history', { params: { limit: 20 } })
    history.value = data.map(item => ({
      id: item.id,
      type: item.templateType,
      desc: item.description,
      result: item.resultJson,
      isPinned: item.pinned,
      time: formatTime(item.createTime)
    }))
  } catch (e) {
    console.error('加载 DEE 历史失败', e)
  }
}

function formatTime(timeStr) {
  if (!timeStr) return ''
  const parts = timeStr.split('T')
  if (parts.length === 2) return parts[1].substring(0, 5)
  return timeStr
}

function typeLabel(key) {
  const found = templateTypes.find(t => t.key === key)
  return found ? found.label : key
}

async function togglePin(item) {
  try {
    await axios.put(`/api/dee/history/${item.id}/pin`)
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
    await axios.delete(`/api/dee/history/${item.id}`)
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
    await axios.delete('/api/dee/history/batch', { data: { ids: selectedIds.value } })
    selectedIds.value = []
    await loadHistory()
    ElMessage.success('批量删除完成')
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('批量删除失败')
    }
  }
}

// ── 生成 ──

async function generate() {
  if (!description.value.trim()) {
    ElMessage.warning('请输入模板需求描述')
    return
  }

  generating.value = true
  resultJson.value = ''

  try {
    const { data } = await axios.post('/api/dee/generate', {
      templateType: selectedType.value,
      description: description.value
    })
    resultJson.value = data.result
    await loadHistory()
  } catch (e) {
    ElMessage.error('生成失败，请检查后端服务')
    console.error('生成 DEE 模板失败', e)
  } finally {
    generating.value = false
  }
}

function copyResult() {
  if (!resultJson.value) return
  navigator.clipboard.writeText(resultJson.value)
  ElMessage.success('已复制到剪贴板')
}

function loadFromHistory(item) {
  selectedType.value = item.type
  description.value = item.desc
  resultJson.value = item.result
}

function clearAll() {
  description.value = ''
  resultJson.value = ''
}

onMounted(() => {
  loadHistory()
})
</script>

<template>
  <div class="dee-copilot">
    <el-row :gutter="16">
      <!-- 左侧：主区域 -->
      <el-col :xs="24" :lg="17">
        <!-- 模板类型选择 -->
        <el-card shadow="never">
          <template #header>
            <span class="card-title">模板类型</span>
          </template>

          <div class="type-grid">
            <div
              v-for="t in templateTypes"
              :key="t.key"
              class="type-item"
              :class="{ active: selectedType === t.key }"
              @click="selectedType = t.key"
            >
              <el-icon :size="24"><component :is="t.icon" /></el-icon>
              <div class="type-label">{{ t.label }}</div>
              <div class="type-desc">{{ t.desc }}</div>
            </div>
          </div>
        </el-card>

        <!-- 输入区 -->
        <el-card shadow="never" class="input-card">
          <template #header>
            <div class="input-header">
              <span class="card-title">需求描述</span>
              <el-button size="small" text type="primary" @click="useQuickDesc">
                填入示例
              </el-button>
            </div>
          </template>

          <el-input
            v-model="description"
            type="textarea"
            :rows="3"
            placeholder="描述你需要的 DEE 模板，例如：生成请假审批流程模板"
          />

          <div class="actions">
            <el-button
              type="primary"
              :loading="generating"
              @click="generate"
            >
              <el-icon v-if="!generating"><Promotion /></el-icon>
              {{ generating ? '生成中...' : '生成模板' }}
            </el-button>
            <el-button @click="clearAll">清空</el-button>
          </div>
        </el-card>

        <!-- 结果区 -->
        <el-card shadow="never" class="result-card">
          <template #header>
            <div class="result-header">
              <span class="card-title">生成结果</span>
              <el-button
                v-if="resultJson"
                size="small"
                type="primary"
                plain
                @click="copyResult"
              >
                <el-icon><CopyDocument /></el-icon>
                复制
              </el-button>
            </div>
          </template>

          <div v-if="resultJson" class="json-output">
            <pre><code>{{ resultJson }}</code></pre>
          </div>
          <el-empty v-else description="选择模板类型并输入需求后点击「生成模板」" :image-size="80" />
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
              <el-tooltip :content="item.desc" placement="left" :show-after="300">
                <div class="history-content" @click="loadFromHistory(item)">
                  <div class="history-meta">
                    <el-tag size="small" type="info">{{ typeLabel(item.type) }}</el-tag>
                    <span class="history-time">{{ item.time }}</span>
                  </div>
                  <div class="history-desc">
                    <el-icon v-if="item.isPinned" class="pin-icon"><StarFilled /></el-icon>
                    {{ item.desc }}
                  </div>
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
.dee-copilot {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.card-title {
  font-size: 15px;
  font-weight: 500;
  color: #262626;
}

/* 模板类型选择 */
.type-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.type-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 16px 8px;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s;
  text-align: center;
}

.type-item:hover {
  border-color: #409eff;
  color: #409eff;
}

.type-item.active {
  border-color: #409eff;
  background: #ecf5ff;
  color: #409eff;
}

.type-label {
  font-size: 13px;
  font-weight: 500;
}

.type-desc {
  font-size: 11px;
  color: #8c8c8c;
}

.type-item.active .type-desc {
  color: #66b1ff;
}

/* 输入区 */
.input-card {
  margin-top: 16px;
}

.input-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}

/* 结果区 */
.result-card {
  margin-top: 16px;
}

.result-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.json-output {
  background: #1e1e1e;
  border-radius: 6px;
  padding: 16px;
  overflow-x: auto;
}

.json-output pre {
  margin: 0;
}

.json-output code {
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
  gap: 4px;
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

.history-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.history-time {
  font-size: 12px;
  color: #bfbfbf;
}

.history-desc {
  font-size: 13px;
  color: #262626;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  overflow: hidden;
}

.pin-icon {
  color: #faad14;
  margin-right: 4px;
  font-size: 12px;
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

@media (max-width: 1200px) {
  .type-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
