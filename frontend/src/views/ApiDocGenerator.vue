<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, StarFilled } from '@element-plus/icons-vue'
import axios from 'axios'
import HistoryPanel from '../components/HistoryPanel.vue'

// ── 数据字典相关 ──

const availableForms = ref([])
const selectedFormCode = ref('')

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

const selectedForm = computed(() => {
  return availableForms.value.find(f => f.formCode === selectedFormCode.value)
})

// ── 接口配置 ──

const interfaceCode = ref('')
const interfaceName = ref('')
const interfaceType = ref('新增接口')
const connectionType = ref('direct') // mule / direct
const sourceSystem = ref('')
const sourceContact = ref('')
const targetContact = ref('')
const otherNotes = ref('')

// ── 生成状态 ──

const generating = ref(false)

// ── 预览数据 ──

const previewData = computed(() => {
  const form = selectedForm.value
  if (!form) return null

  return {
    formName: form.formName,
    tableName: form.tableName,
    interfaceCode: interfaceCode.value || '未填写',
    interfaceName: interfaceName.value || '未填写',
    connectionType: connectionType.value === 'mule' ? '过 Mule' : '直连',
    sourceSystem: sourceSystem.value || '未填写'
  }
})

// ── 历史记录 ──

const history = ref([])
const selectedIds = ref([])
const selectMode = ref(false)

function formatTime(timeStr) {
  if (!timeStr) return ''
  const parts = timeStr.split('T')
  if (parts.length === 2) {
    return parts[1].substring(0, 5)
  }
  return timeStr
}

async function loadHistory() {
  try {
    const { data } = await axios.get('/api/apidoc/history', { params: { limit: 20 } })
    history.value = data.map(item => ({
      id: item.id,
      formCode: item.formCode,
      formName: item.formName,
      interfaceCode: item.interfaceCode,
      interfaceName: item.interfaceName,
      interfaceType: item.interfaceType,
      connectionType: item.connectionType,
      sourceSystem: item.sourceSystem,
      sourceContact: item.sourceContact,
      targetContact: item.targetContact,
      otherNotes: item.otherNotes,
      isPinned: item.pinned,
      time: formatTime(item.createTime)
    }))
  } catch (e) {
    console.error('加载历史记录失败', e)
  }
}

function loadFromHistory(item) {
  selectedFormCode.value = item.formCode || ''
  interfaceCode.value = item.interfaceCode || ''
  interfaceName.value = item.interfaceName || ''
  interfaceType.value = item.interfaceType || '新增接口'
  connectionType.value = item.connectionType || 'direct'
  sourceSystem.value = item.sourceSystem || ''
  sourceContact.value = item.sourceContact || ''
  targetContact.value = item.targetContact || ''
  otherNotes.value = item.otherNotes || ''
}

async function togglePin(item) {
  try {
    await axios.put(`/api/apidoc/history/${item.id}/pin`)
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
    await axios.delete(`/api/apidoc/history/${item.id}`)
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
    await axios.delete('/api/apidoc/history/batch', { data: { ids: selectedIds.value } })
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

// ── 生成 Excel ──

async function generate() {
  if (!selectedFormCode.value) {
    ElMessage.warning('请选择数据字典')
    return
  }
  if (!interfaceCode.value.trim()) {
    ElMessage.warning('请输入接口编码')
    return
  }
  if (!interfaceName.value.trim()) {
    ElMessage.warning('请输入接口名称')
    return
  }

  generating.value = true

  try {
    const config = {
      formCode: selectedFormCode.value,
      interfaceCode: interfaceCode.value.trim(),
      interfaceName: interfaceName.value.trim(),
      interfaceType: interfaceType.value,
      connectionType: connectionType.value,
      sourceSystem: sourceSystem.value,
      sourceContact: sourceContact.value,
      targetContact: targetContact.value,
      triggerMode: '流程触发',
      otherNotes: otherNotes.value
    }

    const response = await axios.post('/api/apidoc/generate', config, {
      responseType: 'blob'
    })

    const blob = new Blob([response.data], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url

    const contentDisposition = response.headers['content-disposition']
    if (contentDisposition) {
      const fileNameMatch = contentDisposition.match(/filename\*=UTF-8''(.+)/)
      if (fileNameMatch) {
        link.download = decodeURIComponent(fileNameMatch[1])
      }
    }
    if (!link.download) {
      link.download = interfaceCode.value + '_' + interfaceName.value + '_接口文档.xlsx'
    }

    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)

    ElMessage.success('接口文档已生成并下载')
    loadHistory()
  } catch (e) {
    ElMessage.error('生成失败，请检查后端服务')
    console.error('生成接口文档失败', e)
  } finally {
    generating.value = false
  }
}

function clearAll() {
  selectedFormCode.value = ''
  interfaceCode.value = ''
  interfaceName.value = ''
  interfaceType.value = '新增接口'
  connectionType.value = 'direct'
  sourceSystem.value = ''
  sourceContact.value = ''
  targetContact.value = ''
  otherNotes.value = ''
}

// ── 初始化 ──

onMounted(() => {
  loadAvailableForms()
  loadHistory()
})
</script>

<template>
  <div class="api-doc-generator">
    <el-row :gutter="16">
      <!-- 左侧：配置区 -->
      <el-col :xs="24" :lg="16">
        <!-- 基础信息 -->
        <div class="panel animate-fade-in-up">
          <div class="panel__header">
            <div class="panel__title">
              <div class="panel__title-icon">
                <el-icon :size="18"><Tickets /></el-icon>
              </div>
              <div>
                <h2 class="panel__title-text">接口基础信息</h2>
                <p class="panel__title-desc">配置接口文档参数</p>
              </div>
            </div>
          </div>
          <div class="panel__body">
            <el-form label-width="140px" size="default">
              <!-- 数据字典选择 -->
              <el-form-item label="数据字典">
                <el-select
                  v-model="selectedFormCode"
                  placeholder="输入关键词搜索表单"
                  clearable
                  filterable
                  remote
                  :remote-method="onFormSearch"
                  style="width: 100%"
                >
                  <el-option
                    v-for="form in availableForms"
                    :key="form.formCode"
                    :label="form.formName + ' (' + form.tableName + ')'"
                    :value="form.formCode"
                  />
                </el-select>
              </el-form-item>

              <!-- 接口编码 -->
              <el-form-item label="接口编码">
                <el-input v-model="interfaceCode" placeholder="如 ystzd001" />
              </el-form-item>

              <!-- 接口名称 -->
              <el-form-item label="接口名称">
                <el-input v-model="interfaceName" placeholder="如 运输通知单" />
              </el-form-item>

              <!-- 接口类型 -->
              <el-form-item label="接口类型">
                <el-select v-model="interfaceType" style="width: 100%">
                  <el-option label="新增接口" value="新增接口" />
                  <el-option label="修改接口" value="修改接口" />
                </el-select>
              </el-form-item>

              <!-- 连接方式 -->
              <el-form-item label="连接方式">
                <el-radio-group v-model="connectionType">
                  <el-radio value="direct">直连（内部系统）</el-radio>
                  <el-radio value="mule">过 Mule（外部系统）</el-radio>
                </el-radio-group>
              </el-form-item>

              <!-- 源系统 -->
              <el-form-item label="源系统">
                <el-input v-model="sourceSystem" placeholder="如 VMI、EBS、DMP" />
              </el-form-item>

              <!-- 源系统联系人 -->
              <el-form-item label="源系统联系人">
                <el-input v-model="sourceContact" placeholder="源系统方联系人" />
              </el-form-item>

              <!-- 目标系统联系人 -->
              <el-form-item label="目标系统联系人">
                <el-input v-model="targetContact" placeholder="OA方联系人" />
              </el-form-item>

              <!-- 其他说明 -->
              <el-form-item label="其他说明">
                <el-input v-model="otherNotes" type="textarea" :rows="2" placeholder="可选" />
              </el-form-item>
            </el-form>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="actions animate-fade-in-up" style="animation-delay: 0.06s">
          <el-button
            type="primary"
            size="large"
            class="btn-primary-action"
            :loading="generating"
            @click="generate"
          >
            <el-icon v-if="!generating"><Download /></el-icon>
            {{ generating ? '生成中...' : '生成 Excel 接口文档' }}
          </el-button>
          <el-button size="large" @click="clearAll">清空</el-button>
        </div>
      </el-col>

      <!-- 右侧：预览 + 历史记录 -->
      <el-col :xs="24" :lg="8">
        <!-- 预览区 -->
        <div class="panel animate-fade-in-up" style="animation-delay: 0.08s">
          <div class="panel__header">
            <div class="panel__title">
              <div class="panel__title-icon" style="background: #f0fdf4; color: var(--color-success)">
                <el-icon :size="18"><View /></el-icon>
              </div>
              <h2 class="panel__title-text">生成预览</h2>
            </div>
          </div>
          <div class="panel__body">
            <div v-if="previewData" class="preview-content">
              <el-descriptions :column="1" border size="small">
                <el-descriptions-item label="表单名称">
                  {{ previewData.formName }}
                </el-descriptions-item>
                <el-descriptions-item label="主表">
                  {{ previewData.tableName }}
                </el-descriptions-item>
                <el-descriptions-item label="接口编码">
                  {{ previewData.interfaceCode }}
                </el-descriptions-item>
                <el-descriptions-item label="接口名称">
                  {{ previewData.interfaceName }}
                </el-descriptions-item>
                <el-descriptions-item label="连接方式">
                  {{ previewData.connectionType }}
                </el-descriptions-item>
                <el-descriptions-item label="源系统">
                  {{ previewData.sourceSystem }}
                </el-descriptions-item>
              </el-descriptions>

              <div class="preview-sheets">
                <h4>将生成的 Sheet：</h4>
                <el-steps direction="vertical" :active="5" :space="40">
                  <el-step title="接口基础信息" description="自动填入配置信息" />
                  <el-step title="Mule-Collection" description="留空，自行插入 Postman JSON" />
                  <el-step title="Data Mapping Table" description="从数据字典自动生成字段映射" />
                  <el-step title="Get Token" description="自动生成 Token 请求示例" />
                  <el-step title="RESTful API" description="自动生成业务接口请求示例" />
                </el-steps>
              </div>
            </div>

            <el-empty v-else description="请先选择数据字典" :image-size="80" />
          </div>
        </div>

        <!-- 历史记录 -->
        <div class="animate-fade-in-up" style="animation-delay: 0.12s; margin-top: 16px">
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
              <div class="apidoc-history-item">
                <div class="apidoc-history-title">
                  <el-icon v-if="item.isPinned" class="apidoc-history-pin"><StarFilled /></el-icon>
                  {{ item.interfaceCode }} - {{ item.interfaceName }}
                </div>
                <div class="apidoc-history-meta">
                  <span class="apidoc-history-time">{{ item.time }}</span>
                  <span v-if="item.formName" class="apidoc-history-form">{{ item.formName }}</span>
                  <span class="apidoc-history-type">{{ item.connectionType === 'mule' ? 'Mule' : '直连' }}</span>
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
.api-doc-generator {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.actions {
  display: flex;
  gap: 12px;
  margin-top: 16px;
  padding: 4px 0;
}

/* 预览区 */
.preview-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.preview-sheets h4 {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin-bottom: 12px;
  font-weight: 600;
}

/* 历史记录自定义内容 */
.apidoc-history-item {
  width: 100%;
}

.apidoc-history-title {
  font-size: 13px;
  color: var(--color-text-primary);
  font-weight: 500;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.apidoc-history-pin {
  color: var(--color-warning);
  margin-right: 4px;
  font-size: 12px;
}

.apidoc-history-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
}

.apidoc-history-time {
  font-size: 12px;
  color: var(--color-text-muted);
}

.apidoc-history-form {
  font-size: 12px;
  color: var(--color-text-muted);
}

.apidoc-history-type {
  font-size: 12px;
  color: var(--color-primary);
  font-weight: 500;
}

:deep(.el-form-item) {
  margin-bottom: 20px;
}

:deep(.el-form-item__label) {
  font-weight: 600;
  color: var(--color-text-secondary);
}
</style>
