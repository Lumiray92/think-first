import { CASES_KEY } from './questions'

export const CHAT_SCRIPTS = {
  q1: {
    text: '我们先做个 60 秒快速判断。\n\n**1. 这个东西是给谁用的？**\n试着说出一个具体的人，比如“28岁产品经理小李”。',
    next: 'q2',
  },
  q2: {
    text: '好。**2. 它解决什么问题？**\n试着描述一个具体的烦恼场景，比如“每次开会记的待办散落在各处，总是漏事”。',
    next: 'q3',
  },
  q3: {
    text: '**3. 第一版只做什么？**\n试着用一句话说清楚最核心的那 1 个功能，比如“打开即记，晚上统一提醒”。',
    next: 'check1',
  },
  check1: {
    text: '第一轮快问快答结束。\n看看刚才这三个问题，你觉得自己的回答有多清晰？',
    type: 'traffic',
  },
  full1: {
    text: '既然还有时间，我们做完整版检查。\n**【问题定义】**：这个问题来自哪里？它是不是重复出现？能一句话描述吗？',
    next: 'full2',
  },
  full2: {
    text: '**【用户理解】**：用户是谁？他现在是怎么解决的？在什么场景下用？他当时是什么情绪？',
    next: 'full3',
  },
  full3: {
    text: '**【风险预判】**：想过什么情况会导致项目失败吗？有没有对应的避坑措施？你的项目边界是什么？',
    next: 'full4',
  },
  full4: {
    text: '**【MVP范围】**：你的核心假设是什么？怎么验证成功？第一版只做哪 3 个 P0 功能？有不做清单吗？',
    next: 'end',
  },
  end: {
    text: '全部问题已问完！\n你可以点击右侧的“实时文档”查看生成的详细需求，或者把这次记录存进左侧案例库。',
    type: 'result',
  },
}

export const ANSWER_IDS = {
  q1: 'chat_q1',
  q2: 'chat_q2',
  q3: 'chat_q3',
  full1: 'chat_full1',
  full2: 'chat_full2',
  full3: 'chat_full3',
  full4: 'chat_full4',
}

export const TRAFFIC_REPLIES = {
  green: [
    '很好，这三问都清晰，说明这个方向基本能立住。',
    '你现在还有 5 分钟吗？如果有，我们做个完整版检查，把风险降到最低。',
  ],
  yellow: [
    '有黄灯说明方向对了，但话还说不利索。建议再想想，或者看看左侧案例库里的例子。',
    '如果你准备好继续，我们可以试着进入完整版，用更多细节把它磨清楚。',
  ],
  red: [
    '如果有红灯，先停下来，不要硬冲。你可以对照诊断表找找原因：不知道做什么项目就回到烦恼日记；说不清用户是谁就回到三维画像。',
  ],
}

export const DEFAULT_CASES = [
  {
    id: 'case1',
    title: '案例：极简待办提醒（TodoFlow）',
    history: [
      { role: 'agent', text: CHAT_SCRIPTS.q1.text },
      { role: 'user', text: '小周，24岁，互联网公司运营，每天被临时任务打断。' },
      { role: 'agent', text: CHAT_SCRIPTS.q2.text },
      { role: 'user', text: '待办散落在微信、便签和纸上，容易漏事，怕被领导说。' },
      { role: 'agent', text: CHAT_SCRIPTS.q3.text },
      { role: 'user', text: '打开即记，晚上统一提醒。' },
    ],
  },
  {
    id: 'case2',
    title: '案例：提醒妈妈吃药',
    history: [
      { role: 'agent', text: CHAT_SCRIPTS.q1.text },
      { role: 'user', text: '我妈，62岁，不会用复杂的App，只会用微信。' },
      { role: 'agent', text: CHAT_SCRIPTS.q2.text },
      { role: 'user', text: '总是忘记吃降压药，或者忘记自己吃没吃过。' },
      { role: 'agent', text: CHAT_SCRIPTS.q3.text },
      { role: 'user', text: '弄个微信小程序，每天定时弹提醒，点一下“已吃”。' },
    ],
  },
]

export function chatKeyForStep(step) {
  if (step <= 0) return 'q1'
  if (step === 1) return 'q2'
  if (step === 2) return 'q3'
  if (step === 3) return 'check1'
  if (step === 4) return 'full1'
  if (step === 5) return 'full2'
  if (step === 6) return 'full3'
  if (step === 7) return 'full4'
  return 'end'
}

export function readSavedCases() {
  try {
    const raw = JSON.parse(localStorage.getItem(CASES_KEY) || '[]')
    return Array.isArray(raw) ? raw : []
  } catch (e) {
    return []
  }
}

export function saveCase(title, history) {
  const saved = readSavedCases()
  saved.push({ title, history: JSON.parse(JSON.stringify(history)) })
  localStorage.setItem(CASES_KEY, JSON.stringify(saved))
  return saved
}
