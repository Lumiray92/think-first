<script setup>
import { ref } from 'vue'

const props = defineProps({
  disabled: { type: Boolean, default: false },
  placeholder: { type: String, default: '说一句就行，剩下的交给智能体…' },
})

const emit = defineEmits(['submit'])

const text = ref('')
const attachment = ref('')
const fileRef = ref(null)
const areaRef = ref(null)

function submit() {
  const value = text.value.trim()
  if (!value || props.disabled) return
  emit('submit', value)
  // 保留草稿，失败时可以直接重新提交；下一张卡片由父级重新挂载。 
}

function onKeydown(event) {
  // 中文输入法用 Enter 选词时不能直接发送
  if (event.key !== 'Enter' || event.shiftKey || event.isComposing) return
  event.preventDefault()
  submit()
}

function onFile(event) {
  const file = event.target.files?.[0]
  if (!file) return
  attachment.value = file.name
  event.target.value = ''
}

function focus() {
  areaRef.value?.focus()
}

defineExpose({ focus })
</script>

<template>
  <div class="v2-input">
    <div v-if="attachment" class="v2-attachment">
      <span>🖼 {{ attachment }}</span>
      <em>原型阶段不解析，接入后端后自动提取需求</em>
      <button type="button" @click="attachment = ''">×</button>
    </div>

    <div class="v2-input-row">
      <textarea
        ref="areaRef"
        v-model="text"
        rows="2"
        :placeholder="placeholder"
        :disabled="disabled"
        @keydown="onKeydown"
      />
      <div class="v2-input-actions">
        <button type="button" class="btn btn-primary" :disabled="disabled || !text.trim()" @click="submit">
          发送
        </button>
      </div>
    </div>
    
  </div>
</template>
