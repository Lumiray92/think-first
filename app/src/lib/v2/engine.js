import {
  CHECK_ITEMS,
  STAGES,
  createState,
  groupOfStage,
  stageByNo,
  stageSlotKeys,
} from './schema.js'

export { createState, groupOfStage, stageByNo }

export const EMPTY_STATE = createState()

export function setSlots(state, updates) {
  if (!updates || typeof updates !== 'object') return state
  Object.keys(updates).forEach((key) => {
    const value = updates[key]
    if (value === undefined) return
    state.slots[key] = value
  })
  state.updatedAt = Date.now()
  return state
}

export function clearStagesFrom(state, stageNo) {
  STAGES.filter((stage) => stage.no >= Number(stageNo)).forEach((stage) => {
    stageSlotKeys(stage.no).forEach((key) => {
      delete state.slots[key]
    })
  })
  state.updatedAt = Date.now()
  return state
}

function lightSet(slots) {
  const answers = slots.s6_answers || {}
  return {
    who: answers.who?.light || '',
    pain: answers.pain?.light || '',
    why: answers.why?.light || '',
  }
}

function checklistFailures(slots) {
  const checklist = slots.s8_checklist || {}
  return CHECK_ITEMS.map((item) => ({
    item,
    value: checklist[item.key],
  })).filter((row) => row.value === false || row.value === 'yellow')
}

export function evaluateGate(state, stageNo) {
  const no = Number(stageNo)

  if (no === 6) {
    const lights = lightSet(state.slots)
    const values = Object.values(lights)
    const reds = values.filter((value) => value === 'red')
    const greens = values.filter((value) => value === 'green')

    if (reds.length) {
      return {
        status: 'red',
        label: '有红灯，先回炉',
        message: '用户是谁还没说清，先别往下走。回到 JTBD 把具体的人补出来。',
        issues: ['用户 / 痛点 / 差异化至少有一项说不清'],
        returnTo: 2,
        blocking: true,
      }
    }

    if (greens.length === values.length && values.length === 3) {
      return {
        status: 'green',
        label: '已经想清楚，可以继续',
        message: '三个问题都能一句话说清，有依据。',
        issues: [],
        returnTo: null,
        blocking: false,
      }
    }

    return {
      status: 'yellow',
      label: '两绿一黄，先做最小实验',
      message: '方向对了，但差异化还没被验证。可以继续，但要带着风险开工。',
      issues: ['为什么选你：建议先找 3 个真实用户验证'],
      returnTo: 5,
      blocking: false,
    }
  }

  if (no === 8) {
    const rows = checklistFailures(state.slots)
    const hard = rows.filter((row) => row.value === false)

    if (hard.length) {
      const item = hard[0].item
      return {
        status: 'no-go',
        label: '暂时别开始，先补充信息',
        message: '还有硬性检查没过，不能进入写 PRD。',
        issues: hard.map((row) => row.item.label),
        returnTo: item.returnTo,
        blocking: true,
      }
    }

    if (rows.length) {
      return {
        status: 'yellow',
        label: '有条件 Go',
        message: '可以继续，但要带着风险开工。建议先做最小实验。',
        issues: rows.map((row) => row.item.label),
        returnTo: rows[0].item.returnTo,
        blocking: false,
      }
    }

    return {
      status: 'go',
      label: 'Go，可以写 PRD',
      message: '六项检查全部通过。',
      issues: [],
      returnTo: null,
      blocking: false,
    }
  }

  return null
}

export function isBlockingGate(gate) {
  if (!gate) return false
  return gate.status === 'red' || gate.status === 'no-go'
}

export function progressOf(state) {
  return {
    stage: state.stage,
    total: STAGES.length,
    group: groupOfStage(state.stage).id,
    groups: 5,
  }
}

export function nextStageNo(stageNo) {
  const next = Number(stageNo) + 1
  return next <= STAGES.length ? next : null
}
