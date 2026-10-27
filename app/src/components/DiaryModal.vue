<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'

const DIARY_KEY = 'thinkfirst.diary'

const props = defineProps({
  open: { type: Boolean, default: false },
})
const emit = defineEmits(['close', 'saved'])

const text = ref('')

watch(
  () => props.open,
  (open) => {
    if (open) text.value = localStorage.getItem(DIARY_KEY) || ''
  },
)

function close() {
  emit('close')
}

function save() {
  localStorage.setItem(DIARY_KEY, text.value)
  emit('saved')
  emit('close')
}

function onKeydown(e) {
  if (e.key === 'Escape' && props.open) close()
}

onMounted(() => window.addEventListener('keydown', onKeydown))
onBeforeUnmount(() => window.removeEventListener('keydown', onKeydown))
</script>

<template>
  <div v-if="open" class="modal-mask" @click.self="close">
    <div class="modal" style="max-width: 500px">
      <div class="modal-head">
        <div>
          <h3>烦恼日记</h3>
          <p>记满 7 天，看看哪条出现最多、情绪最强。</p>
        </div>
        <button class="modal-close" @click="close">×</button>
      </div>
      <div class="modal-body">
        <textarea
          v-model="text"
          class="q-input"
          style="min-height: 120px"
          placeholder="例：10/14 便签丢了重写；10/15 微信刷屏漏了领导安排..."
        ></textarea>
      </div>
      <div class="modal-foot">
        <span class="tip"></span>
        <button class="btn btn-primary" @click="save">保存日记</button>
      </div>
    </div>
  </div>
</template>
