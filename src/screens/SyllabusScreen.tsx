import { useMemo, useState } from 'react';
import {
  PageHeader,
  SectionPanel,
  StudyProgressBar,
  WeightageLabel,
} from '../components/ui/Primitives';
import { StudyStore } from '../hooks/useStudyStore';
import { StudyAnalytics, StudyConstants } from '../types/study';

interface SyllabusScreenProps {
  store: StudyStore;
}

export function SyllabusScreen({ store }: SyllabusScreenProps) {
  const [selectedSubject, setSelectedSubject] = useState('All');

  const subjectsForExam = useMemo(
    () => StudyConstants.subjectsForExam(store.profile.exam),
    [store.profile.exam]
  );

  const effectiveSubject =
    selectedSubject !== 'All' && !subjectsForExam.includes(selectedSubject)
      ? 'All'
      : selectedSubject;

  const filteredChapters = useMemo(
    () => StudyAnalytics.filteredChapters(store.chapters, store.profile),
    [store.chapters, store.profile]
  );

  const doneCount = useMemo(
    () =>
      filteredChapters.filter((ch) =>
        StudyConstants.DONE_STATUSES.has(store.chapterStatuses[ch.id] ?? '')
      ).length,
    [filteredChapters, store.chapterStatuses]
  );

  const progressPct = StudyAnalytics.pct(doneCount, filteredChapters.length);

  const visibleSubjects = useMemo(
    () => subjectsForExam.filter((sub) => effectiveSubject === 'All' || sub === effectiveSubject),
    [subjectsForExam, effectiveSubject]
  );

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="Know your next step"
        title="Syllabus tracker"
        subtitle="Status: Not Started → In Progress → Revised → Mastered."
      />

      <div className="flex flex-wrap items-center gap-3">
        <select
          aria-label="Select class"
          value={store.profile.cls}
          onChange={(e) => void store.updateProfileSelection(e.target.value, store.profile.exam)}
          className="min-h-[42px] rounded-[10px] border border-[#27325A] bg-[#141B33] px-3.5 py-2 text-sm font-medium text-[#EEF1FF] focus:border-[#8B7DFF] focus:outline-none"
        >
          {StudyConstants.CLASSES.map((c) => (
            <option key={c} value={c}>
              Class {c}
            </option>
          ))}
        </select>

        <select
          aria-label="Select exam"
          value={store.profile.exam}
          onChange={(e) => void store.updateProfileSelection(store.profile.cls, e.target.value)}
          className="min-h-[42px] rounded-[10px] border border-[#27325A] bg-[#141B33] px-3.5 py-2 text-sm font-medium text-[#EEF1FF] focus:border-[#8B7DFF] focus:outline-none"
        >
          {StudyConstants.EXAMS.map((ex) => (
            <option key={ex} value={ex}>
              Exam: {ex}
            </option>
          ))}
        </select>

        <div className="flex flex-wrap items-center gap-1 rounded-[10px] border border-[#27325A] bg-[#141B33] p-1">
          {['All', ...subjectsForExam].map((sub) => {
            const active = effectiveSubject === sub;
            return (
              <button
                key={sub}
                type="button"
                onClick={() => setSelectedSubject(sub)}
                className={`min-h-[34px] whitespace-nowrap rounded-[8px] px-3 py-1 text-xs font-semibold transition-colors ${
                  active
                    ? 'bg-[#8B7DFF] text-[#0B1020]'
                    : 'text-[#8F9BC4] hover:text-[#EEF1FF]'
                }`}
              >
                {sub}
              </button>
            );
          })}
        </div>
      </div>

      <SectionPanel>
        <p className="text-xs text-[#8F9BC4]">
          Class {store.profile.cls} · {store.profile.exam}
        </p>
        <p className="font-display tabular-nums mt-1 text-xl font-semibold text-[#EEF1FF]">
          {progressPct}% revised or mastered
        </p>
        <div className="mt-2.5">
          <StudyProgressBar fraction={progressPct / 100} />
        </div>
        <p className="tabular-nums mt-2 text-xs text-[#8F9BC4]">
          {doneCount} of {filteredChapters.length} chapters
        </p>
      </SectionPanel>

      {visibleSubjects.map((subject) => {
        const chaptersInSub = filteredChapters.filter((ch) => ch.subject === subject);
        const subDone = chaptersInSub.filter((ch) =>
          StudyConstants.DONE_STATUSES.has(store.chapterStatuses[ch.id] ?? '')
        ).length;
        return (
          <SectionPanel key={subject}>
            <div className="flex items-center justify-between gap-2">
              <h2 className="font-display text-lg font-semibold text-[#EEF1FF]">{subject}</h2>
              <span className="tabular-nums text-sm font-semibold text-[#8F9BC4]">
                {subDone}/{chaptersInSub.length}
              </span>
            </div>

            <div className="mt-3 space-y-2.5">
              {chaptersInSub.map((chapter) => {
                const currentStatus = store.chapterStatuses[chapter.id] ?? 'Not Started';
                return (
                  <div
                    key={chapter.id}
                    className="flex flex-col gap-2.5 rounded-[11px] border border-[#27325A] bg-[#1B2442] p-3.5 sm:flex-row sm:items-center sm:justify-between"
                  >
                    <div className="min-w-0 flex-1">
                      <p className="text-sm font-semibold text-[#EEF1FF]">{chapter.title}</p>
                      <div className="mt-1 flex flex-wrap items-center gap-x-2 gap-y-1 text-xs text-[#8F9BC4]">
                        <WeightageLabel band={chapter.weightage} labelPrefix="weightage" />
                        <span aria-hidden="true">·</span>
                        <WeightageLabel
                          band={chapter.past10YearQuestionFrequency.marker}
                          labelPrefix="10-yr freq (est.)"
                        />
                      </div>
                    </div>

                    <select
                      aria-label={`Status for ${chapter.title}`}
                      value={currentStatus}
                      onChange={(e) => void store.setChapterStatus(chapter.id, e.target.value)}
                      className="min-h-[38px] shrink-0 rounded-[9px] border border-[#27325A] bg-[#141B33] px-3 py-1.5 text-xs font-semibold text-[#EEF1FF] focus:border-[#8B7DFF] focus:outline-none"
                    >
                      {StudyConstants.CHAPTER_STATUSES.map((st) => (
                        <option key={st} value={st}>
                          {st}
                        </option>
                      ))}
                    </select>
                  </div>
                );
              })}
            </div>
          </SectionPanel>
        );
      })}

      <p className="text-xs text-[#8F9BC4]">
        Weightage &amp; frequency bands are provisional estimates.
      </p>
    </div>
  );
}
