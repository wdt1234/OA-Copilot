<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Setting, Check, Plus, Switch, Delete, Edit } from '@element-plus/icons-vue'
import axios from 'axios'

// ── 状态 ──

const loading = ref(false)
const saving = ref(false)
const configs = ref([])
const showDialog = ref(false)
const isEdit = ref(false)

const formData = ref({
  id: null,
  name: '',
  endpoint: '',
  apiKey: '',
  model: '',
  timeout: 300
})

// ── 加载配置列表 ──

async function loadConfigs() {
  loading.value = true
  try {
    const { data } = await axios.get('/api/ai-configs')
    configs.value = data
  } catch (e) {
    console.error('加载配置失败', e)
    ElMessage.error('加载配置失败')
  } finally {
    loading.value = false
  }
}

// ── 新增 ──

function handleAdd() {
  isEdit.value = false
  formData.value = {
    id: null,
    name: '',
    endpoint: 'https://token-plan-cn.xiaomimimo.com/v1/chat/completions',
    apiKey: '',
    model: 'mimo-v2.5-pro',
    timeout: 300
  }
  showDialog.value = true
}

// ── 编辑 ──

function handleEdit(row) {
  isEdit.value = true
  formData.value = {
    id: row.id,
    name: row.name,
    endpoint: row.endpoint,
    apiKey: '', // 不回显，避免误改
    model: row.model,
    timeout: row.timeout
  }
  showDialog.value = true
}

// ── 保存 ──

async function handleSave() {
  if (!formData.value.name.trim()) {
    ElMessage.warning('请输入配置名称')
    return
  }
  if (!formData.value.endpoint.trim()) {
    ElMessage.warning('请输入 API 端点')
    return
  }
  if (!isEdit.value && !formData.value.apiKey.trim()) {
    ElMessage.warning('请输入 API Key')
    return
  }

  saving.value = true
  try {
    if (isEdit.value) {
      await axios.put(`/api/ai-configs/${formData.value.id}`, formData.value)
      ElMessage.success('更新成功')
    } else {
      await axios.post('/api/ai-configs', formData.value)
      ElMessage.success('添加成功')
    }
    showDialog.value = false
    loadConfigs()
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

// ── 切换激活 ──

async function handleSwitch(row) {
  try {
    await axios.put(`/api/ai-configs/${row.id}/activate`)
    ElMessage.success(`已切换到「${row.name}」，立即生效`)
    loadConfigs()
  } catch (e) {
    ElMessage.error('切换失败')
  }
}

// ── 删除 ──

async function handleDelete(row) {
  if (row.isActive === 1) {
    ElMessage.warning('不能删除当前激活的配置')
    return
  }
  try {
    await ElMessageBox.confirm(`确定要删除「${row.name}」吗？`, '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await axios.delete(`/api/ai-configs/${row.id}`)
    ElMessage.success('删除成功')
    loadConfigs()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// ── 测试连接 ──

async function testConnection(config) {
  try {
    ElMessage.info('正在测试连接...')
    // 临时切换到该配置进行测试
    const { data } = await axios.put(`/api/ai-configs/${config.id}/activate`)
    if (data.success) {
      const health = await axios.get('/api/health')
      if (health.data.ai && health.data.ai.enabled) {
        ElMessage.success(`连接成功！模型: ${health.data.ai.model}`)
      } else {
        ElMessage.warning('AI 未启用')
      }
    }
  } catch (e) {
    ElMessage.error('连接失败')
  }
}

// ── 初始化 ──

onMounted(() => {
  loadConfigs()
})
</script>

<template>
  <div class="system-settings" v-loading="loading">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="page-title">
        <el-icon :size="20"><Setting /></el-icon>
        <span>系统设置</span>
      </div>
      <div class="page-desc">管理 AI 模型配置方案</div>
    </div>

    <!-- 操作栏 -->
    <div class="action-bar">
      <span class="config-count">共 {{ configs.length }} 套配置</span>
      <el-button type="primary" :icon="Plus" @click="handleAdd">
        新增配置
      </el-button>
    </div>

    <!-- 配置列表 -->
    <div class="config-list">
      <div v-if="configs.length === 0" class="empty-state">
        <el-icon :size="48"><Setting /></el-icon>
        <p>暂无配置</p>
        <el-button type="primary" @click="handleAdd">添加第一套配置</el-button>
      </div>

      <div v-for="item in configs" :key="item.id" class="config-card" :class="{ 'config-card--active': item.isActive === 1 }">
        <div class="config-header">
          <div class="config-name">
            <el-tag v-if="item.isActive === 1" type="success" size="small">当前使用</el-tag>
            <span>{{ item.name }}</span>
          </div>
          <div class="config-actions">
            <el-button
              v-if="item.isActive !== 1"
              type="primary"
              :icon="Switch"
              size="small"
              @click="handleSwitch(item)"
            >
              切换
            </el-button>
            <el-button :icon="Edit" circle size="small" @click="handleEdit(item)" />
            <el-button
              :icon="Delete"
              circle
              size="small"
              type="danger"
              :disabled="item.isActive === 1"
              @click="handleDelete(item)"
            />
          </div>
        </div>

        <div class="config-body">
          <div class="config-item">
            <label>模型</label>
            <span>{{ item.model }}</span>
          </div>
          <div class="config-item">
            <label>端点</label>
            <span class="config-endpoint">{{ item.endpoint }}</span>
          </div>
          <div class="config-item">
            <label>API Key</label>
            <span class="config-key">{{ item.apiKey }}</span>
          </div>
          <div class="config-item">
            <label>超时</label>
            <span>{{ item.timeout }}秒</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="showDialog"
      :title="isEdit ? '编辑配置' : '新增配置'"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form :model="formData" label-width="100px">
        <el-form-item label="配置名称" required>
          <el-input v-model="formData.name" placeholder="如：测试环境、生产环境" />
        </el-form-item>

        <el-form-item label="API 端点" required>
          <el-input v-model="formData.endpoint" placeholder="https://api.example.com/v1/chat/completions" />
        </el-form-item>

        <el-form-item label="API Key" :required="!isEdit">
          <el-input
            v-model="formData.apiKey"
            type="password"
            :placeholder="isEdit ? '留空则不修改' : '输入 API Key'"
            show-password
          />
        </el-form-item>

        <el-form-item label="模型名称" required>
          <el-input v-model="formData.model" placeholder="如：mimo-v2.5-pro, gpt-4" />
        </el-form-item>

        <el-form-item label="超时时间">
          <el-input-number v-model="formData.timeout" :min="30" :max="600" :step="30" />
          <span class="form-hint">秒</span>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.system-settings {
  padding: 24px;
  max-width: 900px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
}

.page-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 20px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 8px;
}

.page-desc {
  font-size: 14px;
  color: var(--color-text-secondary);
}

.action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.config-count {
  font-size: 14px;
  color: var(--color-text-muted);
}

.config-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: var(--color-text-muted);
}

.empty-state p {
  margin: 12px 0 16px;
  font-size: 14px;
}

.config-card {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border-light);
  border-radius: 12px;
  overflow: hidden;
  transition: all var(--transition-fast);
}

.config-card--active {
  border-color: var(--color-success);
  box-shadow: 0 0 0 1px var(--color-success);
}

.config-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid var(--color-border-light);
}

.config-name {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.config-actions {
  display: flex;
  gap: 8px;
}

.config-body {
  padding: 16px 20px;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.config-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.config-item label {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.config-item span {
  font-size: 14px;
  color: var(--color-text-primary);
}

.config-endpoint {
  word-break: break-all;
  font-size: 13px !important;
  color: var(--color-text-secondary) !important;
}

.config-key {
  font-family: monospace;
}

.form-hint {
  margin-left: 8px;
  font-size: 12px;
  color: var(--color-text-muted);
}
</style>
