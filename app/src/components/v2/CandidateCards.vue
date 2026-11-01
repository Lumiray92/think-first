<script setup>
defineProps({
  candidates: { type: Array, default: () => [] },
  disabled: { type: Boolean, default: false },
})

const emit = defineEmits(['select', 'regenerate', 'free'])
</script>

<template>
  <div class="v2-candidates">
    <button
      v-for="(item, index) in candidates"
      :key="item.id || index"
      class="v2-candidate"
      :disabled="disabled"
      @click="emit('select', index)"
    >
      <span class="v2-candidate-no">{{ String.fromCharCode(65 + index) }}</span>
      <span class="v2-candidate-body">
        <span class="v2-candidate-text">{{ item.text }}</span>
        
      </span>
    </button>

    <div class="v2-candidate-actions">
      <button class="btn btn-quiet" :disabled="disabled" @click="emit('regenerate')">换一批</button>
      <button class="btn btn-quiet" :disabled="disabled" @click="emit('free')">自己写</button>
      
    </div>
  </div>
</template>
