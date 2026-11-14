export interface FrequencyEstimate {
  marker: string;
  estimated: boolean;
}

export interface Chapter {
  id: string;
  class: number;
  subject: string;
  title: string;
  weightage: string;
  past10YearQuestionFrequency: FrequencyEstimate;
}

export interface ChapterCatalog {
  schemaVersion: number;
  notice: string;
  chapters: Chapter[];
}

export interface PyqQuestion {
  id: string;
  exam: string;
  year: number | null;
  subject: string;
  chapterId: string;
  difficulty: string;
  tags: string[];
  q: string;
  options: string[];
  answer: number;
  explain: string;
  custom?: boolean;
}

export interface PyqCatalog {
  notice: string;
  questions: PyqQuestion[];
}

export interface Flashcard {
  id: string;
  subject: string;
  front: string;
  back: string;
}

export interface UserProfile {
  id: string;
  userId: string;
  name: string;
  cls: string;
  exam: string;
  goalMinutes: number;
}

export interface StudyTask {
  id: string;
  text: string;
  subject: string;
  done: boolean;
  date: string;
  doneOn: string | null;
  createdAtMs: number;
}

export interface StudyNote {
  id: string;
  title: string;
  subject: string;
  body: string;
  imageIds: string[];
  createdAtMs: number;
  updatedAtMs: number;
}

export interface PyqAttempt {
  id: string;
  qid: string;
  chapterId: string;
  subject: string;
  selectedOption: number;
  correct: boolean;
  secs: number;
  confidence: string;
  errorCategory: string;
  dateKey: string;
  createdAtMs: number;
}

export interface ErrorLogEntry {
  id: string;
  qid: string;
  resolved: boolean;
  category: string;
  dateKey: string;
  createdAtMs: number;
}

export interface StudySession {
  id: string;
  dateKey: string;
  minutes: number;
  tag: string;
  createdAtMs: number;
}

export interface PyqAnswerOutcome {
  selectedOption: number;
  correct: boolean;
  secs: number;
  attemptId: string;
  errorId: string | null;
  selectedErrorCategory: string;
}

export interface PomodoroState {
  presetMinutes: number;
  secondsLeft: number;
  isRunning: boolean;
  statusLabel: string;
  tag: string;
  soundEnabled: boolean;
}

export interface WeakSpot {
  chapter: Chapter;
  attemptsCount: number;
  accuracyPct: number | null;
}

export interface StreakInfo {
  days: number;
  gracesUsed: number;
}

export interface ChapterAccStat {
  total: number;
  correct: number;
  accuracyPct: number | null;
}

export type AppRoute =
  | 'dashboard'
  | 'syllabus'
  | 'pyq'
  | 'flashcards'
  | 'errors'
  | 'notes'
  | 'planner'
  | 'analytics'
  | 'settings';

export const StudyConstants = {
  SUBJECTS: ['Physics', 'Chemistry', 'Mathematics', 'Biology'] as const,
  TASK_SUBJECTS: ['Physics', 'Chemistry', 'Mathematics', 'Biology', 'General'] as const,
  CHAPTER_STATUSES: ['Not Started', 'In Progress', 'Revised', 'Mastered'] as const,
  DONE_STATUSES: new Set(['Revised', 'Mastered']),
  CLASSES: ['11', '12'] as const,
  EXAMS: ['JEE', 'NEET', 'Both'] as const,
  DIFFICULTIES: ['Easy', 'Medium', 'Hard'] as const,
  CONFIDENCE_LEVELS: ['LOW', 'MEDIUM', 'HIGH'] as const,
  ERROR_CATEGORIES: [
    'Concept gap',
    'Calculation',
    'Silly mistake',
    'Ran out of time',
  ] as const,
  POMODORO_PRESETS: [25, 45, 50, 60] as const,
  subjectsForExam(exam: string): string[] {
    if (exam === 'JEE') return ['Physics', 'Chemistry', 'Mathematics'];
    if (exam === 'NEET') return ['Physics', 'Chemistry', 'Biology'];
    return ['Physics', 'Chemistry', 'Mathematics', 'Biology'];
  },
};

export const BUNDLED_FLASHCARDS: Flashcard[] = [
  { id: 'f1', subject: 'Physics', front: 'Equations of motion', back: 'v = u + at,  s = ut + ½at²,  v² = u² + 2as' },
  { id: 'f2', subject: 'Physics', front: 'Projectile range', back: 'R = (u² sin 2θ) / g, maximum at θ = 45°' },
  { id: 'f3', subject: 'Physics', front: 'Work–energy theorem', back: 'W_net = ΔK = ½mv² − ½mu²' },
  { id: 'f4', subject: 'Physics', front: 'Resistors in parallel', back: '1 / R_eq = 1 / R₁ + 1 / R₂ + …' },
  { id: 'f5', subject: 'Physics', front: 'Faraday’s law', back: 'ε = −dΦ_B / dt' },
  { id: 'f6', subject: 'Chemistry', front: 'First-order half-life', back: 't₁/₂ = 0.693 / k;  k has unit s⁻¹' },
  { id: 'f7', subject: 'Chemistry', front: 'Nernst equation (298 K)', back: 'E = E° − (0.0591 / n) log Q' },
  { id: 'f8', subject: 'Chemistry', front: 'Ideal gas law', back: 'PV = nRT' },
  { id: 'f9', subject: 'Mathematics', front: 'Quadratic formula', back: 'x = (−b ± √(b² − 4ac)) / 2a;  sum of roots = −b/a' },
  { id: 'f10', subject: 'Mathematics', front: 'Euler’s formula', back: 'e^(iθ) = cos θ + i sin θ' },
  { id: 'f11', subject: 'Mathematics', front: 'Powers of i', back: 'i² = −1,  i³ = −i,  i⁴ = 1  (cycle of 4)' },
  { id: 'f12', subject: 'Biology', front: 'Five-kingdom system', back: 'Monera, Protista, Fungi, Plantae, Animalia — Monera holds all prokaryotes.' },
  { id: 'f13', subject: 'Biology', front: 'Site of fertilisation (human)', back: 'Ampullary region of the fallopian tube.' },
  { id: 'f14', subject: 'Biology', front: 'Double fertilisation', back: 'One male gamete + egg → zygote; the other + two polar nuclei → triploid primary endosperm nucleus.' },
];

export const StudyAnalytics = {
  todayKey(date: Date = new Date()): string {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  },

  lastNDays(n: number, endDate: Date = new Date()): Date[] {
    const out: Date[] = [];
    for (let offset = n - 1; offset >= 0; offset--) {
      const d = new Date(endDate);
      d.setDate(d.getDate() - offset);
      out.push(d);
    }
    return out;
  },

  pct(part: number, total: number): number {
    if (total <= 0) return 0;
    return Math.round((part / total) * 100);
  },

  filteredChapters(allChapters: Chapter[], profile: UserProfile): Chapter[] {
    const clsNum = Number.parseInt(profile.cls, 10) || 11;
    const allowed = new Set(StudyConstants.subjectsForExam(profile.exam));
    return allChapters.filter((ch) => ch.class === clsNum && allowed.has(ch.subject));
  },

  chapterAccuracyMap(attempts: PyqAttempt[]): Record<string, ChapterAccStat> {
    const acc: Record<string, { total: number; correct: number }> = {};
    for (const a of attempts) {
      if (!acc[a.chapterId]) acc[a.chapterId] = { total: 0, correct: 0 };
      acc[a.chapterId].total += 1;
      if (a.correct) acc[a.chapterId].correct += 1;
    }
    const out: Record<string, ChapterAccStat> = {};
    for (const [k, v] of Object.entries(acc)) {
      out[k] = {
        total: v.total,
        correct: v.correct,
        accuracyPct: v.total > 0 ? Math.max(0, Math.min(100, Math.floor((v.correct / v.total) * 100))) : null,
      };
    }
    return out;
  },

  overallAccuracyPct(attempts: PyqAttempt[]): number | null {
    if (attempts.length === 0) return null;
    const ok = attempts.filter((a) => a.correct).length;
    return StudyAnalytics.pct(ok, attempts.length);
  },

  minutesOnDate(sessions: StudySession[], dateKey: string): number {
    return sessions
      .filter((s) => s.dateKey === dateKey)
      .reduce((sum, s) => sum + s.minutes, 0);
  },

  activeDateKeys(sessions: StudySession[], attempts: PyqAttempt[], tasks: StudyTask[]): Set<string> {
    const set = new Set<string>();
    for (const s of sessions) {
      if (s.minutes > 0 && s.dateKey) set.add(s.dateKey);
    }
    for (const a of attempts) {
      if (a.dateKey) set.add(a.dateKey);
    }
    for (const t of tasks) {
      if (t.done && t.doneOn) set.add(t.doneOn);
    }
    return set;
  },

  streak(
    sessions: StudySession[],
    attempts: PyqAttempt[],
    tasks: StudyTask[],
    today: Date = new Date()
  ): StreakInfo {
    const active = StudyAnalytics.activeDateKeys(sessions, attempts, tasks);
    const d = new Date(today);
    let count = 0;
    let graces = 0;
    let pending = 0;
    let prevMiss = false;
    for (let i = 0; i < 400; i++) {
      const k = StudyAnalytics.todayKey(d);
      if (active.has(k)) {
        count++;
        graces += pending;
        pending = 0;
        prevMiss = false;
      } else if (i > 0) {
        if (prevMiss) break;
        prevMiss = true;
        pending++;
      }
      d.setDate(d.getDate() - 1);
    }
    return { days: count, gracesUsed: graces };
  },

  weakSpots(
    chapters: Chapter[],
    statusMap: Record<string, string>,
    attempts: PyqAttempt[]
  ): WeakSpot[] {
    const acc = StudyAnalytics.chapterAccuracyMap(attempts);
    return chapters
      .filter((ch) => ch.weightage === 'High')
      .map((chapter) => {
        const stat = acc[chapter.id];
        return {
          chapter,
          attemptsCount: stat?.total ?? 0,
          accuracyPct: stat?.accuracyPct ?? null,
        };
      })
      .filter((ws) => {
        const isDone = StudyConstants.DONE_STATUSES.has(statusMap[ws.chapter.id] ?? '');
        return ws.attemptsCount === 0
          ? !isDone
          : ws.attemptsCount >= 2 && (ws.accuracyPct ?? 100) < 60;
      })
      .sort((a, b) => (a.accuracyPct ?? -1) - (b.accuracyPct ?? -1));
  },
};
