import {
  Q,
  SCENES,
  SCORE_DIMS,
  STAGES,
  discoverScore,
  featureVerdict,
  scoreVerdict,
  soulState,
} from './questions'

const CN_NUM = ['一', '二', '三', '四', '五', '六', '七', '八', '九']
const EMPTY = '（待填写）'

function text(value) {
  if (value == null) return ''
  return String(value)
    .replace(/\r\n?/g, '\n')
    .replace(/\n+/g, '；')
    .trim()
}

function filled(value) {
  return text(value) || EMPTY
}

function line(L, label, value) {
  L.push('- ' + label + '：' + filled(value))
}

function lightText(value) {
  if (value === 'green') return '🟢 清晰'
  if (value === 'yellow') return '🟡 说不清'
  if (value === 'red') return '🔴 答不出'
  return '⚪ 待点亮'
}

function featureAnswer(feature) {
  const q1 =
    feature.q1 === 1 ? 'Q1 没有它不能用' : feature.q1 === 0 ? 'Q1 没有它也能用' : 'Q1 待选'
  const q2 =
    feature.q2 === 1 ? 'Q2 能验证核心假设' : feature.q2 === 0 ? 'Q2 不能验证核心假设' : 'Q2 待选'
  const q3 =
    feature.q3 === 1 ? 'Q3 第一次就需要' : feature.q3 === 0 ? 'Q3 第一次不需要' : 'Q3 待选'
  return [q1, q2, q3].join(' / ')
}

function pushScorePanel(L, s) {
  const total = discoverScore(s)
  const verdict = scoreVerdict(total)
  L.push('- 五维评分：' + total + ' / 25 · ' + verdict.text)
  SCORE_DIMS.forEach((dim) => {
    const idx = s.scores?.[dim.key]
    const opt = dim.opts[idx]
    L.push('- ' + dim.name + '：' + (opt ? opt.label + '（' + opt.v + ' / 5）' : EMPTY))
  })
}

function pushStageS0(L, s, ans) {
  line(L, '具体麻烦', ans.s0_1)
  line(L, '烦恼日记', ans.s0_2)
  line(L, '频率 × 痛苦', ans.s0_3)
  pushScorePanel(L, s)
  line(L, '一句话说清问题', ans.s0_5)
}

function pushStageS4(L, s, ans) {
  line(L, '核心假设', ans.s4_1)
  line(L, '验证成功', ans.s4_2)
  const features = Array.isArray(s.features) ? s.features : []
  if (!features.length) {
    L.push('- 功能清单：' + EMPTY)
  } else {
    L.push('- 功能清单：')
    features.forEach((feature, i) => {
      const verdict = featureVerdict(feature) || '待判定'
      let row =
        '- 功能 ' +
        (i + 1) +
        '：' +
        (text(feature.name) || '（未命名）') +
        ' · ' +
        verdict +
        ' · ' +
        featureAnswer(feature)
      if (text(feature.whyNot)) row += ' · 暂不做原因：' + text(feature.whyNot)
      L.push(row)
    })
  }
  line(L, '不做清单', ans.s4_4)
  line(L, '不做 / 不追', ans.s4_5)
  line(L, '投入产出', s.focusRoi)
}

function pushStageS5(L, s, ans) {
  line(L, '用户是谁', ans.s5_1)
  line(L, '痛点在哪', ans.s5_2)
  line(L, '痛点强度', ans.s5_3)
  line(L, '现有方案', ans.s5_4)
  line(L, '为什么选你', ans.s5_5)
  line(L, '电梯演讲', ans.s5_6)

  const check = s.selfcheck || {}
  L.push('- 红黄绿结论：')
  L.push('- 用户：' + lightText(check.whoLight))
  L.push('- 痛点：' + lightText(check.painLight))
  L.push('- 为什么选你：' + lightText(check.whyYouLight))
  const status = soulState(s)
  if (status === 'ok') L.push('- 结论：可以开始开发，三个问题全绿。')
  else if (status === 'warn') L.push('- 结论：建议补强，有黄灯，可以先做最小实验再补齐。')
  else if (status === 'stop') L.push('- 结论：先停下来，有红灯，回到对应阶段想清楚再往下走。')
  else L.push('- 结论：' + EMPTY)
}

function pushStageS6(L, s, ans) {
  line(L, '场景说明', ans.s6_1)
  const scene = SCENES.find((item) => item.key === s.adapt?.scene)
  line(L, '场景类型', scene ? scene.title + '（' + scene.desc + '）' : '')
  line(L, scene ? scene.q : '场景补充', s.adapt?.answer)
  line(L, '硬约束', ans.s6_2)
  line(L, '重点补充', ans.s6_3)
  line(L, '场景红线', s.adapt?.redline)
  line(L, '验收标准', ans.s6_4)
}

export function docMarkdown(s, mode = s.mode || 'guide') {
  const L = []
  const today = new Date().toLocaleDateString('zh-CN')
  L.push('# ThinkFirst 需求分析')
  L.push('')
  L.push('> 由 ThinkFirst 引导生成 · ' + today)
  L.push('')

  if (mode === 'chat') {
    const ans = s.chatAnswers || {}
    L.push('## 快速判断')
    L.push('')
    line(L, '给谁用', ans.chat_q1)
    line(L, '解决什么', ans.chat_q2)
    line(L, '第一版做什么', ans.chat_q3)
    L.push('')
    if (ans.chat_full1) {
      L.push('## 完整版检查')
      L.push('')
      line(L, '问题定义', ans.chat_full1)
      line(L, '用户理解', ans.chat_full2)
      line(L, '风险预判', ans.chat_full3)
      line(L, 'MVP范围', ans.chat_full4)
      L.push('')
    }
    return L.join('\n')
  }

  const ans = s.answers || {}
  STAGES.forEach((stage, i) => {
    L.push('## ' + CN_NUM[i] + '、' + stage.name)
    L.push('')

    if (stage.key === 's0') pushStageS0(L, s, ans)
    else if (stage.key === 's4') pushStageS4(L, s, ans)
    else if (stage.key === 's5') pushStageS5(L, s, ans)
    else if (stage.key === 's6') pushStageS6(L, s, ans)
    else {
      ;(stage.ids || []).forEach((id) => {
        line(L, Q[id]?.doc || id, ans[id])
      })
    }

    L.push('')
  })

  return L.join('\n')
}

function escapeHtml(value) {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

export function mdToHtml(md) {
  const out = []
  let inList = false

  const closeList = () => {
    if (!inList) return
    out.push('</ul>')
    inList = false
  }

  md.split('\n').forEach((line) => {
    if (line.startsWith('- ')) {
      if (!inList) {
        out.push('<ul>')
        inList = true
      }
      out.push('<li>' + escapeHtml(line.slice(2)) + '</li>')
      return
    }

    closeList()
    if (line.startsWith('### ')) out.push('<h3>' + escapeHtml(line.slice(4)) + '</h3>')
    else if (line.startsWith('## ')) out.push('<h2>' + escapeHtml(line.slice(3)) + '</h2>')
    else if (line.startsWith('# ')) out.push('<h1>' + escapeHtml(line.slice(2)) + '</h1>')
    else if (line.startsWith('> ')) out.push('<blockquote>' + escapeHtml(line.slice(2)) + '</blockquote>')
    else if (line.trim() === '') out.push('<br>')
    else out.push('<p>' + escapeHtml(line) + '</p>')
  })

  closeList()
  return out.join('')
}

export function buildAiPrompt(s, mode = s.mode || 'guide') {
  return '请帮我实现这个需求：\n\n' + docMarkdown(s, mode)
}

export function downloadMarkdown(s, mode = s.mode || 'guide') {
  const source =
    mode === 'chat'
      ? s.chatAnswers?.chat_q3 || s.chatAnswers?.chat_q1 || '需求分析文档'
      : s.answers?.s0_5 || '需求分析文档'
  const name = source.replace(/[\\/:*?"<>|]/g, '').slice(0, 30)
  const blob = new Blob([docMarkdown(s, mode)], { type: 'text/markdown;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = name + '.md'
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  setTimeout(() => URL.revokeObjectURL(url), 1000)
  return name
}

export function copyText(text) {
  if (navigator.clipboard && navigator.clipboard.writeText) {
    return navigator.clipboard.writeText(text).then(
      () => true,
      () => {
        const ta = document.createElement('textarea')
        ta.value = text
        document.body.appendChild(ta)
        ta.select()
        document.execCommand('copy')
        document.body.removeChild(ta)
        return true
      },
    )
  }
  const ta = document.createElement('textarea')
  ta.value = text
  document.body.appendChild(ta)
  ta.select()
  document.execCommand('copy')
  document.body.removeChild(ta)
  return Promise.resolve(true)
}
