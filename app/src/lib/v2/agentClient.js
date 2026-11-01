import { TOTAL_STAGES, groupById, nextGroupId } from './schema.js'
import {
  clearStagesFrom,
  createState,
  evaluateGate,
  groupOfStage,
  isBlockingGate,
  nextStageNo,
  progressOf,
  setSlots,
  stageByNo,
} from './engine.js'
import {
  mockApplyGroup,
  mockApplyStage,
  mockQuestionForGroup,
  mockQuestionForStage,
  mockSummary,
} from './mockAgent.js'
import { buildPrd } from './prd.js'

const DELAY = 220

function wait(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

function touch(state) {
  state.updatedAt = Date.now()
}

function askStage(state, stageNo) {
  const question = mockQuestionForStage(state, stageNo)
  state.stage = Number(stageNo)
  state.group = groupOfStage(stageNo).id
  state.gate = null
  state.status = 'asking'
  const turn = {
    ...question,
    progress: progressOf(state),
    prd: buildPrd(state.slots, state),
  }
  state.pending = turn
  touch(state)
  return turn
}

function askGroup(state, groupId) {
  const group = groupById(groupId)
  const question = mockQuestionForGroup(state, groupId)
  state.group = group.id
  state.stage = group.stages[0]
  state.gate = null
  state.status = 'asking'
  const turn = {
    ...question,
    progress: progressOf(state),
    prd: buildPrd(state.slots, state),
  }
  state.pending = turn
  touch(state)
  return turn
}

function gateTurn(state, gate, stageNo, groupId) {
  const no = stageNo || state.stage
  return {
    kind: 'gate',
    stage: no,
    group: groupId || null,
    stageName: stageByNo(no).name,
    agent: stageByNo(no).agent,
    title: gate.label,
    message: gate.message,
    gate,
    issues: gate.issues || [],
    returnTo: gate.returnTo,
    canContinue: !isBlockingGate(gate),
    candidates: [],
    progress: progressOf(state),
    prd: buildPrd(state.slots, state),
  }
}

function finish(state) {
  state.status = 'done'
  state.stage = TOTAL_STAGES
  state.group = 5
  state.gate = null
  const turn = {
    kind: 'done',
    stage: TOTAL_STAGES,
    stageName: 'PRD 成稿',
    agent: 'PRD 写手',
    title: 'PRD 已生成',
    message: '12 个步骤全部完成。可以导出 Markdown，也可以回到任意步骤继续改。',
    candidates: [],
    progress: progressOf(state),
    prd: buildPrd(state.slots, state),
  }
  state.pending = turn
  touch(state)
  return turn
}

function answerStage(state, answer) {
  const stageNo = state.stage
  const applied = mockApplyStage(state, stageNo, answer)
  setSlots(state, applied)
  state.history.push({ stage: stageNo, answer, at: Date.now() })

  const gate = evaluateGate(state, stageNo)
  if (gate) {
    state.gate = gate
    if (gate.status !== 'green' && gate.status !== 'go') {
      state.status = 'blocked'
      const turn = gateTurn(state, gate, stageNo, null)
      state.pending = turn
      return turn
    }
  }

  const next = nextStageNo(stageNo)
  if (!next) return finish(state)
  return askStage(state, next)
}

function answerGroup(state, answer) {
  const groupId = state.group
  const applied = mockApplyGroup(state, groupId, answer)
  setSlots(state, applied)
  state.history.push({ group: groupId, answer, at: Date.now() })

  const group = groupById(groupId)
  state.stage = group.stages[group.stages.length - 1]

  let gate = null
  group.stages.forEach((no) => {
    const result = evaluateGate(state, no)
    if (result && result.status !== 'green' && result.status !== 'go') gate = result
  })

  if (gate) {
    state.gate = gate
    state.status = 'blocked'
    const turn = gateTurn(state, gate, state.stage, groupId)
    state.pending = turn
    return turn
  }

  const summary = mockSummary(state, groupId, applied)
  state.status = 'confirming'
  const turn = {
    kind: 'confirm',
    group: groupId,
    stage: state.stage,
    stageName: group.title,
    agent: stageByNo(group.stages[0]).agent,
    title: group.title,
    message: '这些内容符合你的想法吗？',
    summary,
    candidates: [],
    progress: progressOf(state),
    prd: buildPrd(state.slots, state),
  }
  state.pending = turn
  touch(state)
  return turn
}

function confirm(state) {
  const pending = state.pending
  if (!pending) return null
  if (pending.kind === 'done') return pending

  if (pending.kind === 'gate') {
    if (isBlockingGate(pending.gate)) return pending
    if (state.mode === 'quick') {
      const next = nextGroupId(state.group)
      return next ? askGroup(state, next) : finish(state)
    }
    const next = nextStageNo(state.stage)
    return next ? askStage(state, next) : finish(state)
  }

  if (pending.kind === 'confirm' && state.mode === 'quick') {
    const next = nextGroupId(state.group)
    return next ? askGroup(state, next) : finish(state)
  }

  return pending
}

function regenerate(state) {
  const pending = state.pending
  if (!pending || pending.kind !== 'question' || !pending.candidates?.length) return pending
  const candidates = [...pending.candidates]
  candidates.push(candidates.shift())
  const turn = { ...pending, candidates, regenerated: true }
  state.pending = turn
  touch(state)
  return turn
}

function backTo(state, stageNo) {
  const no = Math.max(1, Math.min(Number(stageNo) || 1, TOTAL_STAGES))
  if ((state.mode === 'quick' && groupOfStage(no).id > state.group) || (state.mode !== 'quick' && no > state.stage)) throw new Error('不能跳过前面的步骤')
  clearStagesFrom(state, state.mode === 'quick' ? groupOfStage(no).stages[0] : no)
  state.gate = null
  state.status = 'asking'
  state.stage = no
  state.group = groupOfStage(no).id
  return state.mode === 'quick' ? askGroup(state, state.group) : askStage(state, no)
}

export async function start(state, { idea = '', mode = 'quick' } = {}) {
  const fresh = createState({ idea, mode })
  Object.keys(state).forEach((key) => {
    delete state[key]
  })
  Object.assign(state, fresh)
  await wait(DELAY)
  return state.mode === 'quick' ? askGroup(state, 1) : askStage(state, 1)
}

export async function act(state, action = {}) {
  await wait(DELAY)
  const type = action.type

  if (type === 'regenerate') return regenerate(state)
  if (type === 'back') return backTo(state, action.stage)
  if (type === 'confirm') return confirm(state)

  const pending = state.pending
  const candidates = pending?.candidates || []
  const index = Number.isInteger(action.index) ? action.index : 0
  // 候选换序后根据稳定标识恢复框架位置，避免选中的名称与保存内容不一致。
  const frameworkIndex = ['scaff', 'rgc', 'story', 'universal'].indexOf(candidates[index]?.id)
  const answer = {
    index: frameworkIndex >= 0 ? frameworkIndex : index,
    text: String(action.text || candidates[index]?.text || '').trim(),
  }

  return state.mode === 'quick' ? answerGroup(state, answer) : answerStage(state, answer)
}

export function getPrd(state) {
  return buildPrd(state.slots, state)
}

export const agentClient = { start, act, getPrd }
