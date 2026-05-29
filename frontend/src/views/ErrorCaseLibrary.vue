<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Warning, Document, Connection, Key } from '@element-plus/icons-vue'
import axios from 'axios'

// ── 状态 ──

const loading = ref(false)
const errorCases = ref([])
const activeCategory = ref('all')
const searchKeyword = ref('')

// ── 分类配置 ──

const categories = [
  { value: 'all', label: '全部', icon: Document },
  { value: 'sql', label: 'SQL 错误', icon: Document },
  { value: 'dee', label: 'DEE 错误', icon: Connection },
  { value: 'token', label: 'Token 错误', icon: Key }
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
      params: { keyword: searchKeyword.value }
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
      <div class="page-desc">常见错误案例与解决方案</div>
    </div>

    <!-- 搜索和筛选 -->
    <div class="filter-bar">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索错误案例..."
        :prefix-icon="Search"
        clearable
        @keyup.enter="searchCases"
        @clear="loadErrorCases"
        class="search-input"
      />
      <el-button type="primary" @click="searchCases">搜索</el-button>
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
        <el-icon><component :is="cat.icon" /></el-icon>
        <span>{{ cat.label }}</span>
      </div>
    </div>

    <!-- 错误案例列表 -->
    <div class="cases-list">
      <div v-if="filteredCases.length === 0" class="empty-state">
        <el-icon :size="48"><Warning /></el-icon>
        <p>暂无错误案例</p>
      </div>

      <div v-for="item in filteredCases" :key="item.id" class="case-card">
        <div class="case-header">
          <el-tag :type="getCategoryTag(item.category).type" size="small">
            {{ getCategoryTag(item.category).label }}
          </el-tag>
          <h3 class="case-title">{{ item.title }}</h3>
        </div>

        <div class="case-body">
          <div class="case-section">
            <label>现象</label>
            <p class="case-text case-text--symptom">{{ item.symptom }}</p>
          </div>

          <div class="case-section">
            <label>原因</label>
            <p class="case-text">{{ item.cause }}</p>
          </div>

          <div class="case-section">
            <label>解决方案</label>
            <p class="case-text case-text--solution">{{ item.solution }}</p>
          </div>

          <div v-if="item.example" class="case-example">
            <label>示例</label>
            <div class="example-grid">
              <div v-if="item.example.wrong" class="example-item example-item--wrong">
                <span class="example-label">错误</span>
                <code>{{ item.example.wrong }}</code>
              </div>
              <div v-if="item.example.correct" class="example-item example-item--correct">
                <span class="example-label">正确</span>
                <code>{{ item.example.correct }}</code>
              </div>
            </div>
          </div>
        </div>

        <div v-if="item.tags && item.tags.length" class="case-footer">
          <el-tag
            v-for="tag in item.tags"
            :key="tag"
            size="small"
            type="info"
            class="case-tag"
          >
            {{ tag }}
          </el-tag>
        </div>
      </div>
    </div>
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

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.search-input {
  flex: 1;
  max-width: 400px;
}

.category-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 24px;
  flex-wrap: wrap;
}

.category-tab {
  display: flex;
  align-items: center;
  gap: 6px;
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
  margin-top: 12px;
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
}

.case-tag {
  font-size: 12px;
}
</style>
