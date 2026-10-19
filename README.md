# ThinkFirst

> 动手之前，先想清楚。

ThinkFirst 是一个**纯静态网页工具**：用 8 个阶段、20 个问题，引导你把一个模糊的点子变成一份可以直接交给 AI 或团队的需求分析文档。不用注册、不上传数据，边填边实时生成文档。

**在线体验：<https://lumiray92.github.io/think-first/>**

## 它解决什么问题

很多人做项目时，会直接对 AI 说「帮我写份需求文档」，然后照着那份看起来很专业的文档开工；两三周后才发现方向从一开始就错了。

ThinkFirst 不替你写答案，只在你写第一行代码之前，逼你把「给谁、解决什么问题、最可能怎么失败、第一版只做什么」想一遍。

## MVP 已实现

- **分阶段引导**：发现 → 定位 → 画像 → 任务 → 风险 → 聚焦 → 自检 → 适配，8 个阶段、20 个问题
- **每个字段都有引导层**：为什么问这个、填错的典型样子、一个填对的例子
- **实时文档预览**：右侧边填边生成，完成后可复制为 AI Prompt 或导出 Markdown
- **本地自动保存**：草稿存在浏览器 localStorage，刷新不丢

> ⚠️ 原型实现的引导范围**超出了方案文档最初定的 P0**（原定只做 JTBD + Pre-mortem 两个模型）。这个范围是否过宽、是否稀释了每个模型的深度，是当前开放讨论的问题之一，见 [Issue #1](https://github.com/Lumiray92/think-first/issues/1)。

## 项目结构

```
think-first/
├── README.md                   # 项目简介与架构
├── docs/
│   └── 需求前分析.md            # JTBD、Pre-mortem 与 MVP 方案
├── mvp/
│   ├── README.md               # MVP 体验指南
│   └── index.html              # 单文件原型：首页 + 工作台 + 完成页
└── .github/
    └── workflows/pages.yml     # GitHub Pages 自动部署
```

## 项目架构

**纯静态、零依赖、无需构建。**

- **单文件**：`mvp/index.html` 一个文件装下整个原型。首页、工作台、完成页是同一页内的三个视图（`showPage()` 切换），样式、脚本、问题数据全部内联，不引用任何外部文件 —— 拷走这一个文件就是完整的 MVP
- **数据驱动**：所有问题、阶段和文档模板集中在文件内的 `Q` / `STAGES` 中；新增阶段只需加数据，不改页面结构
- **状态管理**：答案写入浏览器 `localStorage`，视图之间共享；没有后端和账号体系
- **文档生成**：前端实时把答案渲染为 Markdown 预览，并生成带硬性约束的 AI Prompt
- **部署**：GitHub Actions 将 `mvp/` 目录发布到 GitHub Pages（仓库 Pages 源已设为 **GitHub Actions**，因此 `mvp/` 即站点根）

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

当前处于 **MVP 验证阶段**，原型功能已经完整可用（分阶段引导、字段引导层、实时预览与导出）。账号体系、Agent 对话式引导、多人协作、PDF 导出等均刻意不做，取舍理由见[需求前分析](docs/需求前分析.md)中的「不做清单」。

**开放讨论**：原型实测范围比方案文档最初定的 P0 宽（方案原定只做 JTBD + Pre-mortem 两个模型，原型做成了 8 个阶段）。这是有意扩展还是该收回来，欢迎到 [Issue #1](https://github.com/Lumiray92/think-first/issues/1) 说说你的判断。
