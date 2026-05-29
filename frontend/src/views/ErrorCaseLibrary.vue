<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Warning, Plus, Edit, Delete, Star, StarFilled } from '@element-plus/icons-vue'
import axios from 'axios'

// ── 状态 ──

const loading = ref(false)
const errorCases = ref([])
const activeCategory = ref('all')
const searchKeyword = ref('')

// ── 新增/编辑弹窗 ──

const showDialog = ref(false)
const isEdit = ref(false)
const formData = ref({
  id: null,
  category: 'sql',
  title: '',
  symptom: '',
  cause: '',
  solution: '',
  exampleWrong: '',
  exampleCorrect: '',
  tags: ''
})

// ── 分类配置 ──

const categories = [
  { value: 'all', label: '全部' },
  { value: 'sql', label: 'SQL 错误' },
  { value: 'dee', label: 'DEE 错误' },
  { value: 'token', label: 'Token 错误' }
]

// ── 加载数据 ──

async function loadErrorCases() {
  loading.value = true
  try {
    const { data } = await axios.get('/api/error-cases')
    errorCases.value = data
  } catch (e) {
    console.error('加载错误案例失败', e)
    ElMessage.error('加载错误案例失败')
  } finally {
    loading.value = false
  }
}

async function searchCases() {
  if (!searchKeyword.value.trim()) {
    loadErrorCases()
    return
  }
  loading.value = true
  try {
    const { data } = await axios.get('/api/error-cases/search', {
      params: {
        category: activeCategory.value === 'all' ? '' : activeCategory.value,
        keyword: searchKeyword.value
      }
    })
    errorCases.value = data
  } catch (e) {
    console.error('搜索失败', e)
    ElMessage.error('搜索失败')
  } finally {
    loading.value = false
  }
}

// ── 过滤 ──

const filteredCases = computed(() => {
  if (activeCategory.value === 'all') {
    return errorCases.value
  }
  return errorCases.value.filter(c => c.category === activeCategory.value)
})

// ── 分类标签 ──

function getCategoryTag(category) {
  const map = {
    sql: { label: 'SQL', type: 'primary' },
    dee: { label: 'DEE', type: 'success' },
    token: { label: 'Token', type: 'warning' }
  }
  return map[category] || { label: category, type: 'info' }
}

// ── 新增 ──

function handleAdd() {
  isEdit.value = false
  formData.value = {
    id: null,
    category: 'sql',
    title: '',
    symptom: '',
    cause: '',
    solution: '',
    exampleWrong: '',
    exampleCorrect: '',
    tags: ''
  }
  showDialog.value = true
}

// ── 编辑 ──

function handleEdit(row) {
  isEdit.value = true
  formData.value = {
    id: row.id,
    category: row.category,
    title: row.title,
    symptom: row.symptom || '',
    cause: row.cause || '',
    solution: row.solution || '',
    exampleWrong: row.exampleWrong || '',
    exampleCorrect: row.exampleCorrect || '',
    tags: row.tags || ''
  }
  showDialog.value = true
}

// ── 保存 ──

async function handleSave() {
  if (!formData.value.title.trim()) {
    ElMessage.warning('请输入错误标题')
    return
  }

  try {
    if (isEdit.value) {
      await axios.put(`/api/error-cases/${formData.value.id}`, formData.value)
      ElMessage.success('更新成功')
    } else {
      await axios.post('/api/error-cases', formData.value)
      ElMessage.success('添加成功')
    }
    showDialog.value = false
    loadErrorCases()
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

// ── 删除 ──

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定要删除"${row.title}"吗？`, '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await axios.delete(`/api/error-cases/${row.id}`)
    ElMessage.success('删除成功')
    loadErrorCases()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// ── 置顶 ──

async function handleTogglePin(row) {
  try {
    const newPinned = row.isPinned === 1 ? 0 : 1
    await axios.put(`/api/error-cases/${row.id}/pinned`, { isPinned: newPinned })
    ElMessage.success(newPinned === 1 ? '已置顶' : '已取消置顶')
    loadErrorCases()
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

// ── 初始化 ──

onMounted(() => {
  loadErrorCases()
})
</script>

<template>
  <div class="error-case-library" v-loading="loading">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="page-title">
        <el-icon :size="20"><Warning /></el-icon>
        <span>错题库</span>
      </div>
      <div class="page-desc">记录真实遇到的错误案例和解决方案</div>
    </div>

    <!-- 操作栏 -->
    <div class="action-bar">
      <div class="filter-left">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索错误案例..."
          :prefix-icon="Search"
          clearable
          @keyup.enter="searchCases"
          @clear="loadErrorCases"
          class="search-input"
        />
        <el-button @click="searchCases">搜索</el-button>
      </div>
      <el-button type="primary" :icon="Plus" @click="handleAdd">
        新增错误案例
      </el-button>
    </div>

    <!-- 分类标签 -->
    <div class="category-tabs">
      <div
        v-for="cat in categories"
        :key="cat.value"
        class="category-tab"
        :class="{ 'category-tab--active': activeCategory === cat.value }"
        @click="activeCategory = cat.value"
      >
        <span>{{ cat.label }}</span>
      </div>
    </div>

    <!-- 错误案例列表 -->
    <div class="cases-list">
      <div v-if="filteredCases.length === 0" class="empty-state">
        <el-icon :size="48"><Warning /></el-icon>
        <p>暂无错误案例</p>
        <el-button type="primary" @click="handleAdd">添加第一个案例</el-button>
      </div>

      <div v-for="item in filteredCases" :key="item.id" class="case-card">
        <div class="case-header">
          <el-tag :type="getCategoryTag(item.category).type" size="small">
            {{ getCategoryTag(item.category).label }}
          </el-tag>
          <h3 class="case-title">{{ item.title }}</h3>
          <div class="case-actions">
            <el-button
              :icon="item.isPinned === 1 ? StarFilled : Star"
              circle
              size="small"
              @click="handleTogglePin(item)"
              :type="item.isPinned === 1 ? 'warning' : 'default'"
            />
            <el-button :icon="Edit" circle size="small" @click="handleEdit(item)" />
            <el-button :icon="Delete" circle size="small" type="danger" @click="handleDelete(item)" />
          </div>
        </div>

        <div class="case-body">
          <div v-if="item.symptom" class="case-section">
            <label>现象</label>
            <p class="case-text case-text--symptom">{{ item.symptom }}</p>
          </div>

          <div v-if="item.cause" class="case-section">
            <label>原因</label>
            <p class="case-text">{{ item.cause }}</p>
          </div>

          <div v-if="item.solution" class="case-section">
            <label>解决方案</label>
            <p class="case-text case-text--solution">{{ item.solution }}</p>
          </div>

          <div v-if="item.exampleWrong || item.exampleCorrect" class="case-example">
            <label>示例</label>
            <div class="example-grid">
              <div v-if="item.exampleWrong" class="example-item example-item--wrong">
                <span class="example-label">错误</span>
                <code>{{ item.exampleWrong }}</code>
              </div>
              <div v-if="item.exampleCorrect" class="example-item example-item--correct">
                <span class="example-label">正确</span>
                <code>{{ item.exampleCorrect }}</code>
              </div>
            </div>
          </div>
        </div>

        <div v-if="item.tags" class="case-footer">
          <el-tag
            v-for="tag in item.tags.split(',')"
            :key="tag"
            size="small"
            type="info"
            class="case-tag"
          >
            {{ tag.trim() }}
          </el-tag>
        </div>
      </div>
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="showDialog"
      :title="isEdit ? '编辑错误案例' : '新增错误案例'"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form :model="formData" label-width="100px">
        <el-form-item label="分类" required>
          <el-select v-model="formData.category" style="width: 100%">
            <el-option label="SQL 错误" value="sql" />
            <el-option label="DEE 错误" value="dee" />
            <el-option label="Token 错误" value="token" />
          </el-select>
        </el-form-item>

        <el-form-item label="错误标题" required>
          <el-input v-model="formData.title" placeholder="如：ORA-00904: 无效标识符" />
        </el-form-item>

        <el-form-item label="错误现象">
          <el-input v-model="formData.symptom" type="textarea" :rows="2" placeholder="错误提示信息" />
        </el-form-item>

        <el-form-item label="原因分析">
          <el-input v-model="formData.cause" type="textarea" :rows="2" placeholder="导致错误的原因" />
        </el-form-item>

        <el-form-item label="解决方案">
          <el-input v-model="formData.solution" type="textarea" :rows="2" placeholder="如何解决这个错误" />
        </el-form-item>

        <el-form-item label="错误示例">
          <el-input v-model="formData.exampleWrong" type="textarea" :rows="2" placeholder="错误的代码或写法" />
        </el-form-item>

        <el-form-item label="正确示例">
          <el-input v-model="formData.exampleCorrect" type="textarea" :rows="2" placeholder="正确的代码或写法" />
        </el-form-item>

        <el-form-item label="标签">
          <el-input v-model="formData.tags" placeholder="用逗号分隔，如：字段不存在,拼写错误" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.error-case-library {
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

.action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.filter-left {
  display: flex;
  gap: 12px;
}

.search-input {
  width: 300px;
}

.category-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 24px;
  flex-wrap: wrap;
}

.category-tab {
  padding: 8px 16px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-secondary);
  background: var(--color-bg-card);
  border: 1px solid var(--color-border-light);
  transition: all var(--transition-fast);
}

.category-tab:hover {
  background: var(--color-bg-page);
  color: var(--color-text-primary);
}

.category-tab--active {
  background: var(--color-primary);
  color: white;
  border-color: var(--color-primary);
}

.cases-list {
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

.case-card {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border-light);
  border-radius: 12px;
  overflow: hidden;
}

.case-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  border-bottom: 1px solid var(--color-border-light);
}

.case-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
  flex: 1;
}

.case-actions {
  display: flex;
  gap: 8px;
}

.case-body {
  padding: 20px;
}

.case-section {
  margin-bottom: 16px;
}

.case-section:last-child {
  margin-bottom: 0;
}

.case-section label {
  display: block;
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 6px;
}

.case-text {
  margin: 0;
  font-size: 14px;
  line-height: 1.6;
  color: var(--color-text-primary);
}

.case-text--symptom {
  color: var(--color-danger);
  font-family: monospace;
}

.case-text--solution {
  color: var(--color-success);
}

.case-example {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--color-border-light);
}

.example-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.example-item {
  padding: 12px;
  border-radius: 8px;
  background: var(--color-bg-page);
}

.example-label {
  display: inline-block;
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 6px;
}

.example-item--wrong .example-label {
  color: var(--color-danger);
}

.example-item--correct .example-label {
  color: var(--color-success);
}

.example-item code {
  display: block;
  font-size: 13px;
  color: var(--color-text-primary);
  word-break: break-all;
}

.case-footer {
  display: flex;
  gap: 8px;
  padding: 12px 20px;
  border-top: 1px solid var(--color-border-light);
  background: var(--color-bg-page);
  flex-wrap: wrap;
}

.case-tag {
  font-size: 12px;
}
</style>
