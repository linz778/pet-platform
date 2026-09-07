<template>
  <div class="community-page">
    <section class="community-hero">
      <div class="hero-copy">
        <span class="eyebrow">🐾 LECHONG COMMUNITY</span>
        <h1>和懂宠物的人，一起分享与解答</h1>
        <p>晒晒毛孩子的可爱瞬间，提出养宠困惑；认证接单员也会带着服务经验来认真回答。</p>
        <div class="hero-actions">
          <el-button v-if="isOwner" type="primary" size="large" round @click="openComposer(1)">📸 晒晒我家宝贝</el-button>
          <el-button v-if="isOwner" size="large" round @click="openComposer(2)">💬 提一个养宠问题</el-button>
          <el-button v-else type="primary" size="large" round @click="switchType('2')">🩺 去帮助宠物主人</el-button>
        </div>
      </div>
      <div class="hero-visual" aria-hidden="true">
        <span class="pet-bubble cat">🐱</span>
        <span class="pet-bubble dog">🐶</span>
        <span class="pet-bubble rabbit">🐰</span>
        <div class="hero-card">
          <strong>{{ isOwner ? '今天也要记录快乐' : '你的经验很有价值' }}</strong>
          <span>{{ isOwner ? '一张照片，就是一份成长日记' : '一条认真回答，也许能解决主人的焦虑' }}</span>
        </div>
      </div>
    </section>

    <div class="community-layout">
      <aside class="community-side">
        <section class="side-card guide-card">
          <span class="side-icon">🌿</span>
          <h3>社区小约定</h3>
          <p>友善交流、科学养宠，不用未经核实的建议替代专业诊疗。</p>
        </section>
        <section class="side-card fun-card">
          <span class="side-icon">🧩</span>
          <h3>今日互动灵感</h3>
          <ul>
            <li>晒一张最离谱的睡姿</li>
            <li>分享毛孩子的新技能</li>
            <li>问问换粮或行为训练</li>
          </ul>
        </section>
      </aside>

      <main class="feed-panel">
        <div class="feed-head">
          <el-tabs v-model="activeType" @tab-change="onTypeChange">
            <el-tab-pane label="全部动态" name="all" />
            <el-tab-pane label="🐾 晒宠日常" name="1" />
            <el-tab-pane label="💡 养宠问答" name="2" />
          </el-tabs>
          <el-button v-if="isOwner" type="primary" @click="openComposer(activeType === '2' ? 2 : 1)">发布动态</el-button>
        </div>

        <div v-loading="loading" class="post-list">
          <el-empty v-if="!loading && posts.length === 0" description="这里还很安静，来发布第一条动态吧" />
          <article v-for="post in posts" :key="post.id" class="post-card">
            <header class="post-author">
              <el-avatar :size="42" :src="post.authorAvatar">{{ post.authorName?.slice(0, 1) }}</el-avatar>
              <div class="author-info">
                <strong>{{ post.authorName || '社区用户' }}</strong>
                <span>{{ post.createTime }}</span>
              </div>
              <el-tag :type="post.type === 2 ? 'warning' : 'success'" effect="light" round>{{ post.typeText }}</el-tag>
            </header>

            <div class="post-body" @click="openPost(post)">
              <div v-if="post.petName" class="pet-chip">
                <el-avatar :size="24" :src="post.petAvatar">🐾</el-avatar>
                今日主角 · {{ post.petName }}
              </div>
              <h2>{{ post.title }}</h2>
              <p>{{ post.content }}</p>
              <div v-if="post.imageUrls?.length" class="post-images" :class="`count-${Math.min(post.imageUrls.length, 3)}`">
                <el-image
                  v-for="url in post.imageUrls"
                  :key="url"
                  :src="url"
                  :preview-src-list="post.imageUrls"
                  fit="cover"
                  @click.stop
                />
              </div>
            </div>

            <footer class="post-actions">
              <button type="button" class="paw-button" :class="{ liked: post.liked }" @click="toggleLike(post)">
                {{ post.liked ? '🐾 已送爪印' : '🐾 送爪印' }} <span>{{ post.likeCount || 0 }}</span>
              </button>
              <button type="button" @click="openPost(post)">💬 {{ post.type === 2 ? '回答' : '评论' }} {{ post.commentCount || 0 }}</button>
            </footer>
          </article>
        </div>

        <el-pagination
          v-if="total > 0"
          class="pager"
          layout="total, prev, pager, next"
          :total="total"
          :current-page="query.page"
          :page-size="query.size"
          @current-change="onPageChange"
        />
      </main>
    </div>

    <el-dialog v-model="composerVisible" :title="postForm.type === 1 ? '分享我家小宠物' : '提出养宠问题'" width="620px" destroy-on-close>
      <el-form ref="postFormRef" :model="postForm" :rules="postRules" label-position="top">
        <el-form-item label="发布类型" prop="type">
          <el-radio-group v-model="postForm.type">
            <el-radio-button :value="1">📸 晒宠分享</el-radio-button>
            <el-radio-button :value="2">💡 养宠问答</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="关联宠物（可选）">
          <el-select v-model="postForm.petId" clearable placeholder="选择这条动态的主角" class="full">
            <el-option v-for="pet in pets" :key="pet.id" :label="`${pet.name} · ${pet.species || '小宠物'}`" :value="pet.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题" prop="title">
          <el-input v-model="postForm.title" maxlength="100" show-word-limit :placeholder="postForm.type === 1 ? '今天发生了什么有趣的事？' : '用一句话概括你的问题'" />
        </el-form-item>
        <el-form-item label="正文" prop="content">
          <el-input
            v-model="postForm.content"
            type="textarea"
            :rows="6"
            maxlength="2000"
            show-word-limit
            :placeholder="postForm.type === 1 ? '记录它的可爱瞬间、成长变化或生活趣事…' : '描述宠物年龄、症状、持续时间和你已经尝试过的方法，信息越完整越容易得到帮助…'"
          />
        </el-form-item>
        <el-form-item label="图片（最多 5 张）">
          <ImageUpload v-model="postForm.imageUrls" biz-type="common" :limit="5" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="composerVisible = false">取消</el-button>
        <el-button type="primary" :loading="publishing" @click="publish">发布到社区</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="postDrawerVisible" title="社区动态" size="560px">
      <div v-if="currentPost" v-loading="loadingPost" class="post-detail">
        <div class="detail-type">
          <el-tag :type="currentPost.type === 2 ? 'warning' : 'success'">{{ currentPost.typeText }}</el-tag>
          <span>{{ currentPost.createTime }}</span>
        </div>
        <h2>{{ currentPost.title }}</h2>
        <div class="detail-author">
          <el-avatar :src="currentPost.authorAvatar">{{ currentPost.authorName?.slice(0, 1) }}</el-avatar>
          <div><strong>{{ currentPost.authorName }}</strong><span v-if="currentPost.petName">和 {{ currentPost.petName }}</span></div>
        </div>
        <p class="detail-content">{{ currentPost.content }}</p>
        <div v-if="currentPost.imageUrls?.length" class="detail-images">
          <el-image v-for="url in currentPost.imageUrls" :key="url" :src="url" :preview-src-list="currentPost.imageUrls" fit="cover" />
        </div>
        <button type="button" class="paw-button detail-paw" :class="{ liked: currentPost.liked }" @click="toggleLike(currentPost)">
          {{ currentPost.liked ? '🐾 已送爪印' : '🐾 送个爪印' }} · {{ currentPost.likeCount || 0 }}
        </button>

        <div class="comment-title">
          <h3>{{ currentPost.type === 2 ? '回答与讨论' : '大家的评论' }}</h3>
          <span>{{ comments.length }} 条</span>
        </div>
        <el-empty v-if="comments.length === 0" :image-size="60" description="还没有人留言" />
        <div v-else class="comment-list">
          <article v-for="comment in comments" :key="comment.id" class="comment-item">
            <el-avatar :size="36" :src="comment.authorAvatar">{{ comment.authorName?.slice(0, 1) }}</el-avatar>
            <div>
              <div class="comment-author">
                <strong>{{ comment.authorName }}</strong>
                <el-tag v-if="comment.authorRole === 'SITTER'" size="small" type="success" effect="plain">接单员答复</el-tag>
                <span>{{ comment.createTime }}</span>
              </div>
              <p>{{ comment.content }}</p>
            </div>
          </article>
        </div>

        <div class="reply-box">
          <div v-if="isSitter && currentPost.type === 2" class="sitter-answer-tip">🎒 你的回复将带有“接单员答复”标识</div>
          <el-input
            v-model="replyContent"
            type="textarea"
            :rows="3"
            maxlength="1000"
            show-word-limit
            :placeholder="currentPost.type === 2 ? '分享你的经验和建议；涉及疾病请提醒主人及时就医' : '友善地留下一句评论吧'"
          />
          <el-button type="primary" :loading="replying" :disabled="!replyContent.trim()" @click="reply">
            {{ currentPost.type === 2 ? '提交回答' : '发表评论' }}
          </el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  createCommunityComment,
  createCommunityPost,
  getCommunityPost,
  listCommunityComments,
  pageCommunityPosts,
  toggleCommunityLike
} from '@/api/community'
import { listMyPets } from '@/api/pet'
import ImageUpload from '@/components/ImageUpload.vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const isOwner = computed(() => userStore.role === 'USER')
const isSitter = computed(() => userStore.role === 'SITTER')

const activeType = ref(isSitter.value ? '2' : 'all')
const posts = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10 })

const pets = ref([])
const composerVisible = ref(false)
const postFormRef = ref(null)
const publishing = ref(false)
const postForm = reactive({ type: 1, petId: null, title: '', content: '', imageUrls: [] })
const postRules = {
  type: [{ required: true, message: '请选择发布类型', trigger: 'change' }],
  title: [{ required: true, message: '请填写标题', trigger: 'blur' }],
  content: [{ required: true, message: '请填写正文', trigger: 'blur' }]
}

const postDrawerVisible = ref(false)
const loadingPost = ref(false)
const currentPost = ref(null)
const comments = ref([])
const replyContent = ref('')
const replying = ref(false)

async function load() {
  loading.value = true
  try {
    const res = await pageCommunityPosts({
      page: query.page,
      size: query.size,
      type: activeType.value === 'all' ? undefined : Number(activeType.value)
    })
    posts.value = res.records ?? []
    total.value = res.total ?? 0
  } finally {
    loading.value = false
  }
}

function onTypeChange() {
  query.page = 1
  load()
}

function switchType(type) {
  activeType.value = type
  onTypeChange()
  document.querySelector('.feed-panel')?.scrollIntoView({ behavior: 'smooth' })
}

function onPageChange(page) {
  query.page = page
  load()
}

async function openComposer(type) {
  Object.assign(postForm, { type, petId: null, title: '', content: '', imageUrls: [] })
  if (pets.value.length === 0) pets.value = await listMyPets().catch(() => [])
  composerVisible.value = true
}

async function publish() {
  try {
    await postFormRef.value?.validate()
  } catch {
    return
  }
  publishing.value = true
  try {
    await createCommunityPost({
      type: postForm.type,
      petId: postForm.petId,
      title: postForm.title.trim(),
      content: postForm.content.trim(),
      imageUrls: postForm.imageUrls
    })
    ElMessage.success(postForm.type === 1 ? '分享成功，快邀请大家来送爪印吧' : '问题发布成功，等待热心伙伴回答')
    composerVisible.value = false
    activeType.value = String(postForm.type)
    query.page = 1
    await load()
  } finally {
    publishing.value = false
  }
}

async function toggleLike(post) {
  const result = await toggleCommunityLike(post.id)
  post.liked = result.liked
  post.likeCount = result.likeCount
  const listPost = posts.value.find((item) => item.id === post.id)
  if (listPost && listPost !== post) {
    listPost.liked = result.liked
    listPost.likeCount = result.likeCount
  }
}

async function openPost(post) {
  postDrawerVisible.value = true
  loadingPost.value = true
  replyContent.value = ''
  try {
    const [detail, list] = await Promise.all([
      getCommunityPost(post.id),
      listCommunityComments(post.id)
    ])
    currentPost.value = detail
    comments.value = list ?? []
  } finally {
    loadingPost.value = false
  }
}

async function reply() {
  const content = replyContent.value.trim()
  if (!content) return
  replying.value = true
  try {
    await createCommunityComment(currentPost.value.id, content)
    ElMessage.success(currentPost.value.type === 2 ? '回答已发布' : '评论已发布')
    replyContent.value = ''
    comments.value = await listCommunityComments(currentPost.value.id)
    currentPost.value.commentCount = comments.value.length
    const listPost = posts.value.find((item) => item.id === currentPost.value.id)
    if (listPost) listPost.commentCount = comments.value.length
  } finally {
    replying.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.community-page {
  max-width: 1180px;
  margin: 0 auto;
  padding: 24px 18px 48px;
}

.community-hero {
  position: relative;
  overflow: hidden;
  display: grid;
  grid-template-columns: 1.35fr 0.65fr;
  min-height: 270px;
  padding: 42px 48px;
  border-radius: 28px;
  background: linear-gradient(135deg, #e8f5eb 0%, #fdf7e9 55%, #f7e9dd 100%);
  box-shadow: 0 18px 45px rgba(67, 102, 76, 0.1);
}

.hero-copy {
  position: relative;
  z-index: 2;
}

.eyebrow {
  color: var(--pp-primary);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 1.2px;
}

.hero-copy h1 {
  max-width: 620px;
  margin: 12px 0;
  font-size: clamp(28px, 4vw, 44px);
  line-height: 1.18;
}

.hero-copy p {
  max-width: 630px;
  margin: 0;
  color: var(--pp-muted);
  line-height: 1.8;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 24px;
}

.hero-visual {
  position: relative;
  min-height: 180px;
}

.pet-bubble {
  position: absolute;
  display: grid;
  width: 72px;
  height: 72px;
  place-items: center;
  border: 5px solid rgba(255, 255, 255, 0.8);
  border-radius: 50%;
  background: #fff;
  font-size: 36px;
  box-shadow: 0 12px 25px rgba(60, 80, 67, 0.12);
}

.pet-bubble.cat { top: 0; right: 46px; transform: rotate(7deg); }
.pet-bubble.dog { top: 82px; right: 132px; transform: rotate(-7deg); }
.pet-bubble.rabbit { top: 98px; right: 10px; transform: rotate(8deg); }

.hero-card {
  position: absolute;
  right: 20px;
  bottom: -18px;
  display: flex;
  flex-direction: column;
  width: 260px;
  padding: 16px;
  border: 1px solid rgba(255, 255, 255, 0.8);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.78);
  backdrop-filter: blur(12px);
}

.hero-card span { margin-top: 5px; color: var(--pp-muted); font-size: 12px; line-height: 1.5; }

.community-layout {
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr);
  gap: 20px;
  margin-top: 24px;
}

.community-side {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.side-card,
.feed-panel {
  border: 1px solid var(--pp-tint-2);
  border-radius: 20px;
  background: #fff;
  box-shadow: var(--pp-shadow);
}

.side-card { padding: 20px; }
.side-icon { font-size: 28px; }
.side-card h3 { margin: 10px 0 8px; font-size: 15px; }
.side-card p,
.side-card li { color: var(--pp-muted); font-size: 13px; line-height: 1.7; }
.side-card p { margin: 0; }
.side-card ul { margin: 0; padding-left: 18px; }
.fun-card { background: linear-gradient(160deg, #fff 0%, #fff9ec 100%); }

.feed-panel { padding: 18px; }

.feed-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.feed-head :deep(.el-tabs) { flex: 1; }
.feed-head :deep(.el-tabs__header) { margin-bottom: 8px; }

.post-list {
  min-height: 300px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.post-card {
  overflow: hidden;
  border: 1px solid var(--pp-tint-2);
  border-radius: 18px;
  background: #fff;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.post-card:hover { transform: translateY(-2px); box-shadow: var(--pp-shadow-hover); }

.post-author {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 18px 0;
}

.author-info { display: flex; flex: 1; flex-direction: column; }
.author-info span { margin-top: 3px; color: var(--pp-muted); font-size: 12px; }

.post-body { padding: 12px 18px 16px; cursor: pointer; }
.post-body h2 { margin: 10px 0 8px; font-size: 18px; }
.post-body > p { margin: 0; color: #4d5d53; line-height: 1.75; white-space: pre-wrap; }

.pet-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px 4px 4px;
  border-radius: 999px;
  background: var(--pp-tint);
  color: var(--pp-primary);
  font-size: 12px;
}

.post-images {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 7px;
  margin-top: 14px;
}

.post-images.count-1 { grid-template-columns: minmax(180px, 380px); }
.post-images.count-2 { grid-template-columns: repeat(2, minmax(0, 1fr)); }
.post-images :deep(.el-image) { width: 100%; height: 180px; border-radius: 12px; }

.post-actions {
  display: flex;
  gap: 8px;
  padding: 10px 18px;
  border-top: 1px solid #eef3ef;
}

.post-actions button,
.paw-button {
  border: none;
  border-radius: 999px;
  background: transparent;
  color: var(--pp-muted);
  cursor: pointer;
  padding: 7px 12px;
}

.post-actions button:hover,
.paw-button:hover { background: var(--pp-tint); color: var(--pp-primary); }
.paw-button.liked { background: #fff1e5; color: #d06b28; }

.pager { justify-content: center; margin-top: 18px; }
.full { width: 100%; }

.post-detail { padding: 0 4px 24px; }
.detail-type { display: flex; align-items: center; justify-content: space-between; color: var(--pp-muted); font-size: 12px; }
.post-detail h2 { margin: 14px 0; font-size: 24px; }
.detail-author { display: flex; align-items: center; gap: 10px; }
.detail-author > div { display: flex; flex-direction: column; }
.detail-author span { margin-top: 3px; color: var(--pp-muted); font-size: 12px; }
.detail-content { line-height: 1.8; white-space: pre-wrap; }
.detail-images { display: grid; grid-template-columns: repeat(2, 1fr); gap: 8px; }
.detail-images :deep(.el-image) { width: 100%; height: 190px; border-radius: 12px; }
.detail-paw { margin-top: 14px; }

.comment-title { display: flex; align-items: center; justify-content: space-between; margin-top: 22px; border-top: 1px solid var(--pp-tint-2); }
.comment-title h3 { margin: 18px 0 12px; }
.comment-title span { color: var(--pp-muted); font-size: 12px; }
.comment-list { display: flex; flex-direction: column; gap: 14px; }
.comment-item { display: grid; grid-template-columns: 36px 1fr; gap: 10px; }
.comment-author { display: flex; align-items: center; gap: 7px; }
.comment-author > span { margin-left: auto; color: var(--pp-muted); font-size: 11px; }
.comment-item p { margin: 6px 0 0; line-height: 1.65; }
.reply-box { display: flex; flex-direction: column; align-items: flex-end; gap: 10px; margin-top: 20px; padding-top: 16px; border-top: 1px solid var(--pp-tint-2); }
.sitter-answer-tip { width: 100%; color: var(--pp-primary); font-size: 12px; }

@media (max-width: 800px) {
  .community-hero { grid-template-columns: 1fr; padding: 30px 24px; }
  .hero-visual { display: none; }
  .community-layout { grid-template-columns: 1fr; }
  .community-side { display: none; }
  .post-images { grid-template-columns: repeat(2, 1fr); }
}
</style>
