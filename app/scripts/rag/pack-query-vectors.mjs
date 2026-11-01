import { existsSync, readFileSync, writeFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const HERE = dirname(fileURLToPath(import.meta.url))
const CORPUS_DIR = resolve(HERE, '../../rag/course07')
const QUERY_FILE = resolve(CORPUS_DIR, 'query-map.json')
const BIN_FILE = resolve(CORPUS_DIR, 'query-vectors.f32')
const INDEX_FILE = resolve(CORPUS_DIR, 'query-vectors.index.json')
const ENV_FILE =
  process.env.EMBEDDING_ENV_FILE || resolve(HERE, '../../../../业聪明/.env')

function loadEnvFile(path) {
  if (!existsSync(path)) return {}
  const values = {}
  for (const line of readFileSync(path, 'utf8').split(/\r?\n/)) {
    const trimmed = line.trim()
    if (!trimmed || trimmed.startsWith('#')) continue
    const index = trimmed.indexOf('=')
    if (index < 0) continue
    let value = trimmed.slice(index + 1).trim()
    if (
      (value.startsWith('"') && value.endsWith('"')) ||
      (value.startsWith("'") && value.endsWith("'"))
    ) {
      value = value.slice(1, -1)
    }
    values[trimmed.slice(0, index).trim()] = value
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
const dimension = Number(process.env.EMBEDDING_DIMENSION || envFile.EMBEDDING_DIMENSION || 2048)

if (!apiKey) throw new Error(`缺少 EMBEDDING_API_KEY，已尝试 ${ENV_FILE}`)

const queryMap = JSON.parse(readFileSync(QUERY_FILE, 'utf8'))
const queries = queryMap.queries

async function embedBatch(texts) {
  const response = await fetch(`${apiUrl}/embeddings`, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${apiKey}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ model, input: texts, dimension, encoding_format: 'float' }),
    signal: AbortSignal.timeout(90_000),
  })
  if (!response.ok) {
    throw new Error(`Embedding HTTP ${response.status}: ${(await response.text()).slice(0, 300)}`)
  }
  const payload = await response.json()
  const data = payload.data || []
  if (data.length !== texts.length) {
    throw new Error(`返回数量不一致：请求 ${texts.length}，返回 ${data.length}`)
  }
  return {
    vectors: data.map((item) => item.embedding),
    tokens: Number(payload.usage?.total_tokens || 0),
  }
}

const vectors = []
let totalTokens = 0
for (let start = 0; start < queries.length; start += 10) {
  const batch = queries.slice(start, start + 10)
  const result = await embedBatch(batch.map((item) => item.query))
  vectors.push(...result.vectors)
  totalTokens += result.tokens
}

for (const vector of vectors) {
  if (!Array.isArray(vector) || vector.length !== dimension) {
    throw new Error(`向量维度不一致：期望 ${dimension}`)
  }
}

const buffer = Buffer.allocUnsafe(vectors.length * dimension * 4)
let offset = 0
for (const vector of vectors) {
  for (const value of vector) {
    buffer.writeFloatLE(value, offset)
    offset += 4
  }
}
writeFileSync(BIN_FILE, buffer)
writeFileSync(
  INDEX_FILE,
  JSON.stringify({
    format: 'float32-le',
    dimension,
    count: vectors.length,
    steps: queries.map((item) => item.step),
    names: queries.map((item) => item.name),
    model,
    total_tokens: totalTokens,
    generated_at: new Date().toISOString(),
  }),
  'utf8',
)

console.log(`查询向量: ${BIN_FILE}`)
console.log(`条数: ${vectors.length}，维度: ${dimension}，tokens: ${totalTokens}`)
console.log(`索引: ${INDEX_FILE}`)
