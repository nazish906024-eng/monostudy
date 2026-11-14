import { FormEvent, useEffect, useMemo, useState } from 'react';
import { Check, CheckCircle2, Plus } from 'lucide-react';
import {
  EmptyStatePanel,
  PageHeader,
  PanelHeader,
  SectionPanel,
  StudyProgressBar,
} from '../components/ui/Primitives';
import { StudyStore } from '../hooks/useStudyStore';
import { StudyAnalytics, StudyConstants } from '../types/study';
import { TaskRow } from './DashboardScreen';

interface PlannerScreenProps {
  store: StudyStore;
}

export function PlannerScreen({ store }: PlannerScreenProps) {
  const [taskText, setTaskText] = useState('');
  const [taskSubject, setTaskSubject] = useState('General');
  const [goalText, setGoalText] = useState(String(store.profile.goalMinutes));

  useEffect(() => {
    setGoalText(String(store.profile.goalMinutes));
  }, [store.profile.goalMinutes]);

  const today = useMemo(() => StudyAnalytics.todayKey(), []);
  const minutesToday = useMemo(
    () => StudyAnalytics.minutesOnDate(store.sessions, today),
    [store.sessions, today]
  );
  const goalMinutes = Math.max(15, store.profile.goalMinutes);

  const totalSeconds = Math.max(1, store.pomodoro.presetMinutes * 60);
  const progress = store.pomodoro.secondsLeft / totalSeconds;
  const mm = String(Math.floor(store.pomodoro.secondsLeft / 60)).padStart(2, '0');
  const ss = String(store.pomodoro.secondsLeft % 60).padStart(2, '0');

  const radius = 84;
  const circumference = 2 * Math.PI * radius;
  const dashOffset = circumference * (1 - Math.max(0, Math.min(1, progress)));

  function handleAddTask(e: FormEvent) {
    e.preventDefault();
    const clean = taskText.trim();
    if (!clean) return;
    void store.addTask(clean, taskSubject);
    setTaskText('');
  }

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="Make time for what matters"
        title="Study planner"
        subtitle="Plan your day, tag your focus sessions, hit your goal."
      />

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-12">
        <SectionPanel className="lg:col-span-7">
          <PanelHeader
            eyebrow="Daily checklist"
            title="Your tasks"
            actionLabel={
              store.tasks.some((t) => t.done) ? 'Clear completed' : undefined
            }
            onAction={() => void store.clearCompletedTasks()}
          />

          <form onSubmit={handleAddTask} className="mt-3 space-y-2.5">
            <input
              type="text"
              value={taskText}
              maxLength={120}
              onChange={(e) => setTaskText(e.target.value)}
              placeholder="e.g. Revise Newton’s laws"
              className="min-h-[44px] w-full rounded-[10px] border border-[#27325A] bg-[#0B1020] px-3.5 py-2 text-sm text-[#EEF1FF] placeholder-[#8F9BC4] focus:border-[#8B7DFF] focus:outline-none"
            />
            <div className="flex items-center justify-between gap-3">
              <select
                aria-label="Task subject"
                value={taskSubject}
                onChange={(e) => setTaskSubject(e.target.value)}
                className="min-h-[40px] rounded-[9px] border border-[#27325A] bg-[#1B2442] px-3 py-1.5 text-xs font-medium text-[#EEF1FF] focus:border-[#8B7DFF] focus:outline-none"
              >
                {StudyConstants.TASK_SUBJECTS.map((s) => (
                  <option key={s} value={s}>
                    {s}
                  </option>
                ))}
              </select>

              <button
                type="submit"
                className="inline-flex min-h-[40px] items-center gap-1.5 rounded-[10px] bg-[#8B7DFF] px-4 py-2 text-xs font-semibold text-[#0B1020] transition-colors hover:bg-[#7A6BFF]"
              >
                <Plus className="h-4 w-4" aria-hidden="true" />
                <span>Add</span>
              </button>
            </div>
          </form>

          <div className="mt-4">
            {store.tasks.length === 0 ? (
              <EmptyStatePanel
                icon={CheckCircle2}
                message="No tasks yet. Add one small, clear task."
              />
            ) : (
              <div className="space-y-2">
                {store.tasks.map((task) => (
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

        <div className="space-y-4 lg:col-span-5">
          <SectionPanel>
            <p className="text-xs font-semibold tracking-wider text-[#8B7DFF]">
              FOCUS SESSION
            </p>
            <h2 className="font-display mt-0.5 text-lg font-semibold text-[#EEF1FF]">
              Pomodoro timer
            </h2>

            <input
              type="text"
              value={store.pomodoro.tag}
              maxLength={80}
              onChange={(e) => store.setPomodoroTag(e.target.value)}
              placeholder="Tag, e.g. 50m · Electrostatics PYQs"
              className="mt-3 min-h-[42px] w-full rounded-[10px] border border-[#27325A] bg-[#0B1020] px-3.5 py-2 text-sm text-[#EEF1FF] placeholder-[#8F9BC4] focus:border-[#8B7DFF] focus:outline-none"
            />

            <div className="my-5 flex justify-center">
              <div className="relative flex h-[200px] w-[200px] items-center justify-center">
                <svg className="h-full w-full -rotate-90" viewBox="0 0 200 200">
                  <circle
                    cx="100"
                    cy="100"
                    r={radius}
                    fill="none"
                    stroke="#1B2442"
                    strokeWidth="10"
                  />
                  <circle
                    cx="100"
                    cy="100"
                    r={radius}
                    fill="none"
                    stroke="#8B7DFF"
                    strokeWidth="10"
                    strokeLinecap="round"
                    strokeDasharray={circumference}
                    strokeDashoffset={dashOffset}
                  />
                </svg>
                <div className="absolute inset-0 flex flex-col items-center justify-center">
                  <span className="font-mono-data text-4xl font-bold text-[#EEF1FF]">
                    {mm}:{ss}
                  </span>
                  <span className="mt-1 text-[11px] font-medium tracking-wider text-[#8F9BC4]">
                    {store.pomodoro.statusLabel}
                  </span>
                </div>
              </div>
            </div>

            <div className="flex flex-wrap items-center justify-center gap-2.5">
              <button
                type="button"
                onClick={store.togglePomodoroRun}
                className="min-h-[42px] rounded-[10px] bg-[#8B7DFF] px-5 py-2 text-sm font-semibold text-[#0B1020] hover:bg-[#7A6BFF]"
              >
                {store.pomodoro.isRunning
                  ? 'Pause'
                  : store.pomodoro.secondsLeft < store.pomodoro.presetMinutes * 60
                    ? 'Resume'
                    : 'Start focus'}
              </button>
              <button
                type="button"
                onClick={store.resetPomodoro}
                className="min-h-[42px] rounded-[10px] border border-[#27325A] bg-[#1B2442] px-4 py-2 text-sm font-medium text-[#EEF1FF] hover:border-[#8B7DFF]"
              >
                Reset
              </button>
              <label className="flex cursor-pointer items-center gap-1.5 text-xs text-[#8F9BC4]">
                <input
                  type="checkbox"
                  checked={store.pomodoro.soundEnabled}
                  onChange={(e) => store.setPomodoroSound(e.target.checked)}
                  className="h-4 w-4 accent-[#8B7DFF]"
                />
                <span>Sound</span>
              </label>
            </div>

            <div className="mt-4 flex flex-wrap justify-center gap-2">
              {StudyConstants.POMODORO_PRESETS.map((mins) => {
                const active = store.pomodoro.presetMinutes === mins;
                return (
                  <button
                    key={mins}
                    type="button"
                    onClick={() => store.selectPomodoroPreset(mins)}
                    className={`tabular-nums min-h-[36px] rounded-[9px] border px-3 py-1 text-xs font-semibold transition-colors ${
                      active
                        ? 'border-[#8B7DFF] bg-[#8B7DFF] text-[#0B1020]'
                        : 'border-[#27325A] bg-[#1B2442] text-[#8F9BC4] hover:text-[#EEF1FF]'
                    }`}
                  >
                    {mins} min
                  </button>
                );
              })}
            </div>
          </SectionPanel>

          <SectionPanel>
            <p className="text-xs font-semibold tracking-wider text-[#8B7DFF]">
              DAILY TARGET
            </p>
            <h2 className="font-display mt-0.5 text-lg font-semibold text-[#EEF1FF]">
              Study goal
            </h2>

            <div className="mt-3 flex items-center gap-2.5">
              <input
                type="number"
                min={15}
                max={900}
                value={goalText}
                onChange={(e) => setGoalText(e.target.value.slice(0, 3))}
                className="tabular-nums min-h-[40px] w-24 rounded-[9px] border border-[#27325A] bg-[#0B1020] px-3 py-1.5 text-sm text-[#EEF1FF] focus:border-[#8B7DFF] focus:outline-none"
              />
              <span className="text-sm text-[#8F9BC4]">min/day</span>
              <button
                type="button"
                onClick={() =>
                  void store.saveProfile(
                    store.profile.name,
                    store.profile.cls,
                    store.profile.exam,
                    Number.parseInt(goalText, 10) || 0
                  )
                }
                aria-label="Save goal"
                className="inline-flex min-h-[40px] min-w-[40px] items-center justify-center rounded-[9px] border border-[#27325A] bg-[#1B2442] text-[#8B7DFF] hover:border-[#8B7DFF]"
              >
                <Check className="h-4 w-4" />
              </button>
            </div>

            <div className="mt-3">
              <StudyProgressBar fraction={minutesToday / goalMinutes} color="#4AA3FF" />
            </div>
            <p className="tabular-nums mt-2 text-xs text-[#8F9BC4]">
              {minutesToday} of {goalMinutes} minutes today
            </p>
            <button
              type="button"
              onClick={() => void store.logStudyMinutes(25, store.pomodoro.tag)}
              className="mt-2 inline-flex min-h-[38px] items-center gap-1 rounded-[9px] px-2 py-1 text-xs font-semibold text-[#8B7DFF] hover:bg-[#1B2442]"
            >
              + Log 25 minutes manually
            </button>
          </SectionPanel>
        </div>
      </div>
    </div>
  );
}
