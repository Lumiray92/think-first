# ThinkFirst

> 动手之前，先想清楚。

ThinkFirst 是一个**纯前端网页工具**：帮你把一个模糊的点子，推敲成一份可以直接交给 AI 或团队的需求分析文档。不用注册、不上传数据，边填边实时生成文档。

**在线体验：<https://lumiray92.github.io/think-first/>**

## 它解决什么问题

很多人做项目时，会直接对 AI 说「帮我写份需求文档」，然后照着那份看起来很专业的文档开工；两三周后才发现方向从一开始就错了。

ThinkFirst 不替你写答案，只在你写第一行代码之前，逼你把「给谁、解决什么问题、最可能怎么失败、第一版只做什么」想一遍。

## 两种入口

| 模式 | 适合什么时候 | 你会经历什么 |
|---|---|---|
| **快速生成** | 只想先判断方向值不值得做 | 60 秒快问快答，再用红黄绿自评；方向清晰可继续完整版，说不清则回头重想 |
| **逐步思考** | 愿意花十几分钟把需求推敲透 | 9 个阶段、40 多个问题，边填边生成文档，随时可跳转、可回看 |

## 已实现

- **9 个阶段的分阶段引导**：问题发现 → 任务定义（JTBD）→ 用户理解 → 风险预判 → 范围收敛 → 灵魂三问 → 场景适配 → 写 Prompt → 验证复盘
- **每个问题都有示例答案**：不知道怎么写时，照着示例改自己的情况
- **9 阶段完整案例库**：4 个完整案例，可按个人工具 / 家人工具 / 数据分析 / 自动化脚本筛选，逐阶段对照
- **几个会自动算结论的小工具**：
  - 问题发现：五维评分（重复性 / 规则性 / 可验证性 / 敏感性 / 容错性）+ 频率 × 痛苦
  - 范围收敛：功能三问自动判定 **P0 / P1 / P2**
  - 灵魂三问：红黄绿自检，给出「能不能开始开发」的结论
  - 场景适配：4 类场景卡片，切换后追问该场景的专属问题与红线
- **实时文档预览**：可拖动的文档气泡，边填边长
- **烦恼日记**：想不出做什么时，先记一周烦恼
- **本地自动保存**：草稿、日记、案例都存在浏览器 localStorage，刷新不丢
- **出口清晰**：完成后一键复制为 AI Prompt，或导出 Markdown

## 项目结构

```text
think-first/
├── README.md
├── docs/
│   ├── 需求前分析.md              # 问题发现、JTBD、Pre-mortem、场景分析与方案
│   └── 讲解逐字稿.md              # 约 10 分钟的演示讲稿
├── app/                          # 新版应用（Vue 3 + Vite）
│   ├── index.html
│   ├── package.json
│   ├── vite.config.js            # base: /think-first/
│   ├── public/
│   └── src/
│       ├── main.js / App.vue
│       ├── router/index.js       # hash 路由，适配 GitHub Pages
│       ├── views/                # 首页 / 逐步引导 / 快速对话 / 完成页
│       ├── components/           # 案例库、烦恼日记、文档气泡、评分与功能清单等
│       ├── lib/                  # 问题数据、案例数据、对话脚本、文档生成、存储
│       ├── composables/          # 全局会话状态、Toast
│       └── styles/main.css
└── .github/
    └── workflows/pages.yml       # 构建 app/ 并发布 app/dist
```

## 技术栈

- **Vue 3 + Vite + vue-router**，纯前端，无后端、无账号体系
- **数据驱动**：所有问题、阶段、案例、对话脚本分别集中在 `src/lib/questions.js`、`guideCases.js`、`chat.js`；新增阶段只加数据，不改页面骨架
- **状态管理**：一份全局会话状态（`composables/useSession.js`）写入 localStorage，路由切换不丢
- **文档生成**：前端把答案实时渲染成 Markdown，并生成带硬性约束的 AI Prompt
- **路由**：使用 hash 模式（`/#/guide`、`/#/chat`、`/#/done`），GitHub Pages 刷新和深链都不会 404
- **部署**：GitHub Actions 在 `app/` 执行 `npm ci && npm run build`，把 `app/dist` 发布到 GitHub Pages

## 本地运行

需要 Node.js 22 及以上。

```bash
cd app
npm install
npm run dev      # http://localhost:5173/think-first/
```

构建与预览：

```bash
npm run build
npm run preview  # http://localhost:4173/think-first/
```

## 文档

- [需求前分析：问题发现 / 灵魂三问 / 用户故事 / JTBD / Pre-mortem / 场景分析 / MVP 方案](docs/需求前分析.md)
- [讲解逐字稿（约 10 分钟）](docs/讲解逐字稿.md)

## 项目状态

当前处于 **MVP 验证阶段**：两种入口、9 阶段引导、案例库、评分与判定、实时文档、复制 / 导出都已可用。

账号体系、云端同步、多人协作、PDF 导出、真正的 AI Agent 对话等刻意不做，取舍理由见[需求前分析](docs/需求前分析.md)的「不做清单」。

**开放讨论**：新版实现的范围比最初方案定的 P0 宽（原定只做 JTBD + Pre-mortem，现在做成了 9 个阶段 + 快速模式）。这是有意扩展还是该收回来，欢迎到 [Issue #1](https://github.com/Lumiray92/think-first/issues/1) 说说你的判断。
