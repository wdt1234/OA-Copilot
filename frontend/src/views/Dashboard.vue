<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import * as echarts from 'echarts'

// ── Mock 数据 ──

const stats = ref([
  { title: 'SQL 生成次数', value: 128, icon: 'Document', color: '#409eff' },
  { title: 'DEE 生成次数', value: 56, icon: 'Connection', color: '#67c23a' },
  { title: '字段映射次数', value: 89, icon: 'Switch', color: '#e6a23c' },
  { title: '节省开发时间', value: '42h', icon: 'Timer', color: '#f56c6c' }
])

const recentRecords = ref([
  { time: '2026-05-13 10:32', type: 'SQL', desc: '查询 formmain_1001 本月请假单', status: '成功' },
  { time: '2026-05-13 10:15', type: 'DEE', desc: '生成 workflow token 获取模板', status: '成功' },
  { time: '2026-05-13 09:58', type: '映射', desc: 'formmain_2003 字段映射到 JSON', status: '成功' },
  { time: '2026-05-12 17:20', type: 'SQL', desc: '查询 EBS 订单数据关联查询', status: '成功' },
  { time: '2026-05-12 16:45', type: 'DEE', desc: '生成 JSON 拼装模板', status: '失败' },
  { time: '2026-05-12 15:10', type: 'SQL', desc: '查询 DMP 客户信息', status: '成功' },
  { time: '2026-05-12 14:30', type: '映射', desc: 'field0056 到 ERP 字段映射', status: '成功' },
  { time: '2026-05-12 11:00', type: 'SQL', desc: '查询 OA 待办流程列表', status: '成功' }
])

const systemStatus = ref([
  { name: 'OA 连接', status: 'normal', desc: '致远 OA V8.1SP2' },
  { name: 'DEE 平台', status: 'normal', desc: '集成平台正常' },
  { name: 'Oracle', status: 'normal', desc: '只读连接正常' },
  { name: 'AI 服务', status: 'warning', desc: 'Minimax API 响应较慢' }
])

// ── 图表 ──

const chartRef = ref(null)
let chart = null

function initChart() {
  if (!chartRef.value) return
  chart = echarts.init(chartRef.value)

  const option = {
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: ['SQL 生成', 'DEE 生成', '字段映射'],
      bottom: 0
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '12%',
      top: '8%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: ['05-07', '05-08', '05-09', '05-10', '05-11', '05-12', '05-13']
    },
    yAxis: {
      type: 'value',
      minInterval: 1
    },
    series: [
      {
        name: 'SQL 生成',
        type: 'line',
        smooth: true,
        data: [12, 18, 15, 22, 19, 28, 24],
        itemStyle: { color: '#409eff' }
      },
      {
        name: 'DEE 生成',
        type: 'line',
        smooth: true,
        data: [5, 8, 6, 10, 7, 12, 8],
        itemStyle: { color: '#67c23a' }
      },
      {
        name: '字段映射',
        type: 'line',
        smooth: true,
        data: [8, 12, 10, 15, 11, 18, 15],
        itemStyle: { color: '#e6a23c' }
      }
    ]
  }

  chart.setOption(option)
}

function handleResize() {
  chart?.resize()
}

onMounted(() => {
  initChart()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  chart?.dispose()
})

// ── 辅助 ──

function typeTag(type) {
  const map = { SQL: '', DEE: 'success', 映射: 'warning' }
  return map[type] || ''
}

function statusTag(status) {
  return status === '成功' ? 'success' : 'danger'
}

function statusDot(status) {
  return status === 'normal' ? '#67c23a' : '#e6a23c'
}
</script>

<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :xs="12" :sm="6" v-for="item in stats" :key="item.title">
        <el-card shadow="never" class="stat-card">
          <div class="stat-body">
            <div class="stat-info">
              <div class="stat-label">{{ item.title }}</div>
              <div class="stat-value">{{ item.value }}</div>
            </div>
            <el-icon :size="40" :color="item.color" class="stat-icon">
              <component :is="item.icon" />
            </el-icon>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 趋势图 + 系统状态 -->
    <el-row :gutter="16" class="middle-row">
      <el-col :xs="24" :lg="16">
        <el-card shadow="never">
          <template #header>
            <span class="card-title">使用趋势（近 7 天）</span>
          </template>
          <div ref="chartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="8">
        <el-card shadow="never">
          <template #header>
            <span class="card-title">系统状态</span>
          </template>
          <div class="status-list">
            <div
              v-for="item in systemStatus"
              :key="item.name"
              class="status-item"
            >
              <div class="status-left">
                <span
                  class="status-dot"
                  :style="{ backgroundColor: statusDot(item.status) }"
                ></span>
                <span class="status-name">{{ item.name }}</span>
              </div>
              <span class="status-desc">{{ item.desc }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 最近使用记录 -->
    <el-card shadow="never" class="records-card">
      <template #header>
        <span class="card-title">最近使用记录</span>
      </template>
      <el-table :data="recentRecords" stripe size="default">
        <el-table-column prop="time" label="时间" width="170" />
        <el-table-column prop="type" label="类型" width="90">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.type)" size="small">{{ row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="desc" label="描述" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 统计卡片 */
.stat-row .el-col {
  margin-bottom: 0;
}

.stat-card {
  border: 1px solid #e8e8e8;
}

.stat-body {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.stat-label {
  font-size: 14px;
  color: #8c8c8c;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: #262626;
}

.stat-icon {
  opacity: 0.8;
}

/* 中间行 */
.middle-row {
  margin-top: 0;
}

.card-title {
  font-size: 15px;
  font-weight: 500;
  color: #262626;
}

.chart-container {
  height: 300px;
}

/* 系统状态 */
.status-list {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.status-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.status-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.status-name {
  font-size: 14px;
  color: #262626;
  font-weight: 500;
}

.status-desc {
  font-size: 13px;
  color: #8c8c8c;
}

/* 记录表格 */
.records-card {
  border: 1px solid #e8e8e8;
}

:deep(.el-card__header) {
  padding: 14px 20px;
  border-bottom: 1px solid #f0f0f0;
}

:deep(.el-card__body) {
  padding: 16px 20px;
}
</style>
