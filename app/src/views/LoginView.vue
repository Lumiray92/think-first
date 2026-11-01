<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import BrandLogo from '@/components/BrandLogo.vue'
import { authApi } from '@/lib/auth.js'
import { isRemote } from '@/lib/v2/client.js'
import '@/styles/v2.css'
import '@/styles/readability.css'

const route = useRoute()
const router = useRouter()

const mode = ref('login')
const email = ref('')
const password = ref('')
const showPassword = ref(false)
const showConfirmPassword = ref(false)
const confirmPassword = ref('')
const busy = ref(false)
const error = ref('')

const isRegister = computed(() => mode.value === 'register')
const canSubmit = computed(
  () =>
    Boolean(email.value.trim()) &&
    password.value.length >= 8 &&
    (!isRegister.value || password.value === confirmPassword.value) &&
    !busy.value,
)

function switchMode(next) {
  mode.value = next
  error.value = ''
}

async function submit() {
  if (busy.value) return
  if (isRegister.value && password.value !== confirmPassword.value) {
    error.value = '两次输入的密码不一致'
    return
  }
  if (!canSubmit.value) {
    error.value = isRegister.value ? '请填写邮箱，密码至少 8 位' : '请填写邮箱和密码'
    return
  }

  busy.value = true
  error.value = ''
  try {
    if (isRegister.value) {
      await authApi.register(email.value.trim(), password.value)
    } else {
      await authApi.login(email.value.trim(), password.value)
    }
    const requested = typeof route.query.redirect === 'string' ? route.query.redirect : ''
    const redirect = requested.startsWith('/') && !requested.startsWith('//') ? requested : '/v2'
    password.value = ''
    confirmPassword.value = ''
    showPassword.value = false
    showConfirmPassword.value = false
    router.replace(redirect)
  } catch (e) {
    error.value = e?.message || '请求失败，请稍后再试'
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <div class="v2-auth">
    <header class="v2-auth-nav">
      <BrandLogo />
    </header>

    <main class="v2-auth-main">
      <section class="v2-auth-card">
        <template v-if="isRemote">
          <p class="v2-kicker">ThinkFirst 2.0</p>
          <h1 class="v2-auth-title">{{ isRegister ? '创建账号' : '登录 ThinkFirst' }}</h1>
          <p class="v2-auth-sub">
            登录后，连接 DeepSeek 即可开始。
          </p>

          <div class="v2-auth-tabs">
            <button
              type="button"
              :class="{ active: mode === 'login' }"
              @click="switchMode('login')"
            >
              登录
            </button>
            <button
              type="button"
              :class="{ active: mode === 'register' }"
              @click="switchMode('register')"
            >
              注册
            </button>
          </div>

          <form class="v2-auth-form" @submit.prevent="submit">
            <label class="v2-field">
              <span>邮箱</span>
              <input
                v-model="email"
                type="email"
                autocomplete="email"
                placeholder="you@example.com"
                :disabled="busy"
              />
            </label>

            <label class="v2-field">
              <span>密码</span>
              <div class="password-control">
              <input
                v-model="password"
                :type="showPassword ? 'text' : 'password'"
                :autocomplete="isRegister ? 'new-password' : 'current-password'"
                placeholder="至少 8 位"
                :disabled="busy"
              /><button type="button" class="password-eye" :aria-label="showPassword ? '隐藏密码' : '显示密码'" :aria-pressed="showPassword" @click="showPassword = !showPassword"><svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M2 12s4-7 10-7 10 7 10 7-4 7-10 7S2 12 2 12Z"/><circle cx="12" cy="12" r="3"/><path v-if="showPassword" d="m3 3 18 18"/></svg></button></div>
            </label>

            <label v-if="isRegister" class="v2-field">
              <span>确认密码</span>
              <div class="password-control">
              <input
                v-model="confirmPassword"
                :type="showConfirmPassword ? 'text' : 'password'"
                autocomplete="new-password"
                placeholder="再输入一次"
                :disabled="busy"
              /><button type="button" class="password-eye" :aria-label="showConfirmPassword ? '隐藏密码' : '显示密码'" :aria-pressed="showConfirmPassword" @click="showConfirmPassword = !showConfirmPassword"><svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M2 12s4-7 10-7 10 7 10 7-4 7-10 7S2 12 2 12Z"/><circle cx="12" cy="12" r="3"/><path v-if="showConfirmPassword" d="m3 3 18 18"/></svg></button></div>
            </label>

            <p v-if="error" class="v2-auth-error">{{ error }}</p>

            <button class="btn btn-primary btn-lg v2-auth-submit" type="submit" :disabled="!canSubmit">
              {{ busy ? '请稍候…' : isRegister ? '注册并开始' : '登录' }}
            </button>
          </form>

        </template>

        <template v-else>
          <p class="v2-kicker">本地原型模式</p>
          <h1 class="v2-auth-title">当前不需要登录</h1>
          <p class="v2-auth-sub">
            前端正在使用本地模拟智能体。接入后端时设置
            <code>VITE_COACH_API=1</code>，登录和 Key 设置页就会启用。
          </p>
          <RouterLink class="btn btn-primary btn-lg v2-auth-submit" to="/v2">
            直接开始
          </RouterLink>
        </template>
      </section>
    </main>
  </div>
</template>

<style scoped>.password-control{position:relative}.password-control input{padding-right:52px}.password-eye{position:absolute;right:4px;top:4px;width:42px;height:42px;border:0;background:transparent;color:#5e477e;cursor:pointer;display:grid;place-items:center}</style>
