import { readFileSync, statSync, writeFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const HERE = dirname(fileURLToPath(import.meta.url))
const CORPUS_DIR = resolve(HERE, '../../rag/course07')
const CHUNK_FILE = resolve(CORPUS_DIR, 'chunks.jsonl')
const VECTOR_FILE = resolve(CORPUS_DIR, 'embeddings.jsonl')
const BIN_FILE = resolve(CORPUS_DIR, 'embeddings.f32')
const INDEX_FILE = resolve(CORPUS_DIR, 'embeddings.index.json')

function readJsonl(path) {
  return readFileSync(path, 'utf8')
    .split(/\r?\n/)
    .filter(Boolean)
    .map((line) => JSON.parse(line))
}

const chunks = readJsonl(CHUNK_FILE)
const vectors = new Map(readJsonl(VECTOR_FILE).map((row) => [row.id, row.vector]))
const missing = chunks.filter((chunk) => !vectors.has(chunk.id))
if (missing.length) {
  throw new Error(`有 ${missing.length} 个分块缺少向量，例如 ${missing[0].id}`)
}

const dimension = vectors.get(chunks[0].id).length
const buffer = Buffer.allocUnsafe(chunks.length * dimension * 4)
let offset = 0
for (const chunk of chunks) {
  const vector = vectors.get(chunk.id)
  if (vector.length !== dimension) {
    throw new Error(`向量维度不一致: ${chunk.id}`)
  }
  for (const value of vector) {
    buffer.writeFloatLE(value, offset)
    offset += 4
  }
}
writeFileSync(BIN_FILE, buffer)

const index = {
  format: 'float32-le',
  dimension,
  count: chunks.length,
  byte_size: buffer.length,
  ids: chunks.map((chunk) => chunk.id),
  source: 'chunks.jsonl',
  generated_at: new Date().toISOString(),
}
writeFileSync(INDEX_FILE, JSON.stringify(index), 'utf8')

console.log(`向量包: ${BIN_FILE}`)
console.log(`条数: ${chunks.length}，维度: ${dimension}，大小: ${(statSync(BIN_FILE).size / 1024 / 1024).toFixed(1)}MB`)
console.log(`索引: ${INDEX_FILE}`)
