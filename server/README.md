# ThinkFirst Server

ThinkFirst 2.0 的教练后端：登录、API Key 加密托管、course07 RAG 检索、12 步引导、DeepSeek 增强与 PRD 成稿。

## 技术栈

- Java 17 + Spring Boot 3.4
- Spring Web / Spring Security / Spring Data JPA
- JWT（HS256，jjwt 0.11.5）
- H2（本地开发）/ MySQL 8（生产）
- AES-256-GCM（用户 DeepSeek Key 入库加密）
- DeepSeek `deepseek-chat`（用户自己的 Key，失败自动回退模板）

## 本地启动

前置：JDK 17+、Maven 3.9+、Node.js 20.19+ 或 22.12+（前端 Vite 8 的要求）。

```bash
# 1. 后端（默认 H2 文件库，数据在 server/data/）
cd server
mvn spring-boot:run

# 2. 健康检查
curl http://localhost:8080/api/health
```

另开一个终端启动前端：

```bash
cd app
npm install
# 开发时用 Vite 代理 /api -> localhost:8080
VITE_COACH_API=1 npm run dev
```

打开 <http://localhost:5173/ThinkFirst/login>，注册账号后进入 `/v2`。

本地默认值只用于开发：

- JWT 密钥：仓库内的 `thinkfirst-dev-...`
- Key 加密主密钥：仓库内的开发用 Base64
- 数据库：`server/data/thinkfirst.mv.db`

生产环境启动时会由 `ProductionConfigGuard` 拒绝这两项开发默认值。

## 环境变量

Spring Boot 不会自动读取 `.env`。复制 `server/.env.example` 为 `server/.env` 后，在启动前导入：

```bash
cd server
set -a
. ./.env
set +a
java -jar target/thinkfirst-server-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

必填（生产）：

| 变量 | 说明 |
| --- | --- |
| `TF_JWT_SECRET` | JWT 签名密钥，至少 32 字节。生成：`openssl rand -base64 48` |
| `TF_MASTER_KEY` | API Key 入库加密主密钥，32 字节 Base64。生成：`openssl rand -base64 32` |
| `DB_URL` | MySQL JDBC URL |
| `DB_USERNAME` / `DB_PASSWORD` | MySQL 账号；密码不要提交到 Git |
| `TF_CORS_ORIGINS` | 允许的前端源，多个用英文逗号分隔 |

常用可选：

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `SERVER_PORT` | `8080` | 后端端口 |
| `COACH_DATA_DIR` | `../app/rag/course07` | course07 语料目录 |
| `LLM_ENABLED` | `true` | 关闭后所有步骤只用模板 |
| `DEEPSEEK_BASE_URL` | `https://api.deepseek.com` | DeepSeek 地址 |
| `DEEPSEEK_MODEL` | `deepseek-chat` | 模型名 |
| `LLM_TIMEOUT_SECONDS` | `60` | 单次模型调用超时 |
| `LLM_QUESTION_MAX_TOKENS` | `700` | 单步候选改写上限 |
| `LLM_PRD_MAX_TOKENS` | `3000` | PRD 改写上限 |
| `TF_KEY_VALIDATION` | `true` | 保存 Key 时是否调用 DeepSeek 校验 |
| `TF_JWT_EXPIRATION_HOURS` | `168` | 登录有效期 |
| `JPA_DDL_AUTO` | `update` | 生产建议首次启动后改为 `validate` |

## 生产部署

### 1. MySQL

```sql
CREATE DATABASE thinkfirst
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'thinkfirst'@'127.0.0.1' IDENTIFIED BY '<strong-password>';
GRANT ALL PRIVILEGES ON thinkfirst.* TO 'thinkfirst'@'127.0.0.1';
FLUSH PRIVILEGES;
```

`DB_URL` 示例：

```text
jdbc:mysql://127.0.0.1:3306/thinkfirst?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
```

公网连接请追加 `sslMode=REQUIRED`，并配置受信任证书；不要为了省事关掉 TLS。

### 2. 构建与启动

```bash
cd server
mvn -DskipTests package
set -a; . ./.env; set +a
java -jar target/thinkfirst-server-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

首次启动由 JPA 建表。确认表结构后，把 `JPA_DDL_AUTO` 改成 `validate`，后续用迁移脚本管理变更。

### 3. 前端构建

```bash
cd app
VITE_COACH_API=1 npm run build
# 产物在 app/dist/，由 Nginx 托管
```

`vite.config.js` 的 `base` 是 `/ThinkFirst/`。如果部署在域名根路径，改成 `/`。

### 4. Nginx 反向代理

```nginx
server {
    listen 443 ssl http2;
    server_name thinkfirst.example.com;

    ssl_certificate     /etc/letsencrypt/live/thinkfirst.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/thinkfirst.example.com/privkey.pem;

    root /srv/thinkfirst/dist;
    index index.html;

    location /ThinkFirst/ {
        try_files $uri $uri/ /ThinkFirst/index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 90s;
    }
}
```

后端已开启 `server.forward-headers-strategy: framework`。`TF_CORS_ORIGINS` 填 `https://thinkfirst.example.com`。

## API

除健康检查外都需要 `Authorization: Bearer <token>`。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/health` | 健康检查 |
| `POST` | `/api/v1/auth/register` | 注册，返回 JWT |
| `POST` | `/api/v1/auth/login` | 登录 |
| `GET` | `/api/v1/auth/me` | 当前用户 |
| `GET` | `/api/v1/user/api-key` | Key 掩码与更新时间 |
| `PUT` | `/api/v1/user/api-key` | 校验并加密保存 Key |
| `POST` | `/api/v1/user/api-key/validate` | 只校验，不保存 |
| `DELETE` | `/api/v1/user/api-key` | 删除 Key |
| `POST` | `/api/v1/coach/session` | 开始会话，body：`{"idea":"...","mode":"quick"}` |
| `POST` | `/api/v1/coach/session/{id}/turn` | 推进回合，body：`{"type":"confirm","index":0,"text":"...","stage":2}` |
| `GET` | `/api/v1/coach/session/{id}/prd` | 导出 Markdown PRD |
| `POST` | `/api/v1/coach/retrieve` | 单独检索 course07 |

会话绑定创建者；换账号访问同一 session 返回 404。

## Key 安全

- 用户 Key 只在服务端调用 DeepSeek 时解密。
- 入库格式为 AES-256-GCM 密文，`base64(iv):base64(cipher)`，每次随机 IV。
- 接口和界面只返回掩码（如 `sk-****abcd`），不返回明文。
- 日志只记录 userId、步骤和错误类型，不记录 Key。
- `TF_MASTER_KEY` 丢失后旧密文无法解密，必须让用户重新填写 Key；请把它纳入密钥备份。
- 更换 `TF_MASTER_KEY` 前先做数据迁移：用旧密钥解密、再用新密钥加密。
- 项目想法和已确认信息会通过用户自己的 Key 发送给 DeepSeek，设置页已明确告知。

## 成本

每次模型成功调用都会写入 `tf_llm_usage`（用户、用途、步骤、输入/输出 token、时间）。失败回退不产生记录。

```sql
-- 单个用户的累计用量
SELECT purpose,
       COUNT(*)                AS calls,
       SUM(prompt_tokens)      AS prompt_tokens,
       SUM(completion_tokens)  AS completion_tokens
FROM tf_llm_usage
WHERE user_id = ?
GROUP BY purpose;

-- 全站最近 7 天
SELECT DATE(created_at) AS day,
       SUM(prompt_tokens) AS prompt_tokens,
       SUM(completion_tokens) AS completion_tokens
FROM tf_llm_usage
WHERE created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)
GROUP BY DATE(created_at);
```

成本按 DeepSeek 当期单价计算：

```text
成本 = 输入 token / 1,000,000 × 输入单价
     + 输出 token / 1,000,000 × 输出单价
```

实测（2026-10-30，快速模式一次完整会话，`deepseek-chat`）：3 次模型调用，输入 3,435 token、输出 2,282 token。其中第 1 步候选 677/276、第 3 步候选 904/268、PRD 1,854/1,738。

按保守示例单价 ¥2/百万输入、¥8/百万输出计算：

```text
0.003435 × 2 + 0.002282 × 8 ≈ ¥0.025 / 次
10 元 ≈ 400 次快速模式会话
```

如果 DeepSeek 当期单价更低，可跑次数更多；如果后续把更多步骤接入模型、或用户频繁点“换一批”，成本会上升。单价会变，请以 DeepSeek 官网和 `tf_llm_usage` 的实际数据为准。

当前**只记账，不做每日额度硬限制**。正式开放前建议加 Redis/数据库计数器，按用户每日 token 或调用次数封顶。

## 测试

```bash
cd server
mvn -o test          # 后端测试
cd ../app
npm run build        # 前端生产构建
```

## 当前边界

- 只实现第 1–12 步（到 PRD 成稿）；13–16 步通过 `CoachNextPhase` 预留扩展点，本期没有实现，不进入快速模式流程。
- 会话快照保存在数据库（本地 H2 / 生产 MySQL），服务重启后可恢复；内存缓存只用于加速。
- 没有邮箱验证、找回密码和每日额度硬限制；登录、注册和 Key 写入有 10 次/分钟的单实例内存限流，多实例部署需改为 Redis。
- 候选改写只对安全步骤开启；第 6 步灵魂三问、第 7 步场景、第 8 步闸门、第 10–11 步框架仍走确定性模板，避免候选下标与分支语义错位。
