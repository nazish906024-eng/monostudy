import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { User, onAuthStateChanged, signInWithPopup, signOut } from 'firebase/auth';
import { auth, googleProvider } from '../firebase/config';
import { studyRepository } from '../services/studyRepository';
import {
  Chapter,
  ErrorLogEntry,
  PomodoroState,
  PyqAnswerOutcome,
  PyqAttempt,
  PyqQuestion,
  StudyAnalytics,
  StudyNote,
  StudySession,
  StudyTask,
  UserProfile,
} from '../types/study';
import { loadBundledPyqs, loadChapters, parseCustomQuestionsJson } from '../utils/catalogLoader';

function playCompletionBeep() {
  try {
    const AudioCtx =
      window.AudioContext ||
      (window as unknown as { webkitAudioContext?: typeof AudioContext }).webkitAudioContext;
    if (!AudioCtx) return;
    const ctx = new AudioCtx();
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();
    osc.type = 'sine';
    osc.frequency.setValueAtTime(880, ctx.currentTime);
    gain.gain.setValueAtTime(0.08, ctx.currentTime);
    gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.35);
    osc.connect(gain);
    gain.connect(ctx.destination);
    osc.start();
    osc.stop(ctx.currentTime + 0.36);
  } catch {
    // Ignore audio context autoplay restrictions
  }
}

async function compressImageToDataUrl(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onerror = () => reject(new Error('Could not read image'));
    reader.onload = () => {
      const img = new Image();
      img.onerror = () => reject(new Error('Invalid image format'));
      img.onload = () => {
        const maxDim = Math.max(img.width, img.height, 1);
        const scale = Math.min(1, 800 / maxDim);
        const w = Math.max(1, Math.round(img.width * scale));
        const h = Math.max(1, Math.round(img.height * scale));
        const canvas = document.createElement('canvas');
        canvas.width = w;
        canvas.height = h;
        const ctx = canvas.getContext('2d');
        if (!ctx) {
          reject(new Error('Canvas unavailable'));
          return;
        }
        ctx.fillStyle = '#ffffff';
        ctx.fillRect(0, 0, w, h);
        ctx.drawImage(img, 0, 0, w, h);
        const dataUrl = canvas.toDataURL('image/jpeg', 0.7);
        if (dataUrl.length > 495000) {
          resolve(canvas.toDataURL('image/jpeg', 0.45));
        } else {
          resolve(dataUrl);
        }
      };
      img.src = String(reader.result);
    };
    reader.readAsDataURL(file);
  });
}

export function useStudyStore() {
  const chapterCatalog = useMemo(() => loadChapters(), []);
  const pyqCatalog = useMemo(() => loadBundledPyqs(), []);
  const chapterMap = useMemo(() => {
    const map: Record<string, Chapter> = {};
    for (const ch of chapterCatalog.chapters) map[ch.id] = ch;
    return map;
  }, [chapterCatalog]);

  const [authReady, setAuthReady] = useState(false);
  const [user, setUser] = useState<User | null>(null);
  const [isSigningIn, setIsSigningIn] = useState(false);
  const [authError, setAuthError] = useState<string | null>(null);

  const [isLoadingData, setIsLoadingData] = useState(true);
  const [dataError, setDataError] = useState<string | null>(null);
  const [toast, setToast] = useState<string | null>(null);

  const [profile, setProfile] = useState<UserProfile>({
    id: '',
    userId: '',
    name: '',
    cls: '11',
    exam: 'JEE',
    goalMinutes: 180,
  });
  const [chapterStatuses, setChapterStatuses] = useState<Record<string, string>>({});
  const [tasks, setTasks] = useState<StudyTask[]>([]);
  const [notes, setNotes] = useState<StudyNote[]>([]);
  const [noteImages, setNoteImages] = useState<Record<string, string>>({});
  const [attempts, setAttempts] = useState<PyqAttempt[]>([]);
  const [errors, setErrors] = useState<ErrorLogEntry[]>([]);
  const [cardProgress, setCardProgress] = useState<Record<string, string>>({});
  const [sessions, setSessions] = useState<StudySession[]>([]);
  const [customPyqs, setCustomPyqs] = useState<PyqQuestion[]>([]);
  const [answeredPyqs, setAnsweredPyqs] = useState<Record<string, PyqAnswerOutcome>>({});

  const [pomodoro, setPomodoro] = useState<PomodoroState>({
    presetMinutes: 25,
    secondsLeft: 25 * 60,
    isRunning: false,
    statusLabel: 'READY TO FOCUS',
    tag: '',
    soundEnabled: true,
  });

  const qStartTimesRef = useRef<Record<string, number>>({});
  const toastTimerRef = useRef<number | null>(null);

  const showToast = useCallback((msg: string) => {
    setToast(msg);
    if (toastTimerRef.current) window.clearTimeout(toastTimerRef.current);
    toastTimerRef.current = window.setTimeout(() => setToast(null), 3500);
  }, []);

  useEffect(() => {
    const unsub = onAuthStateChanged(auth, (u) => {
      setUser(u);
      setAuthReady(true);
    });
    return () => unsub();
  }, []);

  useEffect(() => {
    if (!authReady || !user) {
      setIsLoadingData(false);
      return;
    }

    const uid = user.uid;
    const defaultName = user.displayName ?? '';
    setIsLoadingData(true);
    setDataError(null);

    void studyRepository.ensureProfile(uid, defaultName).catch((err) => {
      setDataError(err instanceof Error ? err.message : 'Failed to initialize profile');
    });

    const unsubs = [
      studyRepository.observeProfile(uid, (p) => {
        setProfile(
          p ?? {
            id: uid,
            userId: uid,
            name: defaultName,
            cls: '11',
            exam: 'JEE',
            goalMinutes: 180,
          }
        );
        setIsLoadingData(false);
      }),
      studyRepository.observeChapterStatuses(uid, setChapterStatuses),
      studyRepository.observeTasks(uid, setTasks),
      studyRepository.observeNotes(uid, setNotes),
      studyRepository.observeNoteImages(uid, setNoteImages),
      studyRepository.observeAttempts(uid, setAttempts),
      studyRepository.observeErrors(uid, setErrors),
      studyRepository.observeCardProgress(uid, setCardProgress),
      studyRepository.observeSessions(uid, setSessions),
      studyRepository.observeCustomPyqs(uid, setCustomPyqs),
    ];

    return () => {
      unsubs.forEach((fn) => fn());
    };
  }, [authReady, user]);

  useEffect(() => {
    if (!pomodoro.isRunning) return;
    const id = window.setInterval(() => {
      setPomodoro((prev) => {
        if (!prev.isRunning) return prev;
        if (prev.secondsLeft > 1) {
          return { ...prev, secondsLeft: prev.secondsLeft - 1 };
        }
        if (prev.soundEnabled) playCompletionBeep();
        if (user) {
          void studyRepository.logStudySession(user.uid, prev.presetMinutes, prev.tag).then(() => {
            showToast(`Session complete — ${prev.presetMinutes} min logged`);
          });
        }
        return {
          ...prev,
          secondsLeft: prev.presetMinutes * 60,
          isRunning: false,
          statusLabel: 'SESSION COMPLETE',
        };
      });
    }, 1000);
    return () => window.clearInterval(id);
  }, [pomodoro.isRunning, user, showToast]);

  const allPyqs = useMemo(() => {
    const map = new Map<string, PyqQuestion>();
    for (const q of pyqCatalog.questions) map.set(q.id, q);
    for (const q of customPyqs) map.set(q.id, q);
    return Array.from(map.values());
  }, [pyqCatalog.questions, customPyqs]);

  const signInWithGoogle = useCallback(async () => {
    setAuthError(null);
    setIsSigningIn(true);
    try {
      await signInWithPopup(auth, googleProvider);
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Sign-in failed';
      if (!msg.includes('popup-closed-by-user')) {
        setAuthError(msg);
      }
    } finally {
      setIsSigningIn(false);
    }
  }, []);

  const signOutAccount = useCallback(async () => {
    await signOut(auth);
    setAnsweredPyqs({});
  }, []);

  const markQuestionVisible = useCallback((qid: string) => {
    if (!qStartTimesRef.current[qid]) {
      qStartTimesRef.current[qid] = Date.now();
    }
  }, []);

  const saveProfile = useCallback(
    async (name: string, cls: string, exam: string, goalMinutes: number) => {
      if (!user) return;
      if (goalMinutes < 15 || goalMinutes > 900) {
        showToast('Goal must be 15–900 minutes');
        return;
      }
      try {
        await studyRepository.saveProfile(user.uid, name, cls, exam, goalMinutes);
        showToast('Profile saved');
      } catch (err) {
        showToast(err instanceof Error ? err.message : 'Failed to save profile');
      }
    },
    [user, showToast]
  );

  const updateProfileSelection = useCallback(
    async (cls: string, exam: string) => {
      if (!user) return;
      try {
        await studyRepository.saveProfile(user.uid, profile.name, cls, exam, profile.goalMinutes);
      } catch (err) {
        showToast(err instanceof Error ? err.message : 'Failed to update settings');
      }
    },
    [user, profile.name, profile.goalMinutes, showToast]
  );

  const setChapterStatus = useCallback(
    async (chapterId: string, status: string) => {
      if (!user) return;
      try {
        await studyRepository.setChapterStatus(user.uid, chapterId, status);
      } catch (err) {
        showToast(err instanceof Error ? err.message : 'Failed to update chapter status');
      }
    },
    [user, showToast]
  );

  const answerPyq = useCallback(
    async (question: PyqQuestion, selectedOption: number, confidence: string) => {
      if (!user || answeredPyqs[question.id]) return;
      const started = qStartTimesRef.current[question.id] ?? Date.now();
      const secs = Math.max(1, Math.min(7200, Math.round((Date.now() - started) / 1000)));
      const openErrors = errors.filter((e) => e.qid === question.id && !e.resolved);

      try {
        const { attemptId, errorId } = await studyRepository.logAttemptAndSyncErrors(
          user.uid,
          question,
          selectedOption,
          secs,
          confidence,
          openErrors
        );
        setAnsweredPyqs((prev) => ({
          ...prev,
          [question.id]: {
            selectedOption,
            correct: selectedOption === question.answer,
            secs,
            attemptId,
            errorId,
            selectedErrorCategory: '',
          },
        }));
      } catch (err) {
        showToast(err instanceof Error ? err.message : 'Could not save attempt');
      }
    },
    [user, answeredPyqs, errors, showToast]
  );

  const setPyqErrorCategory = useCallback(
    async (qid: string, category: string) => {
      if (!user) return;
      const outcome = answeredPyqs[qid];
      if (!outcome) return;
      try {
        await studyRepository.setAttemptErrorCategory(
          user.uid,
          outcome.attemptId,
          outcome.errorId,
          category
        );
        setAnsweredPyqs((prev) => ({
          ...prev,
          [qid]: { ...outcome, selectedErrorCategory: category },
        }));
        showToast('Error category saved');
      } catch (err) {
        showToast(err instanceof Error ? err.message : 'Could not save error category');
      }
    },
    [user, answeredPyqs, showToast]
  );

  const setFlashcardStatus = useCallback(
    async (cardId: string, status: string) => {
      if (!user) return;
      try {
        await studyRepository.setFlashcardStatus(user.uid, cardId, status);
      } catch (err) {
        showToast(err instanceof Error ? err.message : 'Failed to save card status');
      }
    },
    [user, showToast]
  );

  const toggleErrorResolved = useCallback(
    async (entry: ErrorLogEntry) => {
      if (!user) return;
      try {
        await studyRepository.toggleErrorResolved(user.uid, entry);
      } catch (err) {
        showToast(err instanceof Error ? err.message : 'Failed to update error log');
      }
    },
    [user, showToast]
  );

  const deleteErrorEntry = useCallback(
    async (errorId: string) => {
      if (!user) return;
      try {
        await studyRepository.deleteErrorEntry(user.uid, errorId);
      } catch (err) {
        showToast(err instanceof Error ? err.message : 'Failed to remove entry');
      }
    },
    [user, showToast]
  );

  const createNote = useCallback(async (): Promise<string | null> => {
    if (!user) return null;
    try {
      return await studyRepository.createNote(user.uid);
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Failed to create note');
      return null;
    }
  }, [user, showToast]);

  const updateNote = useCallback(
    async (note: StudyNote, title: string, subject: string, body: string) => {
      if (!user) return;
      try {
        await studyRepository.updateNote(user.uid, note.id, title, subject, body, note.imageIds);
      } catch (err) {
        showToast(err instanceof Error ? err.message : 'Failed to save note');
      }
    },
    [user, showToast]
  );

  const deleteNote = useCallback(
    async (note: StudyNote) => {
      if (!user) return;
      try {
        await studyRepository.deleteNote(user.uid, note);
        showToast('Note deleted');
      } catch (err) {
        showToast(err instanceof Error ? err.message : 'Failed to delete note');
      }
    },
    [user, showToast]
  );

  const attachNoteImage = useCallback(
    async (note: StudyNote, file: File) => {
      if (!user) return;
      if (note.imageIds.length >= 6) {
        showToast('Max 6 images per note');
        return;
      }
      try {
        const dataUrl = await compressImageToDataUrl(file);
        await studyRepository.addNoteImage(user.uid, note, dataUrl);
        showToast('Image attached');
      } catch (err) {
        showToast(err instanceof Error ? err.message : 'Could not attach image');
      }
    },
    [user, showToast]
  );

  const removeNoteImage = useCallback(
    async (note: StudyNote, imageId: string) => {
      if (!user) return;
      try {
        await studyRepository.removeNoteImage(user.uid, note, imageId);
      } catch (err) {
        showToast(err instanceof Error ? err.message : 'Failed to remove image');
      }
    },
    [user, showToast]
  );

  const addTask = useCallback(
    async (text: string, subject: string) => {
      if (!user) return;
      if (!text.trim()) {
        showToast('Type a task first');
        return;
      }
      try {
        await studyRepository.addTask(user.uid, text, subject);
      } catch (err) {
        showToast(err instanceof Error ? err.message : 'Failed to add task');
      }
    },
    [user, showToast]
  );

  const toggleTask = useCallback(
    async (task: StudyTask) => {
      if (!user) return;
      try {
        await studyRepository.toggleTask(user.uid, task);
      } catch (err) {
        showToast(err instanceof Error ? err.message : 'Failed to update task');
      }
    },
    [user, showToast]
  );

  const deleteTask = useCallback(
    async (taskId: string) => {
      if (!user) return;
      try {
        await studyRepository.deleteTask(user.uid, taskId);
      } catch (err) {
        showToast(err instanceof Error ? err.message : 'Failed to delete task');
      }
    },
    [user, showToast]
  );

  const clearCompletedTasks = useCallback(async () => {
    if (!user) return;
    try {
      await studyRepository.clearCompletedTasks(user.uid, tasks);
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Failed to clear tasks');
    }
  }, [user, tasks, showToast]);

  const setPomodoroTag = useCallback((tag: string) => {
    setPomodoro((p) => ({ ...p, tag: tag.slice(0, 80) }));
  }, []);

  const setPomodoroSound = useCallback((soundEnabled: boolean) => {
    setPomodoro((p) => ({ ...p, soundEnabled }));
  }, []);

  const selectPomodoroPreset = useCallback((minutes: number) => {
    setPomodoro((p) => ({
      ...p,
      presetMinutes: minutes,
      secondsLeft: minutes * 60,
      isRunning: false,
      statusLabel: 'READY TO FOCUS',
    }));
  }, []);

  const togglePomodoroRun = useCallback(() => {
    setPomodoro((p) => ({
      ...p,
      isRunning: !p.isRunning,
      statusLabel: !p.isRunning ? 'FOCUSING' : 'PAUSED',
    }));
  }, []);

  const resetPomodoro = useCallback(() => {
    setPomodoro((p) => ({
      ...p,
      secondsLeft: p.presetMinutes * 60,
      isRunning: false,
      statusLabel: 'READY TO FOCUS',
    }));
  }, []);

  const logStudyMinutes = useCallback(
    async (minutes: number, tag: string) => {
      if (!user) return;
      try {
        await studyRepository.logStudySession(user.uid, minutes, tag);
        showToast(`${minutes} minutes logged`);
      } catch (err) {
        showToast(err instanceof Error ? err.message : 'Failed to log study time');
      }
    },
    [user, showToast]
  );

  const importQuestionsFromJson = useCallback(
    async (rawJson: string) => {
      if (!user) return;
      try {
        const parsed = parseCustomQuestionsJson(rawJson, new Set(Object.keys(chapterMap)));
        if (parsed.valid.length === 0) {
          showToast(`No valid questions found (${parsed.rejectedCount} rejected)`);
          return;
        }
        await studyRepository.importCustomQuestions(user.uid, parsed.valid);
        const rej = parsed.rejectedCount > 0 ? `, ${parsed.rejectedCount} rejected` : '';
        showToast(`Imported ${parsed.valid.length} question(s)${rej}`);
      } catch (err) {
        showToast(err instanceof Error ? err.message : 'That file is not valid JSON');
      }
    },
    [user, chapterMap, showToast]
  );

  const clearCustomQuestions = useCallback(async () => {
    if (!user || customPyqs.length === 0) return;
    try {
      await studyRepository.clearCustomQuestions(
        user.uid,
        customPyqs.map((q) => q.id)
      );
      showToast('Imported questions removed');
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Failed to remove imported questions');
    }
  }, [user, customPyqs, showToast]);

  const buildQuestionTemplateJson = useCallback((): string => {
    return JSON.stringify(
      {
        notice: 'My own practice questions',
        questions: [
          {
            id: 'my_phy_001',
            exam: 'JEE',
            year: null,
            subject: 'Physics',
            chapterId: 'class-11-physics-02',
            difficulty: 'Easy',
            tags: ['kinematics'],
            q: 'A car accelerates from rest at $2\\,\\text{m/s}^2$ for $5\\,\\text{s}$. Final speed?',
            options: ['5 m/s', '10 m/s', '15 m/s', '20 m/s'],
            answer: 1,
            explain: '$v=u+at=10\\,\\text{m/s}$.',
          },
        ],
      },
      null,
      2
    );
  }, []);

  const buildBackupJson = useCallback((): string => {
    return JSON.stringify(
      {
        v: 2,
        uid: user?.uid ?? '',
        profile: {
          name: profile.name,
          cls: profile.cls,
          exam: profile.exam,
          goal: profile.goalMinutes,
        },
        status: chapterStatuses,
        tasks: tasks.map((t) => ({
          text: t.text,
          subject: t.subject,
          done: t.done,
          date: t.date,
        })),
        notes: notes.map((n) => ({
          title: n.title,
          subject: n.subject,
          body: n.body,
        })),
        sessions: sessions.map((s) => ({
          date: s.dateKey,
          m: s.minutes,
          tag: s.tag,
        })),
        customPyq: customPyqs,
      },
      null,
      2
    );
  }, [user, profile, chapterStatuses, tasks, notes, sessions, customPyqs]);

  const importBackupJson = useCallback(
    async (rawJson: string) => {
      if (!user) return;
      try {
        const payload = JSON.parse(rawJson) as {
          profile?: { name?: string; cls?: string; exam?: string; goal?: number };
          status?: Record<string, string>;
          tasks?: Array<{ text?: string; subject?: string }>;
          notes?: Array<{ title?: string; subject?: string; body?: string }>;
          sessions?: Array<{ m?: number; tag?: string }>;
          customPyq?: PyqQuestion[];
        };
        if (payload.profile) {
          await studyRepository.saveProfile(
            user.uid,
            payload.profile.name ?? '',
            payload.profile.cls ?? '11',
            payload.profile.exam ?? 'JEE',
            payload.profile.goal ?? 180
          );
        }
        if (payload.status) {
          for (const [chId, status] of Object.entries(payload.status)) {
            if (chapterMap[chId]) {
              await studyRepository.setChapterStatus(user.uid, chId, status);
            }
          }
        }
        if (Array.isArray(payload.tasks)) {
          for (const t of payload.tasks.slice(0, 50)) {
            if (t.text && t.text.trim()) {
              await studyRepository.addTask(user.uid, t.text, t.subject ?? 'General');
            }
          }
        }
        if (Array.isArray(payload.notes)) {
          for (const n of payload.notes.slice(0, 50)) {
            await studyRepository.createNote(
              user.uid,
              n.title ?? '',
              n.subject ?? 'General',
              n.body ?? ''
            );
          }
        }
        if (Array.isArray(payload.sessions)) {
          for (const s of payload.sessions.slice(0, 100)) {
            if (s.m && s.m > 0) {
              await studyRepository.logStudySession(user.uid, s.m, s.tag ?? '');
            }
          }
        }
        if (Array.isArray(payload.customPyq) && payload.customPyq.length > 0) {
          await studyRepository.importCustomQuestions(user.uid, payload.customPyq);
        }
        showToast('Backup imported');
      } catch {
        showToast('Invalid backup file');
      }
    },
    [user, chapterMap, showToast]
  );

  const resetAllData = useCallback(async () => {
    if (!user) return;
    try {
      await studyRepository.resetAllUserData(user.uid);
      setAnsweredPyqs({});
      showToast('Data reset');
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Reset failed');
    }
  }, [user, showToast]);

  const downloadWeeklySummaryPng = useCallback(() => {
    const days = StudyAnalytics.lastNDays(7);
    const keys = new Set(days.map((d) => StudyAnalytics.todayKey(d)));
    let mins = 0;
    keys.forEach((k) => {
      mins += StudyAnalytics.minutesOnDate(sessions, k);
    });
    const weekAttempts = attempts.filter((a) => keys.has(a.dateKey));
    const okAttempts = weekAttempts.filter((a) => a.correct).length;
    const accText =
      weekAttempts.length === 0 ? '—' : `${StudyAnalytics.pct(okAttempts, weekAttempts.length)}%`;
    const streakInfo = StudyAnalytics.streak(sessions, attempts, tasks);

    const canvas = document.createElement('canvas');
    canvas.width = 900;
    canvas.height = 500;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const grad = ctx.createLinearGradient(0, 0, 900, 500);
    grad.addColorStop(0, '#1B1647');
    grad.addColorStop(1, '#0B1020');
    ctx.fillStyle = grad;
    ctx.fillRect(0, 0, 900, 500);

    ctx.fillStyle = '#8B7DFF';
    ctx.font = '700 20px "Plus Jakarta Sans", sans-serif';
    ctx.fillText('monostudy · WEEKLY SUMMARY', 50, 70);

    ctx.fillStyle = '#FFFFFF';
    ctx.font = '700 40px "Space Grotesk", sans-serif';
    ctx.fillText(profile.name.trim() || 'Student', 50, 128);

    const fmt = (d: Date) =>
      d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
    ctx.fillStyle = '#8F9BC4';
    ctx.font = '400 18px "Plus Jakarta Sans", sans-serif';
    ctx.fillText(`${fmt(days[0])} – ${fmt(days[days.length - 1])}`, 50, 165);

    const tiles: Array<[string, string]> = [
      ['Minutes studied', String(mins)],
      ['PYQs attempted', String(weekAttempts.length)],
      ['Accuracy', accText],
      ['Streak', `${streakInfo.days} days`],
    ];

    tiles.forEach(([lbl, val], i) => {
      const px = 50 + (i % 2) * 420;
      const py = 225 + Math.floor(i / 2) * 120;
      ctx.fillStyle = '#141B33';
      ctx.beginPath();
      ctx.roundRect(px, py, 380, 98, 14);
      ctx.fill();

      ctx.fillStyle = '#8F9BC4';
      ctx.font = '500 16px "Plus Jakarta Sans", sans-serif';
      ctx.fillText(lbl, px + 22, py + 34);

      ctx.fillStyle = '#FFFFFF';
      ctx.font = '700 34px "Space Grotesk", sans-serif';
      ctx.fillText(val, px + 22, py + 78);
    });

    const url = canvas.toDataURL('image/png');
    const a = document.createElement('a');
    a.href = url;
    a.download = 'monostudy-weekly-summary.png';
    a.click();
    showToast('Weekly summary card downloaded');
  }, [sessions, attempts, tasks, profile.name, showToast]);

  return {
    authReady,
    user,
    isSigningIn,
    authError,
    isLoadingData,
    dataError,
    toast,
    profile,
    chapters: chapterCatalog.chapters,
    chapterMap,
    chapterStatuses,
    tasks,
    notes,
    noteImages,
    attempts,
    errors,
    cardProgress,
    sessions,
    basePyqs: pyqCatalog.questions,
    customPyqs,
    allPyqs,
    pyqNotice: pyqCatalog.notice,
    answeredPyqs,
    pomodoro,
    signInWithGoogle,
    signOutAccount,
    markQuestionVisible,
    saveProfile,
    updateProfileSelection,
    setChapterStatus,
    answerPyq,
    setPyqErrorCategory,
    setFlashcardStatus,
    toggleErrorResolved,
    deleteErrorEntry,
    createNote,
    updateNote,
    deleteNote,
    attachNoteImage,
    removeNoteImage,
    addTask,
    toggleTask,
    deleteTask,
    clearCompletedTasks,
    setPomodoroTag,
    setPomodoroSound,
    selectPomodoroPreset,
    togglePomodoroRun,
    resetPomodoro,
    logStudyMinutes,
    importQuestionsFromJson,
    clearCustomQuestions,
    buildQuestionTemplateJson,
    buildBackupJson,
    importBackupJson,
    resetAllData,
    downloadWeeklySummaryPng,
  };
}

export type StudyStore = ReturnType<typeof useStudyStore>;
