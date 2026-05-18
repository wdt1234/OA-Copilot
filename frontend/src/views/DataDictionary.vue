<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
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
        <el-card shadow="never">
          <template #header>
            <span class="card-title">录入数据字典</span>
          </template>

          <div class="input-hint">
            从 OA 表单设计器复制字段信息，粘贴到下方文本框，系统自动解析。
          </div>

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
              :loading="parsing"
              @click="parsePreview"
            >
              {{ parsing ? '解析中...' : '解析预览' }}
            </el-button>
            <el-button @click="clearForm">清空</el-button>
          </div>
        </el-card>

        <!-- 预览区 -->
        <el-card v-if="showPreview && parsedResult" shadow="never" class="preview-card">
          <template #header>
            <div class="preview-header">
              <span class="card-title">解析结果预览</span>
              <el-button
                type="primary"
                :loading="saving"
                @click="saveDictionary"
              >
                {{ saving ? '保存中...' : '确认保存' }}
              </el-button>
            </div>
          </template>

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
        </el-card>
      </el-col>

      <!-- 右侧：已有数据字典 -->
      <el-col :xs="24" :lg="10">
        <el-card shadow="never" class="existing-card">
          <template #header>
            <div class="existing-header">
              <span class="card-title">已录入的数据字典</span>
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
          </template>

          <div v-if="existingDictionaries.length === 0" class="empty-hint">
            <el-empty description="暂无数据字典" :image-size="60" />
          </div>

          <div v-else class="dict-list">
            <div
              v-for="item in existingDictionaries"
              :key="item.formCode"
              class="dict-item"
              :class="{ 'dict-item-pinned': item.isPinned }"
            >
              <div class="dict-info" @click="viewDictionary(item)">
                <div class="dict-name">
                  <el-icon v-if="item.isPinned" class="pin-icon"><StarFilled /></el-icon>
                  {{ item.formName }}
                </div>
                <div class="dict-meta">
                  <el-tag size="small" type="info">{{ item.tableName }}</el-tag>
                  <span class="dict-code">{{ item.formCode }}</span>
                </div>
              </div>
              <div class="dict-actions">
                <el-button
                  :type="item.isPinned ? 'warning' : 'info'"
                  size="small"
                  text
                  @click="togglePin(item)"
                  :title="item.isPinned ? '取消置顶' : '置顶'"
                >
                  <el-icon><StarFilled v-if="item.isPinned" /><Star v-else /></el-icon>
                </el-button>
                <el-button
                  type="danger"
                  size="small"
                  text
                  @click="deleteDictionary(item)"
                >
                  删除
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
.data-dictionary {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.card-title {
  font-size: 15px;
  font-weight: 500;
  color: #262626;
}

.input-hint {
  font-size: 13px;
  color: #8c8c8c;
  margin-bottom: 12px;
}

.raw-input {
  margin-bottom: 12px;
}

:deep(.el-textarea__inner) {
  font-size: 13px;
  font-family: 'Consolas', 'Monaco', monospace;
  line-height: 1.6;
}

.actions {
  display: flex;
  gap: 8px;
}

/* 预览区 */
.preview-card {
  margin-top: 16px;
}

.preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.form-code-hint {
  font-size: 12px;
  color: #bfbfbf;
  margin-left: 8px;
}

.fields-preview {
  margin-top: 16px;
}

.fields-preview h4 {
  font-size: 13px;
  color: #595959;
  margin-bottom: 8px;
}

/* 已有列表 */
.existing-card {
  height: 100%;
}

.dict-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.dict-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  border-radius: 4px;
  transition: background-color 0.15s;
}

.dict-item:hover {
  background-color: #f5f7fa;
}

.dict-item-pinned {
  background-color: #fffbe6;
}

.dict-item-pinned:hover {
  background-color: #fff7cc;
}

.pin-icon {
  color: #faad14;
  margin-right: 4px;
  font-size: 14px;
}

.dict-info {
  cursor: pointer;
  flex: 1;
}

.dict-name {
  font-size: 14px;
  color: #262626;
  margin-bottom: 4px;
}

.dict-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.dict-code {
  font-size: 12px;
  color: #bfbfbf;
}

.existing-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.dict-actions {
  display: flex;
  align-items: center;
  gap: 0;
}

.empty-hint {
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
