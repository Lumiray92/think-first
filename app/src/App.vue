<script setup>
import { watch } from 'vue'
import { RouterView, useRoute } from 'vue-router'

import Toast from '@/components/Toast.vue'

const route = useRoute()

watch(
  () => route.name,
  (name) => {
    document.body.classList.toggle('landing', name === 'home')
    document.body.classList.toggle('scrollable', name === 'guide' || name === 'done')
  },
  { immediate: true },
)
</script>

<template>
  <RouterView v-slot="{ Component }">
    <Transition name="fade" mode="out-in">
      <component :is="Component" />
    </Transition>
  </RouterView>
  <Toast />
</template>
