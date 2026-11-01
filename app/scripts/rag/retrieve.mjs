import { existsSync, readFileSync, writeFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const HERE = dirname(fileURLToPath(import.meta.url))
const CORPUS_DIR = resolve(HERE, '../../rag/course07')
const CHUNK_FILE = resolve(CORPUS_DIR, 'chunks.jsonl')
const VECTOR_FILE = resolve(CORPUS_DIR, 'embeddings.jsonl')
const QUERY_FILE = resolve(CORPUS_DIR, 'query-map.json')
const REPORT_FILE = resolve(CORPUS_DIR, 'retrieval-report.json')
const ENV_FILE =
  process.env.EMBEDDING_ENV_FILE || resolve(HERE, '../../../../业聪明/.env')

const args = process.argv.slice(2)
const getArg = (name) => {
  const prefix = name + '='
  const found = args.find((arg) => arg.startsWith(prefix))
  return found ? found.slice(prefix.length) : null
}
const STEP = Number(getArg('--step') || 0)
const CUSTOM_QUERY = getArg('--query')
const TOP_K = Number(getArg('--top') || 0)
const RUN_ALL = args.includes('--all')
const AS_JSON = args.includes('--json')

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

function readJsonl(path) {
  return readFileSync(path, 'utf8')
    .split(/\r?\n/)
    .filter(Boolean)
    .map((line) => JSON.parse(line))
}

async function embedQuery(text) {
  if (!apiKey) throw new Error(`缺少 EMBEDDING_API_KEY，已尝试 ${ENV_FILE}`)
  const response = await fetch(`${apiUrl}/embeddings`, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${apiKey}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ model, input: [text], dimension: 2048, encoding_format: 'float' }),
    signal: AbortSignal.timeout(60_000),
  })
  if (!response.ok) {
    throw new Error(`Embedding HTTP ${response.status}: ${(await response.text()).slice(0, 300)}`)
  }
  const payload = await response.json()
  const vector = payload.data?.[0]?.embedding
  if (!Array.isArray(vector)) throw new Error('Embedding 返回缺少向量')
  return vector
}

function normalize(text) {
  return String(text || '')
    .toLowerCase()
    .replace(/\s+/g, '')
    .replace(/[，。；、：""''（）()【】[\]{}<>《》!?,.;:'"·…—\-_/]/g, '')
}

function tokenize(text) {
  // S.C.A.F.F. / R.G.C. 这类逐字母缩写在语料里常写作 SCAFF、RGC
  const lower = String(text || '')
    .toLowerCase()
    .replace(/\b(?:[a-z]\.){2,}[a-z]?\.?/g, (match) => match.replace(/\./g, ''))
  const tokens = []
  for (const match of lower.matchAll(/[a-z0-9_.+]+/g)) tokens.push(match[0])
  const chinese = lower.replace(/[^一-龥]+/g, ' ')
  for (const segment of chinese.split(/\s+/)) {
    if (!segment) continue
    if (segment.length === 1) tokens.push(segment)
    for (let index = 0; index < segment.length - 1; index += 1) {
      tokens.push(segment.slice(index, index + 2))
    }
  }
  return tokens
}

function cosine(a, b) {
  let dot = 0
  let normA = 0
  let normB = 0
  for (let i = 0; i < a.length; i += 1) {
    dot += a[i] * b[i]
    normA += a[i] * a[i]
    normB += b[i] * b[i]
  }
  if (!normA || !normB) return 0
  return dot / Math.sqrt(normA * normB)
}

function buildIndex() {
  const chunks = readJsonl(CHUNK_FILE)
  const vectors = new Map(readJsonl(VECTOR_FILE).map((row) => [row.id, row.vector]))
  const docs = chunks
    .filter((chunk) => vectors.has(chunk.id))
    .map((chunk) => {
      const tokens = tokenize(`${chunk.title} ${chunk.heading_path.join(' ')} ${chunk.text}`)
      const termFreq = new Map()
      for (const token of tokens) termFreq.set(token, (termFreq.get(token) || 0) + 1)
      return { chunk, vector: vectors.get(chunk.id), termFreq, length: tokens.length }
    })

  const df = new Map()
  for (const doc of docs) {
    for (const term of doc.termFreq.keys()) df.set(term, (df.get(term) || 0) + 1)
  }

  return { docs, df, avgLength: docs.reduce((sum, doc) => sum + doc.length, 0) / docs.length }
}

function bm25(queryTokens, doc, index) {
  const k1 = 1.2
  const b = 0.75
  let score = 0
  for (const term of queryTokens) {
    const tf = doc.termFreq.get(term)
    if (!tf) continue
    const docFreq = index.df.get(term) || 0
    const idf = Math.log(1 + (index.docs.length - docFreq + 0.5) / (docFreq + 0.5))
    score += idf * ((tf * (k1 + 1)) / (tf + k1 * (1 - b + b * (doc.length / index.avgLength))))
  }
  return score
}

function candidatesFor(index, step) {
  if (!step) return index.docs.filter((doc) => doc.chunk.step_no <= 12)
  const exact = index.docs.filter(
    (doc) => (doc.chunk.step_no === step || doc.chunk.step_no === 0) && !doc.chunk.reserved,
  )
  if (exact.length >= 3) return exact
  return index.docs.filter((doc) => doc.chunk.step_no <= 12)
}

function rank(queryText, queryVector, step, topK, index, preferDocTypes = []) {
  const candidates = candidatesFor(index, step)
  const queryTokens = [...new Set(tokenize(queryText))]

  const vectorRanked = [...candidates]
    .map((doc) => ({ doc, vectorScore: cosine(queryVector, doc.vector) }))
    .sort((a, b) => b.vectorScore - a.vectorScore)

  const lexicalRanked = [...candidates]
    .map((doc) => ({ doc, lexicalScore: bm25(queryTokens, doc, index) }))
    .sort((a, b) => b.lexicalScore - a.lexicalScore)

  const fused = new Map()
  vectorRanked.forEach((item, rankIndex) => {
    fused.set(item.doc.chunk.id, {
      doc: item.doc,
      vectorScore: item.vectorScore,
      lexicalScore: 0,
      score: 1 / (60 + rankIndex + 1),
      vectorRank: rankIndex + 1,
      lexicalRank: null,
    })
  })
  lexicalRanked.forEach((item, rankIndex) => {
    const entry = fused.get(item.doc.chunk.id)
    entry.lexicalScore = item.lexicalScore
    entry.lexicalRank = rankIndex + 1
    entry.score += 1 / (60 + rankIndex + 1)
  })

  const ranked = [...fused.values()].sort((a, b) => b.score - a.score)
  const selected = ranked.slice(0, topK)

  // 需要模板的步骤至少保留一条模板，避免解释性正文把可直接套用的模板挤出 top-k
  if (preferDocTypes.includes('template') && !selected.some((entry) => entry.doc.chunk.doc_type === 'template')) {
    const template = ranked.find((entry) => entry.doc.chunk.doc_type === 'template')
    if (template) selected[selected.length - 1] = template
  }

  return selected
    .map((entry) => ({
      id: entry.doc.chunk.id,
      score: Number(entry.score.toFixed(5)),
      vector_score: Number(entry.vectorScore.toFixed(4)),
      lexical_score: Number(entry.lexicalScore.toFixed(3)),
      vector_rank: entry.vectorRank,
      lexical_rank: entry.lexicalRank,
      title: entry.doc.chunk.title,
      heading: entry.doc.chunk.heading,
      heading_path: entry.doc.chunk.heading_path,
      source: entry.doc.chunk.source,
      step_no: entry.doc.chunk.step_no,
      doc_type: entry.doc.chunk.doc_type,
      text: entry.doc.chunk.text,
    }))
}

function coverage(hits, terms) {
  const haystack = normalize(hits.map((hit) => `${hit.title} ${hit.heading_path.join(' ')} ${hit.text}`).join('\n'))
  const matched = []
  const missing = []
  for (const term of terms || []) {
    if (haystack.includes(normalize(term))) matched.push(term)
    else missing.push(term)
  }
  return {
    matched,
    missing,
    rate: terms?.length ? matched.length / terms.length : 1,
  }
}

function printHits(query, step, hits) {
  console.log(`\n查询${step ? ` S${step}` : ''}: ${query}`)
  hits.forEach((hit, index) => {
    const preview = hit.text.replace(/\s+/g, ' ').slice(0, 90)
    console.log(
      `${String(index + 1).padStart(2)}. [${hit.score.toFixed(5)}] S${hit.step_no} ${hit.title} > ${hit.heading}`,
    )
    console.log(`    ${hit.source} | v=${hit.vector_score} l=${hit.lexical_score}`)
    console.log(`    ${preview}`)
  })
}

async function main() {
  const queryMap = JSON.parse(readFileSync(QUERY_FILE, 'utf8'))
  const index = buildIndex()
  console.log(`索引: ${index.docs.length} 个带向量的分块`)

  if (RUN_ALL) {
    const results = []
    for (const item of queryMap.queries) {
      const vector = await embedQuery(item.query)
      const hits = rank(
        item.query,
        vector,
        item.step,
        item.top_k || 6,
        index,
        item.prefer_doc_types || [],
      )
      const result = coverage(hits, item.must_cover)
      results.push({
        step: item.step,
        name: item.name,
        query: item.query,
        top_k: item.top_k || 6,
        coverage: result,
        hits,
      })
      console.log(
        `S${String(item.step).padStart(2)} ${item.name.padEnd(14)} 覆盖 ${Math.round(result.rate * 100)}%` +
          (result.missing.length ? `  缺: ${result.missing.join('、')}` : ''),
      )
      await new Promise((resolveWait) => setTimeout(resolveWait, 100))
    }

    const average = results.reduce((sum, item) => sum + item.coverage.rate, 0) / results.length
    const report = {
      generated_at: new Date().toISOString(),
      chunks: index.docs.length,
      average_coverage: Number(average.toFixed(4)),
      results,
    }
    writeFileSync(REPORT_FILE, JSON.stringify(report, null, 2), 'utf8')
    console.log(`\n平均覆盖率: ${Math.round(average * 100)}%`)
    console.log(`报告: ${REPORT_FILE}`)
    return
  }

  const item =
    (STEP && queryMap.queries.find((query) => query.step === STEP)) || null
  const query = CUSTOM_QUERY || item?.query
  const step = CUSTOM_QUERY ? STEP || 0 : item?.step || STEP
  if (!query) {
    throw new Error('用法: --step=6 | --query="..." | --all')
  }

  const vector = await embedQuery(query)
  const hits = rank(
    query,
    vector,
    step,
    TOP_K || item?.top_k || 6,
    index,
    item?.prefer_doc_types || [],
  )
  const result = coverage(hits, item?.must_cover)

  if (AS_JSON) {
    console.log(JSON.stringify({ query, step, top_k: hits.length, coverage: result, hits }, null, 2))
    return
  }

  printHits(query, step, hits)
  if (item?.must_cover) {
    console.log(
      `\n关键词覆盖: ${Math.round(result.rate * 100)}%` +
        (result.missing.length ? `，缺少: ${result.missing.join('、')}` : ''),
    )
  }
}

main().catch((error) => {
  console.error('失败:', error.message)
  process.exit(1)
})
