<template>
  <div class="auth-page">
    <el-card class="auth-card">
      <h2 class="title">🐾 注册</h2>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent>
        <el-form-item label="身份" prop="role">
          <el-radio-group v-model="form.role">
            <el-radio-button value="USER">我是宠物主人</el-radio-button>
            <el-radio-button value="SITTER">我是接单员</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="3-20 位用户名" clearable />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" clearable />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="选填" clearable />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="6-32 位密码" show-password />
        </el-form-item>
        <el-button type="primary" class="submit" :loading="loading" @click="onSubmit">注册并登录</el-button>
      </el-form>
      <div class="footer">
        已有账号？<router-link to="/login">去登录</router-link>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { HOME_BY_ROLE } from '@/router'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref()
const loading = ref(false)
const form = reactive({ role: 'USER', username: '', phone: '', nickname: '', password: '' })
const rules = {
  role: [{ required: true, message: '请选择身份', trigger: 'change' }],
  username: [{ required: true, min: 3, max: 20, message: '用户名 3-20 位', trigger: 'blur' }],
  phone: [{ required: true, pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  password: [{ required: true, min: 6, max: 32, message: '密码 6-32 位', trigger: 'blur' }]
}

async function onSubmit() {
  await formRef.value.validate()
  loading.value = true
  try {
    const data = await userStore.register({ ...form })
    ElMessage.success('注册成功')
    router.push(HOME_BY_ROLE[data.role] || '/portal')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px 0;
}
.auth-card {
  width: 400px;
}
.title {
  text-align: center;
  margin: 0 0 16px;
}
.submit {
  width: 100%;
}
.footer {
  margin-top: 12px;
  text-align: center;
  font-size: 14px;
  color: #909399;
}
</style>
