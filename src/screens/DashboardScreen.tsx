import { useMemo } from 'react';
import { CheckCircle2, Sparkles, Trash2, TrendingUp } from 'lucide-react';
import {
  EmptyStatePanel,
  PageHeader,
  PanelHeader,
  SectionPanel,
  StatMetricCard,
  WeightageLabel,
} from '../components/ui/Primitives';
import { StudyStore } from '../hooks/useStudyStore';
import { AppRoute, StudyAnalytics, StudyConstants, StudyTask } from '../types/study';

interface DashboardScreenProps {
  store: StudyStore;
  onNavigate: (route: AppRoute) => void;
}

export function TaskRow({
  task,
  onToggle,
  onDelete,
}: {
  task: StudyTask;
  onToggle: () => void;
  onDelete: () => void;
}) {
  return (
    <div className="flex items-center gap-3 rounded-[11px] border border-[#27325A] bg-[#1B2442] px-3 py-2">
      <input
        type="checkbox"
        checked={task.done}
        onChange={onToggle}
        aria-label={`Mark "${task.text}" ${task.done ? 'incomplete' : 'done'}`}
        className="h-4 w-4 shrink-0 cursor-pointer accent-[#8B7DFF]"
      />
      <span
        className={`min-w-0 flex-1 truncate text-sm ${
          task.done ? 'text-[#8F9BC4] line-through' : 'text-[#EEF1FF]'
        }`}
      >
        {task.text}
      </span>
      <span className="shrink-0 text-xs text-[#8F9BC4]">{task.subject}</span>
      <button
        type="button"
        onClick={onDelete}
        aria-label="Delete task"
        className="inline-flex min-h-[36px] min-w-[36px] shrink-0 items-center justify-center rounded-[8px] text-[#8F9BC4] transition-colors hover:bg-[#141B33] hover:text-[#FF6B7A]"
      >
        <Trash2 className="h-4 w-4" />
      </button>
    </div>
  );
}

export function DashboardScreen({ store, onNavigate }: DashboardScreenProps) {
  const today = useMemo(() => StudyAnalytics.todayKey(), []);

  const filteredChapters = useMemo(
    () => StudyAnalytics.filteredChapters(store.chapters, store.profile),
    [store.chapters, store.profile]
  );

  const doneChaptersCount = useMemo(
    () =>
      filteredChapters.filter((ch) =>
        StudyConstants.DONE_STATUSES.has(store.chapterStatuses[ch.id] ?? '')
      ).length,
    [filteredChapters, store.chapterStatuses]
  );

  const syllabusPct = StudyAnalytics.pct(doneChaptersCount, filteredChapters.length);
  const minutesToday = useMemo(
    () => StudyAnalytics.minutesOnDate(store.sessions, today),
    [store.sessions, today]
  );
  const goalMinutes = Math.max(15, store.profile.goalMinutes);

  const openOrTodayTasks = useMemo(
    () => store.tasks.filter((t) => t.date === today || !t.done),
    [store.tasks, today]
  );
  const tasksDoneCount = openOrTodayTasks.filter((t) => t.done).length;

  const overallAccuracy = useMemo(
    () => StudyAnalytics.overallAccuracyPct(store.attempts),
    [store.attempts]
  );

  const streakInfo = useMemo(
    () => StudyAnalytics.streak(store.sessions, store.attempts, store.tasks),
    [store.sessions, store.attempts, store.tasks]
  );

  const weakSpots = useMemo(
    () =>
      StudyAnalytics.weakSpots(filteredChapters, store.chapterStatuses, store.attempts).slice(
        0,
        3
      ),
    [filteredChapters, store.chapterStatuses, store.attempts]
  );

  const nextChapters = useMemo(() => {
    const inProg = filteredChapters.filter(
      (ch) => (store.chapterStatuses[ch.id] ?? 'Not Started') === 'In Progress'
    );
    const highYieldNew = filteredChapters.filter(
      (ch) =>
        (store.chapterStatuses[ch.id] ?? 'Not Started') === 'Not Started' &&
        ch.weightage === 'High'
    );
    return [...inProg, ...highYieldNew].slice(0, 4);
  }, [filteredChapters, store.chapterStatuses]);

  const displayName = store.profile.name.trim() || 'Student';

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="Your prep journey"
        title={`Let’s make today count, ${displayName}.`}
        subtitle="A focused plan today brings you closer to your target."
      />

      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-4">
        <StatMetricCard
          label="Syllabus revised/mastered"
          value={`${syllabusPct}%`}
          caption={`${doneChaptersCount} of ${filteredChapters.length} chapters`}
          progressFraction={syllabusPct / 100}
          accentColor="#8B7DFF"
        />
        <StatMetricCard
          label="Study time today"
          value={`${minutesToday}m`}
          caption={`Goal ${goalMinutes} min`}
          progressFraction={minutesToday / goalMinutes}
          accentColor="#4AA3FF"
        />
        <StatMetricCard
          label="Tasks completed"
          value={`${tasksDoneCount}/${openOrTodayTasks.length}`}
          caption="Open tasks carry over"
          progressFraction={
            openOrTodayTasks.length === 0 ? 0 : tasksDoneCount / openOrTodayTasks.length
          }
          accentColor="#3DDC97"
        />
        <StatMetricCard
          label="PYQ accuracy"
          value={overallAccuracy !== null ? `${overallAccuracy}%` : '—'}
          caption={`${store.attempts.length} attempts · ${streakInfo.days}-day streak`}
          progressFraction={(overallAccuracy ?? 0) / 100}
          accentColor="#FFA94D"
        />
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <SectionPanel>
          <PanelHeader
            eyebrow="Revise next"
            title="High yield · low accuracy"
            actionLabel="Analytics"
            onAction={() => onNavigate('analytics')}
          />
          <div className="mt-3">
            {weakSpots.length === 0 ? (
              <EmptyStatePanel
                icon={TrendingUp}
                message="Practice a few PYQs to surface your weak spots."
              />
            ) : (
              <div className="space-y-2">
                {weakSpots.map((spot) => (
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
                    <span
                      className={`tabular-nums shrink-0 text-xs font-semibold ${
                        spot.accuracyPct === null ? 'text-[#8F9BC4]' : 'text-[#FF6B7A]'
                      }`}
                    >
                      {spot.accuracyPct === null ? 'untested' : `${spot.accuracyPct}%`}
                    </span>
                  </div>
                ))}
              </div>
            )}
          </div>
        </SectionPanel>

        <SectionPanel>
          <PanelHeader
            eyebrow="Pick up where you left off"
            title="Continue learning"
            actionLabel="All chapters"
            onAction={() => onNavigate('syllabus')}
          />
          <div className="mt-3">
            {nextChapters.length === 0 ? (
              <EmptyStatePanel icon={Sparkles} message="Nothing queued. Nice work!" />
            ) : (
              <div className="space-y-2">
                {nextChapters.map((chapter) => {
                  const status = store.chapterStatuses[chapter.id] ?? 'Not Started';
                  return (
                    <div
                      key={chapter.id}
                      className="flex items-center justify-between gap-3 rounded-[11px] border border-[#27325A] bg-[#1B2442] px-3.5 py-2.5"
                    >
                      <div className="min-w-0 flex-1">
                        <p className="truncate text-sm font-medium text-[#EEF1FF]">
                          {chapter.title}
                        </p>
                        <div className="mt-0.5 flex items-center gap-2 text-xs text-[#8F9BC4]">
                          <WeightageLabel band={chapter.weightage} labelPrefix="weightage" />
                          <span aria-hidden="true">·</span>
                          <span>{status}</span>
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </SectionPanel>
      </div>

      <SectionPanel>
        <PanelHeader
          eyebrow="Stay on track"
          title="Today’s plan"
          actionLabel="Planner"
          onAction={() => onNavigate('planner')}
        />
        <div className="mt-3">
          {openOrTodayTasks.length === 0 ? (
            <EmptyStatePanel
              icon={CheckCircle2}
              message="No tasks yet — add one in the planner."
            />
          ) : (
            <div className="space-y-2">
              {openOrTodayTasks.slice(0, 5).map((task) => (
                <TaskRow
                  key={task.id}
                  task={task}
                  onToggle={() => void store.toggleTask(task)}
                  onDelete={() => void store.deleteTask(task.id)}
                />
              ))}
            </div>
          )}
        </div>
      </SectionPanel>
    </div>
  );
}
