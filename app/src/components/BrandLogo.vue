<script setup>
import { RouterLink } from 'vue-router'
</script>

<template>
  <RouterLink class="logo" to="/" style="color: inherit">
    <span class="logo-mark" aria-hidden="true">
      <svg
        viewBox="0 0 24 24"
        width="16"
        height="16"
        fill="none"
        stroke="#fff"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        <circle cx="12" cy="12" r="3.2" />
        <path
          d="M12 2.6v3.4M12 18v3.4M2.6 12h3.4M18 12h3.4M5.4 5.4l2.4 2.4M16.2 16.2l2.4 2.4M18.6 5.4l-2.4 2.4M7.8 16.2l-2.4 2.4"
        />
      </svg>
    </span>
    ThinkFirst
  </RouterLink>
</template>
