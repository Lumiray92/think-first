# ThinkFirst

> 动手之前，先想清楚。

ThinkFirst 用 course07 的 12 步方法论，把一个模糊的点子逼成一份可以直接交给 AI 或团队的需求文档（PRD）。2.0 版本提供“快速思考”工作台：每一步给 3 个候选，用户点选、语音或直接贴文字即可推进，不用从空白页开始写。

## 2.0 已实现

- **登录与成本可控**：邮箱 + 密码注册登录；每个用户使用自己的 DeepSeek API Key。
- **Key 安全托管**：AES-256-GCM 加密入库，界面只显示 `sk-****abcd`，接口不回传明文，日志不记录 Key。
- **快速模式 12 步不少做**：把 12 步合并成 5 张确认卡，但每一步都执行并落槽；第 6 步灵魂三问、第 8 步 Go/No-Go 闸门保留。
- **course07 RAG**：1005 个切片，DashScope 2048 维向量 + 中文 BM25 + RRF 融合，模板兜底。
- **DeepSeek 增强**：候选改写与 PRD 润色使用用户自己的 Key；调用失败、Key 未设置或输出不合规时自动回退内置模板。
- **PRD 成稿**：固定 12 个章节，支持 Markdown 导出；第 13–16 步通过 `CoachNextPhase` 预留扩展点。
- **会话持久化**：会话快照与消息历史存入 H2/MySQL，服务重启后可恢复；历史列表返回最近 100 条。
- **用量记账**：每次模型调用记录输入/输出 token，便于估算成本。

## 项目结构

```text
ThinkFirst/
├── app/                    # Vue 3 + Vite 前端
│   ├── src/views/v2/       # 快速思考工作台
│   ├── src/views/          # 登录、Key 设置
│   ├── src/lib/v2/         # mock / 远程后端适配器
│   └── rag/course07/       # course07 语料
├── server/                 # Spring Boot 3 后端
│   ├── src/main/java/      # auth / vault / coach / llm
│   └── README.md           # 后端运行、生产部署、API 与安全说明
└── docs/
    └── 需求前分析.md
```

## 本地运行

前置：JDK 17+、Maven 3.9+、Node.js 20.19+ 或 22.12+（Vite 8 的要求）。

```bash
# 终端 1：后端（默认 H2，数据在 server/data/）
cd server
mvn spring-boot:run

# 终端 2：前端（Vite 把 /api 代理到 8080）
cd app
npm install
VITE_COACH_API=1 npm run dev
```

打开 <http://localhost:5173/ThinkFirst/login> 注册账号，进入 `/v2`。
没有设置 Key 时全流程仍可跑通，回合上的来源标记会显示“模板”；在“Key 设置”里保存自己的 DeepSeek Key 后，候选和 PRD 会显示“DeepSeek”。

## 配置与部署

生产环境需要：

- MySQL 8
- 必填 `TF_JWT_SECRET`（至少 32 字节）
- 必填 `TF_MASTER_KEY`（32 字节 Base64）
- `TF_CORS_ORIGINS` 指向前端域名
- HTTPS 反向代理

完整的环境变量表、MySQL 建库、构建命令、Nginx 示例、Key 安全与成本估算见 **[server/README.md](server/README.md)**。

## 测试

```bash
cd server && mvn -o test
cd app && npm run build
```

## 项目状态

2.0 目前覆盖第 1–12 步的快速模式与 PRD 成稿，并支持会话持久化。尚未实现：邮箱验证、找回密码、每日额度硬限制，以及第 13–16 步的实际执行。登录、注册和 Key 写入有 10 次/分钟的单实例内存限流；多实例部署需改为 Redis。取舍理由见[需求前分析](docs/需求前分析.md)。
