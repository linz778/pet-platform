<template>
  <div ref="mapEl" class="amap-container" :style="{ height }"></div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { loadAMap } from '@/utils/amap'

const props = defineProps({
  height: { type: String, default: '400px' },
  center: { type: Array, default: () => [116.397428, 39.90923] },
  zoom: { type: Number, default: 12 },
  plugins: { type: Array, default: () => [] }
})
const emit = defineEmits(['loaded'])

const mapEl = ref(null)
let map = null

onMounted(async () => {
  try {
    const AMap = await loadAMap(props.plugins)
    map = new AMap.Map(mapEl.value, { center: props.center, zoom: props.zoom })
    emit('loaded', { AMap, map })
  } catch (e) {
    console.error('[AMap] 加载失败:', e.message)
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
