import { useMemo } from 'react';
import { BarChart3, Download, Timer } from 'lucide-react';
import {
  EmptyStatePanel,
  PageHeader,
  SectionPanel,
  StatMetricCard,
} from '../components/ui/Primitives';
import { StudyStore } from '../hooks/useStudyStore';
import { StudyAnalytics } from '../types/study';

interface AnalyticsScreenProps {
  store: StudyStore;
}

export function AnalyticsScreen({ store }: AnalyticsScreenProps) {
  const filteredChapters = useMemo(
    () => StudyAnalytics.filteredChapters(store.chapters, store.profile),
    [store.chapters, store.profile]
  );

  const chapterAcc = useMemo(
    () => StudyAnalytics.chapterAccuracyMap(store.attempts),
    [store.attempts]
  );

  const overallAcc = useMemo(
    () => StudyAnalytics.overallAccuracyPct(store.attempts),
    [store.attempts]
  );

  const totalMinutes = useMemo(
    () => store.sessions.reduce((sum, s) => sum + s.minutes, 0),
    [store.sessions]
  );

  const streakInfo = useMemo(
    () => StudyAnalytics.streak(store.sessions, store.attempts, store.tasks),
    [store.sessions, store.attempts, store.tasks]
  );

  const weakSpots = useMemo(
    () =>
      StudyAnalytics.weakSpots(filteredChapters, store.chapterStatuses, store.attempts),
    [filteredChapters, store.chapterStatuses, store.attempts]
  );

  const last7Days = useMemo(() => StudyAnalytics.lastNDays(7), []);

  const dayMinutes = useMemo(
    () =>
      last7Days.map((day) => ({
        label: day.toLocaleDateString('en-US', { weekday: 'short' }),
        minutes: StudyAnalytics.minutesOnDate(store.sessions, StudyAnalytics.todayKey(day)),
      })),
    [store.sessions, last7Days]
  );

  const goalMinutes = Math.max(15, store.profile.goalMinutes);
  const maxChartVal = useMemo(
    () => Math.max(goalMinutes, ...dayMinutes.map((d) => d.minutes), 1),
    [goalMinutes, dayMinutes]
  );

  const last7Keys = useMemo(
    () => new Set(last7Days.map((d) => StudyAnalytics.todayKey(d))),
    [last7Days]
  );

  const sessionsByTag = useMemo(() => {
    const map: Record<string, number> = {};
    for (const s of store.sessions) {
      if (!last7Keys.has(s.dateKey)) continue;
      const tag = s.tag.trim() || 'Untagged';
      map[tag] = (map[tag] ?? 0) + s.minutes;
    }
    return Object.entries(map).sort((a, b) => b[1] - a[1]);
  }, [store.sessions, last7Keys]);

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="Measure what you practise"
        title="Performance analytics"
        subtitle="Based on your synced study and practice activity."
        trailing={
          <button
            type="button"
            onClick={store.downloadWeeklySummaryPng}
            className="inline-flex min-h-[42px] items-center gap-2 whitespace-nowrap rounded-[10px] bg-[#8B7DFF] px-4 py-2 text-sm font-semibold text-[#0B1020] transition-colors hover:bg-[#7A6BFF]"
          >
            <Download className="h-4 w-4" aria-hidden="true" />
            <span>Weekly card</span>
          </button>
        }
      />

      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-4">
        <StatMetricCard
          label="Attempts"
          value={String(store.attempts.length)}
          caption="Total PYQs logged"
        />
        <StatMetricCard
          label="Accuracy"
          value={overallAcc !== null ? `${overallAcc}%` : '—'}
          caption="Overall PYQ score"
        />
        <StatMetricCard
          label="Minutes logged"
          value={String(totalMinutes)}
          caption="All focus sessions"
        />
        <StatMetricCard
          label="Streak (1 grace day)"
          value={`${streakInfo.days}d`}
          caption={
            streakInfo.gracesUsed > 0
              ? `${streakInfo.gracesUsed} grace day(s) used`
              : 'no grace used'
          }
        />
      </div>

      <SectionPanel>
        <h2 className="font-display text-lg font-semibold text-[#EEF1FF]">
          Weak-spot heatmap
        </h2>
        <p className="mt-0.5 text-xs text-[#8F9BC4]">
          Accuracy per chapter · grey = no attempts
        </p>

        <div className="mt-3 grid grid-cols-2 gap-2 sm:grid-cols-4 md:grid-cols-6">
          {filteredChapters.map((chapter) => {
            const acc = chapterAcc[chapter.id]?.accuracyPct ?? null;
            const bgClass =
              acc === null
                ? 'bg-[#1B2442]'
                : acc < 40
                  ? 'bg-[#7A2230]'
                  : acc < 60
                    ? 'bg-[#8A5A1D]'
                    : acc < 80
                      ? 'bg-[#2D6A56]'
                      : 'bg-[#1F8A5E]';
            return (
              <div
                key={chapter.id}
                className={`rounded-[9px] border border-[#27325A]/70 p-2.5 ${bgClass}`}
              >
                <p className="tabular-nums text-sm font-bold text-[#EEF1FF]">
                  {acc !== null ? `${acc}%` : '—'}
                </p>
                <p className="mt-0.5 line-clamp-2 text-[11px] leading-snug text-[#EEF1FF]/90">
                  {chapter.title}
                </p>
              </div>
            );
          })}
        </div>
      </SectionPanel>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <SectionPanel>
          <h2 className="font-display text-lg font-semibold text-[#EEF1FF]">
            High yield · low accuracy
          </h2>
          <p className="mt-0.5 text-xs text-[#8F9BC4]">
            High-weightage chapters scoring under 60%, or not yet revised.
          </p>

          <div className="mt-3">
            {weakSpots.length === 0 ? (
              <EmptyStatePanel
                icon={BarChart3}
                message="No weak spots detected yet."
              />
            ) : (
              <div className="space-y-2">
                {weakSpots.slice(0, 8).map((spot) => (
                  <div
                    key={spot.chapter.id}
                    className="flex items-center justify-between gap-3 rounded-[11px] border border-[#27325A] bg-[#1B2442] px-3.5 py-2.5"
                  >
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm font-medium text-[#EEF1FF]">
                        {spot.chapter.title}
                      </p>
                      <p className="text-xs text-[#8F9BC4]">{spot.chapter.subject}</p>
                    </div>
                    <span className="tabular-nums shrink-0 text-xs font-semibold text-[#FF6B7A]">
                      {spot.accuracyPct !== null ? `${spot.accuracyPct}%` : 'untested'}
                    </span>
                  </div>
                ))}
              </div>
            )}
          </div>
        </SectionPanel>

        <SectionPanel>
          <h2 className="font-display text-lg font-semibold text-[#EEF1FF]">
            Study time vs goal (7 days)
          </h2>

          <div className="relative mt-4 h-[180px] pt-5 pb-6">
            <div
              className="pointer-events-none absolute right-0 left-0 border-t-2 border-dashed border-[#FFA94D]"
              style={{
                bottom: `calc(24px + ${Math.min(1, goalMinutes / maxChartVal) * 130}px)`,
              }}
            />
            <div className="grid h-full grid-cols-7 items-end gap-2">
              {dayMinutes.map((day) => {
                const barH = Math.max(
                  4,
                  Math.round((Math.min(1, day.minutes / maxChartVal)) * 130)
                );
                return (
                  <div
                    key={day.label}
                    className="flex h-full flex-col items-center justify-end"
                  >
                    <span className="tabular-nums mb-1 text-[11px] text-[#8F9BC4]">
                      {day.minutes}
                    </span>
                    <div
                      className="w-full max-w-[34px] rounded-t-[6px] bg-[#8B7DFF]"
                      style={{ height: `${barH}px` }}
                    />
                    <span className="mt-1.5 text-[11px] text-[#8F9BC4]">{day.label}</span>
                  </div>
                );
              })}
            </div>
          </div>

          <p className="tabular-nums text-xs text-[#8F9BC4]">
            Dashed orange line = daily goal ({goalMinutes} min).
          </p>
        </SectionPanel>
      </div>

      <SectionPanel>
        <h2 className="font-display text-lg font-semibold text-[#EEF1FF]">
          Focus sessions by tag (last 7 days)
        </h2>
        <div className="mt-3">
          {sessionsByTag.length === 0 ? (
            <EmptyStatePanel
              icon={Timer}
              message="Tag a Pomodoro session to see it here."
            />
          ) : (
            <div className="space-y-2">
              {sessionsByTag.map(([tag, mins]) => (
                <div
                  key={tag}
                  className="flex items-center justify-between rounded-[11px] border border-[#27325A] bg-[#1B2442] px-3.5 py-2.5"
                >
                  <span className="text-sm text-[#EEF1FF]">{tag}</span>
                  <span className="tabular-nums text-xs font-semibold text-[#8B7DFF]">
                    {mins} min
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>
      </SectionPanel>
    </div>
  );
}
