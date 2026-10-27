<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

import { docMarkdown, mdToHtml } from '@/lib/doc'
import { useSession } from '@/composables/useSession'

const { state } = useSession()

const bubbleRef = ref(null)
const open = ref(false)
const dragging = ref(false)
const docHtml = computed(() => mdToHtml(docMarkdown(state)))

let curX = 0
let curY = 0
let startX = 0
let startY = 0
let initialX = 0
let initialY = 0
let hasMoved = false
let rafId = null

function applyPosition() {
  const el = bubbleRef.value
  if (!el) return
  el.style.setProperty('--bx', curX + 'px')
  el.style.setProperty('--by', curY + 'px')
  el.style.transform = `translate(${curX}px, ${curY}px)`
}

function initPosition() {
  const el = bubbleRef.value
  if (!el) return
  const w = el.offsetWidth || 64
  const h = el.offsetHeight || 64
  curX = window.innerWidth - w - 40
  curY = window.innerHeight - h - 100
  applyPosition()
}

function expand() {
  const el = bubbleRef.value
  if (!el) return
  const w = 480
  const h = window.innerHeight * 0.7
  open.value = true
  el.style.width = w + 'px'
  el.style.height = h + 'px'
  el.style.borderRadius = '16px'
  if (curX + w > window.innerWidth - 20) curX = window.innerWidth - w - 20
  if (curY + h > window.innerHeight - 20) curY = window.innerHeight - h - 20
  if (curX < 20) curX = 20
  if (curY < 20) curY = 20
  applyPosition()
}

function collapse() {
  const el = bubbleRef.value
  if (!el) return
  const centerX = curX + el.offsetWidth / 2
  const centerY = curY + el.offsetHeight / 2
  open.value = false
  el.style.width = '64px'
  el.style.height = '64px'
  el.style.borderRadius = '50%'
  curX = centerX - 32
  curY = centerY - 32
  if (curX < 0) curX = 0
  if (curY < 0) curY = 0
  if (curX > window.innerWidth - 64) curX = window.innerWidth - 64
  if (curY > window.innerHeight - 64) curY = window.innerHeight - 64
  applyPosition()
}

function onPointerDown(e) {
  if (e.target.closest('.bubble-close')) return
  if (open.value && !e.target.closest('.bubble-header')) return
  e.preventDefault()
  dragging.value = true
  hasMoved = false
  startX = e.clientX
  startY = e.clientY
  initialX = curX
  initialY = curY
  bubbleRef.value.setPointerCapture(e.pointerId)
}

function onPointerMove(e) {
  if (!dragging.value) return
  const dx = e.clientX - startX
  const dy = e.clientY - startY
  if (Math.abs(dx) > 5 || Math.abs(dy) > 5) hasMoved = true
  if (!hasMoved) return
  if (rafId) cancelAnimationFrame(rafId)
  rafId = requestAnimationFrame(() => {
    const el = bubbleRef.value
    if (!el) return
    const maxLeft = window.innerWidth - el.offsetWidth
    const maxTop = window.innerHeight - el.offsetHeight
    curX = Math.max(0, Math.min(initialX + dx, maxLeft))
    curY = Math.max(0, Math.min(initialY + dy, maxTop))
    applyPosition()
  })
}

function onPointerUp(e) {
  if (!dragging.value) return
  dragging.value = false
  try {
    bubbleRef.value.releasePointerCapture(e.pointerId)
  } catch (err) {
    /* pointer capture 可能已经释放 */
  }
  if (!hasMoved) {
    if (open.value) collapse()
    else expand()
  }
}

function onMouseMove(e) {
  if (open.value) return
  const el = bubbleRef.value
  if (!el) return
  const rect = el.getBoundingClientRect()
  const dx = e.clientX - (rect.left + rect.width / 2)
  const dy = e.clientY - (rect.top + rect.height / 2)
  const dist = Math.sqrt(dx * dx + dy * dy)
  const limit = Math.min(dist / 10, 4)
  const angle = Math.atan2(dy, dx)
  const ox = Math.cos(angle) * limit
  const oy = Math.sin(angle) * limit
  el.querySelectorAll('.pupil').forEach((pupil) => {
    pupil.style.transform = `translate(${ox}px, ${oy}px)`
  })
}

onMounted(() => {
  initPosition()
  window.addEventListener('mousemove', onMouseMove)
})

onBeforeUnmount(() => {
  window.removeEventListener('mousemove', onMouseMove)
  if (rafId) cancelAnimationFrame(rafId)
})
</script>

<template>
  <div
    ref="bubbleRef"
    class="float-bubble"
    :class="{ 'is-open': open, 'is-dragging': dragging }"
    @pointerdown="onPointerDown"
    @pointermove="onPointerMove"
    @pointerup="onPointerUp"
  >
    <div class="bubble-tip">点我预览</div>
    <div class="bubble-eyes">
      <div class="eye"><div class="pupil"></div></div>
      <div class="eye"><div class="pupil"></div></div>
    </div>
    <div class="bubble-header">
      <span>实时文档预览</span>
      <button class="bubble-close" @click.stop="collapse">×</button>
    </div>
    <div class="bubble-body">
      <div class="doc" v-html="docHtml"></div>
    </div>
  </div>
</template>
