<template>
  <section class="reviews">
    <div class="section-head">
      <h4>双方评价</h4>
      <el-button link :loading="loading" @click="load">刷新</el-button>
    </div>

    <div v-loading="loading" class="review-list">
      <el-empty v-if="!loading && reviews.length === 0" :image-size="56" description="双方都还没有评价" />

      <article v-for="review in reviews" :key="review.id" class="review-item">
        <header class="review-head">
          <div class="author">
            <span>{{ authorText(review) }}</span>
            <el-tag v-if="review.mine" size="small" effect="plain">我写的</el-tag>
            <el-tag v-else-if="review.anonymous" size="small" type="info" effect="plain">匿名</el-tag>
          </div>
          <time>{{ review.createTime }}</time>
        </header>
        <el-rate :model-value="review.rating" disabled />
        <p :class="{ muted: !review.content }">{{ review.content || '未填写文字评价' }}</p>
      </article>
    </div>

    <el-alert
      v-if="hasReviewed"
      type="success"
      :closable="false"
      show-icon
      title="你已经评价过这笔订单"
    />

    <div v-else class="review-form">
      <h5>评价{{ targetLabel }}</h5>
      <div class="field-row">
        <span class="field-label">服务评分</span>
        <el-rate v-model="form.rating" show-text :texts="RATE_TEXTS" />
      </div>
      <el-input
        v-model="form.content"
        type="textarea"
        :rows="3"
        maxlength="500"
        show-word-limit
        :placeholder="`说说你对${targetLabel}的体验（选填）`"
      />
      <div class="submit-row">
        <el-checkbox v-model="form.anonymous">向对方匿名</el-checkbox>
        <el-button type="primary" :loading="submitting" @click="submit">提交评价</el-button>
      </div>
      <p class="anonymous-tip">匿名后对方看不到你的账号信息，但你自己仍能看到这条评价由你提交。</p>
    </div>
  </section>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { listOrderReviews, submitOrderReview } from '@/api/review'

const props = defineProps({
  orderId: { type: [Number, String], required: true },
  targetLabel: { type: String, default: '对方' }
})

const emit = defineEmits(['submitted'])
const RATE_TEXTS = ['很差', '失望', '一般', '满意', '非常满意']

const loading = ref(false)
const submitting = ref(false)
const reviews = ref([])
const form = reactive({ rating: 5, content: '', anonymous: false })
const hasReviewed = computed(() => reviews.value.some((review) => review.mine))

function authorText(review) {
  if (review.mine) return review.fromNickname || '我'
  if (review.anonymous) return '匿名用户'
  return review.fromNickname || '对方'
}

async function load() {
  if (!props.orderId) return
  loading.value = true
  try {
    reviews.value = (await listOrderReviews(props.orderId)) ?? []
  } catch {
    reviews.value = []
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (!form.rating) {
    ElMessage.warning('请选择星级')
    return
  }
  submitting.value = true
  try {
    await submitOrderReview({
      orderId: Number(props.orderId),
      rating: form.rating,
      content: form.content.trim() || undefined,
      anonymous: form.anonymous
    })
    ElMessage.success('评价已提交')
    form.content = ''
    form.anonymous = false
    await load()
    emit('submitted')
  } catch {
    // 重复评价与权限错误由统一响应拦截器提示；重新加载以服务端状态为准
    await load()
  } finally {
    submitting.value = false
  }
}

watch(() => props.orderId, load, { immediate: true })
</script>

<style scoped>
.reviews {
  margin-top: 20px;
}

.section-head,
.review-head,
.author,
.field-row,
.submit-row {
  display: flex;
  align-items: center;
}

.section-head,
.review-head,
.submit-row {
  justify-content: space-between;
  gap: 12px;
}

.section-head h4,
.review-form h5 {
  margin: 0;
}

.review-list {
  min-height: 64px;
}

.review-item {
  padding: 12px;
  margin-bottom: 10px;
  border: 1px solid var(--pp-tint-2);
  border-radius: var(--pp-radius);
  background: var(--el-fill-color-lighter);
}

.review-head {
  margin-bottom: 6px;
}

.author {
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
}

.review-head time,
.anonymous-tip {
  font-size: 12px;
  color: var(--pp-muted);
}

.review-item p {
  margin: 6px 0 0;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
}

.review-item p.muted {
  color: var(--pp-muted);
}

.review-form {
  padding: 14px;
  margin-top: 12px;
  border: 1px solid var(--pp-tint-2);
  border-radius: var(--pp-radius);
}

.field-row {
  gap: 12px;
  margin: 12px 0;
}

.field-label {
  font-size: 13px;
  color: var(--pp-muted);
}

.submit-row {
  margin-top: 12px;
}

.anonymous-tip {
  margin: 8px 0 0;
  line-height: 1.5;
}
</style>
