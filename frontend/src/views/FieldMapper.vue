<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { StarFilled } from '@element-plus/icons-vue'
import axios from 'axios'
import HistoryPanel from '../components/HistoryPanel.vue'

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
    selectMode.value = false
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
        <div class="panel animate-fade-in-up">
          <div class="panel__header">
            <div class="panel__title">
              <div class="panel__title-icon">
                <el-icon :size="18"><Switch /></el-icon>
              </div>
              <div>
                <h2 class="panel__title-text">字段映射</h2>
                <p class="panel__title-desc">表单字段自动映射</p>
              </div>
            </div>
          </div>
          <div class="panel__body">
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
                class="btn-primary-action"
                :loading="generating"
                @click="generate"
              >
                <el-icon v-if="!generating"><Promotion /></el-icon>
                {{ generating ? '映射中...' : '生成映射' }}
              </el-button>
              <el-button @click="clearAll">清空</el-button>
            </div>
          </div>
        </div>

        <!-- 映射结果表格 -->
        <div v-if="mappingFields.length > 0" class="panel animate-fade-in-up" style="animation-delay: 0.06s; margin-top: 16px">
          <div class="panel__header">
            <div class="panel__title">
              <div class="panel__title-icon" style="background: #f0fdf4; color: var(--color-success)">
                <el-icon :size="18"><Grid /></el-icon>
              </div>
              <h2 class="panel__title-text">映射结果</h2>
            </div>
            <el-button size="small" type="primary" plain @click="copyResult">
              <el-icon><CopyDocument /></el-icon>
              复制 JSON
            </el-button>
          </div>
          <div class="panel__body">
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
          </div>
        </div>

        <!-- JSON 输出 -->
        <div v-if="resultJson" class="panel animate-fade-in-up" style="animation-delay: 0.12s; margin-top: 16px">
          <div class="panel__header">
            <h2 class="panel__title-text">JSON 输出</h2>
            <el-button size="small" text @click="showTable = !showTable">
              {{ showTable ? '收起表格' : '展开表格' }}
            </el-button>
          </div>
          <div class="panel__body">
            <div class="code-block">
              <pre><code>{{ resultJson }}</code></pre>
            </div>
          </div>
        </div>

        <!-- 空状态 -->
        <div v-if="!resultJson && !generating" class="panel animate-fade-in-up" style="animation-delay: 0.12s; margin-top: 16px">
          <div class="panel__body">
            <el-empty description="输入表单 ID 和字段列表后点击「生成映射」" :image-size="80" />
          </div>
        </div>
      </el-col>

      <!-- 右侧：历史记录 -->
      <el-col :xs="24" :lg="7">
        <div class="animate-fade-in-up" style="animation-delay: 0.16s">
          <HistoryPanel
            :history="history"
            :select-mode="selectMode"
            :selected-ids="selectedIds"
            @load="loadFromHistory"
            @pin="togglePin"
            @delete="deleteHistory"
            @toggle-select="toggleSelect"
            @toggle-select-all="toggleSelectAll"
            @batch-delete="batchDelete"
            @enter-select="selectMode = true"
            @exit-select="selectMode = false; selectedIds = []"
          >
            <template #item="{ item }">
              <div class="fm-history-item">
                <div class="fm-history-meta">
                  <span class="fm-history-form">
                    <el-icon v-if="item.isPinned" class="fm-history-pin"><StarFilled /></el-icon>
                    {{ item.formId }}
                  </span>
                  <span class="fm-history-time">{{ item.time }}</span>
                </div>
              </div>
            </template>
          </HistoryPanel>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.field-mapper {
  display: flex;
  flex-direction: column;
  gap: 0;
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
  color: var(--color-text-muted);
  flex-shrink: 0;
}

.quick-tag {
  cursor: pointer;
  font-weight: 400;
}

.quick-tag:hover {
  color: var(--color-primary);
  border-color: var(--color-primary);
  background: var(--color-primary-light);
}

/* 输入 */
.input-label {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin-bottom: 8px;
  font-weight: 600;
}

.actions {
  display: flex;
  gap: 8px;
  margin-top: 16px;
}

/* 历史记录自定义内容 */
.fm-history-item {
  width: 100%;
}

.fm-history-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.fm-history-form {
  font-size: 13px;
  color: var(--color-text-primary);
  font-weight: 500;
}

.fm-history-pin {
  color: var(--color-warning);
  margin-right: 4px;
  font-size: 12px;
}

.fm-history-time {
  font-size: 12px;
  color: var(--color-text-muted);
}
</style>
