import { useMemo, useState } from 'react';
import { Layers } from 'lucide-react';
import { EmptyStatePanel, PageHeader } from '../components/ui/Primitives';
import { StudyStore } from '../hooks/useStudyStore';
import { BUNDLED_FLASHCARDS, StudyConstants } from '../types/study';
import { formatMathText } from '../utils/mathFormat';

interface FlashcardsScreenProps {
  store: StudyStore;
}

export function FlashcardsScreen({ store }: FlashcardsScreenProps) {
  const [selectedSubject, setSelectedSubject] = useState('');
  const [cardIndex, setCardIndex] = useState(0);
  const [isFlipped, setIsFlipped] = useState(false);

  const deck = useMemo(
    () =>
      BUNDLED_FLASHCARDS.filter(
        (card) => !selectedSubject || card.subject === selectedSubject
      ),
    [selectedSubject]
  );

  const safeIndex = deck.length === 0 ? 0 : cardIndex % deck.length;
  const currentCard = deck[safeIndex] ?? null;
  const knownCount = useMemo(
    () => deck.filter((c) => store.cardProgress[c.id] === 'known').length,
    [deck, store.cardProgress]
  );

  function markCurrent(status: 'known' | 'review') {
    if (!currentCard) return;
    void store.setFlashcardStatus(currentCard.id, status);
    setCardIndex((prev) => prev + 1);
    setIsFlipped(false);
  }

  return (
    <div className="mx-auto max-w-[700px] space-y-4">
      <PageHeader
        eyebrow="Quick revision"
        title="Flashcards"
        subtitle="Click the card to flip between prompt and formula."
        trailing={
          <select
            aria-label="Filter flashcards by subject"
            value={selectedSubject}
            onChange={(e) => {
              setSelectedSubject(e.target.value);
              setCardIndex(0);
              setIsFlipped(false);
            }}
            className="min-h-[42px] rounded-[10px] border border-[#27325A] bg-[#141B33] px-3.5 py-2 text-sm font-medium text-[#EEF1FF] focus:border-[#8B7DFF] focus:outline-none"
          >
            <option value="">All subjects</option>
            {StudyConstants.SUBJECTS.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
        }
      />

      {!currentCard ? (
        <EmptyStatePanel
          icon={Layers}
          message="No flashcards available for this filter."
        />
      ) : (
        <>
          <div className="tabular-nums flex items-center gap-2 text-xs text-[#8F9BC4]">
            <span>
              Card {safeIndex + 1} of {deck.length}
            </span>
            <span aria-hidden="true">·</span>
            <span className="font-semibold text-[#3DDC97]">{knownCount} known</span>
          </div>

          <div className="perspective-1000">
            <button
              type="button"
              onClick={() => setIsFlipped((f) => !f)}
              className="transform-style-3d relative min-h-[260px] w-full cursor-pointer text-center transition-transform duration-500"
              style={{ transform: isFlipped ? 'rotateY(180deg)' : 'rotateY(0deg)' }}
            >
              <div className="backface-hidden absolute inset-0 flex flex-col items-center justify-center rounded-[20px] border border-[#8B7DFF]/50 bg-[#141B33] p-8 shadow-xl">
                <span className="text-xs font-bold tracking-widest text-[#8B7DFF]">
                  {currentCard.subject.toUpperCase()}
                </span>
                <p className="font-display mt-3 text-2xl font-semibold text-[#EEF1FF]">
                  {currentCard.front}
                </p>
                <span className="mt-4 text-xs text-[#8F9BC4]">Click to reveal</span>
              </div>

              <div className="backface-hidden rotate-y-180 absolute inset-0 flex flex-col items-center justify-center rounded-[20px] border border-[#8B7DFF] bg-[#1B2442] p-8 shadow-xl">
                <span className="text-xs font-bold tracking-widest text-[#3DDC97]">
                  ANSWER
                </span>
                <p className="font-display mt-3 text-xl font-semibold text-[#EEF1FF] sm:text-2xl">
                  {formatMathText(currentCard.back)}
                </p>
                <span className="mt-4 text-xs text-[#8F9BC4]">Click to flip back</span>
              </div>
            </button>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <button
              type="button"
              onClick={() => markCurrent('review')}
              className="min-h-[46px] rounded-[11px] border border-[#27325A] bg-[#141B33] px-4 py-2.5 text-sm font-semibold text-[#EEF1FF] transition-colors hover:border-[#8B7DFF]"
            >
              ↺ Review again
            </button>
            <button
              type="button"
              onClick={() => markCurrent('known')}
              className="min-h-[46px] rounded-[11px] bg-[#8B7DFF] px-4 py-2.5 text-sm font-semibold text-[#0B1020] transition-colors hover:bg-[#7A6BFF]"
            >
              ✓ Knew it
            </button>
          </div>

          <p className="text-center text-xs text-[#8F9BC4]">
            Cards are bundled with the app and progress is synced to your account.
          </p>
        </>
      )}
    </div>
  );
}
