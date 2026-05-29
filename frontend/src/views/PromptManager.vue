<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Edit, View, FolderOpened } from '@element-plus/icons-vue'
import axios from 'axios'

// ── 状态 ──

const prompts = ref([])
const loading = ref(false)
const activeCategory = ref('sql')
const editingPrompt = ref(null)
const editContent = ref('')
const saving = ref(false)

// ── 加载 Prompt 列表 ──

async function loadPrompts() {
  loading.value = true
  try {
    const { data } = await axios.get('/api/knowledge/prompts')
    prompts.value = data
  } catch (e) {
    ElMessage.error('加载 Prompt 列表失败')
    console.error(e)
  } finally {
    loading.value = false
  }
}

// ── 按分类过滤 ──

const filteredPrompts = computed(() => {
  return prompts.value.filter(p => p.category === activeCategory.value)
})

// ── 编辑 Prompt ──

function startEdit(prompt) {
  editingPrompt.value = prompt
  editContent.value = prompt.content
}

function cancelEdit() {
  editingPrompt.value = null
  editContent.value = ''
}

async function savePrompt() {
  if (!editingPrompt.value) return

  saving.value = true
  try {
    await axios.put(`/api/knowledge/prompts/${editingPrompt.value.id}`, {
      content: editContent.value
    })
    ElMessage.success('保存成功')
    editingPrompt.value = null
    editContent.value = ''
    await loadPrompts()
  } catch (e) {
    ElMessage.error('保存失败：' + (e.response?.data?.message || e.message))
  } finally {
    saving.value = false
  }
}

// ── 格式化分类名称 ──

function formatCategory(cat) {
  const map = {
    sql: 'SQL 生成',
    dee: 'DEE 模板'
  }
  return map[cat] || cat
}

// ── 格式化 Prompt 名称 ──

function formatName(name) {
  const map = {
    form_query: '表单查询 SQL 生成',
    workflow: '流程节点定义',
    token: 'Token 认证配置',
    json: '字段映射规范',
    java: 'DEE Handler 代码'
  }
  return map[name] || name
}

// ── 初始化 ──

onMounted(() => {
  loadPrompts()
})
</script>

<template>
  <div class="prompt-manager">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="page-title">
        <el-icon :size="20"><FolderOpened /></el-icon>
        <span>AI Prompt 管理</span>
      </div>
      <div class="page-desc">管理 AI 生成所需的 Prompt 模板</div>
    </div>

    <!-- 分类标签 -->
    <div class="category-tabs">
      <button
        :class="['tab-btn', { active: activeCategory === 'sql' }]"
        @click="activeCategory = 'sql'"
      >
        SQL 生成
      </button>
      <button
        :class="['tab-btn', { active: activeCategory === 'dee' }]"
        @click="activeCategory = 'dee'"
      >
        DEE 模板
      </button>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <span>加载中...</span>
    </div>

    <!-- Prompt 列表 -->
    <div v-else class="prompt-list">
      <div
        v-for="prompt in filteredPrompts"
        :key="prompt.id"
        class="prompt-card"
      >
        <div class="prompt-header">
          <div class="prompt-info">
            <span class="prompt-name">{{ formatName(prompt.name) }}</span>
            <span class="prompt-path">{{ prompt.id }}</span>
          </div>
          <div class="prompt-actions">
            <button
              class="action-btn"
              @click="startEdit(prompt)"
              title="编辑"
            >
              <el-icon :size="14"><Edit /></el-icon>
            </button>
          </div>
        </div>
        <div class="prompt-preview">
          {{ prompt.content.substring(0, 200) }}...
        </div>
      </div>

      <div v-if="filteredPrompts.length === 0" class="empty-state">
        暂无 {{ formatCategory(activeCategory) }} 相关的 Prompt 模板
      </div>
    </div>

    <!-- 编辑弹窗 -->
    <el-dialog
      v-model="editingPrompt"
      :title="'编辑 Prompt - ' + (editingPrompt ? formatName(editingPrompt.name) : '')"
      width="80%"
      :close-on-click-modal="false"
      @close="cancelEdit"
    >
      <div class="edit-dialog-content">
        <div class="edit-info">
          <span class="edit-label">分类：</span>
          <span class="edit-value">{{ formatCategory(editingPrompt?.category) }}</span>
          <span class="edit-label" style="margin-left: 20px;">路径：</span>
          <span class="edit-value">{{ editingPrompt?.id }}</span>
        </div>
        <el-input
          v-model="editContent"
          type="textarea"
          :autosize="{ minRows: 20, maxRows: 30 }"
          placeholder="Prompt 内容"
          class="edit-textarea"
        />
      </div>
      <template #footer>
        <el-button @click="cancelEdit">取消</el-button>
        <el-button type="primary" @click="savePrompt" :loading="saving">
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.prompt-manager {
  padding: 24px;
  max-width: 1200px;
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

.category-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 24px;
}

.tab-btn {
  padding: 8px 16px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: white;
  color: var(--color-text-secondary);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.tab-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.tab-btn.active {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: white;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px;
  color: var(--color-text-secondary);
}

.spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--color-border);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 12px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.prompt-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.prompt-card {
  background: white;
  border: 1px solid var(--color-border-light);
  border-radius: 12px;
  padding: 16px;
  transition: all 0.2s;
}

.prompt-card:hover {
  border-color: var(--color-primary);
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.1);
}

.prompt-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.prompt-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.prompt-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.prompt-path {
  font-size: 12px;
  color: var(--color-text-muted);
  font-family: monospace;
}

.prompt-actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  width: 32px;
  height: 32px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: white;
  color: var(--color-text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.action-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: var(--color-primary-light);
}

.prompt-preview {
  font-size: 13px;
  color: var(--color-text-secondary);
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.empty-state {
  text-align: center;
  padding: 60px;
  color: var(--color-text-muted);
  font-size: 14px;
}

.edit-dialog-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.edit-info {
  display: flex;
  align-items: center;
  font-size: 14px;
  color: var(--color-text-secondary);
}

.edit-label {
  color: var(--color-text-muted);
}

.edit-value {
  color: var(--color-text-primary);
  font-family: monospace;
}

.edit-textarea :deep(textarea) {
  font-family: monospace;
  font-size: 13px;
  line-height: 1.6;
}
</style>
