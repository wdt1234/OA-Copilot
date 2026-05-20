<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Star, StarFilled, Delete } from '@element-plus/icons-vue'
import axios from 'axios'

// ── 状态 ──

const formId = ref('')
const inputFields = ref('')
const resultJson = ref('')
const mappingFields = ref([])
const generating = ref(false)
const showTable = ref(true)
const selectedIds = ref([])
const selectMode = ref(false)

// ── 快捷表单 ──

const quickForms = [
  { id: 'formmain_1001', name: '请假申请单' },
  { id: 'formmain_2002', name: '报销申请单' },
  { id: 'formmain_2003', name: '采购申请单' }
]

function useQuickForm(item) {
  formId.value = item.id
  inputFields.value = `field0001\nfield0002\nfield0003\nfield0004\nfield0005\nfield0006`
}

// ── 历史记录 ──

const history = ref([])

async function loadHistory() {
  try {
    const { data } = await axios.get('/api/field-mapping/history', { params: { limit: 20 } })
    history.value = data.map(item => ({
      id: item.id,
      formId: item.formId,
      result: item.resultJson,
      isPinned: item.pinned,
      time: formatTime(item.createTime)
    }))
  } catch (e) {
    console.error('加载映射历史失败', e)
  }
}

function formatTime(timeStr) {
  if (!timeStr) return ''
  const parts = timeStr.split('T')
  if (parts.length === 2) return parts[1].substring(0, 5)
  return timeStr
}

async function togglePin(item) {
  try {
    await axios.put(`/api/field-mapping/history/${item.id}/pin`)
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
    await axios.delete(`/api/field-mapping/history/${item.id}`)
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
    await axios.delete('/api/field-mapping/history/batch', { data: { ids: selectedIds.value } })
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
  if (!formId.value.trim() && !inputFields.value.trim()) {
    ElMessage.warning('请输入表单 ID 或字段列表')
    return
  }

  generating.value = true
  resultJson.value = ''
  mappingFields.value = []

  try {
    const { data } = await axios.post('/api/field-mapping/generate', {
      formId: formId.value,
      inputFields: inputFields.value
    })
    resultJson.value = data.result

    // 解析为表格数据
    try {
      const parsed = JSON.parse(data.result)
      mappingFields.value = parsed.fields || []
    } catch {
      mappingFields.value = []
    }

    await loadHistory()
  } catch (e) {
    ElMessage.error('生成失败，请检查后端服务')
    console.error('字段映射失败', e)
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
  formId.value = item.formId
  resultJson.value = item.result
  try {
    const parsed = JSON.parse(item.result)
    mappingFields.value = parsed.fields || []
  } catch {
    mappingFields.value = []
  }
}

function clearAll() {
  formId.value = ''
  inputFields.value = ''
  resultJson.value = ''
  mappingFields.value = []
}

onMounted(() => {
  loadHistory()
})
</script>

<template>
  <div class="field-mapper">
    <el-row :gutter="16">
      <!-- 左侧：主区域 -->
      <el-col :xs="24" :lg="17">
        <!-- 输入区 -->
        <el-card shadow="never">
          <template #header>
            <span class="card-title">字段映射</span>
          </template>

          <div class="quick-forms">
            <span class="quick-label">快捷表单：</span>
            <el-tag
              v-for="item in quickForms"
              :key="item.id"
              size="small"
              class="quick-tag"
              @click="useQuickForm(item)"
            >{{ item.id }} ({{ item.name }})</el-tag>
          </div>

          <el-row :gutter="12">
            <el-col :span="8">
              <div class="input-label">表单 ID</div>
              <el-input
                v-model="formId"
                placeholder="如 formmain_1001"
              />
            </el-col>
            <el-col :span="16">
              <div class="input-label">字段列表（每行一个）</div>
              <el-input
                v-model="inputFields"
                type="textarea"
                :rows="3"
                placeholder="field0001&#10;field0002&#10;field0003"
              />
            </el-col>
          </el-row>

          <div class="actions">
            <el-button
              type="primary"
              :loading="generating"
              @click="generate"
            >
              <el-icon v-if="!generating"><Promotion /></el-icon>
              {{ generating ? '映射中...' : '生成映射' }}
            </el-button>
            <el-button @click="clearAll">清空</el-button>
          </div>
        </el-card>

        <!-- 映射结果表格 -->
        <el-card v-if="mappingFields.length > 0" shadow="never" class="result-card">
          <template #header>
            <div class="result-header">
              <span class="card-title">映射结果</span>
              <el-button
                size="small"
                type="primary"
                plain
                @click="copyResult"
              >
                <el-icon><CopyDocument /></el-icon>
                复制 JSON
              </el-button>
            </div>
          </template>

          <el-table :data="mappingFields" stripe size="small" border>
            <el-table-column prop="source" label="源字段" width="120" />
            <el-table-column prop="target" label="目标字段" width="140" />
            <el-table-column prop="label" label="中文名称" width="140" />
            <el-table-column prop="type" label="类型" width="90">
              <template #default="{ row }">
                <el-tag size="small" :type="row.type === 'number' ? 'warning' : row.type === 'date' ? 'success' : ''">
                  {{ row.type }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <!-- JSON 输出 -->
        <el-card v-if="resultJson" shadow="never" class="json-card">
          <template #header>
            <div class="result-header">
              <span class="card-title">JSON 输出</span>
              <el-button size="small" text @click="showTable = !showTable">
                {{ showTable ? '收起表格' : '展开表格' }}
              </el-button>
            </div>
          </template>
          <div class="json-output">
            <pre><code>{{ resultJson }}</code></pre>
          </div>
        </el-card>

        <!-- 空状态 -->
        <el-card v-if="!resultJson && !generating" shadow="never" class="empty-card">
          <el-empty description="输入表单 ID 和字段列表后点击「生成映射」" :image-size="80" />
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
              <el-tooltip :content="item.formId" placement="left" :show-after="300">
                <div class="history-content" @click="loadFromHistory(item)">
                  <div class="history-meta">
                    <span class="history-form">
                      <el-icon v-if="item.isPinned" class="pin-icon"><StarFilled /></el-icon>
                      {{ item.formId }}
                    </span>
                    <span class="history-time">{{ item.time }}</span>
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
.field-mapper {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.card-title {
  font-size: 15px;
  font-weight: 500;
  color: #262626;
}

/* 快捷表单 */
.quick-forms {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.quick-label {
  font-size: 13px;
  color: #8c8c8c;
  flex-shrink: 0;
}

.quick-tag {
  cursor: pointer;
  font-weight: 400;
}

.quick-tag:hover {
  color: #409eff;
  border-color: #409eff;
}

/* 输入 */
.input-label {
  font-size: 13px;
  color: #595959;
  margin-bottom: 6px;
}

.actions {
  display: flex;
  gap: 8px;
  margin-top: 16px;
}

/* 结果 */
.result-card {
  margin-top: 16px;
}

.result-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

/* JSON */
.json-card {
  margin-top: 16px;
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

/* 空状态 */
.empty-card {
  margin-top: 16px;
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

.history-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.history-form {
  font-size: 13px;
  color: #262626;
  font-weight: 500;
}

.pin-icon {
  color: #faad14;
  margin-right: 4px;
  font-size: 12px;
}

.history-time {
  font-size: 12px;
  color: #bfbfbf;
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
