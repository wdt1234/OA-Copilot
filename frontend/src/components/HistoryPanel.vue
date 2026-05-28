<script setup>
import { Star, StarFilled, Delete } from '@element-plus/icons-vue'

const props = defineProps({
  history: { type: Array, default: () => [] },
  selectMode: { type: Boolean, default: false },
  selectedIds: { type: Array, default: () => [] },
  title: { type: String, default: '历史记录' },
  maxHeight: { type: String, default: 'none' }
})

const emit = defineEmits([
  'load', 'pin', 'delete',
  'toggleSelect', 'toggleSelectAll', 'batchDelete',
  'enterSelect', 'exitSelect'
])

function isSelected(id) {
  return props.selectedIds.includes(id)
}
</script>

<template>
  <div class="panel">
    <div class="panel__header">
      <h3 class="panel__title">
        <el-icon :size="16"><Clock /></el-icon>
        <span class="panel__title-text">{{ title }}</span>
      </h3>
      <div class="history-panel__actions">
        <template v-if="!selectMode">
          <button
            v-if="history.length > 0"
            class="history-panel__btn"
            @click="emit('enterSelect')"
          >选择</button>
        </template>
        <template v-else>
          <button class="history-panel__btn" @click="emit('toggleSelectAll')">
            {{ selectedIds.length === history.length ? '取消全选' : '全选' }}
          </button>
          <button
            v-if="selectedIds.length > 0"
            class="history-panel__btn history-panel__btn--danger"
            @click="emit('batchDelete')"
          >删除 ({{ selectedIds.length }})</button>
          <button class="history-panel__btn" @click="emit('exitSelect')">取消</button>
        </template>
      </div>
    </div>

    <div class="panel__body" :style="{ maxHeight, overflowY: maxHeight !== 'none' ? 'auto' : 'visible' }">
      <div v-if="history.length === 0" class="history-empty">
        <p>暂无历史记录</p>
      </div>

      <div v-else class="history-list">
        <div
          v-for="item in history"
          :key="item.id"
          class="history-item"
          :class="{
            'history-item--pinned': item.isPinned,
            'history-item--selected': isSelected(item.id)
          }"
        >
          <input
            v-if="selectMode"
            type="checkbox"
            class="history-panel__checkbox"
            :checked="isSelected(item.id)"
            @change="emit('toggleSelect', item.id)"
            @click.stop
          />
          <div class="history-item__content" @click="emit('load', item)">
            <slot name="item" :item="item">
              <span class="history-panel__default-text">{{ item.prompt || item.desc || item.formId || '' }}</span>
            </slot>
          </div>
          <div v-if="!selectMode" class="history-item__actions">
            <button
              class="history-panel__icon-btn"
              :class="{ 'history-panel__icon-btn--pinned': item.isPinned }"
              @click.stop="emit('pin', item)"
              :title="item.isPinned ? '取消置顶' : '置顶'"
            >
              <el-icon :size="14"><StarFilled v-if="item.isPinned" /><Star v-else /></el-icon>
            </button>
            <button
              class="history-panel__icon-btn history-panel__icon-btn--danger"
              @click.stop="emit('delete', item)"
            >
              <el-icon :size="14"><Delete /></el-icon>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.history-panel__actions {
  display: flex;
  gap: 4px;
}

.history-panel__btn {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--color-text-muted);
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast);
  font-weight: 500;
}

.history-panel__btn:hover {
  color: var(--color-primary);
  background: var(--color-primary-light);
}

.history-panel__btn--danger:hover {
  color: var(--color-danger);
  background: #fef2f2;
}

.history-panel__checkbox {
  width: 14px;
  height: 14px;
  accent-color: var(--color-primary);
  cursor: pointer;
  flex-shrink: 0;
  margin-right: 8px;
}

.history-panel__default-text {
  font-size: 13px;
  color: var(--color-text-primary);
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.history-panel__icon-btn {
  width: 24px;
  height: 24px;
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

.history-panel__icon-btn:hover {
  background: var(--color-bg-page);
  color: var(--color-text-secondary);
}

.history-panel__icon-btn--pinned {
  color: var(--color-warning);
}

.history-panel__icon-btn--danger:hover {
  color: var(--color-danger);
  background: #fef2f2;
}
</style>
