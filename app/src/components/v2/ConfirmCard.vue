<script setup>
import { ref, watch } from 'vue'
const props = defineProps({
  turn: { type: Object, required: true },
  disabled: { type: Boolean, default: false },
})

const emit = defineEmits(['confirm', 'retry', 'edit'])
const rows = ref([])
watch(() => props.turn, turn => { rows.value = (turn.summary || []).map(row => ({ ...row })) }, { immediate: true })
function save() {
  const changed = rows.value.some((row, index) => row.value !== props.turn.summary[index]?.value)
  if (changed) emit('edit', rows.value.map(row => `${row.label}：${row.value}`).join('\n'))
  else emit('confirm')
}
</script>

<template>
  <div class="v2-confirm">
    <div class="v2-confirm-head">
      <span class="v2-kicker">确认卡</span>
    </div>
    <p class="v2-confirm-message">{{ turn.message }}</p>

    <dl class="v2-summary">
      <div v-for="row in rows" :key="row.label" class="v2-summary-row">
        <dt>{{ row.label }}</dt>
        <dd><textarea v-model="row.value" :aria-label="row.label" :disabled="disabled" rows="3" class="confirm-editor" /></dd>
      </div>
    </dl>

    <div class="v2-actions">
      <button class="btn btn-primary" :disabled="disabled" @click="save">
        保存并继续
      </button>
      <button class="btn btn-ghost" :disabled="disabled" @click="emit('retry')">
        修改回答
      </button>
    </div>
  </div>
</template>

<style scoped>
.confirm-editor{width:100%;box-sizing:border-box;resize:vertical;font:inherit;line-height:1.7;color:inherit;background:#fafcfb;border:1px solid #e2e8e4;border-radius:8px;padding:10px}.confirm-editor:focus{outline:2px solid #a5b7ad}
</style>
