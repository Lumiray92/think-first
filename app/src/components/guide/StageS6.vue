<script setup>
import { computed } from 'vue'

import QuestionBlock from './QuestionBlock.vue'
import { SCENES } from '@/lib/questions'
import { useSession } from '@/composables/useSession'

const { state, saveNow } = useSession()

const currentScene = computed(() => SCENES.find((scene) => scene.key === state.adapt.scene))

const adaptAnswer = computed({
  get: () => state.adapt.answer || '',
  set: (value) => {
    state.adapt.answer = value
    saveNow()
  },
})

const redline = computed({
  get: () => state.adapt.redline || '',
  set: (value) => {
    state.adapt.redline = value
    saveNow()
  },
})

function pickScene(key) {
  state.adapt.scene = key
  state.adapt.answer = ''
  saveNow()
}
</script>

<template>
  <QuestionBlock id="s6_1" index="01" />

  <div class="q">
    <div class="q-head">
      <span class="q-index">02</span>
      <div>
        <div class="q-title">属于哪类场景？</div>
      </div>
    </div>
    <div class="scene-grid">
      <button
        v-for="scene in SCENES"
        :key="scene.key"
        class="scene-card"
        :class="{ 'is-on': state.adapt.scene === scene.key }"
        @click="pickScene(scene.key)"
      >
        <div class="scene-title">{{ scene.title }}</div>
        <div class="scene-desc">{{ scene.desc }}</div>
      </button>
    </div>
  </div>

  <div v-if="currentScene" class="q">
    <div class="q-head">
      <span class="q-index">03</span>
      <div>
        <div class="q-title">{{ currentScene.q }}</div>
      </div>
    </div>
    <textarea v-model="adaptAnswer" class="q-input" rows="3" :placeholder="currentScene.ph"></textarea>
  </div>

  <QuestionBlock id="s6_2" index="04" />
  <QuestionBlock id="s6_3" index="05" />

  <div class="q">
    <div class="q-head">
      <span class="q-index">06</span>
      <div>
        <div class="q-title">这个场景有什么「不能做」的红线？</div>
      </div>
    </div>
    <textarea
      v-model="redline"
      class="q-input"
      rows="3"
      placeholder="例：打开到记完 ≤3 步；手机单手可用；离线可用；不登录。"
    ></textarea>
  </div>

  <QuestionBlock id="s6_4" index="07" />
</template>
