<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'

import BrandLogo from '@/components/BrandLogo.vue'
import { apiKeyApi, authApi } from '@/lib/auth.js'
import { isRemote } from '@/lib/v2/client.js'
import '@/styles/v2.css'
import '@/styles/readability.css'

const router = useRouter()

const email = ref('')
const apiKeyMask = ref('')
const hasKey = ref(false)
const updatedAt = ref('')
const draft = ref('')
const loading = ref(true)
const busy = ref(false)
const error = ref('')
const notice = ref('')

const canSave = computed(() => draft.value.trim().length >= 12 && !busy.value)

const updatedLabel = computed(() => {
  if (!updatedAt.value) return ''
  return new Date(updatedAt.value).toLocaleString('zh-CN', { hour12: false })
})

onMounted(load)

async function load() {
  if (!isRemote) {
    loading.value = false
    return
  }
  loading.value = true
  error.value = ''
  try {
    const user = await authApi.me()
    email.value = user.email || ''
    apiKeyMask.value = user.apiKeyMask || ''
    hasKey.value = Boolean(user.hasApiKey)
    updatedAt.value = user.apiKeyUpdatedAt || ''
  } catch (e) {
    error.value = e?.message || '读取账号信息失败'
  } finally {
    loading.value = false
  }
}

function applyKeyView(view) {
  apiKeyMask.value = view?.apiKeyMask || ''
  hasKey.value = Boolean(view?.hasApiKey)
  updatedAt.value = view?.updatedAt || ''
}

async function saveKey() {
  if (!canSave.value) {
    error.value = 'Key 应以 sk- 开头，长度至少 12 位'
    return
  }
  busy.value = true
  error.value = ''
  notice.value = ''
  try {
    applyKeyView(await apiKeyApi.save(draft.value.trim()))
    draft.value = ''
    notice.value = '已连接，可以开始对话了'
  } catch (e) {
    error.value = e?.message || '保存失败，请稍后再试'
  } finally {
    busy.value = false
  }
}

async function removeKey() {
  if (!window.confirm('删除后需要重新粘贴 DeepSeek 密钥，确定删除吗？')) return
  busy.value = true
  error.value = ''
  notice.value = ''
  try {
    applyKeyView(await apiKeyApi.remove())
    notice.value = '已删除 API Key'
  } catch (e) {
    error.value = e?.message || '删除失败，请稍后再试'
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <div class="v2-auth">
    <header class="v2-auth-nav">
      <BrandLogo />
      <button class="btn btn-quiet" type="button" @click="router.push('/v2')">
        返回对话
      </button>
    </header>

    <main class="v2-auth-main">
      <section class="v2-auth-card v2-settings-card">
        <template v-if="isRemote">
          <p class="v2-kicker">连接 AI</p>
          <h1 class="v2-auth-title">连接 DeepSeek</h1>
          <p class="v2-auth-sub">
            从 DeepSeek 开放平台复制 API Key（调用密钥），粘贴到下方。费用由 DeepSeek 按用量收取。
          </p>

          <p v-if="loading" class="v2-settings-loading">正在读取…</p>

          <template v-else>
            <dl class="v2-settings-list">
              <div>
                <dt>登录邮箱</dt>
                <dd>{{ email || '—' }}</dd>
              </div>
              <div>
                <dt>已保存的密钥</dt>
                <dd>
                  <span v-if="hasKey" class="v2-key-mask">{{ apiKeyMask }}</span>
                  <span v-else class="v2-settings-empty">尚未保存</span>
                  <span v-if="updatedLabel" class="v2-settings-time">更新于 {{ updatedLabel }}</span>
                </dd>
              </div>
            </dl>

            <form class="v2-auth-form" @submit.prevent="saveKey">
              <label class="v2-field">
                <span>{{ hasKey ? '更换密钥' : '粘贴 DeepSeek 密钥' }}</span>
                <input
                  v-model="draft"
                  type="password"
                  autocomplete="off"
                  spellcheck="false"
                  placeholder="sk-..."
                  :disabled="busy"
                />
              </label>

              <p v-if="error" class="v2-auth-error">{{ error }}</p>
              <p v-if="notice" class="v2-auth-ok">{{ notice }}</p>

              <div class="v2-settings-actions">
                <button class="btn btn-primary" type="submit" :disabled="!canSave">
                  {{ busy ? '校验中…' : '检查并保存' }}
                </button>
                <button
                  v-if="hasKey"
                  class="btn btn-ghost"
                  type="button"
                  :disabled="busy"
                  @click="removeKey"
                >
                  删除密钥
                </button>
              </div>
            </form>

            <p class="v2-auth-note">
              密钥会加密保存。你的想法和回答会发送给 DeepSeek，用于生成选项和需求文档。
            </p>
          </template>
        </template>

        <template v-else>
          <p class="v2-kicker">本地原型模式</p>
          <h1 class="v2-auth-title">Key 设置需要后端</h1>
          <p class="v2-auth-sub">
            设置 <code>VITE_COACH_API=1</code> 并启动 ThinkFirst 服务后，才能保存和校验 Key。
          </p>
          <RouterLink class="btn btn-primary btn-lg v2-auth-submit" to="/v2">
            返回对话
          </RouterLink>
        </template>
      </section>
    </main>
  </div>
</template>
