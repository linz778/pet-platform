<template>
  <div class="review-page">
    <header class="review-hero">
      <div><span>COMMUNITY SAFETY</span><h1>社区内容审核</h1><p>集中查看晒宠动态与养宠问答，对违规帖子和评论进行隐藏或恢复。</p></div>
      <div class="hero-total"><strong>{{ total }}</strong><small>条匹配内容</small></div>
    </header>

    <section class="filter-panel">
      <el-select v-model="query.type" clearable placeholder="全部内容类型" @change="changeFilter">
        <el-option label="晒宠分享" :value="1" /><el-option label="养宠问答" :value="2" />
      </el-select>
      <el-select v-model="query.status" clearable placeholder="全部展示状态" @change="changeFilter">
        <el-option label="公开" :value="1" /><el-option label="已隐藏" :value="0" />
      </el-select>
      <el-input v-model="query.keyword" clearable placeholder="搜索动态内容" @keyup.enter="changeFilter" />
      <el-button :loading="loading" @click="changeFilter">查询</el-button>
    </section>

    <section class="table-panel">
      <el-table v-loading="loading" :data="posts" empty-text="当前没有社区内容">
        <el-table-column label="发布者" width="150">
          <template #default="{ row }"><strong>{{ row.authorName || '未知用户' }}</strong><small>{{ row.authorRole === 'SITTER' ? '接单员' : '宠物主人' }}</small></template>
        </el-table-column>
        <el-table-column label="内容" min-width="350">
          <template #default="{ row }"><div class="post-title"><el-tag size="small" effect="plain">{{ row.typeText }}</el-tag><strong v-if="row.title && row.title !== row.content?.slice(0, 100)">{{ row.title }}</strong></div><p>{{ row.content }}</p></template>
        </el-table-column>
        <el-table-column label="互动" width="110"><template #default="{ row }">🐾 {{ row.likeCount || 0 }}　💬 {{ row.commentCount || 0 }}</template></el-table-column>
        <el-table-column prop="createTime" label="发布时间" width="170" />
        <el-table-column label="状态" width="90"><template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '公开' : '已隐藏' }}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button link @click="openComments(row)">审核评论</el-button>
            <el-button link :type="row.status === 1 ? 'danger' : 'success'" @click="togglePost(row)">{{ row.status === 1 ? '隐藏' : '恢复' }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-if="total" layout="total, prev, pager, next" :total="total" :page-size="query.size" :current-page="query.page" @current-change="changePage" />
    </section>

    <el-dialog v-model="commentVisible" :title="`评论审核 · ${currentPost?.content?.slice(0, 20) || ''}`" width="680px">
      <div v-loading="commentLoading" class="comment-list">
        <el-empty v-if="!commentLoading && !comments.length" description="该帖子暂无评论" />
        <article v-for="item in comments" :key="item.id" :class="{ hidden: item.status === 0 }">
          <div><strong>{{ item.authorName || '未知用户' }}</strong><el-tag v-if="item.authorRole === 'SITTER'" size="small" type="success" effect="plain">接单员</el-tag><time>{{ item.createTime }}</time></div>
          <p>{{ item.content }}</p>
          <el-button link :type="item.status === 1 ? 'danger' : 'success'" @click="toggleComment(item)">{{ item.status === 1 ? '隐藏评论' : '恢复评论' }}</el-button>
        </article>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listCommunityComments, pageCommunityPosts, setCommunityCommentStatus, setCommunityPostStatus } from '@/api/adminCommunity'

const query = reactive({ page: 1, size: 10, type: null, status: null, keyword: '' })
const posts = ref([])
const total = ref(0)
const loading = ref(false)
const commentVisible = ref(false)
const commentLoading = ref(false)
const currentPost = ref(null)
const comments = ref([])

async function loadPosts() {
  loading.value = true
  try {
    const params = { page: query.page, size: query.size, keyword: query.keyword.trim() || undefined }
    if (query.type != null) params.type = query.type
    if (query.status != null) params.status = query.status
    const data = await pageCommunityPosts(params)
    posts.value = data.records || []
    total.value = data.total || 0
  } finally { loading.value = false }
}

function changeFilter() { query.page = 1; loadPosts() }
function changePage(page) { query.page = page; loadPosts() }

async function togglePost(post) {
  const status = post.status === 1 ? 0 : 1
  const action = status === 0 ? '隐藏' : '恢复'
  const confirmed = await ElMessageBox.confirm(`确认${action}「${post.content.slice(0, 30)}」吗？`, `${action}社区内容`, { type: status === 0 ? 'warning' : 'success' }).catch(() => false)
  if (!confirmed) return
  await setCommunityPostStatus(post.id, status)
  post.status = status
  ElMessage.success(`帖子已${action}`)
}

async function openComments(post) {
  currentPost.value = post
  commentVisible.value = true
  commentLoading.value = true
  try { comments.value = await listCommunityComments(post.id) } finally { commentLoading.value = false }
}

async function toggleComment(comment) {
  const status = comment.status === 1 ? 0 : 1
  await setCommunityCommentStatus(comment.id, status)
  comment.status = status
  currentPost.value.commentCount = Math.max(0, Number(currentPost.value.commentCount || 0) + (status === 1 ? 1 : -1))
  ElMessage.success(status === 1 ? '评论已恢复' : '评论已隐藏')
}

onMounted(loadPosts)
</script>

<style scoped>
.review-page { min-height: calc(100vh - 100px); color: #24332a; }
.review-hero { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; padding: 25px 30px; border-radius: 20px; background: linear-gradient(120deg, #342d4f, #655889 70%, #8f7fb0); color: #fff; box-shadow: 0 14px 32px rgb(58 47 88 / 18%); }
.review-hero span { color: #d9d0ef; font-size: 10px; font-weight: 700; letter-spacing: 2px; }.review-hero h1 { margin: 6px 0 4px; font-size: 27px; }.review-hero p { margin: 0; color: rgb(255 255 255 / 72%); font-size: 12px; }
.hero-total { display: flex; align-items: center; flex-direction: column; min-width: 92px; padding: 12px 18px; border-radius: 15px; background: rgb(255 255 255 / 12%); }.hero-total strong { font-size: 27px; }.hero-total small { color: rgb(255 255 255 / 70%); }
.filter-panel { display: flex; gap: 10px; margin-bottom: 14px; padding: 14px 17px; border: 1px solid #e4e3eb; border-radius: 15px; background: #fff; }.filter-panel .el-select { width: 170px; }.filter-panel .el-input { width: 260px; }
.table-panel { padding: 8px 18px 18px; border: 1px solid #e4e7e5; border-radius: 18px; background: #fff; box-shadow: 0 8px 25px rgb(40 68 51 / 6%); }.table-panel strong,.table-panel small { display: block; }.table-panel small { margin-top: 3px; color: #8b958f; }.post-title { display: flex; align-items: center; gap: 8px; }.post-title strong { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.table-panel p { overflow: hidden; margin: 5px 0 0; color: #718078; font-size: 11px; text-overflow: ellipsis; white-space: nowrap; }.table-panel :deep(.el-pagination) { justify-content: center; margin-top: 18px; }
.comment-list { min-height: 120px; max-height: 520px; overflow-y: auto; }.comment-list article { position: relative; margin-bottom: 10px; padding: 13px 100px 13px 15px; border: 1px solid #e4ebe6; border-radius: 13px; }.comment-list article.hidden { background: #f5f5f5; opacity: .68; }.comment-list article > div { display: flex; align-items: center; gap: 7px; }.comment-list time { margin-left: auto; color: #99a19c; font-size: 10px; }.comment-list p { margin: 8px 0 0; color: #48554d; }.comment-list article > .el-button { position: absolute; right: 13px; bottom: 10px; }
@media (max-width: 760px) { .review-hero,.filter-panel { align-items: stretch; flex-direction: column; }.filter-panel .el-select,.filter-panel .el-input { width: 100%; } }
</style>
