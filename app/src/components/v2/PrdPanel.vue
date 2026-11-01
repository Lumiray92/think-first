<script setup>
import { computed } from 'vue'

import { prdHtml } from '@/lib/v2/prd.js'

const props = defineProps({
  markdown: { type: String, default: '' },
  title: { type: String, default: '需求文档' },
  busy: { type: Boolean, default: false },
})

const emit = defineEmits(['copy', 'download', 'close'])

const html = computed(() => prdHtml(props.markdown))
const chars = computed(() => String(props.markdown || '').length)
</script>

<template>
  <aside class="v2-prd">
    <div class="v2-prd-head">
      <div class="v2-prd-title">
        <strong>{{ title }}</strong>
      </div>
      <div class="v2-prd-actions">
        <span class="v2-prd-chars">{{ chars }} 字</span>
        <button class="btn btn-quiet" :disabled="busy" @click="emit('copy')">复制</button>
        <button class="btn btn-quiet" :disabled="busy" @click="emit('download')">导出</button>
        <button class="v2-icon-btn v2-prd-close" title="收起" @click="emit('close')">×</button>
      </div>
    </div>

    <div v-if="markdown" class="v2-prd-body" v-html="html"></div>
    <div v-else class="v2-prd-empty">
      <p>你的回答会整理成需求文档。</p>
      <p>继续回答问题即可。</p>
    </div>
  </aside>
</template>
