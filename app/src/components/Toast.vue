<script setup>
import { useToast } from '@/composables/useToast'

const { message } = useToast()
</script>

<template>
  <div class="toast" :class="{ 'is-on': message }">{{ message }}</div>
</template>
