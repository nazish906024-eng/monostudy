import {
  Timestamp,
  collection,
  deleteDoc,
  doc,
  getDoc,
  getDocs,
  onSnapshot,
  serverTimestamp,
  setDoc,
  updateDoc,
  writeBatch,
} from 'firebase/firestore';
import { db } from '../firebase/config';
import { OperationType, handleFirestoreError } from '../firebase/errorHandler';
import {
  ErrorLogEntry,
  PyqAttempt,
  PyqQuestion,
  StudyAnalytics,
  StudyConstants,
  StudyNote,
  StudySession,
  StudyTask,
  UserProfile,
} from '../types/study';

function tsToMillis(ts: unknown): number {
  if (ts instanceof Timestamp) return ts.toMillis();
  if (ts && typeof ts === 'object' && 'toMillis' in ts && typeof (ts as { toMillis: unknown }).toMillis === 'function') {
    return (ts as { toMillis: () => number }).toMillis();
  }
  return Date.now();
}

function userDocRef(uid: string) {
  return doc(db, 'users', uid);
}

function userSubcollection(uid: string, sub: string) {
  return collection(db, 'users', uid, sub);
}

function makeId(prefix: string): string {
  const raw =
    typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function'
      ? crypto.randomUUID()
      : `${Date.now()}_${Math.random().toString(36).slice(2, 10)}`;
  return `${prefix}_${raw}`;
}

export const studyRepository = {
  observeProfile(uid: string, onChange: (profile: UserProfile | null) => void): () => void {
    const ref = userDocRef(uid);
    return onSnapshot(
      ref,
      (snap) => {
        if (!snap.exists()) {
          onChange(null);
          return;
        }
        const d = snap.data();
        onChange({
          id: snap.id,
          userId: String(d.userId ?? uid),
          name: String(d.name ?? ''),
          cls: String(d.cls ?? '11'),
          exam: String(d.exam ?? 'JEE'),
          goalMinutes: typeof d.goalMinutes === 'number' ? Math.round(d.goalMinutes) : 180,
        });
      },
      (error) => handleFirestoreError(error, OperationType.GET, ref.path)
    );
  },

  observeChapterStatuses(uid: string, onChange: (statuses: Record<string, string>) => void): () => void {
    const col = userSubcollection(uid, 'chapter_statuses');
    return onSnapshot(
      col,
      (snap) => {
        const map: Record<string, string> = {};
        snap.forEach((d) => {
          const data = d.data();
          const key = typeof data.chapterId === 'string' && data.chapterId ? data.chapterId : d.id;
          map[key] = String(data.status ?? 'Not Started');
        });
        onChange(map);
      },
      (error) => handleFirestoreError(error, OperationType.LIST, `users/${uid}/chapter_statuses`)
    );
  },

  observeTasks(uid: string, onChange: (tasks: StudyTask[]) => void): () => void {
    const col = userSubcollection(uid, 'tasks');
    return onSnapshot(
      col,
      (snap) => {
        const list: StudyTask[] = snap.docs.map((d) => {
          const data = d.data();
          return {
            id: d.id,
            text: String(data.text ?? ''),
            subject: String(data.subject ?? 'General'),
            done: Boolean(data.done),
            date: String(data.date ?? ''),
            doneOn: typeof data.doneOn === 'string' ? data.doneOn : null,
            createdAtMs: tsToMillis(data.createdAt),
          };
        });
        list.sort((a, b) => Number(a.done) - Number(b.done) || b.createdAtMs - a.createdAtMs);
        onChange(list);
      },
      (error) => handleFirestoreError(error, OperationType.LIST, `users/${uid}/tasks`)
    );
  },

  observeNotes(uid: string, onChange: (notes: StudyNote[]) => void): () => void {
    const col = userSubcollection(uid, 'notes');
    return onSnapshot(
      col,
      (snap) => {
        const list: StudyNote[] = snap.docs.map((d) => {
          const data = d.data();
          return {
            id: d.id,
            title: String(data.title ?? ''),
            subject: String(data.subject ?? 'General'),
            body: String(data.body ?? ''),
            imageIds: Array.isArray(data.imageIds) ? data.imageIds.map(String) : [],
            createdAtMs: tsToMillis(data.createdAt),
            updatedAtMs: tsToMillis(data.updatedAt ?? data.createdAt),
          };
        });
        list.sort((a, b) => b.updatedAtMs - a.updatedAtMs);
        onChange(list);
      },
      (error) => handleFirestoreError(error, OperationType.LIST, `users/${uid}/notes`)
    );
  },

  observeNoteImages(uid: string, onChange: (images: Record<string, string>) => void): () => void {
    const col = userSubcollection(uid, 'note_images');
    return onSnapshot(
      col,
      (snap) => {
        const map: Record<string, string> = {};
        snap.forEach((d) => {
          map[d.id] = String(d.data().data ?? '');
        });
        onChange(map);
      },
      (error) => handleFirestoreError(error, OperationType.LIST, `users/${uid}/note_images`)
    );
  },

  observeAttempts(uid: string, onChange: (attempts: PyqAttempt[]) => void): () => void {
    const col = userSubcollection(uid, 'attempts');
    return onSnapshot(
      col,
      (snap) => {
        const list: PyqAttempt[] = snap.docs.map((d) => {
          const data = d.data();
          return {
            id: d.id,
            qid: String(data.qid ?? ''),
            chapterId: String(data.chapterId ?? ''),
            subject: String(data.subject ?? 'Physics'),
            selectedOption: typeof data.selectedOption === 'number' ? data.selectedOption : 0,
            correct: Boolean(data.correct),
            secs: typeof data.secs === 'number' ? data.secs : 1,
            confidence: String(data.confidence ?? 'MEDIUM'),
            errorCategory: String(data.errorCategory ?? ''),
            dateKey: String(data.dateKey ?? ''),
            createdAtMs: tsToMillis(data.createdAt),
          };
        });
        list.sort((a, b) => a.createdAtMs - b.createdAtMs);
        onChange(list);
      },
      (error) => handleFirestoreError(error, OperationType.LIST, `users/${uid}/attempts`)
    );
  },

  observeErrors(uid: string, onChange: (errors: ErrorLogEntry[]) => void): () => void {
    const col = userSubcollection(uid, 'errors');
    return onSnapshot(
      col,
      (snap) => {
        const list: ErrorLogEntry[] = snap.docs.map((d) => {
          const data = d.data();
          return {
            id: d.id,
            qid: String(data.qid ?? ''),
            resolved: Boolean(data.resolved),
            category: String(data.category ?? ''),
            dateKey: String(data.dateKey ?? ''),
            createdAtMs: tsToMillis(data.createdAt),
          };
        });
        list.sort((a, b) => b.createdAtMs - a.createdAtMs);
        onChange(list);
      },
      (error) => handleFirestoreError(error, OperationType.LIST, `users/${uid}/errors`)
    );
  },

  observeCardProgress(uid: string, onChange: (cards: Record<string, string>) => void): () => void {
    const col = userSubcollection(uid, 'card_progress');
    return onSnapshot(
      col,
      (snap) => {
        const map: Record<string, string> = {};
        snap.forEach((d) => {
          const data = d.data();
          const key = typeof data.cardId === 'string' && data.cardId ? data.cardId : d.id;
          map[key] = String(data.status ?? 'review');
        });
        onChange(map);
      },
      (error) => handleFirestoreError(error, OperationType.LIST, `users/${uid}/card_progress`)
    );
  },

  observeSessions(uid: string, onChange: (sessions: StudySession[]) => void): () => void {
    const col = userSubcollection(uid, 'study_sessions');
    return onSnapshot(
      col,
      (snap) => {
        const list: StudySession[] = snap.docs.map((d) => {
          const data = d.data();
          return {
            id: d.id,
            dateKey: String(data.dateKey ?? ''),
            minutes: typeof data.minutes === 'number' ? data.minutes : 25,
            tag: String(data.tag ?? ''),
            createdAtMs: tsToMillis(data.createdAt),
          };
        });
        list.sort((a, b) => b.createdAtMs - a.createdAtMs);
        onChange(list);
      },
      (error) => handleFirestoreError(error, OperationType.LIST, `users/${uid}/study_sessions`)
    );
  },

  observeCustomPyqs(uid: string, onChange: (pyqs: PyqQuestion[]) => void): () => void {
    const col = userSubcollection(uid, 'custom_pyqs');
    return onSnapshot(
      col,
      (snap) => {
        const list: PyqQuestion[] = snap.docs.map((d) => {
          const data = d.data();
          return {
            id: d.id,
            exam: String(data.exam ?? 'JEE'),
            year: typeof data.year === 'number' ? data.year : null,
            subject: String(data.subject ?? 'Physics'),
            chapterId: String(data.chapterId ?? ''),
            difficulty: String(data.difficulty ?? 'Medium'),
            tags: Array.isArray(data.tags) ? data.tags.map(String) : [],
            q: String(data.question ?? ''),
            options: Array.isArray(data.options) ? data.options.map(String) : [],
            answer: typeof data.answerIndex === 'number' ? data.answerIndex : 0,
            explain: String(data.explain ?? ''),
            custom: true,
          };
        });
        onChange(list);
      },
      (error) => handleFirestoreError(error, OperationType.LIST, `users/${uid}/custom_pyqs`)
    );
  },

  async ensureProfile(uid: string, defaultName: string): Promise<void> {
    const ref = userDocRef(uid);
    try {
      const snap = await getDoc(ref);
      if (!snap.exists()) {
        await setDoc(ref, {
          userId: uid,
          name: defaultName.trim().slice(0, 40),
          cls: '11',
          exam: 'JEE',
          goalMinutes: 180,
          createdAt: serverTimestamp(),
          updatedAt: serverTimestamp(),
        });
      }
    } catch (error) {
      handleFirestoreError(error, OperationType.WRITE, ref.path);
    }
  },

  async saveProfile(uid: string, name: string, cls: string, exam: string, goalMinutes: number): Promise<void> {
    const ref = userDocRef(uid);
    const safeName = name.trim().slice(0, 40);
    const safeCls = (StudyConstants.CLASSES as readonly string[]).includes(cls) ? cls : '11';
    const safeExam = (StudyConstants.EXAMS as readonly string[]).includes(exam) ? exam : 'JEE';
    const safeGoal = Math.max(15, Math.min(900, Math.round(goalMinutes)));
    try {
      const snap = await getDoc(ref);
      if (snap.exists()) {
        await updateDoc(ref, {
          name: safeName,
          cls: safeCls,
          exam: safeExam,
          goalMinutes: safeGoal,
          updatedAt: serverTimestamp(),
        });
      } else {
        await setDoc(ref, {
          userId: uid,
          name: safeName,
          cls: safeCls,
          exam: safeExam,
          goalMinutes: safeGoal,
          createdAt: serverTimestamp(),
          updatedAt: serverTimestamp(),
        });
      }
    } catch (error) {
      handleFirestoreError(error, OperationType.UPDATE, ref.path);
    }
  },

  async setChapterStatus(uid: string, chapterId: string, status: string): Promise<void> {
    const safeStatus = (StudyConstants.CHAPTER_STATUSES as readonly string[]).includes(status)
      ? status
      : 'Not Started';
    const ref = doc(db, 'users', uid, 'chapter_statuses', chapterId);
    try {
      const snap = await getDoc(ref);
      if (snap.exists()) {
        await updateDoc(ref, {
          status: safeStatus,
          updatedAt: serverTimestamp(),
        });
      } else {
        await setDoc(ref, {
          chapterId,
          status: safeStatus,
          createdAt: serverTimestamp(),
          updatedAt: serverTimestamp(),
        });
      }
    } catch (error) {
      handleFirestoreError(error, OperationType.WRITE, ref.path);
    }
  },

  async addTask(uid: string, text: string, subject: string): Promise<string> {
    const cleanText = text.trim().slice(0, 120);
    if (!cleanText) throw new Error('Task text must not be empty');
    const safeSub = (StudyConstants.TASK_SUBJECTS as readonly string[]).includes(subject)
      ? subject
      : 'General';
    const id = makeId('tsk');
    const ref = doc(db, 'users', uid, 'tasks', id);
    try {
      await setDoc(ref, {
        text: cleanText,
        subject: safeSub,
        done: false,
        date: StudyAnalytics.todayKey(),
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
      });
      return id;
    } catch (error) {
      handleFirestoreError(error, OperationType.CREATE, ref.path);
    }
  },

  async toggleTask(uid: string, task: StudyTask): Promise<void> {
    const ref = doc(db, 'users', uid, 'tasks', task.id);
    const newDone = !task.done;
    try {
      await updateDoc(ref, {
        done: newDone,
        doneOn: newDone ? StudyAnalytics.todayKey() : null,
        updatedAt: serverTimestamp(),
      });
    } catch (error) {
      handleFirestoreError(error, OperationType.UPDATE, ref.path);
    }
  },

  async deleteTask(uid: string, taskId: string): Promise<void> {
    const ref = doc(db, 'users', uid, 'tasks', taskId);
    try {
      await deleteDoc(ref);
    } catch (error) {
      handleFirestoreError(error, OperationType.DELETE, ref.path);
    }
  },

  async clearCompletedTasks(uid: string, tasks: StudyTask[]): Promise<void> {
    const completed = tasks.filter((t) => t.done);
    if (completed.length === 0) return;
    try {
      for (let i = 0; i < completed.length; i += 400) {
        const batch = writeBatch(db);
        completed.slice(i, i + 400).forEach((t) => {
          batch.delete(doc(db, 'users', uid, 'tasks', t.id));
        });
        await batch.commit();
      }
    } catch (error) {
      handleFirestoreError(error, OperationType.DELETE, `users/${uid}/tasks`);
    }
  },

  async createNote(uid: string, title = '', subject = 'General', body = ''): Promise<string> {
    const id = makeId('note');
    const ref = doc(db, 'users', uid, 'notes', id);
    const safeSub = (StudyConstants.TASK_SUBJECTS as readonly string[]).includes(subject)
      ? subject
      : 'General';
    try {
      await setDoc(ref, {
        title: title.slice(0, 120),
        subject: safeSub,
        body: body.slice(0, 20000),
        imageIds: [],
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
      });
      return id;
    } catch (error) {
      handleFirestoreError(error, OperationType.CREATE, ref.path);
    }
  },

  async updateNote(
    uid: string,
    noteId: string,
    title: string,
    subject: string,
    body: string,
    imageIds: string[]
  ): Promise<void> {
    const ref = doc(db, 'users', uid, 'notes', noteId);
    const safeSub = (StudyConstants.TASK_SUBJECTS as readonly string[]).includes(subject)
      ? subject
      : 'General';
    try {
      await updateDoc(ref, {
        title: title.slice(0, 120),
        subject: safeSub,
        body: body.slice(0, 20000),
        imageIds: imageIds.slice(0, 6),
        updatedAt: serverTimestamp(),
      });
    } catch (error) {
      handleFirestoreError(error, OperationType.UPDATE, ref.path);
    }
  },

  async addNoteImage(uid: string, note: StudyNote, dataUrl: string): Promise<string> {
    if (note.imageIds.length >= 6) throw new Error('Max 6 images per note');
    const imgId = makeId('img');
    const imgRef = doc(db, 'users', uid, 'note_images', imgId);
    const noteRef = doc(db, 'users', uid, 'notes', note.id);
    try {
      const batch = writeBatch(db);
      batch.set(imgRef, {
        data: dataUrl,
        createdAt: serverTimestamp(),
      });
      batch.update(noteRef, {
        imageIds: [...note.imageIds, imgId].slice(0, 6),
        updatedAt: serverTimestamp(),
      });
      await batch.commit();
      return imgId;
    } catch (error) {
      handleFirestoreError(error, OperationType.WRITE, noteRef.path);
    }
  },

  async removeNoteImage(uid: string, note: StudyNote, imageId: string): Promise<void> {
    const imgRef = doc(db, 'users', uid, 'note_images', imageId);
    const noteRef = doc(db, 'users', uid, 'notes', note.id);
    try {
      const batch = writeBatch(db);
      batch.update(noteRef, {
        imageIds: note.imageIds.filter((id) => id !== imageId),
        updatedAt: serverTimestamp(),
      });
      batch.delete(imgRef);
      await batch.commit();
    } catch (error) {
      handleFirestoreError(error, OperationType.WRITE, noteRef.path);
    }
  },

  async deleteNote(uid: string, note: StudyNote): Promise<void> {
    const noteRef = doc(db, 'users', uid, 'notes', note.id);
    try {
      const batch = writeBatch(db);
      batch.delete(noteRef);
      note.imageIds.forEach((imgId) => {
        batch.delete(doc(db, 'users', uid, 'note_images', imgId));
      });
      await batch.commit();
    } catch (error) {
      handleFirestoreError(error, OperationType.DELETE, noteRef.path);
    }
  },

  async logAttemptAndSyncErrors(
    uid: string,
    question: PyqQuestion,
    selectedOption: number,
    secs: number,
    confidence: string,
    existingOpenErrorsForQid: ErrorLogEntry[]
  ): Promise<{ attemptId: string; errorId: string | null }> {
    const attemptId = makeId('att');
    const attemptRef = doc(db, 'users', uid, 'attempts', attemptId);
    const isCorrect = selectedOption === question.answer;
    const safeSecs = Math.max(1, Math.min(7200, Math.round(secs)));
    const safeConf = (StudyConstants.CONFIDENCE_LEVELS as readonly string[]).includes(confidence)
      ? confidence
      : 'MEDIUM';
    const dateKey = StudyAnalytics.todayKey();
    let createdErrorId: string | null = null;

    try {
      const batch = writeBatch(db);
      batch.set(attemptRef, {
        qid: question.id.slice(0, 64),
        chapterId: question.chapterId.slice(0, 64),
        subject: (StudyConstants.SUBJECTS as readonly string[]).includes(question.subject)
          ? question.subject
          : 'Physics',
        selectedOption: Math.max(0, Math.min(3, Math.round(selectedOption))),
        correct: isCorrect,
        secs: safeSecs,
        confidence: safeConf,
        errorCategory: '',
        dateKey,
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
      });

      if (!isCorrect && existingOpenErrorsForQid.length === 0) {
        const errId = makeId('err');
        createdErrorId = errId;
        batch.set(doc(db, 'users', uid, 'errors', errId), {
          qid: question.id.slice(0, 64),
          resolved: false,
          category: '',
          dateKey,
          createdAt: serverTimestamp(),
          updatedAt: serverTimestamp(),
        });
      } else if (isCorrect && existingOpenErrorsForQid.length > 0) {
        existingOpenErrorsForQid.forEach((entry) => {
          batch.update(doc(db, 'users', uid, 'errors', entry.id), {
            resolved: true,
            updatedAt: serverTimestamp(),
          });
        });
      }

      await batch.commit();
      return { attemptId, errorId: createdErrorId };
    } catch (error) {
      handleFirestoreError(error, OperationType.WRITE, attemptRef.path);
    }
  },

  async setAttemptErrorCategory(
    uid: string,
    attemptId: string | null,
    errorId: string | null,
    category: string
  ): Promise<void> {
    const cleanCat = category.slice(0, 40);
    try {
      const batch = writeBatch(db);
      if (attemptId) {
        batch.update(doc(db, 'users', uid, 'attempts', attemptId), {
          errorCategory: cleanCat,
          updatedAt: serverTimestamp(),
        });
      }
      if (errorId) {
        batch.update(doc(db, 'users', uid, 'errors', errorId), {
          category: cleanCat,
          updatedAt: serverTimestamp(),
        });
      }
      await batch.commit();
    } catch (error) {
      handleFirestoreError(error, OperationType.UPDATE, `users/${uid}/attempts`);
    }
  },

  async toggleErrorResolved(uid: string, entry: ErrorLogEntry): Promise<void> {
    const ref = doc(db, 'users', uid, 'errors', entry.id);
    try {
      await updateDoc(ref, {
        resolved: !entry.resolved,
        updatedAt: serverTimestamp(),
      });
    } catch (error) {
      handleFirestoreError(error, OperationType.UPDATE, ref.path);
    }
  },

  async deleteErrorEntry(uid: string, errorId: string): Promise<void> {
    const ref = doc(db, 'users', uid, 'errors', errorId);
    try {
      await deleteDoc(ref);
    } catch (error) {
      handleFirestoreError(error, OperationType.DELETE, ref.path);
    }
  },

  async setFlashcardStatus(uid: string, cardId: string, status: string): Promise<void> {
    const safeStatus = status === 'known' ? 'known' : 'review';
    const ref = doc(db, 'users', uid, 'card_progress', cardId);
    try {
      const snap = await getDoc(ref);
      if (snap.exists()) {
        await updateDoc(ref, {
          status: safeStatus,
          updatedAt: serverTimestamp(),
        });
      } else {
        await setDoc(ref, {
          cardId,
          status: safeStatus,
          createdAt: serverTimestamp(),
          updatedAt: serverTimestamp(),
        });
      }
    } catch (error) {
      handleFirestoreError(error, OperationType.WRITE, ref.path);
    }
  },

  async logStudySession(uid: string, minutes: number, tag: string): Promise<string> {
    const safeMins = Math.max(1, Math.min(900, Math.round(minutes)));
    const cleanTag = tag.trim().slice(0, 80) || 'Untagged';
    const id = makeId('ses');
    const ref = doc(db, 'users', uid, 'study_sessions', id);
    try {
      await setDoc(ref, {
        dateKey: StudyAnalytics.todayKey(),
        minutes: safeMins,
        tag: cleanTag,
        createdAt: serverTimestamp(),
      });
      return id;
    } catch (error) {
      handleFirestoreError(error, OperationType.CREATE, ref.path);
    }
  },

  async importCustomQuestions(uid: string, questions: PyqQuestion[]): Promise<void> {
    if (questions.length === 0) return;
    try {
      for (let i = 0; i < questions.length; i += 200) {
        const batch = writeBatch(db);
        questions.slice(i, i + 200).forEach((q) => {
          const ref = doc(db, 'users', uid, 'custom_pyqs', q.id);
          const payload: Record<string, unknown> = {
            exam: q.exam,
            subject: q.subject,
            chapterId: q.chapterId,
            difficulty: q.difficulty,
            tags: q.tags,
            question: q.q,
            options: q.options,
            answerIndex: q.answer,
            explain: q.explain,
            createdAt: serverTimestamp(),
          };
          if (q.year !== null) payload.year = q.year;
          batch.set(ref, payload);
        });
        await batch.commit();
      }
    } catch (error) {
      handleFirestoreError(error, OperationType.WRITE, `users/${uid}/custom_pyqs`);
    }
  },

  async clearCustomQuestions(uid: string, questionIds: string[]): Promise<void> {
    if (questionIds.length === 0) return;
    try {
      for (let i = 0; i < questionIds.length; i += 400) {
        const batch = writeBatch(db);
        questionIds.slice(i, i + 400).forEach((id) => {
          batch.delete(doc(db, 'users', uid, 'custom_pyqs', id));
        });
        await batch.commit();
      }
    } catch (error) {
      handleFirestoreError(error, OperationType.DELETE, `users/${uid}/custom_pyqs`);
    }
  },

  async resetAllUserData(uid: string): Promise<void> {
    const subs = [
      'chapter_statuses',
      'tasks',
      'notes',
      'note_images',
      'attempts',
      'errors',
      'card_progress',
      'study_sessions',
      'custom_pyqs',
    ];
    try {
      for (const sub of subs) {
        const snap = await getDocs(userSubcollection(uid, sub));
        const docs = snap.docs;
        for (let i = 0; i < docs.length; i += 400) {
          const batch = writeBatch(db);
          docs.slice(i, i + 400).forEach((d) => batch.delete(d.ref));
          await batch.commit();
        }
      }
      await studyRepository.saveProfile(uid, '', '11', 'JEE', 180);
    } catch (error) {
      handleFirestoreError(error, OperationType.DELETE, `users/${uid}`);
    }
  },
};
