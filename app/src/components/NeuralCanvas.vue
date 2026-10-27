<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'

const canvasRef = ref(null)
let api = null

onMounted(() => {
  const canvas = canvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  const reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  let w = 0
  let h = 0
  let dpr = 1
  let nodes = []
  let raf = null
  const mouse = { x: -9999, y: -9999, active: false }
  const LINK = 158
  const MOUSE_LINK = 200

  function resize() {
    const cw = canvas.clientWidth
    const ch = canvas.clientHeight
    if (!cw || !ch) return false
    dpr = Math.min(window.devicePixelRatio || 1, 2)
    w = cw
    h = ch
    canvas.width = Math.round(w * dpr)
    canvas.height = Math.round(h * dpr)
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
    build()
    return true
  }

  function build() {
    const count = Math.max(34, Math.min(84, Math.round((w * h) / 17000)))
    nodes = []
    for (let i = 0; i < count; i++) {
      nodes.push({
        x: Math.random() * w,
        y: Math.random() * h,
        vx: (Math.random() - 0.5) * 0.34,
        vy: (Math.random() - 0.5) * 0.34,
        r: 1.1 + Math.random() * 1.5,
        hub: Math.random() < 0.16,
      })
    }
  }

  function draw() {
    ctx.clearRect(0, 0, w, h)
    for (let i = 0; i < nodes.length; i++) {
      const a = nodes[i]
      for (let j = i + 1; j < nodes.length; j++) {
        const b = nodes[j]
        const dx = a.x - b.x
        const dy = a.y - b.y
        const d2 = dx * dx + dy * dy
        if (d2 > LINK * LINK) continue
        const d = Math.sqrt(d2)
        ctx.strokeStyle = 'rgba(108, 130, 226,' + ((1 - d / LINK) * 0.3).toFixed(3) + ')'
        ctx.lineWidth = 1
        ctx.beginPath()
        ctx.moveTo(a.x, a.y)
        ctx.lineTo(b.x, b.y)
        ctx.stroke()
      }
    }
    if (mouse.active) {
      for (let i = 0; i < nodes.length; i++) {
        const n = nodes[i]
        const dx = n.x - mouse.x
        const dy = n.y - mouse.y
        const d2 = dx * dx + dy * dy
        if (d2 > MOUSE_LINK * MOUSE_LINK) continue
        const d = Math.sqrt(d2)
        ctx.strokeStyle = 'rgba(67, 56, 202,' + ((1 - d / MOUSE_LINK) * 0.36).toFixed(3) + ')'
        ctx.lineWidth = 1
        ctx.beginPath()
        ctx.moveTo(n.x, n.y)
        ctx.lineTo(mouse.x, mouse.y)
        ctx.stroke()
      }
    }
    for (let i = 0; i < nodes.length; i++) {
      const n = nodes[i]
      n.x += n.vx
      n.y += n.vy
      if (n.x < -24) n.x = w + 24
      if (n.x > w + 24) n.x = -24
      if (n.y < -24) n.y = h + 24
      if (n.y > h + 24) n.y = -24
      if (n.hub) {
        ctx.beginPath()
        ctx.fillStyle = 'rgba(99, 102, 241, 0.10)'
        ctx.arc(n.x, n.y, n.r * 5, 0, Math.PI * 2)
        ctx.fill()
        ctx.beginPath()
        ctx.fillStyle = 'rgba(67, 56, 202, 0.55)'
        ctx.arc(n.x, n.y, n.r + 0.7, 0, Math.PI * 2)
        ctx.fill()
      } else {
        ctx.beginPath()
        ctx.fillStyle = 'rgba(99, 122, 220, 0.40)'
        ctx.arc(n.x, n.y, n.r, 0, Math.PI * 2)
        ctx.fill()
      }
    }
  }

  function frame() {
    draw()
    raf = requestAnimationFrame(frame)
  }

  function start() {
    if (reduce) {
      if (!w || !h) resize()
      draw()
      return
    }
    if (raf) return
    if (!w || !h) {
      if (!resize()) return
    }
    raf = requestAnimationFrame(frame)
  }

  function stop() {
    if (raf) {
      cancelAnimationFrame(raf)
      raf = null
    }
  }

  function onResize() {
    stop()
    if (resize()) {
      if (reduce) draw()
      else start()
    }
  }

  function onMove(e) {
    const rect = canvas.getBoundingClientRect()
    mouse.x = e.clientX - rect.left
    mouse.y = e.clientY - rect.top
    mouse.active = true
  }

  function onLeave() {
    mouse.active = false
    mouse.x = -9999
    mouse.y = -9999
  }

  window.addEventListener('resize', onResize)
  window.addEventListener('mousemove', onMove)
  window.addEventListener('mouseout', onLeave)
  if (resize()) {
    if (reduce) draw()
    else start()
  }

  api = {
    destroy() {
      stop()
      window.removeEventListener('resize', onResize)
      window.removeEventListener('mousemove', onMove)
      window.removeEventListener('mouseout', onLeave)
    },
  }
})

onBeforeUnmount(() => {
  if (api) {
    api.destroy()
    api = null
  }
})
</script>

<template>
  <canvas ref="canvasRef" class="neural" aria-hidden="true"></canvas>
</template>
