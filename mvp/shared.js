/* ==========================================================
   ThinkFirst · 共享数据 + 文档生成
   —— 新增一个阶段 = 往 STAGES 和 Q 里加数据，页面结构不用改
   ========================================================== */

const LS_KEY = 'thinkfirst.mvp.v1';
const EMPTY_STATE = {
  answers: {}, checks: [], openEx: {},
  checker: { name: '', q1: null, q2: null, q3: null }
};

/* ---------- 问题内容 ---------- */
const Q = {
  name: {
    doc: '项目名称', h1: true,
    title: '项目名称',
    hint: '一句话，说清楚「给谁、解决什么问题、产出什么」',
    ph: '例：ThinkFirst —— 10 分钟把点子变成能交给 AI 的需求文档',
    bad: '「AI 效率工具」「智能助手平台」「我的第二个项目」——只有品类词，没有对象、没有场景、没有结果。',
    good: '「ThinkFirst —— 10 分钟把点子变成能交给 AI 的需求文档」。念一遍，能不能回答「谁在什么时候用它、用完得到什么」。'
  },
  audience: {
    doc: '目标用户与要解决的问题',
    title: '它解决的是谁的什么问题？',
    hint: '具体到一个人，不要写「大家」「所有人」',
    ph: '例：给第一次独立用 AI 做完整项目的人（22-30 岁学生 / 独立开发者），「做到一半发现方向错了」是他们的高频恐惧。',
    bad: '「给所有想提升效率的人」「给学生和职场人」——范围大到无法判断真假，也找不到第一个人来试用。',
    good: '「给第一次独立用 AI 做完整项目的人（22-30 岁学生 / 独立开发者）」。你能马上说出三个符合这个描述的人吗？'
  },
  selfuse: {
    doc: '我自己会在什么时刻用它',
    title: '你自己会在什么时刻用它？',
    hint: '写一个具体的使用时刻，不是「随时都可能」',
    ph: '例：每次我冒出一个新点子、准备对 Claude Code 说「帮我做一个 XX」之前。',
    bad: '「我随时都可能用」「有需要的时候」——等于没有场景，你也无法判断某个功能到底该不该做。',
    good: '「每次冒出新点子、准备对 AI 说『帮我做一个 XX』之前」。这句话能不能对应一个每周都会真实发生的情境？'
  },
  jtbd: {
    doc: '核心任务（处境 → 动机 → 结果）',
    title: '当……时，他们想要……，以便于……',
    hint: '按这个句式填，不要改结构',
    ph: '当 刚有一个点子、准备动手之前 时，他们想要 把模糊的想法变成一份清晰的需求文档，以便于 不在错误的方向上浪费时间和 token。',
    bad: '「用户想要一个 AI 需求文档工具」——这是把你的方案当成了用户的需求。方案会变，处境不会。',
    good: '「当刚有一个点子、准备动手之前，他们想要把模糊的想法变成一份清晰的需求文档，以便于不在错误的方向上浪费时间和 token。」'
  },
  alternative: {
    doc: '现有替代方案与不足',
    title: '他们现在是怎么凑合解决的？哪里不够好？',
    hint: '写出至少 2 个替代方案，以及每个方案「不好在哪」',
    ph: '① 直接对 AI 说「帮我写份需求文档」→ 它会照做但容易跑偏，来回改；\n② 搜需求分析模板 → 模板是空的，不知道填到什么程度算对；\n③ 问朋友 → 只会得到「挺好的」。',
    bad: '「他们会自己想办法」「他们没什么好办法」——这说明你没观察过真实用户，只是在想象。',
    good: '「① 直接对 AI 说『帮我写份需求文档』→ 产出跑偏、来回改；② 搜模板 → 空模板不知道填到什么程度算对。」加分项：写下用户「宁可忍受」的那部分。'
  },
  emotion: {
    doc: '用户真正想要的结果',
    title: '他们真正想要的，不是那个工具，而是什么？',
    hint: '写情感层面的结果，不要写功能',
    ph: '例：他们想要的是「我在正确的路上」的确定感，不用再担心做到一半发现全错了。',
    bad: '「他们想要一个方便好用的工具」——这还是功能，不是结果。',
    good: '「他们想要『我在正确路上』的确定感，不用再担心做到一半发现全错了。」假设产品明天消失了，用户会用哪个词形容他失去的东西？'
  },
  failures: {
    doc: '三个月后失败的可能原因',
    title: '现在是 3 个月后，项目彻底失败了。写出 5 条可能的原因',
    hint: '一行一条，先写满 5 条，不要急着筛',
    ph: '① 用户填到第 2 页就跳出，因为前 10 分钟没建立价值感\n② 用户填完从不导出，产出物没有去处\n③ 用户说「这不就是模板吗」\n④ 我自己到第 3 周就厌倦了\n⑤ 阶段划分太细，像流水线',
    bad: '「因为没有推广预算」「因为竞品太多」——全是外部归因，等于承认「我控制不了」，这类原因写了也用不上。',
    good: '「① 用户填到第 2 页就跳出；② 产出物从不被导出；③ 用户说『这不就是模板吗』；④ 我自己第 3 周就厌倦了。」至少写 1 条「关于我自己」的原因。'
  },
  deadliest: {
    doc: '最致命的一条',
    title: '这几条里，哪一条最致命？为什么是它？',
    hint: '选一条，说清楚它为什么会拖垮其他所有事',
    ph: '例：①最致命。如果用户在第 2 页就走了，后面功能做得再好也没人看到——它是唯一能让其他努力全部归零的风险。',
    bad: '「都很重要，都要解决」——这等于没有做判断，也等于第一周你会什么都做一点。',
    good: '「①最致命。如果用户在第 2 页就走了，后面功能做得再好也没人看到。」判断方法：如果只能解决一条，解决哪条能让其他问题变得不那么重要？'
  },
  prevention: {
    doc: '第一周预防措施',
    title: '为了防住它，你第 1 周具体做什么？',
    hint: '写动作，不写愿望。要能在这周内做完、周五能判断做完没有',
    ph: '① 首屏放一个失败案例，而不是流程说明\n② 每个字段加「填错的典型样子」\n③ 本周内找 5 个人用静态页跑一遍，只记录他们卡住的位置',
    bad: '「做好产品体验」「多和用户沟通」——无法执行、无法完成、无法判断做到了没有。',
    good: '「① 首屏放一个失败案例；② 每字段加错例正例；③ 本周找 5 个人跑一遍，只记录卡住的位置。」每条后面加一句：周五我能不能判断它做完了？'
  }
};

/* ---------- 阶段定义（数组顺序 = 页面顺序 = 文档顺序） ---------- */
const STAGES = [
  {
    key: 'basic', name: '定盘子',
    title: '先把「你到底要做什么」钉死',
    desc: '这一步只做一件事：把项目用一句话说清楚。<b>不用想功能、不用想技术栈、不用想怎么推广</b>——那些在这一步都不重要。',
    ids: ['name', 'audience', 'selfuse']
  },
  {
    key: 'jtbd', name: 'JTBD',
    title: '把「功能」翻译成「处境」',
    desc: '功能会过时，处境不会。这一步<b>不要写你的产品有什么功能</b>，只写：用户处在什么处境里、想拿到什么结果、现在是怎么凑合解决的。',
    ids: ['jtbd', 'alternative', 'emotion']
  },
  {
    key: 'premortem', name: 'Pre-mortem',
    title: '假设它已经失败了，把原因挖出来',
    desc: '这一步<b>不需要你乐观，只需要你诚实</b>。事前验尸比事后复盘便宜 100 倍——花 5 分钟想象失败，能省下 3 周的白干。',
    ids: ['failures', 'deadliest', 'prevention']
  },
  {
    key: 'check', name: '功能自检', checker: true,
    title: '用灵魂三问，决定先做什么',
    desc: '写下你想做的功能，用三个问题过一遍。<b>第一版只做 P0</b>——满足其中一两项的放进 P1，其余全部进「不做清单」。',
    ids: []
  }
];

const REQUIRED_IDS = STAGES.filter(s => !s.checker).reduce((a, s) => a.concat(s.ids), []);

/* ---------- 状态读写 ---------- */
function readDraft() {
  try {
    const raw = localStorage.getItem(LS_KEY);
    if (!raw) return JSON.parse(JSON.stringify(EMPTY_STATE));
    return Object.assign(JSON.parse(JSON.stringify(EMPTY_STATE)), JSON.parse(raw));
  } catch (e) { return JSON.parse(JSON.stringify(EMPTY_STATE)); }
}
function writeDraft(s) {
  try { localStorage.setItem(LS_KEY, JSON.stringify(s)); } catch (e) {}
}
function built(s, id) { return ((s.answers || {})[id] || '').trim(); }
function stageDone(s, st) {
  if (st.checker) return (s.checks || []).length > 0;
  return st.ids.every(id => built(s, id).length > 0);
}

/* ---------- 生成需求文档 ---------- */
function docMarkdown(s) {
  const L = [];
  const today = new Date().toLocaleDateString('zh-CN');
  L.push('# ' + (built(s, 'name') || '未命名项目'));
  L.push('');
  L.push('> 一页纸需求分析文档 · 由 ThinkFirst 引导生成 · ' + today);
  L.push('');

  STAGES.forEach((st, i) => {
    L.push('## ' + cn(i + 1) + '、' + st.name);
    L.push('');
    if (st.checker) {
      if ((s.checks || []).length) {
        L.push('| 功能 | Q1 没有它还能用吗 | Q2 能验证核心假设吗 | Q3 第一次就需要吗 | 优先级 |');
        L.push('|---|:---:|:---:|:---:|:---:|');
        s.checks.forEach(c => L.push('| ' + c.name + ' | ' + c.q1 + ' | ' + c.q2 + ' | ' + c.q3 + ' | **' + c.level + '** |'));
      } else {
        L.push('_待补充：把想做的功能用灵魂三问过一遍_');
      }
      L.push('');
      return;
    }
    st.ids.forEach(id => {
      if (Q[id].h1) return;                       // 项目名已经作为 H1
      L.push('### ' + Q[id].doc);
      L.push('');
      L.push(built(s, id) || '_待补充_');
      L.push('');
    });
  });

  L.push('## 下一步');
  L.push('');
  L.push('- 第一版只做 P0，P1 在跑通之后再加，P2 进「不做清单」');
  L.push('- 把这份文档复制成 AI Prompt，交给编码 AI 或团队成员');
  L.push('- 本周内找 5 个目标用户跑一遍流程，只记录他们卡住的位置');
  L.push('');
  return L.join('\n');
}

function cn(n) {
  return ['一', '二', '三', '四', '五', '六', '七', '八', '九', '十'][n - 1] || n;
}

/* ---------- 生成给编码 AI 的 prompt ---------- */
function buildAiPrompt(s) {
  return [
    '你是一名资深产品经理 + 全栈工程师。请根据下面这份需求分析文档，帮我实现这个项目的 MVP。',
    '',
    '【硬性约束】',
    '1. 只做文档里判定为 P0 的功能。不要自作主张增加登录、支付、后台管理、多语言、精美UI等没写进文档的东西。',
    '2. 遇到文档里没写清楚的决策，先问我，不要自己假设。',
    '3. 每完成一个模块，先说明你要做什么、会改动哪些文件，再动手写。',
    '',
    '【交付要求】',
    '- 第一步：先给出模块清单和实现顺序，等我确认后再开始。',
    '- 第二步：逐个实现，每个模块都要能直接跑起来，不要留 TODO 或占位。',
    '- 第三步：给我一份「怎么运行 / 怎么验证」的说明。',
    '',
    '================ 需求分析文档 ================',
    docMarkdown(s),
    '============================================='
  ].join('\n');
}

/* ---------- 极简 Markdown 渲染（预览用） ---------- */
function esc(s) {
  return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}
function inline(s) {
  return esc(s)
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/`(.+?)`/g, '<code>$1</code>')
    .replace(/_(.+?)_/g, '<span class="empty">$1</span>');
}
function mdToHtml(md) {
  const lines = md.split('\n');
  const out = [];
  let i = 0;
  while (i < lines.length) {
    const line = lines[i];
    if (/^###\s/.test(line)) { out.push('<h3>' + inline(line.slice(4)) + '</h3>'); i++; continue; }
    if (/^##\s/.test(line)) { out.push('<h2>' + inline(line.slice(3)) + '</h2>'); i++; continue; }
    if (/^#\s/.test(line)) { out.push('<h1>' + inline(line.slice(2)) + '</h1>'); i++; continue; }
    if (/^>\s/.test(line)) { out.push('<blockquote>' + inline(line.replace(/^>\s?/, '')) + '</blockquote>'); i++; continue; }
    if (/^---/.test(line)) { out.push('<hr>'); i++; continue; }
    if (/^\|/.test(line)) {
      const rows = [];
      while (i < lines.length && /^\|/.test(lines[i])) {
        const cells = lines[i].split('|').slice(1, -1).map(c => c.trim());
        if (!/^[\s:|-]+$/.test(cells.join(''))) rows.push(cells);
        i++;
      }
      if (rows.length) {
        let t = '<table><tr>';
        rows[0].forEach(c => { t += '<th>' + inline(c) + '</th>'; });
        t += '</tr>';
        rows.slice(1).forEach(r => {
          t += '<tr>';
          r.forEach(c => { t += '<td>' + inline(c) + '</td>'; });
          t += '</tr>';
        });
        out.push(t + '</table>');
      }
      continue;
    }
    if (/^-\s/.test(line)) {
      const items = [];
      while (i < lines.length && /^-\s/.test(lines[i])) { items.push(lines[i].slice(2)); i++; }
      out.push('<ul>' + items.map(t => '<li>' + inline(t) + '</li>').join('') + '</ul>');
      continue;
    }
    if (line.trim() === '') { i++; continue; }
    out.push('<p>' + inline(line) + '</p>');
    i++;
  }
  return out.join('\n');
}

/* ---------- 通用工具 ---------- */
function copyText(text) {
  if (navigator.clipboard && navigator.clipboard.writeText) {
    return navigator.clipboard.writeText(text).then(() => true).catch(() => fallbackCopy(text));
  }
  return Promise.resolve(fallbackCopy(text));
}
function fallbackCopy(text) {
  const ta = document.createElement('textarea');
  ta.value = text;
  ta.style.position = 'fixed'; ta.style.top = '-1000px';
  document.body.appendChild(ta); ta.select();
  let ok = false;
  try { ok = document.execCommand('copy'); } catch (e) { ok = false; }
  document.body.removeChild(ta);
  return ok;
}
let __toastTimer;
function toast(msg) {
  const t = document.getElementById('toast');
  if (!t) return;
  t.textContent = msg;
  t.classList.add('is-on');
  clearTimeout(__toastTimer);
  __toastTimer = setTimeout(() => t.classList.remove('is-on'), 4000);
}
function downloadMarkdown(s) {
  const name = (built(s, 'name').replace(/[\\/:*?"<>|]/g, '').slice(0, 30) || '需求分析文档');
  const blob = new Blob([docMarkdown(s)], { type: 'text/markdown;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url; a.download = name + '.md';
  document.body.appendChild(a); a.click(); document.body.removeChild(a);
  setTimeout(() => URL.revokeObjectURL(url), 1000);
  return name;
}