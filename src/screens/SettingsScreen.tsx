import { FormEvent, useEffect, useRef, useState } from 'react';
import { PageHeader, SectionPanel } from '../components/ui/Primitives';
import { StudyStore } from '../hooks/useStudyStore';
import { StudyConstants } from '../types/study';

interface SettingsScreenProps {
  store: StudyStore;
}

function downloadJsonFile(filename: string, content: string) {
  const blob = new Blob([content], { type: 'application/json' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}

export function SettingsScreen({ store }: SettingsScreenProps) {
  const [name, setName] = useState(store.profile.name);
  const [cls, setCls] = useState(store.profile.cls);
  const [exam, setExam] = useState(store.profile.exam);
  const [goalText, setGoalText] = useState(String(store.profile.goalMinutes));

  const [confirmClearCustom, setConfirmClearCustom] = useState(false);
  const [confirmResetAll, setConfirmResetAll] = useState(false);

  const importBackupRef = useRef<HTMLInputElement | null>(null);
  const importQuestionsRef = useRef<HTMLInputElement | null>(null);

  useEffect(() => {
    setName(store.profile.name);
    setCls(store.profile.cls);
    setExam(store.profile.exam);
    setGoalText(String(store.profile.goalMinutes));
  }, [store.profile]);

  function handleSaveProfile(e: FormEvent) {
    e.preventDefault();
    void store.saveProfile(name, cls, exam, Number.parseInt(goalText, 10) || 0);
  }

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="Your space, your goals"
        title="Profile & settings"
        subtitle="Synced with your Google account."
      />

      <SectionPanel>
        <h2 className="font-display text-lg font-semibold text-[#EEF1FF]">
          Student profile
        </h2>

        <form onSubmit={handleSaveProfile} className="mt-4 max-w-xl space-y-3">
          <div>
            <label
              htmlFor="settings-name"
              className="mb-1 block text-xs font-medium text-[#8F9BC4]"
            >
              Display name
            </label>
            <input
              id="settings-name"
              type="text"
              maxLength={40}
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="min-h-[42px] w-full rounded-[10px] border border-[#27325A] bg-[#0B1020] px-3.5 py-2 text-sm text-[#EEF1FF] focus:border-[#8B7DFF] focus:outline-none"
            />
          </div>

          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div>
              <label
                htmlFor="settings-class"
                className="mb-1 block text-xs font-medium text-[#8F9BC4]"
              >
                Class
              </label>
              <select
                id="settings-class"
                value={cls}
                onChange={(e) => setCls(e.target.value)}
                className="min-h-[42px] w-full rounded-[10px] border border-[#27325A] bg-[#0B1020] px-3 py-2 text-sm text-[#EEF1FF] focus:border-[#8B7DFF] focus:outline-none"
              >
                {StudyConstants.CLASSES.map((c) => (
                  <option key={c} value={c}>
                    Class {c}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label
                htmlFor="settings-exam"
                className="mb-1 block text-xs font-medium text-[#8F9BC4]"
              >
                Target exam
              </label>
              <select
                id="settings-exam"
                value={exam}
                onChange={(e) => setExam(e.target.value)}
                className="min-h-[42px] w-full rounded-[10px] border border-[#27325A] bg-[#0B1020] px-3 py-2 text-sm text-[#EEF1FF] focus:border-[#8B7DFF] focus:outline-none"
              >
                {StudyConstants.EXAMS.map((ex) => (
                  <option key={ex} value={ex}>
                    {ex}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div>
            <label
              htmlFor="settings-goal"
              className="mb-1 block text-xs font-medium text-[#8F9BC4]"
            >
              Daily goal (minutes)
            </label>
            <input
              id="settings-goal"
              type="number"
              min={15}
              max={900}
              value={goalText}
              onChange={(e) => setGoalText(e.target.value.slice(0, 3))}
              className="tabular-nums min-h-[42px] w-full rounded-[10px] border border-[#27325A] bg-[#0B1020] px-3.5 py-2 text-sm text-[#EEF1FF] focus:border-[#8B7DFF] focus:outline-none"
            />
          </div>

          <button
            type="submit"
            className="min-h-[42px] rounded-[10px] bg-[#8B7DFF] px-5 py-2 text-sm font-semibold text-[#0B1020] hover:bg-[#7A6BFF]"
          >
            Save profile
          </button>
        </form>
      </SectionPanel>

      <SectionPanel>
        <h2 className="font-display text-lg font-semibold text-[#EEF1FF]">
          Backups &amp; custom questions
        </h2>

        <div className="mt-3 flex flex-wrap gap-2.5">
          <button
            type="button"
            onClick={() =>
              downloadJsonFile('monostudy-backup.json', store.buildBackupJson())
            }
            className="min-h-[40px] rounded-[10px] border border-[#27325A] bg-[#1B2442] px-4 py-2 text-xs font-semibold text-[#EEF1FF] hover:border-[#8B7DFF]"
          >
            Export backup
          </button>

          <input
            ref={importBackupRef}
            type="file"
            accept=".json,application/json,text/plain"
            className="hidden"
            onChange={(e) => {
              const file = e.target.files?.[0];
              if (file) void file.text().then(store.importBackupJson);
              e.target.value = '';
            }}
          />
          <button
            type="button"
            onClick={() => importBackupRef.current?.click()}
            className="min-h-[40px] rounded-[10px] border border-[#27325A] bg-[#1B2442] px-4 py-2 text-xs font-semibold text-[#EEF1FF] hover:border-[#8B7DFF]"
          >
            Import backup
          </button>
        </div>

        <div className="mt-3 flex flex-wrap gap-2.5">
          <input
            ref={importQuestionsRef}
            type="file"
            accept=".json,application/json,text/plain"
            className="hidden"
            onChange={(e) => {
              const file = e.target.files?.[0];
              if (file) void file.text().then(store.importQuestionsFromJson);
              e.target.value = '';
            }}
          />
          <button
            type="button"
            onClick={() => importQuestionsRef.current?.click()}
            className="min-h-[40px] rounded-[10px] border border-[#27325A] bg-[#1B2442] px-4 py-2 text-xs font-semibold text-[#EEF1FF] hover:border-[#8B7DFF]"
          >
            Import questions (.json)
          </button>

          <button
            type="button"
            onClick={() =>
              downloadJsonFile(
                'monostudy-questions-template.json',
                store.buildQuestionTemplateJson()
              )
            }
            className="min-h-[40px] rounded-[10px] border border-[#27325A] bg-[#1B2442] px-4 py-2 text-xs font-semibold text-[#EEF1FF] hover:border-[#8B7DFF]"
          >
            Question file template
          </button>

          {confirmClearCustom ? (
            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={() => {
                  setConfirmClearCustom(false);
                  void store.clearCustomQuestions();
                }}
                className="min-h-[40px] rounded-[10px] bg-[#FF6B7A] px-3.5 py-2 text-xs font-semibold text-[#1A0509]"
              >
                Confirm remove
              </button>
              <button
                type="button"
                onClick={() => setConfirmClearCustom(false)}
                className="min-h-[40px] rounded-[10px] border border-[#27325A] px-3 py-2 text-xs text-[#8F9BC4]"
              >
                Cancel
              </button>
            </div>
          ) : (
            <button
              type="button"
              disabled={store.customPyqs.length === 0}
              onClick={() => setConfirmClearCustom(true)}
              className="min-h-[40px] rounded-[10px] border border-[#27325A] bg-[#1B2442] px-4 py-2 text-xs font-semibold text-[#EEF1FF] hover:border-[#FF6B7A] disabled:opacity-40"
            >
              Remove imported
            </button>
          )}
        </div>

        <p className="tabular-nums mt-3 text-xs text-[#8F9BC4]">
          {store.customPyqs.length} imported question(s) · {store.basePyqs.length} built in
        </p>
      </SectionPanel>

      <SectionPanel>
        <h2 className="font-display text-lg font-semibold text-[#EEF1FF]">
          Account &amp; danger zone
        </h2>
        {store.user?.email && (
          <p className="mt-1 text-xs text-[#8F9BC4]">Signed in as {store.user.email}</p>
        )}

        <div className="mt-4 flex flex-wrap items-center gap-3">
          <button
            type="button"
            onClick={() => void store.signOutAccount()}
            className="min-h-[42px] rounded-[10px] border border-[#FF6B7A]/60 bg-[#1B2442] px-4 py-2 text-sm font-semibold text-[#FF6B7A] hover:border-[#FF6B7A]"
          >
            Log out
          </button>

          {confirmResetAll ? (
            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={() => {
                  setConfirmResetAll(false);
                  void store.resetAllData();
                }}
                className="min-h-[42px] rounded-[10px] bg-[#FF6B7A] px-4 py-2 text-sm font-semibold text-[#1A0509]"
              >
                Confirm reset all data
              </button>
              <button
                type="button"
                onClick={() => setConfirmResetAll(false)}
                className="min-h-[42px] rounded-[10px] border border-[#27325A] px-3 py-2 text-xs text-[#8F9BC4]"
              >
                Cancel
              </button>
            </div>
          ) : (
            <button
              type="button"
              onClick={() => setConfirmResetAll(true)}
              className="min-h-[42px] rounded-[10px] bg-[#FF6B7A] px-4 py-2 text-sm font-semibold text-[#1A0509] hover:opacity-90"
            >
              Reset all data
            </button>
          )}
        </div>
      </SectionPanel>
    </div>
  );
}
