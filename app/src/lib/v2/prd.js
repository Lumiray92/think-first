import { CHECK_ITEMS, SCENES } from './schema.js'

const EMPTY = '（待填写）'

function text(value) {
  if (value == null) return ''
  return String(value).replace(/\r\n?/g, '\n').trim()
}

function filled(value) {
  return text(value) || EMPTY
}

function list(value) {
  if (!Array.isArray(value) || !value.length) return EMPTY
  return value
    .map((item) => (typeof item === 'string' ? item : text(item?.name || item?.reason || '')))
    .filter(Boolean)
    .join('、')
}

function lightText(light) {
  if (light === 'green') return '🟢 清晰'
  if (light === 'yellow') return '🟡 说不清'
  if (light === 'red') return '🔴 答不出'
  return '⚪ 待判断'
}

function checkText(value) {
  if (value === true) return '✅ 通过'
  if (value === 'yellow') return '🟡 有风险'
  if (value === false) return '❌ 未通过'
  return '⚪ 待检查'
}

function sceneTitle(key) {
  return SCENES.find((scene) => scene.key === key)?.title || ''
}

function pushSection(L, title, lines) {
  L.push('## ' + title)
  L.push('')
  lines.filter((line) => line != null && line !== '').forEach((line) => L.push(line))
  L.push('')
}

function pushUser(L, s) {
  const profile = s.s5_profile || {}
  const lines = ['- ' + filled(s.s2_user)]
  if (profile.surface) lines.push('- 表面属性：' + profile.surface)
  if (profile.behavior) lines.push('- 行为习惯：' + profile.behavior)
  if (profile.motive) lines.push('- 深层动机：' + profile.motive)
  if (s.s5_lowest) lines.push('- 情绪最低点：' + s.s5_lowest)
  if (s.s5_improvement) lines.push('- 对应改进：' + s.s5_improvement)
  pushSection(L, '目标用户', lines)
}

function pushProblem(L, s) {
  const diary = Array.isArray(s.s1_diary) ? s.s1_diary : []
  const lines = ['- ' + filled(s.s1_problem)]
  if (s.s2_alternatives) lines.push('- 现有替代方案：' + s.s2_alternatives)
  if (diary.length) {
    lines.push('- 烦恼日记：')
    diary.forEach((row) => lines.push('  - ' + row))
  }
  pushSection(L, '要解决的问题', lines)
}

function pushTask(L, s) {
  const layers = s.s2_layers || {}
  const lines = ['- ' + filled(s.s2_jtbd)]
  if (layers.functional) lines.push('- 功能任务：' + layers.functional)
  if (layers.emotional) lines.push('- 情感任务：' + layers.emotional)
  if (layers.social) lines.push('- 社会任务：' + layers.social)
  pushSection(L, '用户想完成的事（JTBD）', lines)
}

function pushFeatures(L, s) {
  const features = Array.isArray(s.s4_features) ? s.s4_features : []
  const p0 = features.filter((feature) => feature.priority === 'P0')
  const lines = p0.length ? p0.map((feature) => '- [ ] ' + feature.name) : ['- ' + EMPTY]
  if (s.s4_hypothesis) lines.push('', '- 核心假设：' + s.s4_hypothesis)
  if (s.s4_scope_note) lines.push('- 范围提醒：' + s.s4_scope_note)
  pushSection(L, '核心功能（P0，必须有）', lines)
}

function pushNotDoing(L, s) {
  const doing = s.s4_notdoing || {}
  const lines = [
    '- 不做的功能：' + list(doing.features),
    '- 不服务的用户：' + list(doing.users),
    '- 不追的指标：' + list(doing.metrics),
  ]
  pushSection(L, '不做的功能', lines)
}

function pushFlow(L, s) {
  const acts = s.s5_acts || {}
  const lines = []
  if (acts.open) lines.push('- 开端：' + acts.open)
  if (acts.conflict) lines.push('- 冲突：' + acts.conflict)
  if (acts.solve) lines.push('- 解决：' + acts.solve)
  if (acts.ending) lines.push('- 结局：' + acts.ending)

  const journey = Array.isArray(s.s5_journey) ? s.s5_journey : []
  if (journey.length) {
    lines.push('', '- 用户旅程：')
    journey.forEach((step) => {
      const parts = [step.stage, step.action, step.emotion].filter(Boolean)
      lines.push('  - ' + parts.join(' · '))
    })
  }
  pushSection(L, '用户流程', lines.length ? lines : ['- ' + EMPTY])
}

function pushConstraints(L, s) {
  const lines = []
  const scene = sceneTitle(s.s7_scene)
  if (scene) lines.push('- 场景类型：' + scene)
  if (s.s7_constraints) lines.push('- 硬约束：' + s.s7_constraints)
  if (s.s7_redlines) lines.push('- 红线：' + s.s7_redlines)
  if (s.s1_redline) lines.push('- 问题阶段红线：' + s.s1_redline)
  if (s.s1_scores) {
    const score = s.s1_scores
    lines.push('五维评分：每项0–5分，总分25分，为AI初步估计，需用户核实。')
    for (const [key, label, meaning] of [['repeat','重复性','越频繁分越高'],['rule','规则性','步骤越固定分越高'],['verify','可验证性','结果越容易检查分越高'],['sensitive','敏感性','数据越不敏感分越高'],['fault','容错性','错误越容易纠正分越高']]) {
      lines.push(`- ${label}（${meaning}）：${filled(score[key])}/5；依据：${score[key + '_reason'] || '待确认'}`)
    }
    lines.push(
      '- 五维评分：' +
        filled(score.total) +
        ' / 25（重复性 ' +
        filled(score.repeat) +
        '，规则性 ' +
        filled(score.rule) +
        '，可验证性 ' +
        filled(score.verify) +
        '，敏感性 ' +
        filled(score.sensitive) +
        '，容错性 ' +
        filled(score.fault) +
        '）',
    )
  }
  pushSection(L, '技术约束', lines.length ? lines : ['- ' + EMPTY])
}

function pushSuccess(L, s) {
  const lines = []
  if (s.s4_validation) lines.push('- 验证标准：' + s.s4_validation)
  if (s.s7_acceptance) lines.push('- 验收标准：' + s.s7_acceptance)
  if (s.s6_elevator) lines.push('- 电梯演讲：' + s.s6_elevator)
  pushSection(L, '成功标准', lines.length ? lines : ['- ' + EMPTY])
}

function pushRisks(L, s) {
  const top3 = Array.isArray(s.s3_top3) ? s.s3_top3 : []
  const lines = []
  if (top3.length) {
    lines.push('- 最需要注意的三个问题与对策：')
    top3.forEach((risk) => {
      lines.push(
        '  - ' +
          risk.reason +
          '（可能性 ' +
          filled(risk.likelihood) +
          ' / 严重度 ' +
          filled(risk.severity) +
          '）→ ' +
          filled(risk.prevention),
      )
    })
  }
  if (s.s3_signals) lines.push('- 失败信号：' + s.s3_signals)
  if (s.s3_adjust) lines.push('- 调整方案：' + s.s3_adjust)
  pushSection(L, '风险与对策', lines.length ? lines : ['- ' + EMPTY])
}

function pushSelfCheck(L, s) {
  const answers = s.s6_answers || {}
  const lines = [
    '- 用户是谁：' + filled(answers.who?.answer) + '（' + lightText(answers.who?.light) + '）',
    '- 痛点在哪：' + filled(answers.pain?.answer) + '（' + lightText(answers.pain?.light) + '）',
    '- 为什么选你：' + filled(answers.why?.answer) + '（' + lightText(answers.why?.light) + '）',
  ]

  const checklist = s.s8_checklist || {}
  lines.push('', '- 开发前检查：')
  CHECK_ITEMS.forEach((item) => {
    lines.push('  - ' + item.label + '：' + checkText(checklist[item.key]))
  })
  if (s.s8_gate) lines.push('- 结论：' + s.s8_gate)
  if (s.s12_restate) lines.push('- AI 复述：' + s.s12_restate)
  pushSection(L, '最终自检', lines)
}

function pushPrompt(L, s) {
  const prompt = text(s.s11_prompt) || text(s.s10_prompt)
  const context = s.s9_context || {}
  const lines = []
  lines.push('提示词框架：' + (s.s10_framework || '待确认'))
  lines.push('框架说明：S.C.A.F.F.包含情境、挑战、受众、输出格式和基础约束；R.G.C.包含角色、目标和约束；故事化包含用户处境、问题和期望；万能模板包含角色、背景、任务、资料、约束、输出和验收。示例和自查属于技巧，不是框架。')

  if (context.project) lines.push('- 项目上下文：' + context.project)
  if (context.domain) lines.push('- 领域上下文：' + context.domain)
  if (context.task) lines.push('- 任务上下文：' + context.task)
  if (s.s11_techniques?.length) lines.push('- 使用技巧：' + list(s.s11_techniques))
  if (lines.length) lines.push('')
  lines.push(prompt || EMPTY)
  pushSection(L, '给 AI 的 Prompt', lines)
}

export function buildPrd(slots = {}, state = {}) {
  const s = slots || {}
  const title = text(s.s1_title) || (text(state.idea) ? text(state.idea).slice(0, 24) : '需求文档')
  const L = []

  L.push('# ' + title)
  L.push('')
  L.push(
    '> 由 ThinkFirst 智能体按 course07-Prompt 全流程引导生成 · ' +
      new Date().toLocaleDateString('zh-CN'),
  )
  L.push('')

  pushSection(L, '一句话描述', [
    '- ' + filled(s.s6_elevator || s.s1_problem || state.idea),
  ])
  pushUser(L, s)
  pushProblem(L, s)
  pushTask(L, s)
  pushFeatures(L, s)
  pushNotDoing(L, s)
  pushFlow(L, s)
  pushConstraints(L, s)
  pushSuccess(L, s)
  pushRisks(L, s)
  pushSelfCheck(L, s)
  pushPrompt(L, s)

  return L.join('\n').replace(/\n{3,}/g, '\n\n').trim() + '\n'
}

export function prdTitle(slots = {}, state = {}) {
  return text(slots.s1_title) || (text(state.idea) ? text(state.idea).slice(0, 24) : '需求文档')
}

function escapeHtml(value) {
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

export function prdHtml(markdown = '') {
  const out = []
  let inList = false

  const closeList = () => {
    if (!inList) return
    out.push('</ul>')
    inList = false
  }

  String(markdown)
    .split('\n')
    .forEach((line) => {
      const trimmed = line.trim()
      if (!trimmed) {
        closeList()
        return
      }

      if (trimmed.startsWith('- ')) {
        if (!inList) {
          out.push('<ul>')
          inList = true
        }
        const nested = line.startsWith('  - ') || line.startsWith('    - ')
        out.push(
          '<li' + (nested ? ' class="sub"' : '') + '>' + escapeHtml(trimmed.slice(2)) + '</li>',
        )
        return
      }

      closeList()
      if (trimmed.startsWith('### ')) out.push('<h3>' + escapeHtml(trimmed.slice(4)) + '</h3>')
      else if (trimmed.startsWith('## ')) out.push('<h2>' + escapeHtml(trimmed.slice(3)) + '</h2>')
      else if (trimmed.startsWith('# ')) out.push('<h1>' + escapeHtml(trimmed.slice(2)) + '</h1>')
      else if (trimmed.startsWith('> '))
        out.push('<blockquote>' + escapeHtml(trimmed.slice(2)) + '</blockquote>')
      else out.push('<p>' + escapeHtml(trimmed) + '</p>')
    })

  closeList()
  return out.join('\n')
}
