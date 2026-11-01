import { createHash } from 'node:crypto'
import { mkdirSync, readdirSync, readFileSync, statSync, writeFileSync } from 'node:fs'
import { dirname, join, relative, resolve, sep } from 'node:path'
import { fileURLToPath } from 'node:url'

const HERE = dirname(fileURLToPath(import.meta.url))
const OUT_DIR = resolve(HERE, '../../rag/course07')

// 语料目录不写死在本机路径上：同事可能把 course07-Prompt 放在任意位置。
// 用法：node build-corpus.mjs <course07-Prompt 目录>，或先设置环境变量 COURSE07_DIR。
const corpusArg = process.argv[2] || process.env.COURSE07_DIR
if (!corpusArg) {
  console.error(
    '缺少语料目录。请传入路径，例如：\n' +
      '  node scripts/rag/build-corpus.mjs /path/to/course07-Prompt\n' +
      '或设置环境变量 COURSE07_DIR。',
  )
  process.exit(1)
}
const ROOT = resolve(corpusArg)

const MAX_CHUNK = 1100
const TARGET_CHUNK = 800
const MIN_CHUNK = 120

const STEP_BY_CHAPTER = {
  '1-mindset/ch01-thinking-upgrade': 2,
  '1-mindset/ch02-inversion-thinking': 3,
  '1-mindset/ch03-subtraction-thinking': 4,
  '1-mindset/ch04-story-thinking': 5,
  '1-mindset/ch05-soul-questions': 6,
  '1-mindset/ch06-scenario-application': 7,
  '1-mindset/ch07-problem-discovery': 1,
  '1-mindset/ch08-chapter-summary': 8,
  '1-mindset/appendix': 0,
  '2-technique/ch01-vibecoding': 0,
  '2-technique/ch02-basic-prompts': 10,
  '2-technique/ch03': 10,
  '2-technique/ch04': 11,
  '2-technique/ch05': 12,
  '2-technique/ch06': 14,
  '2-technique/ch07': 15,
  '2-technique/ch08': 16,
}

function walk(dir, files = []) {
  let names
  try {
    names = readdirSync(dir).sort()
  } catch (error) {
    // 语料里有无法枚举的空目录/云占位目录，跳过即可（只需要 .md）
    console.warn(`跳过目录 ${dir}: ${error.code || error.message}`)
    return files
  }

  for (const name of names) {
    const full = join(dir, name)
    let info
    try {
      info = statSync(full)
    } catch {
      continue
    }
    if (info.isDirectory()) {
      walk(full, files)
    } else if (name.toLowerCase().endsWith('.md')) {
      files.push(full)
    }
  }
  return files
}

function classify(rel) {
  const posix = rel.split(sep).join('/')

  if (posix === 'course07-Prompt-全流程手册.md') {
    return { doc_type: 'manual', chapter: 'manual', step_no: 0, stage: 'overview' }
  }
  if (posix === 'answer.md') {
    return { doc_type: 'answer', chapter: 'example', step_no: 0, stage: 'overview' }
  }

  const parts = posix.split('/')
  const chapterKey = parts.slice(0, 2).join('/')
  let stepNo = STEP_BY_CHAPTER[chapterKey] ?? 0
  let docType = posix.startsWith('1-mindset/') ? 'mindset' : 'technique'

  if (/\/templates?\//.test(posix) || /\/prompt-templates\//.test(posix) || /\/tecs-template\//.test(posix)) {
    docType = 'template'
  }
  if (/\/prd-templates\//.test(posix) || /\/projs\//.test(posix)) {
    docType = 'template'
  }
  if (/\/appendix\//.test(posix)) docType = 'appendix'
  if (posix.includes('answer') || posix.includes('case-stud')) docType = 'case'

  // 三轮开发（第 13 步）的模板单独归位，避免被 PRD 覆盖
  if (/\/prd-templates\/three-stages\//.test(posix) || /\/projs\/stage\//.test(posix)) {
    stepNo = 13
  }

  return {
    doc_type: docType,
    chapter: chapterKey,
    step_no: stepNo,
    stage: stageOf(stepNo),
  }
}

function stageOf(stepNo) {
  if (stepNo >= 1 && stepNo <= 8) return 'mindset'
  if (stepNo >= 9 && stepNo <= 12) return 'technique'
  if (stepNo >= 13 && stepNo <= 16) return 'build'
  return 'overview'
}

function cleanMarkdown(raw) {
  return raw
    .replace(/\r\n/g, '\n')
    .replace(/^\[toc\]\s*$/gim, '')
    .replace(/!\[[^\]]*\]\([^)]*\)/g, '')
    .replace(/<!--[\s\S]*?-->/g, '')
    .replace(/^\s*[-*]\s*\[[^\]]*\]\(#[^)]*\)\s*$/gim, '')
    .replace(/\n{3,}/g, '\n\n')
    .trim()
}

function isLowValue(text) {
  if (!text) return true
  const linkOnly = text.replace(/\[[^\]]*\]\([^)]*\)/g, '').replace(/[\s|>-]/g, '')
  return linkOnly.length < 40
}

function stepFromHeading(title, path, meta) {
  const match = title.match(/第\s*(\d+)\s*步/)
  if (match) return Number(match[1])

  // answer.md 的九个段落直接对应流程步骤，作为 few-shot 示例
  if (meta.doc_type === 'answer') {
    const answerSteps = [
      [/问题发现/, 1],
      [/任务定义|JTBD/i, 2],
      [/用户理解|故事思维/, 5],
      [/风险预判|逆向思维/, 3],
      [/范围收敛|减法思维/, 4],
      [/灵魂三问/, 6],
      [/场景适配/, 7],
      [/写\s*Prompt|提示词/, 10],
      [/验证复盘/, 16],
    ]
    for (const [pattern, step] of answerSteps) {
      if (pattern.test(title)) return step
    }
  }

  // 提示词基础里「上下文为王」整段属于第 9 步，其余属于第 10 步
  if (meta.chapter === '2-technique/ch02-basic-prompts') {
    const full = [...path, title].join(' ')
    if (/Context is King|上下文|三层上下文|项目上下文|任务上下文|对话上下文/i.test(full)) {
      return 9
    }
  }

  return null
}

function splitFrontmatter(raw) {
  const match = raw.match(/^---\n([\s\S]*?)\n---\n?/)
  if (!match || !/^\s*title\s*:/m.test(match[1])) {
    return { body: raw, title: '' }
  }
  const title = (match[1].match(/^\s*title\s*:\s*(.+)$/m) || [])[1]?.trim() || ''
  return { body: raw.slice(match[0].length), title }
}

function parseSections(text, fallbackTitle = '正文') {
  const lines = text.split('\n')
  const sections = []
  let stack = []
  let current = {
    heading: fallbackTitle,
    path: fallbackTitle === '正文' ? [] : [fallbackTitle],
    lines: [],
  }

  const flush = () => {
    const body = cleanMarkdown(current.lines.join('\n'))
    if (body) {
      sections.push({ heading: current.heading, path: current.path, body })
    }
  }

  for (const line of lines) {
    const match = line.match(/^(#{1,3})\s+(.+?)\s*$/)
    if (!match) {
      current.lines.push(line)
      continue
    }

    flush()
    const level = match[1].length
    stack = stack.filter((item) => item.level < level)
    stack.push({ level, title: match[2].trim() })
    current = {
      heading: match[2].trim(),
      path: stack.map((item) => item.title),
      lines: [],
    }
  }

  flush()

  if (sections.length <= 1) {
    const chinese = parseChineseSections(text)
    if (chinese.length >= 3) return chinese
  }

  return sections
}

// 像 answer.md 这种没有 Markdown 标题、只用「一、二、三」分段的语料
function parseChineseSections(text) {
  const sections = []
  let current = { heading: '正文', path: [], lines: [] }

  const flush = () => {
    const body = cleanMarkdown(current.lines.join('\n'))
    if (body) {
      sections.push({ heading: current.heading, path: current.path, body })
    }
  }

  for (const line of text.split('\n')) {
    const match = line.match(/^\s*([一二三四五六七八九十]+)、\s*(.+?)\s*$/)
    if (!match) {
      current.lines.push(line)
      continue
    }
    flush()
    const heading = `${match[1]}、${match[2]}`
    current = { heading, path: [heading], lines: [] }
  }

  flush()
  return sections
}

function splitText(text) {
  if (text.length <= MAX_CHUNK) return [text]

  const paragraphs = text.split(/\n\n+/).map((part) => part.trim()).filter(Boolean)
  const groups = []
  let buffer = ''

  const pushBuffer = () => {
    if (buffer.trim()) groups.push(buffer.trim())
    buffer = ''
  }

  for (const paragraph of paragraphs) {
    if (paragraph.length > MAX_CHUNK) {
      pushBuffer()
      const sentences = paragraph.split(/(?<=[。！？!?])\s*/)
      let sentenceBuffer = ''
      for (const sentence of sentences) {
        if ((sentenceBuffer + sentence).length > TARGET_CHUNK) {
          if (sentenceBuffer.trim()) groups.push(sentenceBuffer.trim())
          sentenceBuffer = ''
        }
        sentenceBuffer += sentence
      }
      if (sentenceBuffer.trim()) groups.push(sentenceBuffer.trim())
      continue
    }

    if (buffer && (buffer + '\n\n' + paragraph).length > TARGET_CHUNK) {
      pushBuffer()
    }
    buffer = buffer ? buffer + '\n\n' + paragraph : paragraph
  }

  pushBuffer()

  // 代码块/表格往往没有句号，按段落切完仍可能超长，再按行兜底
  const result = []
  for (const group of groups) {
    if (group.length <= MAX_CHUNK) {
      result.push(group)
      continue
    }
    let lineBuffer = ''
    for (const line of group.split('\n')) {
      if (!lineBuffer && line.length > MAX_CHUNK) {
        for (let offset = 0; offset < line.length; offset += TARGET_CHUNK) {
          result.push(line.slice(offset, offset + TARGET_CHUNK))
        }
        continue
      }
      if (lineBuffer && (lineBuffer + '\n' + line).length > TARGET_CHUNK) {
        result.push(lineBuffer)
        lineBuffer = ''
      }
      lineBuffer = lineBuffer ? lineBuffer + '\n' + line : line
    }
    if (lineBuffer.trim()) result.push(lineBuffer.trim())
  }
  return result
}

function chunkHash(source, headingPath, part) {
  return createHash('sha1').update(`${source}|${headingPath.join(' > ')}|${part}`).digest('hex').slice(0, 16)
}

function mergeSmallChunks(chunks) {
  const merged = []
  for (const chunk of chunks) {
    const previous = merged[merged.length - 1]
    const mergeable =
      previous &&
      previous.source === chunk.source &&
      previous.step_no === chunk.step_no &&
      chunk.text.length < MIN_CHUNK &&
      previous.text.length + chunk.text.length <= MAX_CHUNK

    if (mergeable) {
      previous.text = `${previous.text}\n\n${chunk.text}`
      previous.char_count = previous.text.length
    } else {
      merged.push(chunk)
    }
  }

  // 只剩占位符的小块没有检索价值（模板除外：占位符本身就是模板内容）
  return merged.filter(
    (chunk) =>
      chunk.doc_type === 'template' ||
      chunk.text.length >= 50 ||
      !/^[[（(【]/.test(chunk.text),
  )
}

function buildFileChunks(fullPath) {
  const rel = relative(ROOT, fullPath).split(sep).join('/')
  const meta = classify(rel)
  const raw = readFileSync(fullPath, 'utf8')
  const { body, title: frontmatterTitle } = splitFrontmatter(raw)
  const firstHeading = (body.match(/^#\s+(.+?)\s*$/m) || [])[1] || ''
  const fallbackTitle = rel.split('/').pop().replace(/\.md$/i, '')
  const docTitle = frontmatterTitle || firstHeading || fallbackTitle
  const sections = parseSections(body, docTitle)
  const chunks = []
  // 同一文件里可能出现两个同名小节（模板 + 填写示例），只靠标题路径会撞 id
  const usedIds = new Map()

  for (const section of sections) {
    // 模板类文档几乎全是占位符，段落很短但正是要检索的内容，不能按正文长度过滤
    if (meta.doc_type !== 'template' && isLowValue(section.body)) continue

    const headingStep = stepFromHeading(section.heading, section.path, meta)
    const stepNo = headingStep == null ? meta.step_no : headingStep
    // 模板合并后只剩正文会丢掉字段名（目标用户/P0/成功标准），把标题写回正文
    const sectionText =
      meta.doc_type === 'template' ? `## ${section.heading}\n\n${section.body}` : section.body
    const parts = splitText(sectionText)

    parts.forEach((text, index) => {
      const headingPath = section.path.length ? section.path : [section.heading]
      const pathText = headingPath.join(' > ')
      const baseId = chunkHash(rel, headingPath, index)
      const seen = usedIds.get(baseId) || 0
      const id = seen === 0 ? baseId : `${baseId}-${seen + 1}`
      usedIds.set(baseId, seen + 1)
      chunks.push({
        id,
        source: rel,
        title: docTitle,
        chapter: meta.chapter,
        doc_type: meta.doc_type,
        stage: stageOf(stepNo),
        step_no: stepNo,
        reserved: stepNo >= 13,
        heading: section.heading,
        heading_path: headingPath,
        part: index + 1,
        char_count: text.length,
        text,
        embed_text: [pathText.startsWith(docTitle) ? pathText : `${docTitle} > ${pathText}`, text]
          .filter(Boolean)
          .join('\n'),
      })
    })
  }

  return mergeSmallChunks(chunks)
}

function main() {
  if (!statSync(ROOT).isDirectory()) {
    throw new Error(`语料目录不存在: ${ROOT}`)
  }

  mkdirSync(OUT_DIR, { recursive: true })
  const files = walk(ROOT)
  const chunks = files.flatMap(buildFileChunks).sort((a, b) => {
    if (a.step_no !== b.step_no) return a.step_no - b.step_no
    if (a.source !== b.source) return a.source.localeCompare(b.source, 'zh-CN')
    return a.part - b.part
  })

  const byStep = {}
  const byType = {}
  const bySource = new Map()
  let totalChars = 0
  let totalEmbedChars = 0

  for (const chunk of chunks) {
    totalChars += chunk.char_count
    totalEmbedChars += chunk.embed_text.length
    byStep[chunk.step_no] = (byStep[chunk.step_no] || 0) + 1
    byType[chunk.doc_type] = (byType[chunk.doc_type] || 0) + 1
    bySource.set(chunk.source, (bySource.get(chunk.source) || 0) + 1)
  }

  const jsonl = chunks.map((chunk) => JSON.stringify(chunk)).join('\n') + '\n'
  writeFileSync(join(OUT_DIR, 'chunks.jsonl'), jsonl, 'utf8')

  const manifest = {
    version: 1,
    root: ROOT,
    generated_at: new Date().toISOString(),
    file_count: files.length,
    chunk_count: chunks.length,
    total_chars: totalChars,
    total_embed_chars: totalEmbedChars,
    estimated_tokens: Math.round(totalEmbedChars / 1.55),
    by_step: byStep,
    by_type: byType,
    sources: [...bySource.entries()]
      .sort((a, b) => b[1] - a[1])
      .map(([source, count]) => ({ source, chunks: count })),
  }
  writeFileSync(join(OUT_DIR, 'manifest.json'), JSON.stringify(manifest, null, 2), 'utf8')

  console.log(`语料目录: ${ROOT}`)
  console.log(`Markdown: ${files.length} 个文件`)
  console.log(`切块: ${chunks.length} 块，${totalChars} 字，约 ${manifest.estimated_tokens} tokens`)
  console.log('按步骤:', Object.entries(byStep).map(([step, count]) => `S${step}=${count}`).join(' '))
  console.log('按类型:', Object.entries(byType).map(([type, count]) => `${type}=${count}`).join(' '))
  console.log(`输出: ${OUT_DIR}`)
}

main()
