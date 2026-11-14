import { useMemo, useState } from 'react';
import { Flag } from 'lucide-react';
import { EmptyStatePanel, PageHeader, SectionPanel } from '../components/ui/Primitives';
import { StudyStore } from '../hooks/useStudyStore';
import { PyqQuestion } from '../types/study';
import { formatMathText } from '../utils/mathFormat';

interface ErrorLogScreenProps {
  store: StudyStore;
}

export function ErrorLogScreen({ store }: ErrorLogScreenProps) {
  const [showResolved, setShowResolved] = useState(false);

  const pyqMap = useMemo(() => {
    const map: Record<string, PyqQuestion> = {};
    for (const q of store.allPyqs) map[q.id] = q;
    return map;
  }, [store.allPyqs]);

  const filteredErrors = useMemo(
    () =>
      store.errors.filter(
        (e) => e.resolved === showResolved && Boolean(pyqMap[e.qid])
      ),
    [store.errors, showResolved, pyqMap]
  );

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="Learn from mistakes"
        title="Error log vault"
        subtitle="Questions you got wrong during practice."
        trailing={
          <div className="inline-flex rounded-[10px] border border-[#27325A] bg-[#141B33] p-1">
            <button
              type="button"
              onClick={() => setShowResolved(false)}
              className={`min-h-[36px] rounded-[8px] px-3.5 py-1 text-xs font-semibold transition-colors ${
                !showResolved
                  ? 'bg-[#8B7DFF] text-[#0B1020]'
                  : 'text-[#8F9BC4] hover:text-[#EEF1FF]'
              }`}
            >
              Open
            </button>
            <button
              type="button"
              onClick={() => setShowResolved(true)}
              className={`min-h-[36px] rounded-[8px] px-3.5 py-1 text-xs font-semibold transition-colors ${
                showResolved
                  ? 'bg-[#8B7DFF] text-[#0B1020]'
                  : 'text-[#8F9BC4] hover:text-[#EEF1FF]'
              }`}
            >
              Resolved
            </button>
          </div>
        }
      />

      {filteredErrors.length === 0 ? (
        <EmptyStatePanel
          icon={Flag}
          message={
            !showResolved
              ? 'No open errors. Wrong answers in the PYQ vault land here.'
              : 'Nothing resolved yet.'
          }
        />
      ) : (
        <div className="space-y-4">
          {filteredErrors.map((entry) => {
            const question = pyqMap[entry.qid];
            if (!question) return null;
            const chapterTitle = store.chapterMap[question.chapterId]?.title ?? '';
            const ansText = question.options[question.answer] ?? '';

            return (
              <SectionPanel key={entry.id}>
                <div className="flex flex-wrap items-center gap-x-2 gap-y-1 text-xs text-[#8F9BC4]">
                  <span className="font-medium text-[#EEF1FF]">{question.subject}</span>
                  {chapterTitle && (
                    <>
                      <span aria-hidden="true">·</span>
                      <span>{chapterTitle}</span>
                    </>
                  )}
                  {entry.category && (
                    <>
                      <span aria-hidden="true">·</span>
                      <span className="font-semibold text-[#FF6B7A]">{entry.category}</span>
                    </>
                  )}
                  {entry.dateKey && (
                    <>
                      <span aria-hidden="true">·</span>
                      <span className="tabular-nums">{entry.dateKey}</span>
                    </>
                  )}
                </div>

                <p className="mt-2.5 text-base font-medium text-[#EEF1FF]">
                  {formatMathText(question.q)}
                </p>

                <div className="mt-3 rounded-[10px] border border-[#8B7DFF]/50 bg-[#1B2442] p-3.5">
                  <p className="text-sm font-bold text-[#3DDC97]">
                    Answer: {'ABCD'[question.answer]}. {formatMathText(ansText)}
                  </p>
                  {question.explain && (
                    <p className="mt-1.5 text-sm text-[#EEF1FF]">
                      {formatMathText(question.explain)}
                    </p>
                  )}
                </div>

                <div className="mt-3 flex flex-wrap gap-2.5">
                  <button
                    type="button"
                    onClick={() => void store.toggleErrorResolved(entry)}
                    className="min-h-[40px] rounded-[10px] border border-[#27325A] bg-[#1B2442] px-4 py-2 text-xs font-semibold text-[#EEF1FF] transition-colors hover:border-[#8B7DFF]"
                  >
                    {entry.resolved ? 'Reopen' : 'Mark resolved'}
                  </button>
                  <button
                    type="button"
                    onClick={() => void store.deleteErrorEntry(entry.id)}
                    className="min-h-[40px] rounded-[10px] border border-[#27325A] bg-[#1B2442] px-4 py-2 text-xs font-semibold text-[#FF6B7A] transition-colors hover:border-[#FF6B7A]"
                  >
                    Remove
                  </button>
                </div>
              </SectionPanel>
            );
          })}
        </div>
      )}
    </div>
  );
}
