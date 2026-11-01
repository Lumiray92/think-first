import { CHECK_ITEMS, SCENES, groupById, stageByNo } from './schema.js'

// ---------------------------------------------------------------------------
// 语料画像：原型阶段用固定画像模拟 RAG 检索到的案例。
// 接真实后端后，这一层换成 step-scoped 检索结果即可，输出契约不变。
// ---------------------------------------------------------------------------

const RISKS = [
  '功能太多做不完，第一版拖成第三版',
  '用起来比现有工具还麻烦，用户试一次就放弃',
  '做完只有自己用，没有真实用户',
  '核心假设没验证就开写',
  '用户群判断错，服务了不会用的人',
  '替代方案已经够用，用户没有迁移理由',
  '关键提醒不准，用户失去信任',
  '数据丢一次，用户再也不来了',
  '高估使用意愿，低估学习成本',
  '没有推广，做完没人知道',
]

const TOP3 = [
  {
    reason: '操作太麻烦，用户试一次就放弃',
    likelihood: '高',
    severity: '高',
    prevention: '首屏只留一个输入框，打开即可用',
  },
  {
    reason: '功能太多做不完',
    likelihood: '高',
    severity: '中',
    prevention: 'P0 不超过 3 个，有不做清单',
  },
  {
    reason: '做完只有自己用',
    likelihood: '中',
    severity: '高',
    prevention: '第一周找 3 个真实用户试用',
  },
]

const PROFILES = [
  {
    id: 'todo',
    keywords: ['待办', 'todo', '清单', '记事', '备忘', '任务'],
    name: '极简待办 TodoFlow',
    user: '28 岁的互联网运营小李',
    situation: '早上刚到工位、临时任务最多的时候',
    problem: '每天要花 10 分钟把微信、便签、邮件里的待办抄到纸上，一天至少 3 次，漏掉就会忘事',
    frequency: '3 次',
    duration: '10 分钟',
    pain: '漏事被领导说',
    alternatives: '便签、微信收藏、纸质清单',
    alternativePain: '分散、会丢、不会提醒',
    task: '把脑子里和聊天里的待办，在 30 秒内收进一个地方',
    outcome: '不再漏事，下班时心里是清的',
    functional: '快速记录、按时提醒',
    emotional: '安心、不焦虑',
    social: '在同事眼里靠谱、不掉链子',
    scene: 'personal',
    p0: ['快速记录', '定时提醒', '本地保存'],
    p1: ['删除待办', '完成勾选'],
    p2: ['分类标签', '搜索', '统计报表', '多人协作'],
    hypothesis: '经常漏事的上班族愿意用一个打开即记的工具，因为它比便签更快、比微信更不容易丢。',
    validation: '2 周内 10 个试用者中至少 6 人连续 7 天每天记录 ≥1 条。',
    constraints: '打开到记完 ≤3 步；手机单手可用；离线可用；不登录。',
    redlines: '不覆盖原始记录；存储失败必须提示；不静默丢数据。',
    acceptance: '从点开到记完 ≤30 秒；连续 7 天使用；无数据丢失。',
    elevator:
      '很多上班族的待办散在微信、便签和纸上，最后漏事。TodoFlow 打开就能记，30 秒收完，到点提醒。',
    context: {
      project: '个人待办工具，第一版只做记录和提醒',
      domain: '上班族临时任务多、待办分散',
      task: '用 Vue 3 写一个免登录的单页应用',
    },
    framework: 'few-shot',
  },
  {
    id: 'medicine',
    keywords: ['吃药', '用药', '药物', '血压'],
    name: '吃药提醒小助手',
    user: '62 岁、只会用微信的妈妈',
    situation: '每天早晚该吃药的时候',
    problem: '总是忘记吃降压药，或者忘记自己吃没吃过',
    frequency: '2 次',
    duration: '5 分钟',
    pain: '血压不稳、家人担心',
    alternatives: '手机闹钟、家人打电话、分格药盒',
    alternativePain: '闹钟关了就当吃过，电话不可能天天打',
    task: '每天到点收到提醒，点一下“已吃”，家人能看到记录',
    outcome: '不漏药，家人放心',
    functional: '定时提醒、一键已吃、记录可查',
    emotional: '安心，不怕忘',
    social: '让子女觉得自己被照顾到了',
    scene: 'family',
    p0: ['定时提醒', '一键已吃', '家人查看记录'],
    p1: ['补吃提醒', '用药备注'],
    p2: ['多药品管理', '统计报表', '医生共享'],
    hypothesis:
      '记性不好的老人愿意用一个“到点响、点一下就好”的提醒，因为它比闹钟更明确、比家人打电话更不打扰。',
    validation: '2 周内妈妈连续 7 天按提醒完成“已吃”，且家人能查到记录。',
    constraints: '字 ≥20px；按钮 ≥屏幕宽 40%；≤3 步；不登录；微信内可用。',
    redlines: '不能把用药记录发给无关的人；提醒失败必须明确提示。',
    acceptance: '连续 7 天按时收到提醒；妈妈能独立完成“已吃”；记录可追溯。',
    elevator:
      '妈妈总是忘记吃降压药。这个小程序每天到点提醒，点一下“已吃”，家人能看到记录。',
    context: {
      project: '家人吃药提醒，第一版只做提醒和已吃记录',
      domain: '老人记性差、不会用复杂 App',
      task: '用微信小程序做一个三步内完成的提醒工具',
    },
    framework: 'few-shot',
  },
  {
    id: 'weekly',
    keywords: ['周报', '汇报', '团队', '收集', '汇总'],
    name: '周报收集器',
    user: '8–12 人小团队的负责人',
    situation: '每周五下午收周报的时候',
    problem: '周报散落在群聊、私聊、文档里，要花 40 分钟收齐并统一格式，还容易漏人',
    frequency: '每周 1 次',
    duration: '40 分钟',
    pain: '漏人被老板说，格式还得手动改',
    alternatives: '飞书表格 + 手动复制',
    alternativePain: '依赖自觉、格式乱、私聊容易漏',
    task: '10 分钟内把所有人的周报收齐并统一格式',
    outcome: '下班前发给老板，不漏人',
    functional: '统一收集、缺人提醒、格式导出',
    emotional: '不再为收周报焦虑',
    social: '在团队和老板面前显得专业、可靠',
    scene: 'auto',
    p0: ['统一收集入口', '缺人提醒', '格式统一导出'],
    p1: ['自动催办', '历史归档'],
    p2: ['数据看板', '跨团队汇总', '权限管理'],
    hypothesis:
      '小团队负责人愿意用一个固定入口收周报，因为它比表格加私聊更省时间、更不容易漏人。',
    validation: '连续 4 周，负责人收齐周报的时间从 40 分钟降到 10 分钟以内，漏人次数为 0。',
    constraints: '先手动跑通再自动化；不覆盖成员原始提交；缺人必须提示。',
    redlines: '不修改成员原始提交；解析失败必须提示；不静默丢数据。',
    acceptance: '10 分钟内收齐；漏人率为 0；格式统一。',
    elevator:
      '小团队负责人每周五都要花 40 分钟收周报，还容易漏人。这个工具用固定入口收齐，自动提醒缺人，10 分钟导出。',
    context: {
      project: '小团队周报收集，第一版只做收集、提醒和导出',
      domain: '周报分散在群聊、私聊和文档里',
      task: '用脚本 + 简单页面跑通一条收集流程',
    },
    framework: 'few-shot',
  },
]

// 「在……的时候」用于「当……在 X 时」句式时，去掉重复的「的时候」
function whenClause(profile) {
  return String(profile.situation || '').replace(/的时候$/, '')
}

function genericProfile(idea) {
  const raw = String(idea || '').trim()
  const name = raw ? (raw.length > 16 ? raw.slice(0, 16) + '…' : raw) : '一个还没想清楚的小工具'
  return {
    id: 'generic',
    keywords: [],
    name,
    user: '一个正在被这个问题困扰的具体用户',
    situation: '每天都会遇到这个问题的时候',
    problem: raw || '每天都要重复处理同一件麻烦事，步骤多、容易漏',
    frequency: '每天 2–3 次',
    duration: '10 分钟',
    pain: '重复、易错、心里不踏实',
    alternatives: '手动处理、现成工具、先忍着',
    alternativePain: '分散、不稳定、不够贴合',
    task: '用更少的步骤把这件事做完',
    outcome: '省下时间，少出错',
    functional: '核心动作、结果保存、失败提示',
    emotional: '省心，不焦虑',
    social: '在同事眼里靠谱',
    scene: 'personal',
    p0: ['核心动作', '结果保存', '失败提示'],
    p1: ['撤销', '历史记录'],
    p2: ['分类', '统计', '多人协作'],
    hypothesis:
      '被这个问题困扰的人愿意用一个更短路径的工具，因为它比现有方案少几步、更不容易出错。',
    validation: '2 周内 10 个试用者中至少 6 人连续 7 天使用。',
    constraints: '打开到完成 ≤3 步；免登录；离线可用。',
    redlines: '不丢数据；失败必须提示。',
    acceptance: '核心动作 ≤3 步完成；连续 7 天使用；无数据丢失。',
    elevator: '很多人都在为“' + (raw || '这件麻烦事') + '”反复花时间。这个工具用最少的步骤把它做完。',
    context: {
      project: raw || '一个还没想清楚的小工具',
      domain: '用户在这个场景下反复手动处理',
      task: '先做一个能跑通核心流程的最小版本',
    },
    framework: 'few-shot',
  }
}

export function pickProfile(idea) {
  const text = String(idea || '').toLowerCase()
  const found = PROFILES.find((profile) => profile.keywords.some((word) => text.includes(word)))
  return found || genericProfile(idea)
}

// ---------------------------------------------------------------------------
// 检索引用：原型里模拟 course07 的命中片段，展示“候选来自哪一条知识”。
// ---------------------------------------------------------------------------

const CITATIONS = {
  1: [
    {
      source: 'course07-Prompt-全流程手册.md',
      step: 1,
      title: '问题发现',
      snippet: '从五个渠道列 10 个烦恼，记 7 天烦恼日记，算频率 × 痛苦，再做五维评分。',
    },
    {
      source: '1-mindset/ch07-problem-discovery',
      step: 1,
      title: '烦恼日记模板',
      snippet: '日期 / 烦恼瞬间 / 频率(1–5) / 痛苦(1–5) / 频率×痛苦。',
    },
  ],
  2: [
    {
      source: 'course07-Prompt-全流程手册.md',
      step: 2,
      title: 'JTBD 任务定义',
      snippet: '当 [用户] 在 [情境] 下，想要 [任务]，以便于 [结果]。',
    },
    {
      source: '1-mindset/ch01-thinking-upgrade/templates/jtbd-task.md',
      step: 2,
      title: 'JTBD 任务模板',
      snippet: '拆功能任务、情感任务、社会任务三层，并写清现有替代方案。',
    },
  ],
  3: [
    {
      source: 'course07-Prompt-全流程手册.md',
      step: 3,
      title: 'Pre-mortem',
      snippet: '假设 3 个月后失败，按需求、技术、场景、习惯四层写 10 条原因，再选 Top3。',
    },
    {
      source: '1-mindset/ch02-inversion-thinking',
      step: 3,
      title: '失败模式库',
      snippet: '功能爆炸、需求脱节、高估使用意愿、低估迁移成本。',
    },
  ],
  4: [
    {
      source: 'course07-Prompt-全流程手册.md',
      step: 4,
      title: '减法思维 / MVP',
      snippet: '用三个问题筛功能：没它能不能用、能否验证假设、首次是否需要。',
    },
    {
      source: '1-mindset/ch03-subtraction-thinking',
      step: 4,
      title: '功能优先级',
      snippet: 'P0 不超过 3–5 个，并写不做的功能、不服务的用户、不追的指标。',
    },
  ],
  5: [
    {
      source: 'course07-Prompt-全流程手册.md',
      step: 5,
      title: '用户的使用过程',
      snippet: '四幕故事、三维画像、旅程四段，找到情绪最低点并转成一条设计改进。',
    },
    {
      source: '1-mindset/ch04-story-thinking',
      step: 5,
      title: '故事化 Prompt 四要素',
      snippet: '我是谁 / 我的现状 / 我的痛点 / 我的期望。',
    },
  ],
  6: [
    {
      source: 'course07-Prompt-全流程手册.md',
      step: 6,
      title: '用户需求确认',
      snippet: '三绿开工；两绿一黄先做最小实验；有红回炉。',
    },
  ],
  7: [
    {
      source: 'course07-Prompt-全流程手册.md',
      step: 7,
      title: '在哪里使用',
      snippet: '数据分析 / 自动化脚本 / 个人工具 / 家人工具，各有硬约束和红线。',
    },
  ],
  8: [
    {
      source: 'course07-Prompt-全流程手册.md',
      step: 8,
      title: '开发前检查',
      snippet: '问题定义、用户理解、风险预判、MVP 范围、用户需求确认、场景约束六项。',
    },
  ],
  9: [
    {
      source: 'course07-Prompt-全流程手册.md',
      step: 9,
      title: '补充项目背景',
      snippet: '把项目背景、领域知识、本次任务分成三层交给 AI。',
    },
  ],
  10: [
    {
      source: 'course07-Prompt-全流程手册.md',
      step: 10,
      title: '告诉 AI 要做什么',
      snippet: 'Zero-shot / Few-shot / CoT / ToT，按任务需要选择。',
    },
  ],
  11: [
    {
      source: 'course07-Prompt-全流程手册.md',
      step: 11,
      title: '高级技巧',
      snippet: '需要固定格式加 Few-shot；多步推理加 CoT；核心代码加 Self-Critique。',
    },
  ],
  12: [
    {
      source: 'course07-Prompt-全流程手册.md',
      step: 12,
      title: '写 PRD',
      snippet: '按成果映射表填 8 个字段，再让 AI 检查遗漏、复述需求。',
    },
    {
      source: 'course07-Prompt-answer.md',
      step: 12,
      title: 'PRD 示例',
      snippet: '一句话描述、目标用户、要解决的问题、核心功能、不做的功能、用户流程、技术约束、成功标准。',
    },
  ],
}

function citationsFor(stageNos) {
  return stageNos
    .map((no) => CITATIONS[no] || [])
    .reduce((all, row) => all.concat(row), [])
    .slice(0, 3)
}

// ---------------------------------------------------------------------------
// 问题与候选
// ---------------------------------------------------------------------------

function stageCandidates(profile, no) {
  if (no === 1) {
    return [
      { id: 'p', text: profile.problem, why: '来自真实的一周烦恼记录' },
      {
        id: 'a',
        text: `每天 ${profile.frequency}，每次 ${profile.duration}，最痛的是${profile.pain}。`,
        why: '把频率和痛苦说清楚',
      },
      {
        id: 'b',
        text: `现在用${profile.alternatives}，但${profile.alternativePain}。`,
        why: '强调现有方案为什么不行',
      },
    ]
  }

  if (no === 2) {
    return [
      {
        id: 'p',
        text: `当${profile.user}在${whenClause(profile)}时，想要${profile.task}，以便${profile.outcome}。`,
        why: '符合 JTBD 的 4+2 句式',
      },
      {
        id: 'a',
        text: `${profile.user}现在靠${profile.alternatives}，问题是${profile.alternativePain}。`,
        why: '从现有替代方案切入',
      },
      { id: 'b', text: `他想完成的是${profile.task}。`, why: '只保留任务，不猜方案' },
    ]
  }

  if (no === 3) {
    return [
      { id: 'p', text: `${RISKS[0]}；${RISKS[1]}；${RISKS[2]}`, why: '按需求/习惯/场景分层' },
      { id: 'a', text: RISKS.slice(0, 5).join('；'), why: '先铺满，再选 Top3' },
      {
        id: 'b',
        text: '需求没验证、用户群判断错、替代方案太强',
        why: '从需求层反向检查',
      },
    ]
  }

  if (no === 4) {
    return [
      {
        id: 'p',
        text: `只做 ${profile.p0.join('、')}，其余全部进不做清单。`,
        why: 'P0 不超过 3～5 个',
      },
      {
        id: 'a',
        text: `先做 ${profile.p0[0]}，验证核心假设后再加 ${profile.p1[0]}。`,
        why: '一次只验证一个假设',
      },
      {
        id: 'b',
        text: `${profile.p0.join('、')}，再加 ${profile.p2[0]}，一次做完整。`,
        why: '完整方案，但风险更高',
      },
    ]
  }

  if (no === 5) {
    return [
      {
        id: 'p',
        text: `${profile.user}，在${profile.situation}，最怕${profile.pain}。`,
        why: '具体到一个人和一个场景',
      },
      {
        id: 'a',
        text: `${profile.user}；表面是${profile.problem}，深层是${profile.pain}。`,
        why: '区分表面痛点和深层动机',
      },
      { id: 'b', text: '先不限定用户，谁都能用。', why: '范围更大，但更难验证' },
    ]
  }

  if (no === 6) {
    return [
      {
        id: 'green',
        text: '我知道给谁用、解决什么问题，也了解他们为什么愿意用。',
        why: '可以直接开工',
      },
      {
        id: 'yellow',
        text: '用户和痛点清楚，为什么选我还说不清。（两绿一黄）',
        why: '建议先做最小实验',
      },
      {
        id: 'red',
        text: '用户是谁还说不清，先别写代码。（有红灯）',
        why: '回到 JTBD 补课',
      },
    ]
  }

  if (no === 7) {
    return SCENES.map((scene) => ({
      id: scene.key,
      text: `${scene.title}：${scene.desc}`,
      why: scene.constraint,
    }))
  }

  if (no === 8) {
    return [
      {
        id: 'go',
        text: '准备好了：知道给谁用、解决什么问题，主要风险有应对办法，第一版只做最需要的功能。',
        why: '可以进入写 PRD',
      },
      {
        id: 'yellow',
        text: '还不太确定有没有人需要，想先找人试用，再决定要不要继续。',
        why: '允许继续，但要带风险',
      },
      {
        id: 'no-go',
        text: '还没想清楚要做哪些功能、可能遇到什么问题，先补充这些。',
        why: '闸门拦截，回到第 4 步',
      },
    ]
  }

  if (no === 9) {
    return [
      {
        id: 'p',
        text: `项目：${profile.context.project}；领域：${profile.context.domain}；任务：${profile.context.task}。`,
        why: '三层上下文齐全',
      },
      {
        id: 'a',
        text: `项目：${profile.context.project}；任务：${profile.context.task}。`,
        why: '先给项目层和任务层',
      },
      { id: 'b', text: '不写上下文，直接把想法丢给 AI。', why: '省事，但 AI 容易自由发挥' },
    ]
  }

  if (no === 10) {
    return [
      { id: 'scaff', text: 'S.C.A.F.F.：完整说明背景、任务、使用者、交付格式和约束，适合开发项目。' },
      { id: 'rgc', text: 'R.G.C.：说明角色、目标和约束，适合简单任务。' },
      { id: 'story', text: '故事化 Prompt：讲清用户处境、问题和期望。' },
      { id: 'universal', text: '万能模板：逐项整理背景、资料、任务与验收要求。' },
    ]
  }

  if (no === 11) {
    return [
      {
        id: 'both',
        text: 'Few-shot + Self-Critique：先给示例，再让 AI 自查一遍。',
        why: '格式和正确性都要',
      },
      { id: 'self', text: '只加 Self-Critique：生成后自查修正。', why: '核心代码/公开文档' },
      { id: 'none', text: '先不加，直接跑第一版。', why: '先看效果，再决定' },
    ]
  }

  return [
    { id: 'ok', text: '准确，可以导出。', why: 'PRD 字段齐全' },
    { id: 'edit', text: '大方向对，细节我再改。', why: '进入编辑模式' },
    { id: 'back', text: '不对，我要回到对应步骤重想。', why: '回炉' },
  ]
}

export function mockQuestionForStage(state, stageNo) {
  const no = Number(stageNo)
  const stage = stageByNo(no)
  const profile = pickProfile([state.idea, ...state.history.map(item => item.answer?.text || '')].join('；'))
  return {
    kind: 'question',
    stage: no,
    stageName: stage.name,
    agent: stage.agent,
    title: stage.name,
    question: stage.driver,
    hint: stage.goal,
    candidates: stageCandidates(profile, no),
    citations: citationsFor([no]),
  }
}

export function mockQuestionForGroup(state, groupId) {
  const group = groupById(groupId)
  const profile = pickProfile([state.idea, ...state.history.map(item => item.answer?.text || '')].join('；'))
  let candidates = []

  if (group.id === 1) {
    candidates = [
      {
        id: 'p',
        text: `我想做一个${profile.name}：${profile.user}在${profile.situation}，${profile.problem}。`,
        why: '把用户、场景、麻烦一次说清',
      },
      {
        id: 'a',
        text: `${profile.problem}，现在用${profile.alternatives}，但${profile.alternativePain}。`,
        why: '从现有方案切入',
      },
      {
        id: 'b',
        text: `帮我做一个${profile.name}，先解决${profile.task}。`,
        why: '一句话版本，细节交给后面',
      },
    ]
  } else if (group.id === 2) {
    candidates = [
      {
        id: 'p',
        text: `最可能死在${RISKS[1]}和${RISKS[0]}。`,
        why: '先抓两个最致命的',
      },
      {
        id: 'a',
        text: '功能太多做不完、没人用、做完只有自己用。',
        why: '需求/习惯/场景三层各一条',
      },
      { id: 'b', text: '还没想过，先做起来再说。', why: '风险未评估，后面会被闸门拦' },
    ]
  } else if (group.id === 3) {
    candidates = stageCandidates(profile, 6)
  } else if (group.id === 4) {
    candidates = stageCandidates(profile, 7)
  } else {
    candidates = [
      {
        id: 'p',
        text: `先跑通「${profile.p0[0]} → ${profile.p0[1]}」这条最短流程。`,
        why: 'P0 只留两个，最快验证',
      },
      {
        id: 'a',
        text: `把 ${profile.p0.join('、')} 都做出来。`,
        why: '完整 P0，风险略高',
      },
      { id: 'b', text: '先做一个能点的静态原型，不接数据。', why: '验证交互，不验证功能' },
    ]
  }

  return {
    kind: 'question',
    group: group.id,
    stage: group.stages[0],
    stageName: group.title,
    agent: stageByNo(group.stages[0]).agent,
    title: group.title,
    question: group.driver,
    hint: group.hint,
    candidates,
    citations: citationsFor(group.stages),
  }
}

// ---------------------------------------------------------------------------
// 应用答案：填槽位
// ---------------------------------------------------------------------------

function checklistFromLights(slots) {
  const answers = slots.s6_answers || {}
  const lights = [answers.who?.light, answers.pain?.light, answers.why?.light]
  const hasRed = lights.includes('red')
  const hasYellow = lights.includes('yellow')
  return {
    problem: true,
    user: answers.who?.light !== 'red',
    risk: true,
    scope: true,
    soul: hasRed ? false : hasYellow ? 'yellow' : true,
    scene: true,
  }
}

export function mockApplyStage(state, stageNo, answer = {}) {
  const no = Number(stageNo)
  const profile = pickProfile([state.idea, ...state.history.map(item => item.answer?.text || '')].join('；'))
  const idx = Number.isInteger(answer.index) ? answer.index : 0
  const text = String(answer.text || '').trim()
  const slots = {}

  if (no === 1) {
    slots.s1_title = profile.name
    slots.s1_problem = idx === 0 ? profile.problem : text || profile.problem
    slots.s1_diary = [
      `周一：${profile.problem}（频率 5 / 痛苦 4）`,
      `周三：又遇到一次，${profile.pain}（频率 4 / 痛苦 4）`,
      `周五：${profile.alternativePain}（频率 3 / 痛苦 3）`,
    ]
    slots.s1_scores = { repeat: 5, rule: 4, verify: 4, sensitive: 4, fault: 3, total: 20 }
    slots.s1_redline = '不碰资金、不存他人隐私、不做需要长期维护的同步服务。'
  }

  if (no === 2) {
    slots.s2_user = profile.user
    slots.s2_jtbd =
      idx === 0
        ? `当${profile.user}在${whenClause(profile)}时，想要${profile.task}，以便${profile.outcome}。`
        : text || `当${profile.user}在${whenClause(profile)}时，想要${profile.task}，以便${profile.outcome}。`
    slots.s2_layers = {
      functional: profile.functional,
      emotional: profile.emotional,
      social: profile.social,
    }
    slots.s2_alternatives = `${profile.alternatives}；问题是${profile.alternativePain}。`
  }

  if (no === 3) {
    slots.s3_failures = RISKS
    slots.s3_top3 = TOP3
    slots.s3_signals = `连续 3 天没有新增使用；试用者第二次不再打开；${profile.pain}再次发生。`
    slots.s3_adjust = '先访谈 3 个试用者；操作问题就简化入口；需求问题就回到第 1 步重选。'
  }

  if (no === 4) {
    slots.s4_hypothesis = profile.hypothesis
    slots.s4_validation = profile.validation
    slots.s4_features = [
      ...profile.p0.map((name) => ({ name, q1: 1, q2: 1, q3: 1, priority: 'P0' })),
      ...profile.p1.map((name) => ({ name, q1: 0, q2: 1, q3: 0, priority: 'P1' })),
      ...profile.p2.map((name) => ({ name, q1: 0, q2: 0, q3: 0, priority: 'P2' })),
    ]
    slots.s4_notdoing = {
      features: profile.p2,
      users: ['团队用户', '非目标场景用户'],
      metrics: ['日活', '下载量'],
    }
    slots.s4_scope_note =
      idx === 2 ? '用户选择了完整方案，风险：第一版范围过大，建议回到减法再砍一次。' : ''
  }

  if (no === 5) {
    slots.s5_profile = {
      surface: `${profile.user}，手机为主`,
      behavior: `在${profile.situation}处理这件事`,
      motive: `怕${profile.pain}，想要掌控感`,
    }
    slots.s5_acts = {
      open: `早上，${profile.user}又遇到：${profile.problem}`,
      conflict: `试过${profile.alternatives}，但${profile.alternativePain}`,
      solve: `打开工具，${profile.p0.join('，')}`,
      ending: profile.outcome,
    }
    slots.s5_journey = [
      {
        stage: '发现',
        action: '同事推荐或搜索',
        touchpoint: '群聊 / 搜索',
        emotion: '好奇',
        pain: '不确定好不好用',
      },
      {
        stage: '首次使用',
        action: '打开即用，不用注册',
        touchpoint: '首屏',
        emotion: '紧张',
        pain: '步骤一多就放弃',
      },
      {
        stage: '核心流程',
        action: `完成「${profile.p0[0]}」`,
        touchpoint: '主界面',
        emotion: '顺畅',
        pain: '失败没有提示',
      },
      {
        stage: '长期使用',
        action: '每天固定时间使用',
        touchpoint: '提醒',
        emotion: '安心',
        pain: '提醒不准会失去信任',
      },
    ]
    slots.s5_lowest = '首次使用——如果要注册或配置，用户可能直接关掉。'
    slots.s5_improvement = '免注册、打开即用、默认焦点在输入框，三步内完成。'
    slots.s5_prompt4 = {
      who: profile.user,
      status: `现在用${profile.alternatives}，${profile.alternativePain}`,
      pain: profile.pain,
      expect: profile.outcome,
    }
  }

  if (no === 6) {
    const light = idx === 0 ? 'green' : idx === 1 ? 'yellow' : 'red'
    slots.s6_answers = {
      who: { answer: profile.user, light: idx === 2 ? 'red' : 'green' },
      pain: {
        answer: `${profile.problem}；深层是${profile.pain}。`,
        light: idx === 2 ? 'yellow' : 'green',
      },
      why: {
        answer: `${profile.p0.join('、')}，比${profile.alternatives}更省步骤。`,
        light,
      },
    }
    slots.s6_elevator = profile.elevator
  }

  if (no === 7) {
    const scene = SCENES[idx] || SCENES.find((item) => item.key === profile.scene) || SCENES[2]
    slots.s7_scene = scene.key
    slots.s7_constraints = `${profile.constraints}；${scene.constraint}`
    slots.s7_redlines = profile.redlines
    slots.s7_acceptance = profile.acceptance
  }

  if (no === 8) {
    const checklist = checklistFromLights(state.slots)
    if (idx === 1) checklist.soul = 'yellow'
    if (idx === 2) {
      checklist.user = false
      checklist.risk = false
    }
    slots.s8_checklist = checklist
    slots.s8_gate =
      checklist.soul === false ? 'No-Go' : checklist.soul === 'yellow' ? '有条件 Go' : 'Go'
  }

  if (no === 9) {
    slots.s9_context = profile.context
  }

  if (no === 10) {
    const framework = ['S.C.A.F.F.', 'R.G.C.', '故事化 Prompt', '万能模板'][idx] || 'S.C.A.F.F.'
    slots.s10_framework = framework
    slots.s10_prompt = [
      `采用框架：${framework}。`,
      '框架结构：S.C.A.F.F. 包含情境、挑战、受众、交付格式、基础约束；R.G.C. 包含角色、目标、约束；故事化包含用户处境、问题和期望；万能模板包含背景、任务、资料、约束、输出和验收。',
      `项目背景：${state.idea}；技术栈未指定时先确认。`,
      '',
      `目标用户：${profile.user}`,
      `要解决的问题：${profile.problem}`,
      `核心功能：${profile.p0.join('、')}`,
      `不做的功能：${profile.p2.join('、')}`,
      `技术约束：${profile.constraints}`,
      `成功标准：${profile.validation}`,
      '',
      '先复述你的理解，再按 P0 分三轮实现。',
    ].join('\n')
  }

  if (no === 11) {
    const techniques =
      idx === 0
        ? ['few-shot', 'self-critique']
        : idx === 1
          ? ['self-critique']
          : []
    slots.s11_techniques = techniques
    const base =
      state.slots.s10_prompt ||
      `你是一个资深产品工程师。请用 Vue 3 实现「${profile.name}」。`
    slots.s11_prompt = techniques.length
      ? `${base}\n\n要求：每个功能先给 2～3 个示例，完成后自查：是否有遗漏、是否有边界情况没写。`
      : base
  }

  if (no === 12) {
    slots.s12_selfcheck = [
      'PRD 是否足够清晰，AI 能准确理解？',
      '有没有哪里 AI 不确定该怎么做？',
      '有没有遗漏的边界情况？',
    ]
    slots.s12_restate = `你要做的是「${profile.name}」，给${profile.user}用，解决${profile.problem}。第一版只做${profile.p0.join('、')}，不做${profile.p2.join('、')}。`
  }

  return slots
}

export function mockApplyGroup(state, groupId, answer = {}) {
  const id = Number(groupId)
  const idx = Number.isInteger(answer.index) ? answer.index : 0
  const profile = pickProfile([state.idea, ...state.history.map(item => item.answer?.text || '')].join('；'))
  const slots = {}

  if (id === 1) {
    Object.assign(slots, mockApplyStage(state, 1, answer))
    Object.assign(slots, mockApplyStage(state, 2, { index: 0 }))
    if (idx !== 0) {
      slots.s1_problem = answer.text || profile.problem
    }
  } else if (id === 2) {
    Object.assign(slots, mockApplyStage(state, 3, answer))
    Object.assign(slots, mockApplyStage(state, 4, { index: idx === 2 ? 2 : 0 }))
  } else if (id === 3) {
    Object.assign(slots, mockApplyStage(state, 5, { index: 0 }))
    Object.assign(slots, mockApplyStage(state, 6, answer))
  } else if (id === 4) {
    Object.assign(slots, mockApplyStage(state, 7, answer))
    Object.assign(slots, mockApplyStage(state, 8, { index: 0 }))
  } else if (id === 5) {
    const s9 = mockApplyStage(state, 9, { index: 0 })
    const s10 = mockApplyStage(state, 10, answer)
    const staged = { ...state, slots: { ...state.slots, ...s9, ...s10 } }
    const s11 = mockApplyStage(staged, 11, { index: idx === 2 ? 2 : 0 })
    const staged2 = { ...staged, slots: { ...staged.slots, ...s11 } }
    const s12 = mockApplyStage(staged2, 12, { index: 0 })
    Object.assign(slots, s9, s10, s11, s12)
  }

  return slots
}

// ---------------------------------------------------------------------------
// 确认卡摘要
// ---------------------------------------------------------------------------

export function mockSummary(state, groupId, applied) {
  const s = { ...state.slots, ...applied }
  const checklist = s.s8_checklist || {}
  const answers = s.s6_answers || {}
  const lightText = { green: '绿', yellow: '黄', red: '红' }

  if (Number(groupId) === 1) {
    return [
      { label: '要解决的问题', value: s.s1_problem || '' },
      { label: '目标用户', value: s.s2_user || '' },
      { label: '用户想完成的事', value: s.s2_jtbd || '' },
    ]
  }

  if (Number(groupId) === 2) {
    const features = Array.isArray(s.s4_features) ? s.s4_features : []
    const p0 = features.filter((feature) => feature.priority === 'P0').map((feature) => feature.name)
    return [
      { label: '核心假设', value: s.s4_hypothesis || '' },
      { label: 'P0 功能', value: p0.join('、') },
      {
        label: '最需要注意的三个问题',
        value: (s.s3_top3 || []).map((risk) => risk.reason).join('；'),
      },
      { label: '不做的功能', value: (s.s4_notdoing?.features || []).join('、') },
    ]
  }

  if (Number(groupId) === 3) {
    return [
      { label: '用户画像', value: s.s5_profile?.surface || '' },
      { label: '用户是谁', value: `${answers.who?.answer || ''}（${lightText[answers.who?.light] || '待判断'}）` },
      { label: '痛点在哪', value: `${answers.pain?.answer || ''}（${lightText[answers.pain?.light] || '待判断'}）` },
      { label: '为什么选你', value: `${answers.why?.answer || ''}（${lightText[answers.why?.light] || '待判断'}）` },
    ]
  }

  if (Number(groupId) === 4) {
    const scene = SCENES.find((item) => item.key === s.s7_scene)
    return [
      { label: '场景', value: scene ? scene.title : '' },
      { label: '硬约束', value: s.s7_constraints || '' },
      { label: '验收标准', value: s.s7_acceptance || '' },
      { label: '开发前检查', value: s.s8_gate || '' },
    ]
  }

  return [
    { label: '提示词框架', value: s.s10_framework || '' },
    { label: '使用技巧', value: (s.s11_techniques || []).map(value => ({'few-shot':'提供示例','self-critique':'生成后自查'}[value] || value)).join('、') },
    { label: 'PRD 自检', value: (s.s12_selfcheck || []).join('；') },
    { label: 'AI 复述', value: s.s12_restate || '' },
  ]
}

export function mockChecklistLabels() {
  return CHECK_ITEMS.map((item) => item.label)
}
