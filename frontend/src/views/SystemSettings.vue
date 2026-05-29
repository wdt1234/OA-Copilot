<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Setting, Check, RefreshRight } from '@element-plus/icons-vue'
import axios from 'axios'

// ── 状态 ──

const loading = ref(false)
const saving = ref(false)
const aiConfig = ref({
  enabled: true,
  endpoint: '',
  apiKey: '',
  model: '',
  timeout: 300
})
const serverConfig = ref({
  port: 8080
})

// ── 加载设置 ──

async function loadSettings() {
  loading.value = true
  try {
    const { data } = await axios.get('/api/settings')
    if (data.ai) {
      aiConfig.value = { ...aiConfig.value, ...data.ai }
    }
    if (data.server) {
      serverConfig.value = { ...serverConfig.value, ...data.server }
    }
  } catch (e) {
    console.error('加载设置失败', e)
    ElMessage.error('加载设置失败')
  } finally {
    loading.value = false
  }
}

// ── 保存 AI 设置 ──

async function saveAiSettings() {
  saving.value = true
  try {
    const { data } = await axios.put('/api/settings/ai', aiConfig.value)
    if (data.success) {
      ElMessage.success(data.message || '保存成功')
    } else {
      ElMessage.error(data.message || '保存失败')
    }
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

// ── 重置设置 ──

async function resetSettings() {
  try {
    await ElMessageBox.confirm('确定要重置为默认设置吗？', '确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    aiConfig.value = {
      enabled: true,
      endpoint: 'https://token-plan-cn.xiaomimimo.com/v1/chat/completions',
      apiKey: '',
      model: 'mimo-v2.5-pro',
      timeout: 300
    }
    ElMessage.info('已重置为默认设置，请修改后保存')
  } catch (e) {
    // 取消
  }
}

// ── 测试连接 ──

async function testConnection() {
  try {
    ElMessage.info('正在测试连接...')
    const { data } = await axios.get('/api/health')
    if (data.ai && data.ai.enabled) {
      ElMessage.success(`连接成功！模型: ${data.ai.model}`)
    } else {
      ElMessage.warning('AI 未启用')
    }
  } catch (e) {
    ElMessage.error('连接失败')
  }
}

// ── 初始化 ──

onMounted(() => {
  loadSettings()
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
      <div class="page-desc">配置 AI 模型和系统参数</div>
    </div>

    <!-- AI 模型配置 -->
    <div class="settings-card">
      <div class="card-header">
        <h3>AI 模型配置</h3>
        <el-tag :type="aiConfig.enabled ? 'success' : 'info'" size="small">
          {{ aiConfig.enabled ? '已启用' : '已禁用' }}
        </el-tag>
      </div>

      <div class="settings-form">
        <!-- 启用状态 -->
        <div class="form-item">
          <label>启用 AI</label>
          <el-switch v-model="aiConfig.enabled" />
        </div>

        <!-- API 端点 -->
        <div class="form-item">
          <label>API 端点</label>
          <el-input
            v-model="aiConfig.endpoint"
            placeholder="https://api.example.com/v1/chat/completions"
          />
          <span class="form-hint">兼容 OpenAI 格式的 API 地址</span>
        </div>

        <!-- API Key -->
        <div class="form-item">
          <label>API Key</label>
          <el-input
            v-model="aiConfig.apiKey"
            type="password"
            placeholder="输入 API Key"
            show-password
          />
          <span class="form-hint">当前: {{ aiConfig.apiKey }}</span>
        </div>

        <!-- 模型名称 -->
        <div class="form-item">
          <label>模型名称</label>
          <el-input
            v-model="aiConfig.model"
            placeholder="mimo-v2.5-pro"
          />
          <span class="form-hint">如: mimo-v2.5-pro, gpt-4, claude-3</span>
        </div>

        <!-- 超时时间 -->
        <div class="form-item">
          <label>超时时间（秒）</label>
          <el-input-number
            v-model="aiConfig.timeout"
            :min="30"
            :max="600"
            :step="30"
          />
          <span class="form-hint">Prompt 较长时需要更多时间</span>
        </div>
      </div>

      <div class="card-actions">
        <el-button @click="testConnection" :loading="loading">
          <el-icon><RefreshRight /></el-icon>
          测试连接
        </el-button>
        <el-button type="primary" @click="saveAiSettings" :loading="saving">
          <el-icon><Check /></el-icon>
          保存设置
        </el-button>
      </div>
    </div>

    <!-- 服务器配置 -->
    <div class="settings-card">
      <div class="card-header">
        <h3>服务器配置</h3>
      </div>

      <div class="settings-form">
        <div class="form-item">
          <label>服务端口</label>
          <el-input-number
            v-model="serverConfig.port"
            :min="1024"
            :max="65535"
            disabled
          />
          <span class="form-hint">修改端口需要重启服务</span>
        </div>
      </div>
    </div>

    <!-- 操作按钮 -->
    <div class="settings-actions">
      <el-button @click="resetSettings">重置为默认</el-button>
      <el-button type="primary" @click="saveAiSettings" :loading="saving">
        保存所有设置
      </el-button>
    </div>

    <!-- 提示信息 -->
    <div class="settings-tip">
      <el-alert
        title="修改配置后需要重启后端服务才能生效"
        type="warning"
        show-icon
        :closable="false"
      />
    </div>
  </div>
</template>

<style scoped>
.system-settings {
  padding: 24px;
  max-width: 800px;
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

.settings-card {
  background: white;
  border: 1px solid var(--color-border-light);
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.card-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.settings-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-item label {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-primary);
}

.form-hint {
  font-size: 12px;
  color: var(--color-text-muted);
}

.card-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid var(--color-border-light);
}

.settings-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-bottom: 20px;
}

.settings-tip {
  margin-top: 20px;
}
</style>
