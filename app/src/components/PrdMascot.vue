<script setup>
defineEmits(['chat'])
</script>
<template>
  <button class="mascot-brand" type="button" aria-label="点我修改" @click="$emit('chat')">
    <svg viewBox="0 0 100 56" width="80" height="48" aria-hidden="true">
      <path d="M12 43Q8 30 22 27Q23 5 45 9Q65 5 72 27Q92 27 89 43Z" fill="#8060d6"/>
      <ellipse cx="31" cy="43" rx="18" ry="8" fill="#a58be9"/><ellipse cx="70" cy="43" rx="18" ry="8" fill="#a58be9"/>
      <g class="mascot-eyes"><ellipse cx="38" cy="27" rx="4" ry="6" fill="white"/><ellipse cx="57" cy="27" rx="4" ry="6" fill="white"/><circle cx="39" cy="28" r="2" fill="#362459"/><circle cx="58" cy="28" r="2" fill="#362459"/></g>
      <path d="M44 36Q49 40 54 36" fill="none" stroke="#402664" stroke-width="2" stroke-linecap="round"/>
    </svg><span class="mascot-tip">点我修改</span>
  </button>
</template>
<style scoped>
.mascot-tip{position:absolute;bottom:100%;white-space:nowrap;padding:6px 10px;background:#49346b;color:white;border-radius:8px;font-size:14px;opacity:0;pointer-events:none}.mascot-brand:hover .mascot-tip,.mascot-brand:focus-visible .mascot-tip{opacity:1}.mascot-brand{position:relative;display:flex;align-items:center;gap:6px;background:none;border:0;cursor:pointer;font-family:inherit;font-size:20px;font-weight:700;color:#32274c;padding:0}.mascot-eyes{transform-origin:48px 27px;animation:blink 5.8s infinite}@keyframes blink{0%,44%,48%,94%,98%,100%{transform:scaleY(1)}46%,96%{transform:scaleY(.08)}}@media(prefers-reduced-motion:reduce){.mascot-eyes{animation:none}}
</style>
