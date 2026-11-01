import { appendFileSync, existsSync, readFileSync, writeFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const HERE = dirname(fileURLToPath(import.meta.url))
const CORPUS_DIR = resolve(HERE, '../../rag/course07')
const CHUNK_FILE = resolve(CORPUS_DIR, 'chunks.jsonl')
const VECTOR_FILE = resolve(CORPUS_DIR, 'embeddings.jsonl')
const META_FILE = resolve(CORPUS_DIR, 'embeddings.meta.json')
const ENV_FILE =
  process.env.EMBEDDING_ENV_FILE || resolve(HERE, '../../../../业聪明/.env')

const args = new Set(process.argv.slice(2))
const DRY_RUN = args.has('--dry-run')
const FORCE = args.has('--force')
const LIMIT = readNumberArg('--limit')
const BATCH_SIZE = readNumberArg('--batch') || 10

function readNumberArg(name) {
  const prefix = name + '='
  const found = process.argv.find((arg) => arg.startsWith(prefix))
  return found ? Number(found.slice(prefix.length)) : 0
}

function loadEnvFile(path) {
  if (!existsSync(path)) return {}
  const values = {}
  for (const line of readFileSync(path, 'utf8').split(/\r?\n/)) {
    const trimmed = line.trim()
    if (!trimmed || trimmed.startsWith('#')) continue
    const index = trimmed.indexOf('=')
    if (index < 0) continue
    const key = trimmed.slice(0, index).trim()
    let value = trimmed.slice(index + 1).trim()
    if (
      (value.startsWith('"') && value.endsWith('"')) ||
      (value.startsWith("'") && value.endsWith("'"))
    ) {
      value = value.slice(1, -1)
    }
    values[key] = value
  }
  return values
}

const envFile = loadEnvFile(ENV_FILE)
const apiKey = process.env.EMBEDDING_API_KEY || envFile.EMBEDDING_API_KEY || ''
const apiUrl = (
  process.env.EMBEDDING_API_URL ||
  envFile.EMBEDDING_API_URL ||
  'https://dashscope.aliyuncs.com/compatible-mode/v1'
).replace(/\/+$/, '')
const model = process.env.EMBEDDING_API_MODEL || envFile.EMBEDDING_API_MODEL || 'text-embedding-v4'
const dimension = Number(
  process.env.EMBEDDING_DIMENSION || envFile.EMBEDDING_DIMENSION || 2048,
)

if (!apiKey) {
  console.error(`缺少 EMBEDDING_API_KEY。已尝试读取: ${ENV_FILE}`)
  process.exit(1)
}

function readJsonl(path) {
  if (!existsSync(path)) return []
  return readFileSync(path, 'utf8')
    .split(/\r?\n/)
    .filter(Boolean)
    .map((line) => {
      try {
        return JSON.parse(line)
      } catch {
        return null
      }
    })
    .filter(Boolean)
}

function loadMeta() {
  if (!existsSync(META_FILE)) return null
  try {
    return JSON.parse(readFileSync(META_FILE, 'utf8'))
  } catch {
    return null
  }
}

function sameModel(meta) {
  return meta && meta.model === model && Number(meta.dimension) === dimension
}

async function embedBatch(texts, attempt = 1) {
  try {
    const response = await fetch(`${apiUrl}/embeddings`, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${apiKey}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        model,
        input: texts,
        dimension,
        encoding_format: 'float',
      }),
      signal: AbortSignal.timeout(90_000),
    })

    if (!response.ok) {
      const body = await response.text()
      throw new Error(`HTTP ${response.status}: ${body.slice(0, 500)}`)
    }

    const payload = await response.json()
    const data = Array.isArray(payload.data) ? payload.data : []
    if (data.length !== texts.length) {
      throw new Error(`返回数量不一致：请求 ${texts.length}，返回 ${data.length}`)
    }

    const vectors = new Array(texts.length)
    data.forEach((item, fallbackIndex) => {
      const index = Number.isInteger(item.index) ? item.index : fallbackIndex
      const embedding = item.embedding
      if (!Array.isArray(embedding) || embedding.length !== dimension) {
        throw new Error(
          `向量维度不一致：期望 ${dimension}，实际 ${Array.isArray(embedding) ? embedding.length : 'N/A'}`,
        )
      }
      vectors[index] = embedding
    })

    if (vectors.some((vector) => !vector)) {
      throw new Error('返回的向量有空位')
    }

    return {
      vectors,
      tokens: Number(payload.usage?.total_tokens || 0),
    }
  } catch (error) {
    if (attempt >= 3) throw error
    const delay = 1200 * attempt
    console.warn(`  请求失败，${delay}ms 后重试（${attempt}/3）：${error.message}`)
    await new Promise((resolveWait) => setTimeout(resolveWait, delay))
    return embedBatch(texts, attempt + 1)
  }
}

function saveMeta(meta) {
  writeFileSync(META_FILE, JSON.stringify(meta, null, 2), 'utf8')
}

async function main() {
  const chunks = readJsonl(CHUNK_FILE)
  if (!chunks.length) {
    throw new Error(`找不到分块文件: ${CHUNK_FILE}`)
  }

  const meta = loadMeta()
  if (meta && !sameModel(meta) && !FORCE) {
    throw new Error(
      `已有向量使用的模型/维度不同（${meta.model}/${meta.dimension}），加 --force 才会覆盖`,
    )
  }

  // --force：分块内容改过时旧向量全部作废，清空后重新生成
  if (FORCE && !DRY_RUN) writeFileSync(VECTOR_FILE, '')
  const done = new Set(FORCE ? [] : readJsonl(VECTOR_FILE).map((row) => row.id))
  let pending = chunks.filter((chunk) => !done.has(chunk.id))
  if (LIMIT > 0) pending = pending.slice(0, LIMIT)
  if (DRY_RUN) pending = pending.slice(0, 3)

  console.log(`模型: ${model}，维度: ${dimension}，批次: ${BATCH_SIZE}`)
  console.log(`分块总数: ${chunks.length}，已有向量: ${done.size}，待生成: ${pending.length}`)
  if (DRY_RUN) console.log('试运行：只请求前 3 条，不写入文件')

  if (!pending.length) {
    console.log('没有需要生成的分块。')
    return
  }

  let totalTokens = FORCE ? 0 : meta?.total_tokens || 0
  const startedAt = Date.now()

  for (let start = 0; start < pending.length; start += BATCH_SIZE) {
    const batch = pending.slice(start, start + BATCH_SIZE)
    const { vectors, tokens } = await embedBatch(batch.map((chunk) => chunk.embed_text))
    totalTokens += tokens

    if (!DRY_RUN) {
      const lines = batch
        .map((chunk, index) => JSON.stringify({ id: chunk.id, vector: vectors[index] }))
        .join('\n')
      appendFileSync(VECTOR_FILE, lines + '\n', 'utf8')
    }

    const finished = Math.min(start + batch.length, pending.length)
    console.log(
      `  ${finished}/${pending.length}，tokens=${tokens}，累计≈${(totalTokens / 1_000_000) * 0.5} 元`,
    )

    if (!DRY_RUN) {
      saveMeta({
        model,
        dimension,
        chunk_file: 'chunks.jsonl',
        vector_file: 'embeddings.jsonl',
        vector_count: done.size + finished,
        total_tokens: totalTokens,
        updated_at: new Date().toISOString(),
      })
    }

    if (start + BATCH_SIZE < pending.length) {
      await new Promise((resolveWait) => setTimeout(resolveWait, 120))
    }
  }

  const seconds = ((Date.now() - startedAt) / 1000).toFixed(1)
  console.log(`完成：${pending.length} 条，${seconds}s，累计 tokens=${totalTokens}`)
  console.log(`估算费用：${((totalTokens / 1_000_000) * 0.5).toFixed(4)} 元（按 0.5 元/百万 token，实际以账单为准）`)
  if (DRY_RUN) {
    console.log('试运行未写入 embeddings.jsonl')
  } else {
    console.log(`向量文件: ${VECTOR_FILE}`)
  }
}

main().catch((error) => {
  console.error('失败:', error.message)
  process.exit(1)
})
