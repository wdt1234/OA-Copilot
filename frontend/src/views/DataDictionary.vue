<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { StarFilled } from '@element-plus/icons-vue'
import axios from 'axios'

// ── 状态 ──

const rawText = ref('')
const formCode = ref('')
const formName = ref('')
const tableName = ref('')
const parsedResult = ref(null)
const parsing = ref(false)
const saving = ref(false)
const existingDictionaries = ref([])
const showPreview = ref(false)

// ── 解析预览 ──

async function parsePreview() {
  if (!rawText.value.trim()) {
    ElMessage.warning('请粘贴表单数据字典文本')
    return
  }

  parsing.value = true
  try {
    const { data } = await axios.post('/api/dictionary/parse', { rawText: rawText.value })
    parsedResult.value = data
    formName.value = data.formName || ''
    tableName.value = data.mainTable?.tableName || ''
    formCode.value = data.suggestedFormCode || ''
    showPreview.value = true
    ElMessage.success('解析成功，请确认后保存')
  } catch (e) {
    ElMessage.error('解析失败：' + (e.response?.data?.message || e.message))
  } finally {
    parsing.value = false
  }
}

// ── 保存 ──

async function saveDictionary() {
  if (!parsedResult.value) {
    ElMessage.error('请先解析文本')
    return
  }
  if (!formCode.value.trim()) {
    ElMessage.warning('请输入表单代码')
    return
  }

  saving.value = true
  try {
    const { data } = await axios.post('/api/dictionary/save', {
      rawText: rawText.value,
      formCode: formCode.value.trim()
    })
    ElMessage.success(data.message || '保存成功')
    showPreview.value = false
    parsedResult.value = null
    rawText.value = ''
    await loadExisting()
  } catch (e) {
    ElMessage.error('保存失败：' + (e.response?.data?.message || e.message))
  } finally {
    saving.value = false
  }
}

// ── 查看/删除 ──

async function loadExisting() {
  try {
    const { data } = await axios.get('/api/dictionary/list')
    existingDictionaries.value = data
  } catch (e) {
    console.error('加载数据字典列表失败', e)
  }
}

async function viewDictionary(item) {
  try {
    const { data } = await axios.get('/api/dictionary/' + item.formCode)
    rawText.value = data.rawText || ''
    formCode.value = data.formCode
    formName.value = data.formName
    tableName.value = data.tableName
    parsedResult.value = null
    showPreview.value = false
    ElMessage.info('已加载原始文本，可编辑后重新保存')
  } catch (e) {
    ElMessage.error('加载失败')
  }
}

async function deleteDictionary(item) {
  try {
    await ElMessageBox.confirm(
      '确定删除数据字典「' + item.formName + '」？',
      '确认删除',
      { type: 'warning' }
    )
    await axios.delete('/api/dictionary/' + item.formCode)
    ElMessage.success('删除成功')
    await loadExisting()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

async function togglePin(item) {
  try {
    const { data } = await axios.put('/api/dictionary/' + item.formCode + '/pin')
    ElMessage.success(data.message)
    await loadExisting()
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

async function clearAllDictionaries() {
  try {
    await ElMessageBox.confirm(
      '确定清空所有数据字典？此操作不可恢复！',
      '确认清空',
      { type: 'error', confirmButtonText: '确认清空', cancelButtonText: '取消' }
    )
    const { data } = await axios.delete('/api/dictionary/all')
    ElMessage.success(data.message)
    await loadExisting()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('清空失败')
    }
  }
}

function clearForm() {
  rawText.value = ''
  formCode.value = ''
  formName.value = ''
  tableName.value = ''
  parsedResult.value = null
  showPreview.value = false
}

// ── 初始化 ──

onMounted(() => {
  loadExisting()
})
</script>

<template>
  <div class="data-dictionary">
    <el-row :gutter="16">
      <!-- 左侧：输入区 -->
      <el-col :xs="24" :lg="14">
        <div class="panel animate-fade-in-up">
          <div class="panel__header">
            <div class="panel__title">
              <div class="panel__title-icon">
                <el-icon :size="18"><Notebook /></el-icon>
              </div>
              <div>
                <h2 class="panel__title-text">录入数据字典</h2>
                <p class="panel__title-desc">粘贴 OA 表单字段信息，自动解析</p>
              </div>
            </div>
          </div>
          <div class="panel__body">
            <el-input
              v-model="rawText"
              type="textarea"
              :rows="16"
              placeholder="粘贴 OA 表单字段信息..."
              class="raw-input"
            />
            <div class="actions">
              <el-button
                type="primary"
                class="btn-primary-action"
                :loading="parsing"
                @click="parsePreview"
              >
                {{ parsing ? '解析中...' : '解析预览' }}
              </el-button>
              <el-button @click="clearForm">清空</el-button>
            </div>
          </div>
        </div>

        <!-- 预览区 -->
        <div v-if="showPreview && parsedResult" class="panel animate-fade-in-up" style="animation-delay: 0.06s; margin-top: 16px">
          <div class="panel__header">
            <div class="panel__title">
              <div class="panel__title-icon" style="background: #f0fdf4; color: var(--color-success)">
                <el-icon :size="18"><View /></el-icon>
              </div>
              <h2 class="panel__title-text">解析结果预览</h2>
            </div>
            <el-button
              type="primary"
              class="btn-primary-action"
              :loading="saving"
              @click="saveDictionary"
            >
              {{ saving ? '保存中...' : '确认保存' }}
            </el-button>
          </div>
          <div class="panel__body">
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="表单名称">
                {{ formName }}
              </el-descriptions-item>
              <el-descriptions-item label="主表">
                {{ tableName }}
              </el-descriptions-item>
              <el-descriptions-item label="表单代码" :span="2">
                <el-input v-model="formCode" size="small" style="width: 240px" />
                <span class="form-code-hint">可修改，用于后续引用</span>
              </el-descriptions-item>
            </el-descriptions>

            <!-- 主表字段预览 -->
            <div class="fields-preview" v-if="parsedResult.mainTable">
              <h4>主表字段（{{ parsedResult.mainTable.fields?.length || 0 }} 个）</h4>
              <el-table :data="parsedResult.mainTable.fields" size="small" max-height="300" border>
                <el-table-column prop="fieldName" label="字段名" width="120" />
                <el-table-column prop="displayName" label="显示名" width="160" />
                <el-table-column prop="inputType" label="输入类型" width="100" />
                <el-table-column prop="dbFinalType" label="数据库类型" width="160" />
                <el-table-column prop="isSpecial" label="特殊字段" width="80">
                  <template #default="{ row }">
                    <el-tag v-if="row.isSpecial" type="warning" size="small">是</el-tag>
                    <span v-else>否</span>
                  </template>
                </el-table-column>
                <el-table-column prop="refTable" label="关联表" width="120" />
              </el-table>
            </div>

            <!-- 从表预览 -->
            <div
              v-for="(son, idx) in parsedResult.sonTables"
              :key="idx"
              class="fields-preview"
            >
              <h4>{{ son.sonName || '从表' + (idx + 1) }}（{{ son.fields?.length || 0 }} 个字段）</h4>
              <el-table :data="son.fields" size="small" max-height="200" border>
                <el-table-column prop="fieldName" label="字段名" width="120" />
                <el-table-column prop="displayName" label="显示名" width="160" />
                <el-table-column prop="inputType" label="输入类型" width="100" />
                <el-table-column prop="dbFinalType" label="数据库类型" width="160" />
                <el-table-column prop="isSpecial" label="特殊字段" width="80">
                  <template #default="{ row }">
                    <el-tag v-if="row.isSpecial" type="warning" size="small">是</el-tag>
                    <span v-else>否</span>
                  </template>
                </el-table-column>
                <el-table-column prop="refTable" label="关联表" width="120" />
              </el-table>
            </div>
          </div>
        </div>
      </el-col>

      <!-- 右侧：已有数据字典 -->
      <el-col :xs="24" :lg="10">
        <div class="panel animate-fade-in-up" style="animation-delay: 0.08s">
          <div class="panel__header">
            <div class="panel__title">
              <div class="panel__title-icon">
                <el-icon :size="18"><List /></el-icon>
              </div>
              <h2 class="panel__title-text">已录入的数据字典</h2>
            </div>
            <el-button
              v-if="existingDictionaries.length > 0"
              type="danger"
              size="small"
              text
              @click="clearAllDictionaries"
            >
              清空全部
            </el-button>
          </div>
          <div class="panel__body">
            <div v-if="existingDictionaries.length === 0" class="history-empty">
              <p>暂无数据字典</p>
            </div>

            <div v-else class="dict-list">
              <div
                v-for="item in existingDictionaries"
                :key="item.formCode"
                class="dict-item"
                :class="{ 'dict-item--pinned': item.isPinned }"
              >
                <div class="dict-info" @click="viewDictionary(item)">
                  <div class="dict-name">
                    <el-icon v-if="item.isPinned" class="dict-pin"><StarFilled /></el-icon>
                    {{ item.formName }}
                  </div>
                  <div class="dict-meta">
                    <el-tag size="small" type="info">{{ item.tableName }}</el-tag>
                    <span class="dict-code">{{ item.formCode }}</span>
                  </div>
                </div>
                <div class="dict-actions">
                  <button
                    class="dict-action-btn"
                    :class="{ 'dict-action-btn--pinned': item.isPinned }"
                    @click="togglePin(item)"
                    :title="item.isPinned ? '取消置顶' : '置顶'"
                  >
                    <el-icon :size="14"><StarFilled v-if="item.isPinned" /><Star v-else /></el-icon>
                  </button>
                  <button
                    class="dict-action-btn dict-action-btn--danger"
                    @click="deleteDictionary(item)"
                  >
                    <el-icon :size="14"><Delete /></el-icon>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.data-dictionary {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.raw-input {
  margin-bottom: 4px;
}

:deep(.el-textarea__inner) {
  font-size: 13px;
  font-family: 'SF Mono', 'Consolas', 'Monaco', monospace;
  line-height: 1.6;
  border-radius: var(--radius-md) !important;
}

.actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}

/* 预览区 */
.form-code-hint {
  font-size: 12px;
  color: var(--color-text-muted);
  margin-left: 8px;
}

.fields-preview {
  margin-top: 16px;
}

.fields-preview h4 {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin-bottom: 8px;
  font-weight: 600;
}

/* 字典列表 */
.dict-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.dict-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  border-radius: var(--radius-md);
  transition: background-color var(--transition-fast);
}

.dict-item:hover {
  background-color: var(--color-bg-page);
}

.dict-item--pinned {
  background-color: #fef3c7;
}

.dict-item--pinned:hover {
  background-color: #fde68a;
}

.dict-info {
  cursor: pointer;
  flex: 1;
  min-width: 0;
}

.dict-name {
  font-size: 14px;
  color: var(--color-text-primary);
  font-weight: 500;
  margin-bottom: 4px;
}

.dict-pin {
  color: var(--color-warning);
  margin-right: 4px;
  font-size: 14px;
}

.dict-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.dict-code {
  font-size: 12px;
  color: var(--color-text-muted);
}

.dict-actions {
  display: flex;
  align-items: center;
  gap: 2px;
}

.dict-action-btn {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  border-radius: 6px;
  cursor: pointer;
  color: var(--color-text-muted);
  transition: all var(--transition-fast);
}

.dict-action-btn:hover {
  background: var(--color-bg-page);
  color: var(--color-text-secondary);
}

.dict-action-btn--pinned {
  color: var(--color-warning);
}

.dict-action-btn--danger:hover {
  color: var(--color-danger);
  background: #fef2f2;
}
</style>
