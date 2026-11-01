import { act, getPrd, start } from '../src/lib/v2/agentClient.js'
import { createState } from '../src/lib/v2/engine.js'

const REQUIRED = [
  '# ',
  '## 一句话描述',
  '## 目标用户',
  '## 要解决的问题',
  '## 用户任务（JTBD）',
  '## 核心功能（P0，必须有）',
  '## 不做的功能',
  '## 用户流程',
  '## 技术约束',
  '## 成功标准',
  '## 风险与对策',
  '## 最终自检',
  '## 给 AI 的 Prompt',
]

function fail(message) {
  console.error('FAIL:', message)
  process.exitCode = 1
}

function assert(condition, message) {
  if (!condition) fail(message)
}

function assertPrd(state, label) {
  const prd = getPrd(state)
  REQUIRED.forEach((section) => {
    assert(prd.includes(section), `${label} PRD 缺少「${section}」`)
  })
  assert(!prd.includes('（待填写）'), `${label} PRD 仍有待填写字段`)
  assert(Object.keys(state.slots || {}).length > 20, `${label} 槽位数量过少`)
  return prd
}

async function runQuick(idea) {
  const state = createState()
  let turn = await start(state, { idea, mode: 'quick' })
  let guard = 0

  while (turn.kind !== 'done' && guard < 30) {
    guard += 1
    if (turn.kind === 'question') {
      turn = await act(state, { type: 'select', index: 0 })
    } else if (turn.kind === 'confirm') {
      turn = await act(state, { type: 'confirm' })
    } else if (turn.kind === 'gate') {
      if (turn.gate.status === 'red' || turn.gate.status === 'no-go') {
        fail(`快速模式意外被闸门拦截：${turn.gate.status}`)
        break
      }
      turn = await act(state, { type: 'confirm' })
    } else {
      break
    }
  }

  assert(turn.kind === 'done', '快速模式没有走到 PRD 成稿')
  assert(state.status === 'done', '快速模式最终状态不是 done')
  return { state, turn, guard }
}

async function runFull(idea) {
  const state = createState()
  let turn = await start(state, { idea, mode: 'full' })
  let guard = 0

  while (turn.kind !== 'done' && guard < 40) {
    guard += 1
    if (turn.kind === 'question') {
      turn = await act(state, { type: 'select', index: 0 })
    } else if (turn.kind === 'gate') {
      if (turn.gate.status === 'red' || turn.gate.status === 'no-go') {
        fail(`完整模式意外被闸门拦截：${turn.gate.status}`)
        break
      }
      turn = await act(state, { type: 'confirm' })
    } else if (turn.kind === 'confirm') {
      turn = await act(state, { type: 'confirm' })
    } else {
      break
    }
  }

  assert(turn.kind === 'done', '完整模式没有走到 PRD 成稿')
  assert(state.stage === 12, '完整模式没有走完 12 个步骤')
  return { state, turn, guard }
}

async function runRedGate() {
  const state = createState()
  let turn = await start(state, { idea: '做一个待办工具', mode: 'quick' })

  turn = await act(state, { type: 'select', index: 0 })
  turn = await act(state, { type: 'confirm' })
  turn = await act(state, { type: 'select', index: 0 })
  turn = await act(state, { type: 'confirm' })
  turn = await act(state, { type: 'select', index: 2 })

  assert(turn.kind === 'gate', '选红灯后没有出现闸门')
  assert(turn.gate.status === 'red', '闸门状态不是 red')
  assert(turn.gate.returnTo === 2, '红灯没有指回第 2 步')

  turn = await act(state, { type: 'back', stage: turn.gate.returnTo })
  assert(turn.kind === 'question', '回炉后没有重新提问')
  assert(state.group === 1, '回炉后没有回到第 1 组')
  return state
}

async function main() {
  const quick = await runQuick('做一个提醒妈妈吃药的小程序')
  const quickPrd = assertPrd(quick.state, '快速模式')
  assert(
    quick.state.slots.s11_prompt.includes('示例'),
    '快速模式的高级技巧没有叠加进 Prompt',
  )
  assert(!quickPrd.includes('每天在每天'), 'PRD 出现了重复的「每天在每天」')
  assert(!quickPrd.includes('（green）'), 'PRD 暴露了英文灯色 green')
  console.log('快速模式：', quick.guard, '次交互，', Object.keys(quick.state.slots).length, '个槽位')

  const full = await runFull('做一个周报收集器')
  const fullPrd = assertPrd(full.state, '完整模式')
  console.log('完整模式：', full.guard, '次交互，', Object.keys(full.state.slots).length, '个槽位')

  await runRedGate()
  console.log('闸门：红灯可拦截，并回跳到第 1 组')

  const preview = quickPrd.split('\n').slice(0, 14).join('\n')
  console.log('\n--- 快速模式 PRD 预览 ---\n' + preview)
  console.log('\n--- 完整模式 PRD 前 8 行 ---\n' + fullPrd.split('\n').slice(0, 8).join('\n'))

  if (!process.exitCode) console.log('\n全部通过')
}

main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
