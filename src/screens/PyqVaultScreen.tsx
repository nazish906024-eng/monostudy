import { useEffect, useMemo, useState } from 'react';
import { HelpCircle, Search } from 'lucide-react';
import { EmptyStatePanel, PageHeader, SectionPanel } from '../components/ui/Primitives';
import { StudyStore } from '../hooks/useStudyStore';
import { StudyConstants } from '../types/study';
import { formatMathText } from '../utils/mathFormat';

interface PyqVaultScreenProps {
  store: StudyStore;
}

export function PyqVaultScreen({ store }: PyqVaultScreenProps) {
  const [searchQuery, setSearchQuery] = useState('');
  const [filterSubject, setFilterSubject] = useState('');
  const [filterChapter, setFilterChapter] = useState('');
  const [filterDifficulty, setFilterDifficulty] = useState('');
  const [filterExam, setFilterExam] = useState('');
  const [filterTag, setFilterTag] = useState('');
  const [confidenceByQid, setConfidenceByQid] = useState<Record<string, string>>({});

  const availableChapters = useMemo(() => {
    const set = new Set<string>();
    for (const q of store.allPyqs) {
      if (q.chapterId) set.add(q.chapterId);
    }
    return Array.from(set);
  }, [store.allPyqs]);

  const availableTags = useMemo(() => {
    const set = new Set<string>();
    for (const q of store.allPyqs) {
      for (const t of q.tags) set.add(t);
    }
    return Array.from(set).sort();
  }, [store.allPyqs]);

  const filteredQuestions = useMemo(() => {
    const query = searchQuery.trim().toLowerCase();
    return store.allPyqs.filter(
      (q) =>
        (!filterSubject || q.subject === filterSubject) &&
        (!filterChapter || q.chapterId === filterChapter) &&
        (!filterDifficulty || q.difficulty === filterDifficulty) &&
        (!filterExam || q.exam === filterExam) &&
        (!filterTag || q.tags.includes(filterTag)) &&
        (!query || `${q.q} ${q.tags.join(' ')}`.toLowerCase().includes(query))
    );
  }, [
    store.allPyqs,
    searchQuery,
    filterSubject,
    filterChapter,
    filterDifficulty,
    filterExam,
    filterTag,
  ]);

  useEffect(() => {
    filteredQuestions.forEach((q) => store.markQuestionVisible(q.id));
  }, [filteredQuestions, store]);

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="Past-paper practice"
        title="PYQ vault"
        subtitle="Search and filter questions, then log how confident you were."
      />

      <SectionPanel>
        <div className="relative">
          <Search
            className="pointer-events-none absolute top-1/2 left-3.5 h-4 w-4 -translate-y-1/2 text-[#8F9BC4]"
            aria-hidden="true"
          />
          <input
            type="search"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search questions or tags…"
            className="min-h-[44px] w-full rounded-[10px] border border-[#27325A] bg-[#0B1020] pr-3.5 pl-10 text-sm text-[#EEF1FF] placeholder-[#8F9BC4] focus:border-[#8B7DFF] focus:outline-none"
          />
        </div>

        <div className="mt-3 grid grid-cols-2 gap-2.5 sm:grid-cols-3 lg:grid-cols-5">
          <select
            aria-label="Filter by subject"
            value={filterSubject}
            onChange={(e) => setFilterSubject(e.target.value)}
            className="min-h-[40px] rounded-[9px] border border-[#27325A] bg-[#1B2442] px-3 py-1.5 text-xs font-medium text-[#EEF1FF] focus:border-[#8B7DFF] focus:outline-none"
          >
            <option value="">All subjects</option>
            {StudyConstants.SUBJECTS.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>

          <select
            aria-label="Filter by chapter"
            value={filterChapter}
            onChange={(e) => setFilterChapter(e.target.value)}
            className="min-h-[40px] rounded-[9px] border border-[#27325A] bg-[#1B2442] px-3 py-1.5 text-xs font-medium text-[#EEF1FF] focus:border-[#8B7DFF] focus:outline-none"
          >
            <option value="">All chapters</option>
            {availableChapters.map((id) => (
              <option key={id} value={id}>
                {store.chapterMap[id]?.title ?? id}
              </option>
            ))}
          </select>

          <select
            aria-label="Filter by difficulty"
            value={filterDifficulty}
            onChange={(e) => setFilterDifficulty(e.target.value)}
            className="min-h-[40px] rounded-[9px] border border-[#27325A] bg-[#1B2442] px-3 py-1.5 text-xs font-medium text-[#EEF1FF] focus:border-[#8B7DFF] focus:outline-none"
          >
            <option value="">Any difficulty</option>
            {StudyConstants.DIFFICULTIES.map((d) => (
              <option key={d} value={d}>
                {d}
              </option>
            ))}
          </select>

          <select
            aria-label="Filter by exam"
            value={filterExam}
            onChange={(e) => setFilterExam(e.target.value)}
            className="min-h-[40px] rounded-[9px] border border-[#27325A] bg-[#1B2442] px-3 py-1.5 text-xs font-medium text-[#EEF1FF] focus:border-[#8B7DFF] focus:outline-none"
          >
            <option value="">Any exam</option>
            <option value="JEE">JEE</option>
            <option value="NEET">NEET</option>
          </select>

          <select
            aria-label="Filter by concept tag"
            value={filterTag}
            onChange={(e) => setFilterTag(e.target.value)}
            className="min-h-[40px] rounded-[9px] border border-[#27325A] bg-[#1B2442] px-3 py-1.5 text-xs font-medium text-[#EEF1FF] focus:border-[#8B7DFF] focus:outline-none"
          >
            <option value="">Any concept tag</option>
            {availableTags.map((t) => (
              <option key={t} value={t}>
                #{t}
              </option>
            ))}
          </select>
        </div>

        {store.pyqNotice && <p className="mt-3 text-xs text-[#8F9BC4]">{store.pyqNotice}</p>}
      </SectionPanel>

      {filteredQuestions.length === 0 ? (
        <EmptyStatePanel icon={HelpCircle} message="No questions match these filters." />
      ) : (
        <div className="space-y-4">
          {filteredQuestions.map((question) => {
            const outcome = store.answeredPyqs[question.id];
            const confidence = confidenceByQid[question.id] ?? 'MEDIUM';
            const chapterTitle = store.chapterMap[question.chapterId]?.title ?? '';

            return (
              <SectionPanel key={question.id}>
                <div className="flex flex-wrap items-center gap-x-2 gap-y-1 text-xs text-[#8F9BC4]">
                  <span className="font-medium text-[#8B7DFF]">
                    {question.exam} {question.year ?? '· practice'}
                  </span>
                  <span aria-hidden="true">·</span>
                  <span>{question.subject}</span>
                  <span aria-hidden="true">·</span>
                  <span>{question.difficulty}</span>
                  {chapterTitle && (
                    <>
                      <span aria-hidden="true">·</span>
                      <span>{chapterTitle}</span>
                    </>
                  )}
                  {question.tags.map((tag) => (
                    <span key={tag}>· #{tag}</span>
                  ))}
                </div>

                <p className="mt-2.5 text-base font-medium leading-relaxed text-[#EEF1FF]">
                  {formatMathText(question.q)}
                </p>

                <div className="mt-3 flex flex-wrap items-center gap-2">
                  <span className="text-xs text-[#8F9BC4]">Confidence:</span>
                  <div className="inline-flex rounded-[9px] border border-[#27325A] bg-[#0B1020] p-0.5">
                    {StudyConstants.CONFIDENCE_LEVELS.map((level) => {
                      const label = level.charAt(0) + level.slice(1).toLowerCase();
                      const active = confidence === level;
                      return (
                        <button
                          key={level}
                          type="button"
                          disabled={Boolean(outcome)}
                          onClick={() =>
                            setConfidenceByQid((prev) => ({ ...prev, [question.id]: level }))
                          }
                          className={`rounded-[7px] px-2.5 py-1 text-xs font-medium transition-colors ${
                            active
                              ? 'bg-[#8B7DFF] text-[#0B1020]'
                              : 'text-[#8F9BC4] hover:text-[#EEF1FF]'
                          }`}
                        >
                          {label}
                        </button>
                      );
                    })}
                  </div>
                </div>

                <div className="mt-3 grid grid-cols-1 gap-2 sm:grid-cols-2">
                  {question.options.map((optText, idx) => {
                    const isCorrectOpt = idx === question.answer;
                    const isSelectedOpt = outcome?.selectedOption === idx;
                    let optClass =
                      'border-[#27325A] bg-[#1B2442] text-[#EEF1FF] hover:border-[#8B7DFF]';
                    if (outcome) {
                      if (isCorrectOpt) {
                        optClass = 'border-[#3DDC97] bg-[#3DDC97]/15 text-[#EEF1FF]';
                      } else if (isSelectedOpt) {
                        optClass = 'border-[#FF6B7A] bg-[#FF6B7A]/15 text-[#EEF1FF]';
                      } else {
                        optClass = 'border-[#27325A] bg-[#1B2442]/50 text-[#8F9BC4]';
                      }
                    }

                    return (
                      <button
                        key={idx}
                        type="button"
                        disabled={Boolean(outcome)}
                        onClick={() => void store.answerPyq(question, idx, confidence)}
                        className={`min-h-[44px] rounded-[10px] border px-3.5 py-2.5 text-left text-sm transition-colors ${optClass}`}
                      >
                        {'ABCD'[idx]}. {formatMathText(optText)}
                      </button>
                    );
                  })}
                </div>

                {outcome && (
                  <div className="mt-3.5 rounded-[10px] border border-[#8B7DFF]/50 bg-[#1B2442] p-3.5">
                    <p
                      className={`tabular-nums text-sm font-bold ${
                        outcome.correct ? 'text-[#3DDC97]' : 'text-[#FF6B7A]'
                      }`}
                    >
                      {outcome.correct
                        ? `✓ Correct · ${outcome.secs}s`
                        : `✗ Not quite — added to your Error Log · ${outcome.secs}s`}
                    </p>
                    {question.explain && (
                      <p className="mt-1.5 text-sm text-[#EEF1FF]">
                        {formatMathText(question.explain)}
                      </p>
                    )}

                    {!outcome.correct && (
                      <div className="mt-3 border-t border-[#27325A] pt-3">
                        <p className="text-xs text-[#8F9BC4]">Why did you miss it?</p>
                        <div className="mt-2 flex flex-wrap gap-2">
                          {StudyConstants.ERROR_CATEGORIES.map((cat) => {
                            const active = outcome.selectedErrorCategory === cat;
                            return (
                              <button
                                key={cat}
                                type="button"
                                onClick={() => void store.setPyqErrorCategory(question.id, cat)}
                                className={`min-h-[34px] rounded-[8px] border px-3 py-1 text-xs font-medium transition-colors ${
                                  active
                                    ? 'border-[#8B7DFF] bg-[#8B7DFF] text-[#0B1020]'
                                    : 'border-[#27325A] bg-[#141B33] text-[#8F9BC4] hover:text-[#EEF1FF]'
                                }`}
                              >
                                {cat}
                              </button>
                            );
                          })}
                        </div>
                      </div>
                    )}
                  </div>
                )}
              </SectionPanel>
            );
          })}
        </div>
      )}
    </div>
  );
}
