<template>
  <div class="evidence-list">
    <el-empty v-if="evidences.length === 0" :image-size="60" description="还没有履约存证" />

    <div v-for="e in evidences" :key="e.id" class="ev-item" :class="`ev-type-${e.type}`">
      <div class="ev-head">
        <span class="ev-title">{{ TYPE_ICON[e.type] ?? '📎' }} {{ titleOf(e) }}</span>
        <span class="ev-time">{{ e.createTime }}</span>
      </div>

      <!-- 打卡：只有坐标与距离，没有照片 -->
      <div v-if="e.type === 1" class="ev-line">
        <span>坐标 {{ e.lng }}, {{ e.lat }}</span>
        <span v-if="e.remark" class="ev-muted">（{{ e.remark }}）</span>
      </div>

      <!-- 清单存证：一项一张照片，点开看大图 -->
      <el-image
        v-else-if="e.type === 2 && e.imageUrl"
        class="ev-photo"
        :src="e.imageUrl"
        :preview-src-list="[e.imageUrl]"
        preview-teleported
        fit="cover"
      />

      <!-- 轨迹：没有地图 key 时只报点数与时间跨度，不硬画一条看不见的线 -->
      <div v-else-if="e.type === 3" class="ev-line">
        <span>{{ trackCount(e) }} 个轨迹点</span>
        <span v-if="trackSpan(e)" class="ev-muted">{{ trackSpan(e) }}</span>
      </div>

      <div v-if="e.remark && e.type !== 1" class="ev-line ev-muted">备注：{{ e.remark }}</div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  // GET /order/{id}/evidence 的原样数组
  evidences: { type: Array, default: () => [] }
})

const TYPE_ICON = { 1: '📍', 2: '📷', 3: '🦮' }

function titleOf(e) {
  if (e.type === 2) return e.checkItem ? `作业清单 · ${e.checkItem}` : '作业清单存证'
  return e.typeText || '存证'
}

function trackCount(e) {
  return e.trackPoints?.length ?? 0
}

// 后端解析 track_json 失败时只丢这一条的轨迹点（键还在、数组为空），
// 所以点数和时间跨度都要能各自缺省
function trackSpan(e) {
  const points = e.trackPoints ?? []
  if (points.length < 2) return ''
  const first = points[0].time
  const last = points[points.length - 1].time
  return first && last ? `${first.slice(11)} ~ ${last.slice(11)}` : ''
}
</script>

<style scoped>
.evidence-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.ev-item {
  padding: 10px 12px;
  border: 1px solid var(--pp-tint-2);
  border-radius: var(--pp-radius);
  background: #fff;
}

.ev-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}

.ev-title {
  font-size: 13px;
  font-weight: 600;
}

.ev-time {
  font-size: 12px;
  color: var(--pp-muted);
  white-space: nowrap;
}

.ev-line {
  margin-top: 6px;
  font-size: 12px;
}

.ev-muted {
  color: var(--pp-muted);
}

.ev-photo {
  margin-top: 8px;
  width: 120px;
  height: 120px;
  border-radius: 8px;
  cursor: zoom-in;
}
</style>
