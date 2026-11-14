import { ReactNode } from 'react';
import {
  BarChart3,
  BookOpen,
  CalendarClock,
  FileText,
  Flag,
  HelpCircle,
  LayoutDashboard,
  Layers,
  Settings,
} from 'lucide-react';
import { AppRoute } from '../types/study';

export const NAV_ITEMS: Array<{
  route: AppRoute;
  label: string;
  icon: typeof LayoutDashboard;
}> = [
  { route: 'dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { route: 'syllabus', label: 'Syllabus', icon: BookOpen },
  { route: 'pyq', label: 'PYQ Vault', icon: HelpCircle },
  { route: 'flashcards', label: 'Flashcards', icon: Layers },
  { route: 'errors', label: 'Error Log', icon: Flag },
  { route: 'notes', label: 'Notes', icon: FileText },
  { route: 'planner', label: 'Planner', icon: CalendarClock },
  { route: 'analytics', label: 'Analytics', icon: BarChart3 },
  { route: 'settings', label: 'Settings', icon: Settings },
];

interface StudyLayoutProps {
  currentRoute: AppRoute;
  onNavigate: (route: AppRoute) => void;
  userEmail: string | null;
  toast: string | null;
  children: ReactNode;
}

export function StudyLayout({
  currentRoute,
  onNavigate,
  userEmail,
  toast,
  children,
}: StudyLayoutProps) {
  return (
    <div className="min-h-screen bg-[#0B1020] text-[#EEF1FF] lg:flex">
      {/* Desktop sidebar */}
      <aside className="hidden w-[256px] shrink-0 flex-col border-r border-[#27325A] bg-[#141B33] p-4 lg:sticky lg:top-0 lg:flex lg:h-screen">
        <div className="flex items-center gap-3 px-2 py-2">
          <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-[10px] bg-[#8B7DFF] text-lg font-extrabold text-[#0B1020]">
            m
          </div>
          <span className="font-display whitespace-nowrap text-lg font-bold tracking-tight text-[#EEF1FF]">
            mono<span className="text-[#8B7DFF]">study</span>
          </span>
        </div>

        <nav className="mt-5 flex flex-1 flex-col gap-1 overflow-y-auto">
          {NAV_ITEMS.map(({ route, label, icon: Icon }) => {
            const active = currentRoute === route;
            return (
              <button
                key={route}
                type="button"
                onClick={() => onNavigate(route)}
                className={`flex min-h-[42px] w-full items-center gap-3 whitespace-nowrap rounded-[10px] px-3 py-2 text-left text-sm font-medium transition-colors ${
                  active
                    ? 'bg-[#8B7DFF]/15 text-[#8B7DFF]'
                    : 'text-[#8F9BC4] hover:bg-[#1B2442] hover:text-[#EEF1FF]'
                }`}
              >
                <Icon className="h-[18px] w-[18px] shrink-0" aria-hidden="true" />
                <span>{label}</span>
              </button>
            );
          })}
        </nav>

        {userEmail && (
          <div className="mt-3 border-t border-[#27325A] px-2 pt-3 text-xs text-[#8F9BC4]">
            <p className="truncate">{userEmail}</p>
          </div>
        )}
      </aside>

      {/* Main area */}
      <div className="flex min-w-0 flex-1 flex-col">
        {/* Mobile header — compact single row */}
        <header className="sticky top-0 z-20 flex items-center justify-between gap-4 border-b border-[#27325A] bg-[#141B33] px-4 py-2.5 lg:hidden">
          <div className="flex items-center gap-2.5">
            <div className="flex h-8 w-8 items-center justify-center rounded-[8px] bg-[#8B7DFF] text-base font-extrabold text-[#0B1020]">
              m
            </div>
            <span className="font-display whitespace-nowrap text-base font-bold text-[#EEF1FF]">
              mono<span className="text-[#8B7DFF]">study</span>
            </span>
          </div>
          <button
            type="button"
            onClick={() => onNavigate('settings')}
            className="whitespace-nowrap rounded-[8px] border border-[#27325A] bg-[#1B2442] px-3 py-1.5 text-xs font-medium text-[#EEF1FF]"
          >
            Settings
          </button>
        </header>

        {/* Mobile horizontal nav bar */}
        <nav
          aria-label="Primary navigation"
          className="flex gap-1 overflow-x-auto border-b border-[#27325A] bg-[#0B1020] px-3 py-2 lg:hidden"
        >
          {NAV_ITEMS.filter((item) => item.route !== 'settings').map(({ route, label }) => {
            const active = currentRoute === route;
            return (
              <button
                key={route}
                type="button"
                onClick={() => onNavigate(route)}
                className={`min-h-[36px] shrink-0 whitespace-nowrap rounded-[8px] px-3 py-1.5 text-xs font-medium transition-colors ${
                  active
                    ? 'bg-[#8B7DFF] text-[#0B1020]'
                    : 'text-[#8F9BC4] hover:bg-[#141B33] hover:text-[#EEF1FF]'
                }`}
              >
                {label}
              </button>
            );
          })}
        </nav>

        <main className="mx-auto w-full max-w-[1160px] flex-1 px-4 py-5 pb-16 sm:px-6 sm:py-6">
          {children}
        </main>
      </div>

      {toast && (
        <div
          role="status"
          aria-live="polite"
          className="fixed right-5 bottom-5 z-50 rounded-[12px] border border-[#8B7DFF]/50 bg-[#1B2442] px-4 py-2.5 text-sm font-medium text-[#EEF1FF] shadow-xl"
        >
          {toast}
        </div>
      )}
    </div>
  );
}
