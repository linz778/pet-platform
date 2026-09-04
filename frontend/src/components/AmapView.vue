<template>
  <div ref="mapEl" class="amap-container" :style="{ height }"></div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { loadAMap, AMAP_PLUGINS } from '@/utils/amap'

const props = defineProps({
  height: { type: String, default: '400px' },
  center: { type: Array, default: () => [116.397428, 39.90923] },
  zoom: { type: Number, default: 12 },
  plugins: { type: Array, default: () => AMAP_PLUGINS }
})
// error 是 key 无效 / 超配额 / 断网时父页面切换到降级 UI 的唯一信号源。
// 没有它，加载失败只会留下一个 400px 的灰色空 div，控制台之外毫无提示。
const emit = defineEmits(['loaded', 'error'])

const mapEl = ref(null)
let map = null

onMounted(async () => {
  try {
    const AMap = await loadAMap(props.plugins)
    map = new AMap.Map(mapEl.value, { center: props.center, zoom: props.zoom })
    emit('loaded', { AMap, map })
  } catch (e) {
    console.error('[AMap] 加载失败:', e.message)
    emit('error', e)
  }
})

onBeforeUnmount(() => {
  if (map) map.destroy()
})
</script>

<style scoped>
.amap-container {
  width: 100%;
}
</style>
