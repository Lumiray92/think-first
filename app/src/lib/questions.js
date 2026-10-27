export const LS_KEY = 'thinkfirst.v5'
export const DIARY_KEY = 'thinkfirst.diary'
export const CASES_KEY = 'thinkfirst.cases'

export const EMPTY_STATE = {
  answers: {},
  chatAnswers: {},
  mode: 'guide',
  stage: 0,
  scores: { repeat: 2, rule: 2, verify: 2, sensitive: 2, fault: 2 },
  features: [],
  focusRoi: '',
  selfcheck: { whoLight: null, painLight: null, whyYouLight: null },
  adapt: { scene: null, answer: '', redline: '' },
  chatHistory: [],
  chatStep: 0,
  chatLights: {},
}

export const Q = {
  s0_1: { doc: '具体麻烦', title: '你观察到的具体麻烦是什么？多久发生一次？', ph: '例：我每天上班要花 10 分钟把微信、便签、邮件里的待办抄到纸上；一天至少 3 次，漏掉就会忘事。' },
  s0_2: { doc: '烦恼日记', title: '烦恼日记里，哪条出现最多、情绪最强？', ph: '例：10/14 便签丢了重写；10/15 微信刷屏漏了领导安排；10/16 纸没带。频率 5，痛苦 4。' },
  s0_3: { doc: '粗略筛选', title: '频率分 × 痛苦分是多少？', ph: '例：5 × 4 = 20，属于很值得解决。' },
  s0_5: { doc: '一句话说清问题', title: '用一句话说清问题', ph: '例：帮我用一句话快速记下待办，并在每天固定时间提醒，做到不丢、不漏。' },
  s1_1: { doc: '谁在什么情境', title: '谁，在什么情境下？', ph: '例：经常被临时任务打断的上班族，在早上刚到工位、事情最多的时候。' },
  s1_2: { doc: '完成什么任务', title: '他想完成什么任务？', ph: '例：把脑子里和聊天里的待办，在 30 秒内收进一个地方。' },
  s1_3: { doc: '结果或感受', title: '为了什么结果或感受？', ph: '例：不再漏事，下班时心里是清的。' },
  s1_4: { doc: '三层任务', title: '功能、情感、社会任务分别是什么？', ph: '例：功能：快速记录、按时提醒；情感：安心、不焦虑；社会：在同事眼里靠谱、不掉链子。' },
  s1_5: { doc: '替代方案', title: '现在用什么替代？问题在哪？', ph: '例：便签、微信收藏、纸质清单；问题是分散、会丢、不会提醒。' },
  s1_6: { doc: '推导功能', title: '这句话能推导出功能吗？', ph: '例：能：快速录入对应输入框和快捷键；不丢对应本地保存；提醒对应定时通知。' },
  s2_1: { doc: '主角是谁', title: '主角是谁？给他名字和一句介绍', ph: '例：小李，28 岁，互联网公司运营，手机里装着 12 个 App，事情一多就靠脑子记。' },
  s2_2: { doc: '四幕故事', title: '四幕故事是什么？', ph: '例：开端：早会下来 5 件事；冲突：便签、微信、纸上都有，下午找不到；解决：一个入口 30 秒记完，晚上统一提醒；结局：一周没漏事，下班能安心关电脑。' },
  s2_3: { doc: '三维画像', title: '表面属性、行为习惯、深层动机？', ph: '例：表面：28 岁、运营、安卓；行为：每天打开微信 50 次、习惯语音；深层：怕被说不靠谱，想要掌控感。' },
  s2_4: { doc: '5 Why', title: '连续问 5 个为什么', ph: '例：为什么记待办？怕忘。为什么怕忘？漏了会被领导说。为什么在意？想被当成靠谱的人。' },
  s2_5: { doc: '旅程与情绪', title: '旅程四段和情绪最低点？', ph: '例：发现：同事推荐；首次：打开就能输入；核心：30 秒记完；长期：每天提醒。最低点在首次——如果要注册，他可能直接关掉。' },
  s2_6: { doc: '对应改进', title: '最低点对应什么改进？', ph: '例：免注册、打开即用、默认焦点在输入框。' },
  s3_1: { doc: '10条原因', title: '假设 3 个月后失败，写 10 条原因', ph: '例：① 功能太多做不完；② 录入太麻烦没人用；③ 提醒不准；④ 数据丢失；⑤ 手机端体验差；⑥ 用户已有微信收藏；⑦ 三分钟热度；⑧ 没有推广；⑨ 只有我一个人用；⑩ 做成了另一个 Notion。' },
  s3_2: { doc: '最致命三条', title: '哪三条最致命？可能性和严重度？', ph: '例：录入麻烦（高/高）、功能太多（高/中）、只有我一个人用（中/高）。' },
  s3_3: { doc: '预防措施', title: '每条怎么预防？谁、何时、多少成本？', ph: '例：录入麻烦→首屏只留输入框，第一周找 3 个同事试用；功能太多→P0 只留 3 个，砍掉分类和统计。' },
  s3_4: { doc: '失败信号', title: '什么信号说明正在失败？', ph: '例：连续 3 天没有新增记录；试用者第二次不再打开；记录后仍靠纸提醒。' },
  s3_5: { doc: '怎么调整', title: '出现信号怎么调整？', ph: '例：先访谈 3 个试用者；若是录入问题就改语音输入；若是需求问题就回 S0 重选。' },
  s4_1: { doc: '核心假设', title: '核心假设一句话', ph: '例：我假设经常漏事的上班族，愿意用打开即记的工具，因为它比便签更快、比微信更不容易丢。' },
  s4_2: { doc: '验证成功', title: '怎么算验证成功？', ph: '例：2 周内 10 个试用者中至少 6 人连续 7 天每天记录 ≥1 条。' },
  s4_4: { doc: '不做清单', title: '哪些功能这版不做？为什么？', ph: '例：P1：分类、搜索；P2：统计、同步、多人协作。它们不影响验证，先不做。' },
  s4_5: { doc: '不做/不追', title: '不做功能、不做用户、不追指标', ph: '例：不做登录、支付、后台；不服务团队用户；不追日活，只看 7 日留存和记录次数。' },
  s5_1: { doc: '用户是谁', title: '用户是谁？说一个具体的人', ph: '例：小李，28 岁运营，每天被临时任务打断，靠脑子记事。' },
  s5_2: { doc: '痛点在哪', title: '痛点是什么？表面和深层分别是什么？', ph: '例：表面是待办分散；深层是怕漏事被说不靠谱。' },
  s5_3: { doc: '痛点强度', title: '痛点强度是多少？', ph: '例：频率 5 × 严重度 4 = 20，很值得解决。' },
  s5_4: { doc: '现有方案', title: '现在怎么解决？为什么不够好？', ph: '例：便签 + 微信收藏 + 纸；分散、会丢、不提醒。' },
  s5_5: { doc: '为什么选你', title: '为什么选你？差异化在哪？', ph: '例：更简单：打开就输入，30 秒记完；更聚焦：只做记录和提醒，不做项目管理。' },
  s5_6: { doc: '电梯演讲', title: '30 秒电梯演讲', ph: '例：很多上班族的待办散在微信、便签和纸上，最后漏事。TodoFlow 打开就能记，30 秒收完，到点提醒。比便签快，比微信不容易丢。' },
  s6_1: { doc: '场景类型', title: '属于哪类场景？为什么？', ph: '例：个人工具，主要是我自己每天用。' },
  s6_2: { doc: '硬约束', title: '这个场景的硬约束是什么？', ph: '例：打开到记完 ≤3 步；手机单手可用；离线可用；不登录。' },
  s6_3: { doc: '重点补充', title: '按场景重点补哪几个问题？', ph: '例：个人工具只保留一个核心功能；不做分类和统计；先自己用一周。' },
  s6_4: { doc: '验收标准', title: '验收标准是什么？', ph: '例：从点开到记完 ≤30 秒；连续 7 天使用；无数据丢失。' },
  s7_1: { doc: '60秒三问', title: '60 秒三问能答吗？', ph: '例：用户：小李这样的上班族；痛点：待办分散会漏；为什么选我：打开即记、30 秒完成。' },
  s7_2: { doc: '故事化Prompt', title: '故事化 Prompt 四要素', ph: '例：身份：28 岁运营；现状：待办散在微信、便签、纸；痛点：漏事被批评；期望：一个入口 30 秒记完并提醒。' },
  s7_3: { doc: '结构化需求', title: '结构化需求六块', ph: '例：背景：待办分散；核心问题：会漏事；只做：输入、保存、提醒；不做：登录、分类、统计；异常：存储失败要提示；验收：30 秒记完、7 天不丢。' },
  s7_4: { doc: '检查清单', title: '开发前检查清单全过吗？', ph: '例：问题真、用户具体、风险有对策、P0 只有 3 个、三问两绿一黄——先做 5 人试用再补差异化。' },
  s8_1: { doc: '实际数据', title: '实际数据是什么？', ph: '例：2 周 10 人试用，6 人连续 7 天记录，日均 2.3 条。' },
  s8_2: { doc: '假设成立吗', title: '假设成立吗？', ph: '例：核心假设成立：打开即记确实被使用；但提醒被忽略。' },
  s8_3: { doc: '下一步', title: '下一步做什么？', ph: '例：保留 P0，改提醒文案和时间；暂不加分类。' },
  s8_4: { doc: '学到什么', title: '学到什么？文档怎么更新？', ph: '例：用户要的是“不漏”，不是“管理”；把这句话写回需求文档。' },
}

export const STAGES = [
  { key: 's0', name: '问题发现', title: '问题发现', special: 's0' },
  { key: 's1', name: '任务定义', title: '任务定义（JTBD）', ids: ['s1_1', 's1_2', 's1_3', 's1_4', 's1_5', 's1_6'] },
  { key: 's2', name: '用户理解', title: '用户理解（故事思维）', ids: ['s2_1', 's2_2', 's2_3', 's2_4', 's2_5', 's2_6'] },
  { key: 's3', name: '风险预判', title: '风险预判（逆向思维）', ids: ['s3_1', 's3_2', 's3_3', 's3_4', 's3_5'] },
  { key: 's4', name: '范围收敛', title: '范围收敛（减法思维）', special: 's4' },
  { key: 's5', name: '灵魂三问', title: '灵魂三问', special: 's5' },
  { key: 's6', name: '场景适配', title: '场景适配', special: 's6' },
  { key: 's7', name: '写 Prompt', title: '写 Prompt', ids: ['s7_1', 's7_2', 's7_3', 's7_4'] },
  { key: 's8', name: '验证复盘', title: '验证复盘', ids: ['s8_1', 's8_2', 's8_3', 's8_4'] },
]

export const SCORE_DIMS = [
  { key: 'repeat', name: '重复性', opts: [{ label: '只一次', v: 1 }, { label: '每季', v: 2 }, { label: '每月', v: 3 }, { label: '每周', v: 4 }, { label: '每天', v: 5 }] },
  { key: 'rule', name: '规则性', opts: [{ label: '完全主观', v: 1 }, { label: '靠经验', v: 2 }, { label: '一半一半', v: 3 }, { label: '大部分固定', v: 4 }, { label: '完全固定', v: 5 }] },
  { key: 'verify', name: '可验证性', opts: [{ label: '完全主观', v: 1 }, { label: '难判断', v: 2 }, { label: '需人工审', v: 3 }, { label: '有检查标准', v: 4 }, { label: '对错分明', v: 5 }] },
  { key: 'sensitive', name: '敏感性', opts: [{ label: '极度敏感', v: 1 }, { label: '高度敏感', v: 2 }, { label: '中等敏感', v: 3 }, { label: '轻微敏感', v: 4 }, { label: '完全不敏感', v: 5 }] },
  { key: 'fault', name: '容错性', opts: [{ label: '不可挽回', v: 1 }, { label: '很严重', v: 2 }, { label: '比较麻烦', v: 3 }, { label: '有点麻烦', v: 4 }, { label: '错了没影响', v: 5 }] },
]

export const SCENES = [
  { key: 'data', title: '数据分析', desc: '先有业务问题，再找数据', q: '受众是谁、支持什么决策、3～5 张图讲完', ph: '例：老板要在周会上解释上月客户流失原因，5 分钟内看完；核心问题是「流失主要集中在哪个渠道」' },
  { key: 'auto', title: '自动化脚本', desc: '频率 × 单次耗时 × 年限', q: '先手动跑通，再自动化；不覆盖原始文件', ph: '例：每天下班前要把 5 个系统的订单导出合并，40 分钟；文件格式变了会出错，希望跳过并提醒我' },
  { key: 'personal', title: '个人工具', desc: '解决自己本周真实痛点', q: '只做一个核心功能；打开到完成 ≤3 步', ph: '例：每次开会记的待办散落在本子、微信、备忘录里；第一版只做「所有待办集中到一个地方」' },
  { key: 'family', title: '家人工具', desc: '老人、小孩、非技术用户', q: '字 ≥20px；按钮 ≥屏幕宽 40%；≤3 步；不登录', ph: '例：我妈 62 岁，只会用微信；我把待办发到家庭群，她点开就能看到今天要做什么' },
]

export function discoverScore(s) {
  let total = 0
  SCORE_DIMS.forEach((d) => {
    const idx = s.scores[d.key]
    if (d.opts[idx]) total += d.opts[idx].v
  })
  return total
}

export function scoreVerdict(total) {
  if (total >= 20) return { cls: 's-ok', text: '很适合（20~25）' }
  if (total >= 15) return { cls: 's-mid', text: '适合（15~19）' }
  if (total >= 10) return { cls: 's-warn', text: '谨慎（10~14）' }
  return { cls: 's-bad', text: '不建议（10 以下）' }
}

export function featureVerdict(f) {
  if (f.q1 === null || f.q2 === null || f.q3 === null) return null
  const sc = (f.q1 === 1 ? 1 : 0) + (f.q2 === 1 ? 1 : 0) + (f.q3 === 1 ? 1 : 0)
  if (sc === 3) return 'P0'
  if (sc > 0) return 'P1'
  return 'P2'
}

export function soulState(s) {
  const sc = s.selfcheck
  const lights = [sc.whoLight, sc.painLight, sc.whyYouLight]
  if (lights.some((l) => !l)) return 'pending'
  if (lights.some((l) => l === 'red')) return 'stop'
  if (lights.some((l) => l === 'yellow')) return 'warn'
  return 'ok'
}

export function stageDone(s, st) {
  if (st.key === 's0') return !!(s.answers.s0_1?.trim() && s.answers.s0_2?.trim() && s.answers.s0_3?.trim() && s.answers.s0_5?.trim())
  if (st.key === 's4') {
    const f = s.features || []
    return !!(s.answers.s4_1?.trim() && s.answers.s4_2?.trim() && f.length > 0 && f.every((x) => x.name.trim() && x.q1 !== null && x.q2 !== null && x.q3 !== null))
  }
  if (st.key === 's5') {
    const sc = s.selfcheck
    return !!(s.answers.s5_1?.trim() && s.answers.s5_2?.trim() && s.answers.s5_5?.trim() && sc.whoLight && sc.painLight && sc.whyYouLight)
  }
  if (st.key === 's6') return !!(s.adapt.scene && s.adapt.answer?.trim() && s.adapt.redline?.trim() && s.answers.s6_1?.trim() && s.answers.s6_3?.trim() && s.answers.s6_4?.trim())
  return (st.ids || []).every((id) => (s.answers[id] || '').trim().length > 0)
}
