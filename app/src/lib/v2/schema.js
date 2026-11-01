// ThinkFirst 2.0 数据层：12 个步骤、槽位、快速模式分组、场景与检查项。
// 所有步骤都执行，只是快速模式把用户交互合并成 5 张确认卡。

export const TOTAL_STAGES = 12

export const SCENES = [
  {
    key: 'data',
    title: '数据分析',
    desc: '先有业务问题，再找数据',
    constraint: '图表少而准、结论先行；3～5 张图讲完',
  },
  {
    key: 'auto',
    title: '自动化脚本',
    desc: '先手动跑通，再自动化',
    constraint: '先自动化最痛的一步；保留原始文件；失败必须提示',
  },
  {
    key: 'personal',
    title: '个人工具',
    desc: '解决自己本周真实痛点',
    constraint: '打开到完成 ≤3 步；免登录；离线可用',
  },
  {
    key: 'family',
    title: '家人工具',
    desc: '老人、小孩、非技术用户',
    constraint: '字 ≥20px；按钮 ≥屏幕宽 40%；≤3 步；不登录',
  },
]

export const CHECK_ITEMS = [
  { key: 'problem', label: '问题定义：真实、可量化、频率够高', returnTo: 1 },
  { key: 'user', label: '用户理解：能说出一个具体的人', returnTo: 2 },
  { key: 'risk', label: '风险预判：最需要注意的三个问题都有对策', returnTo: 3 },
  { key: 'scope', label: 'MVP 范围：P0 ≤ 5 个，有不做清单', returnTo: 4 },
  { key: 'soul', label: '用户需求确认：至少两绿，无红灯', returnTo: 6 },
  { key: 'scene', label: '场景约束：硬约束、红线、验收标准都写了', returnTo: 7 },
]

export const STAGES = [
  {
    no: 1,
    key: 's1',
    name: '问题发现',
    agent: '提问官',
    group: 1,
    goal: '找出够频繁、够痛、够安全的问题',
    driver: '先说说你观察到的具体麻烦：多久发生一次？最痛的是什么？',
    slots: ['s1_title', 's1_problem', 's1_diary', 's1_scores', 's1_redline'],
    retrieval: {
      query: '问题发现 烦恼日记 五维评分 频率 痛苦 红线清单',
      docTypes: ['理论', '模板', '案例'],
    },
  },
  {
    no: 2,
    key: 's2',
    name: 'JTBD 任务定义',
    agent: '提问官',
    group: 1,
    goal: '说清谁、在什么情境、要完成什么任务',
    driver: '谁，在什么情境下，想完成什么任务？',
    slots: ['s2_user', 's2_jtbd', 's2_layers', 's2_alternatives'],
    retrieval: {
      query: 'JTBD 4+2 任务描述 三层任务 功能任务 情感任务 社会任务 替代方案',
      docTypes: ['理论', '模板', '案例'],
    },
  },
  {
    no: 3,
    key: 's3',
    name: '逆向思维 / Pre-mortem',
    agent: '反方',
    group: 2,
    goal: '先假设失败，找出最致命的三个原因',
    driver: '假设 3 个月后这个项目已经失败了，你觉得最可能是什么原因？',
    slots: ['s3_failures', 's3_top3', 's3_signals', 's3_adjust'],
    retrieval: {
      query: '逆向思维 Pre-mortem 失败原因 四层分类 可能性 严重度 预防措施 失败信号',
      docTypes: ['理论', '模板', '案例', '错误'],
    },
  },
  {
    no: 4,
    key: 's4',
    name: '减法思维 / MVP',
    agent: '减法官',
    group: 2,
    goal: '第一版只做什么，明确不做什么',
    driver: '第一版你只想保留哪个功能？',
    slots: ['s4_hypothesis', 's4_validation', 's4_features', 's4_notdoing', 's4_scope_note'],
    retrieval: {
      query: '减法思维 MVP 核心假设 验证标准 功能优先级 P0 P1 P2 不做清单',
      docTypes: ['理论', '模板', '案例'],
    },
  },
  {
    no: 5,
    key: 's5',
    name: '用户的使用过程',
    agent: '提问官',
    group: 3,
    goal: '把用户写成一个具体的人，找到情绪最低点',
    driver: '用户是谁？他在什么情绪下使用？',
    slots: ['s5_profile', 's5_acts', 's5_journey', 's5_lowest', 's5_improvement', 's5_prompt4'],
    retrieval: {
      query: '用户的使用过程 四幕故事 三维画像 用户旅程 情绪最低点 故事化 Prompt 四要素',
      docTypes: ['理论', '模板', '案例'],
    },
  },
  {
    no: 6,
    key: 's6',
    name: '用户需求确认',
    agent: '提问官',
    group: 3,
    gate: true,
    goal: '用户是谁、痛点在哪、为什么选你，三句话能说清吗',
    driver: '用一句话回答三个问题：用户是谁、痛点在哪、为什么选你？',
    slots: ['s6_answers', 's6_elevator'],
    retrieval: {
      query: '用户需求确认 用户是谁 痛点在哪 为什么选你 红黄绿 电梯演讲',
      docTypes: ['理论', '模板', '清单'],
    },
  },
  {
    no: 7,
    key: 's7',
    name: '在哪里使用',
    agent: '提问官',
    group: 4,
    goal: '套用场景模板，写出硬约束、红线和验收标准',
    driver: '这个产品最像哪类场景？',
    slots: ['s7_scene', 's7_constraints', 's7_redlines', 's7_acceptance'],
    retrieval: {
      query: '在哪里使用 数据分析 自动化脚本 个人工具 家人工具 硬约束 红线 验收标准',
      docTypes: ['理论', '模板', '清单'],
    },
  },
  {
    no: 8,
    key: 's8',
    name: '开发前检查',
    agent: 'PRD 写手',
    group: 4,
    gate: true,
    goal: '确认是否准备好开始做第一版',
    driver: '现在可以开始做第一版了吗？',
    slots: ['s8_checklist', 's8_gate'],
    retrieval: {
      query: '开发前检查 项目启动检查清单 Go No-Go 用户需求确认 场景约束',
      docTypes: ['理论', '清单', '模板'],
    },
  },
  {
    no: 9,
    key: 's9',
    name: '补充项目背景',
    agent: 'PRD 写手',
    group: 5,
    goal: '给 AI 三层上下文：项目、领域、任务',
    driver: 'AI 需要知道哪三层背景？',
    slots: ['s9_context'],
    retrieval: {
      query: '补充项目背景 三层上下文 项目上下文 领域上下文 任务上下文',
      docTypes: ['理论', '模板'],
    },
  },
  {
    no: 10,
    key: 's10',
    name: '告诉 AI 要做什么',
    agent: 'PRD 写手',
    group: 5,
    goal: '按决策树选框架，写出第一版 Prompt',
    driver: '你希望 AI 怎么回答？',
    slots: ['s10_framework', 's10_prompt'],
    retrieval: {
      query: 'Prompt 框架 Zero-shot Few-shot CoT ToT 第一版 Prompt 决策树',
      docTypes: ['理论', '模板', '案例'],
    },
  },
  {
    no: 11,
    key: 's11',
    name: '补充回答要求',
    agent: 'PRD 写手',
    group: 5,
    goal: '按需加示例、分步推理、多方案或自查',
    driver: '需要给例子、分步推理、多方案对比，还是让 AI 自查？',
    slots: ['s11_techniques', 's11_prompt'],
    retrieval: {
      query: '高级技巧 Few-shot 示例 CoT 分步推理 ToT 多方案 Self-Critique 自查',
      docTypes: ['理论', '模板', '案例'],
    },
  },
  {
    no: 12,
    key: 's12',
    name: '写 PRD',
    agent: 'PRD 写手',
    group: 5,
    goal: '把前 11 步的产出映射成一份 AI 能执行的任务书',
    driver: '确认一下：这份 PRD 准确吗？',
    slots: ['s12_selfcheck', 's12_restate'],
    retrieval: {
      query: '写 PRD PRD 模板 八字段 成果映射表 PRD 自检 复述需求',
      docTypes: ['理论', '模板', '清单'],
    },
  },
]

export const QUICK_GROUPS = [
  {
    id: 1,
    stages: [1, 2],
    title: '给谁用，解决什么问题',
    driver: '谁会用这个产品？你想帮他解决什么问题？',
    hint: '选一个接近的，也可以自己写。',
  },
  {
    id: 2,
    stages: [3, 4],
    title: '先做什么，担心什么',
    driver: '你最担心这个项目遇到什么问题？',
    hint: '说一个原因就行，我会按需求、技术、场景、习惯四层补全。',
  },
  {
    id: 3,
    stages: [5, 6],
    title: '确认用户需求',
    driver: '用一句话说说：用户是谁、他最痛的是什么、为什么用你的？',
    hint: '说清楚给谁用、解决什么问题、为什么会用。',
  },
  {
    id: 4,
    stages: [7, 8],
    title: '使用方式与准备检查',
    driver: '这个产品最像哪类场景？',
    hint: '选完场景，我会自动套硬约束、红线和验收标准，再过开发前检查。',
  },
  {
    id: 5,
    stages: [9, 10, 11, 12],
    title: '整理需求文档',
    driver: '第一版想先跑通哪条流程？',
    hint: '我会把上下文、Prompt 框架和 PRD 一次成稿。',
  },
]

export function stageByNo(no) {
  return STAGES.find((stage) => stage.no === Number(no)) || STAGES[0]
}

export function groupById(id) {
  return QUICK_GROUPS.find((group) => group.id === Number(id)) || QUICK_GROUPS[0]
}

export function groupOfStage(no) {
  return groupById(stageByNo(no).group)
}

export function nextGroupId(id) {
  const next = Number(id) + 1
  return next <= QUICK_GROUPS.length ? next : null
}

export function stageSlotKeys(no) {
  return stageByNo(no).slots
}

export function createState({ idea = '', mode = 'quick' } = {}) {
  return {
    version: 2,
    idea,
    mode: mode === 'full' ? 'full' : 'quick',
    stage: 1,
    group: 1,
    status: 'idle', // idle | asking | confirming | blocked | done
    slots: {},
    gate: null,
    pending: null,
    history: [],
    updatedAt: Date.now(),
  }
}
