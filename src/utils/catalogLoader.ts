import chapterData from '../data/chapter-data.json';
import pyqData from '../data/pyq-data.json';
import { ChapterCatalog, PyqCatalog, PyqQuestion, StudyConstants } from '../types/study';

export function loadChapters(): ChapterCatalog {
  return chapterData as ChapterCatalog;
}

export function loadBundledPyqs(): PyqCatalog {
  return pyqData as PyqCatalog;
}

export interface ImportParseResult {
  valid: PyqQuestion[];
  rejectedCount: number;
}

export function parseCustomQuestionsJson(
  rawJson: string,
  validChapterIds: Set<string>
): ImportParseResult {
  const parsed: unknown = JSON.parse(rawJson);
  let items: unknown[];
  if (Array.isArray(parsed)) {
    items = parsed;
  } else if (
    parsed &&
    typeof parsed === 'object' &&
    Array.isArray((parsed as { questions?: unknown }).questions)
  ) {
    items = (parsed as { questions: unknown[] }).questions;
  } else {
    throw new Error('Expected a list, or an object with a "questions" list');
  }

  const ok: PyqQuestion[] = [];
  let bad = 0;

  for (const item of items) {
    if (!item || typeof item !== 'object') {
      bad++;
      continue;
    }
    const obj = item as Record<string, unknown>;
    const rawId = typeof obj.id === 'string' ? obj.id.trim() : '';
    const safeId = rawId.replace(/[^a-zA-Z0-9_-]/g, '_').slice(0, 64);
    const qText = typeof obj.q === 'string' ? obj.q.trim().slice(0, 2000) : '';
    const subject = typeof obj.subject === 'string' ? obj.subject : '';
    const rawOptions = Array.isArray(obj.options) ? obj.options : null;
    const options =
      rawOptions
        ?.map((o) => (typeof o === 'string' ? o.trim().slice(0, 300) : ''))
        .filter((s) => s.length > 0) ?? [];
    const answerIdx = typeof obj.answer === 'number' && Number.isInteger(obj.answer) ? obj.answer : null;

    const valid =
      safeId.length > 0 &&
      qText.length > 0 &&
      (StudyConstants.SUBJECTS as readonly string[]).includes(subject) &&
      rawOptions !== null &&
      options.length === rawOptions.length &&
      options.length >= 2 &&
      options.length <= 4 &&
      answerIdx !== null &&
      answerIdx >= 0 &&
      answerIdx < options.length;

    if (!valid) {
      bad++;
      continue;
    }

    const examRaw = typeof obj.exam === 'string' ? obj.exam : '';
    const exam = examRaw === 'JEE' || examRaw === 'NEET' ? examRaw : 'JEE';
    const yearRaw = typeof obj.year === 'number' && Number.isInteger(obj.year) ? obj.year : null;
    const year = yearRaw !== null && yearRaw >= 1970 && yearRaw <= 2100 ? yearRaw : null;
    const chapterRaw = typeof obj.chapterId === 'string' ? obj.chapterId : '';
    const chapterId = validChapterIds.has(chapterRaw) ? chapterRaw : '';
    const diffRaw = typeof obj.difficulty === 'string' ? obj.difficulty : '';
    const difficulty = (StudyConstants.DIFFICULTIES as readonly string[]).includes(diffRaw)
      ? diffRaw
      : 'Medium';
    const tags = Array.isArray(obj.tags)
      ? obj.tags
          .filter((t): t is string => typeof t === 'string')
          .map((t) => t.trim().slice(0, 30))
          .filter((t) => t.length > 0)
          .slice(0, 8)
      : [];
    const explain = typeof obj.explain === 'string' ? obj.explain.slice(0, 1500) : '';

    ok.push({
      id: safeId,
      exam,
      year,
      subject,
      chapterId,
      difficulty,
      tags,
      q: qText,
      options,
      answer: answerIdx,
      explain,
      custom: true,
    });
  }

  return { valid: ok, rejectedCount: bad };
}
