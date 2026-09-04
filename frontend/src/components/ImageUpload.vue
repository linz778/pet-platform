<template>
  <div class="image-upload">
    <el-upload
      v-model:file-list="fileList"
      :class="{ 'is-full': fileList.length >= limit }"
      list-type="picture-card"
      accept="image/jpeg,image/png,image/webp"
      :limit="limit"
      :http-request="doUpload"
      :on-preview="handlePreview"
      :on-error="handleError"
      :on-exceed="handleExceed"
    >
      <span class="upload-plus">＋</span>
    </el-upload>

    <el-dialog v-model="previewVisible" title="图片预览" width="600px" append-to-body>
      <img :src="previewUrl" class="preview-img" alt="图片预览" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadImage } from '@/api/file'

const props = defineProps({
  // limit=1 时是字符串，limit>1 时是字符串数组，方便直接绑到 avatar / vaccineCert 这类单值字段
  modelValue: { type: [String, Array], default: '' },
  bizType: { type: String, default: 'common' },
  limit: { type: Number, default: 1 }
})
const emit = defineEmits(['update:modelValue'])

const fileList = ref([])
const previewVisible = ref(false)
const previewUrl = ref('')

// ready/uploading 的文件还没有 url，此时既不能回填也不能对外抛值，
// 否则正在上传的那一项会被 collectUrls 过滤掉、再被回填逻辑从列表里冲掉。
const uploading = computed(() =>
  fileList.value.some((f) => f.status === 'ready' || f.status === 'uploading')
)

function normalize(val) {
  if (Array.isArray(val)) return val.filter(Boolean)
  return val ? [val] : []
}

function collectUrls(list) {
  return list
    .filter((f) => f.status === 'success')
    .map((f) => f.response?.url ?? f.url)
    .filter(Boolean)
}

function sameUrls(a, b) {
  return a.length === b.length && a.every((v, i) => v === b[i])
}

watch(
  () => props.modelValue,
  (val) => {
    if (uploading.value) return
    const incoming = normalize(val)
    if (sameUrls(incoming, collectUrls(fileList.value))) return
    fileList.value = incoming.map((url, i) => ({ name: `image-${i + 1}`, url, status: 'success' }))
  },
  { immediate: true }
)

watch(
  fileList,
  () => {
    if (uploading.value) return
    const urls = collectUrls(fileList.value)
    emit('update:modelValue', props.limit === 1 ? urls[0] ?? '' : urls)
  },
  { deep: true }
)

// 返回 Promise 即可：el-upload 内部对 http-request 的结果做 then(onSuccess, onError)，
// 手动再调 options.onSuccess 会重复触发一次 success 回调。
function doUpload(options) {
  return uploadImage(options.file, props.bizType)
}

async function handleError(_err, file) {
  // 失败原因已由 request.js 的响应拦截器弹过提示，这里只把红色破图卡片从列表移除
  await nextTick()
  fileList.value = fileList.value.filter((f) => f.uid !== file.uid)
}

function handleExceed() {
  ElMessage.warning(`最多上传 ${props.limit} 张图片`)
}

function handlePreview(file) {
  previewUrl.value = file.response?.url ?? file.url
  previewVisible.value = true
}
</script>

<style scoped>
.upload-plus {
  font-size: 22px;
  line-height: 1;
  color: var(--pp-muted);
}

/* 达到上限后隐藏新增卡片，避免用户点出一个只会弹警告的空框 */
.is-full :deep(.el-upload--picture-card) {
  display: none;
}

.preview-img {
  display: block;
  width: 100%;
}
</style>
