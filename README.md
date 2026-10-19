# ThinkFirst

> 动手之前，先想清楚。

ThinkFirst 是一个**纯静态网页工具**：用 4 个阶段、12 个问题，引导你把一个模糊的点子变成一份可以直接交给 AI 或团队的需求分析文档。不用注册、不上传数据，边填边实时生成文档。

**在线体验：<https://lumiray92.github.io/think-first/>**

## 它解决什么问题

很多人做项目时，会直接对 AI 说「帮我写份需求文档」，然后照着那份看起来很专业的文档开工；两三周后才发现方向从一开始就错了。

ThinkFirst 不替你写答案，只在你写第一行代码之前，逼你把「给谁、解决什么问题、最可能怎么失败、第一版只做什么」想一遍。

## MVP 已实现

- **分阶段引导**：定盘子 → JTBD → Pre-mortem → 功能自检，4 个阶段、12 个问题
- **每个字段都有引导层**：为什么问这个、填错的典型样子、一个填对的例子
- **实时文档预览**：右侧边填边生成，完成后可复制为 AI Prompt 或导出 Markdown
- **本地自动保存**：草稿存在浏览器 localStorage，刷新不丢

## 项目结构

```
think-first/
├── README.md                   # 项目简介与架构
├── docs/
│   └── 需求前分析.md            # JTBD、Pre-mortem 与 MVP 方案
├── mvp/
│   ├── README.md               # MVP 体验指南
│   ├── index.html              # 落地页：失败案例与价值说明
│   ├── guide.html              # 工作台：引导表单 + 实时文档预览
│   ├── done.html               # 完成页：导出、复制与自检
│   ├── shared.js               # 问题数据、阶段定义、文档与 Prompt 生成
│   └── styles.css              # 样式
└── .github/
    └── workflows/pages.yml     # GitHub Pages 自动部署
```

## 项目架构

**纯静态、零依赖、无需构建。**

- **页面流**：`index.html`（价值说明）→ `guide.html`（引导填写）→ `done.html`（导出与自检）
- **数据驱动**：所有问题、阶段和文档模板集中在 `shared.js` 的 `Q` / `STAGES` 中；新增阶段只需加数据，不改页面结构
- **状态管理**：答案写入浏览器 `localStorage`，页面之间共享；没有后端和账号体系
- **文档生成**：前端实时把答案渲染为 Markdown 预览，并生成带硬性约束的 AI Prompt
- **部署**：GitHub Actions 将 `mvp/` 目录发布到 GitHub Pages

## 本地运行

```bash
cd mvp
python -m http.server 8000
# 打开 http://localhost:8000/
```

也可以直接双击 `mvp/index.html` 打开（复制功能在 `file://` 下有降级处理）。

## 文档

- [需求前分析：JTBD / Pre-mortem / MVP 方案](docs/需求前分析.md)
- [MVP 体验指南](mvp/README.md)

## 项目状态

当前处于 **MVP 验证阶段**，只做 P0 功能：分阶段引导、字段引导层、实时预览与导出。账号体系、Agent 引导、多人协作等均刻意不做，取舍理由见[需求前分析](docs/需求前分析.md)中的「不做清单」。
