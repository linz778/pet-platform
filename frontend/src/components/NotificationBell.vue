<template>
  <el-popover placement="bottom-end" :width="380" trigger="click" @show="refresh">
    <template #reference>
      <el-badge :value="unread" :hidden="!unread" :max="99"><el-button circle aria-label="站内消息">🔔</el-button></el-badge>
    </template>
    <div class="inbox-head"><strong>站内消息</strong><el-button v-if="unread" link type="primary" @click="readAll">全部已读</el-button></div>
    <div class="inbox-list">
      <el-empty v-if="!items.length" :image-size="70" description="暂无消息" />
      <button v-for="item in items" :key="item.id" :class="['message', { unread: item.readStatus === 0 }]" @click="openMessage(item)">
        <i /><span><strong>{{ item.title }}</strong><small>{{ item.content }}</small><time>{{ timeText(item.createTime) }}</time></span>
      </button>
    </div>
  </el-popover>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getInbox, markAllNotificationsRead, markNotificationRead } from '@/api/notification'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const items = ref([])
const unread = ref(0)
let timer

async function refresh() {
  try {
    const data = await getInbox()
    items.value = data.items || []
    unread.value = Number(data.unreadCount || 0)
  } catch { /* 后台轮询失败时保持安静，下次自动重试 */ }
}

async function openMessage(item) {
  if (item.readStatus === 0) {
    await markNotificationRead(item.id)
    item.readStatus = 1
    unread.value = Math.max(0, unread.value - 1)
  }
  if (item.type === 'SITTER_AUDIT') router.push(userStore.role === 'ADMIN' ? '/admin/audit' : '/sitter/hall')
  if (item.type === 'ARBITRATION') router.push(userStore.role === 'ADMIN' ? '/admin/arbitration' : userStore.role === 'SITTER' ? '/sitter/orders' : '/user/orders')
  if (item.type === 'BOUNTY_REVIEW') router.push(userStore.role === 'ADMIN' ? '/admin/bounty-review' : userStore.role === 'SITTER' ? '/sitter/orders' : '/user/orders')
}

async function readAll() {
  await markAllNotificationsRead()
  items.value.forEach((item) => { item.readStatus = 1 })
  unread.value = 0
}

function timeText(value) { return value ? value.replace('T', ' ').slice(0, 16) : '' }

onMounted(() => { refresh(); timer = window.setInterval(refresh, 15000) })
onBeforeUnmount(() => window.clearInterval(timer))
</script>

<style scoped>
.inbox-head { display: flex; align-items: center; justify-content: space-between; padding: 2px 4px 10px; border-bottom: 1px solid #edf0ee; }.inbox-list { max-height: 430px; overflow-y: auto; }.message { display: flex; width: 100%; gap: 10px; padding: 13px 5px; border: 0; border-bottom: 1px solid #f0f2f1; background: transparent; color: #334139; text-align: left; cursor: pointer; }.message:hover { background: #f6faf7; }.message i { width: 7px; height: 7px; margin-top: 6px; border-radius: 50%; background: transparent; }.message.unread i { background: var(--pp-primary); }.message span,.message strong,.message small,.message time { display: block; }.message span { flex: 1; min-width: 0; }.message small { margin-top: 4px; color: #69766f; line-height: 1.5; }.message time { margin-top: 5px; color: #a0a8a3; font-size: 11px; }
</style>
