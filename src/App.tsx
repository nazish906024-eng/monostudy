import { useState } from 'react';
import { AuthScreen } from './components/AuthScreen';
import { StudyLayout } from './components/StudyLayout';
import { useStudyStore } from './hooks/useStudyStore';
import { AnalyticsScreen } from './screens/AnalyticsScreen';
import { DashboardScreen } from './screens/DashboardScreen';
import { ErrorLogScreen } from './screens/ErrorLogScreen';
import { FlashcardsScreen } from './screens/FlashcardsScreen';
import { NotesScreen } from './screens/NotesScreen';
import { PlannerScreen } from './screens/PlannerScreen';
import { PyqVaultScreen } from './screens/PyqVaultScreen';
import { SettingsScreen } from './screens/SettingsScreen';
import { SyllabusScreen } from './screens/SyllabusScreen';
import { AppRoute } from './types/study';

export function App() {
  const store = useStudyStore();
  const [currentRoute, setCurrentRoute] = useState<AppRoute>('dashboard');

  if (!store.authReady) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[#0B1020] px-6 text-[#EEF1FF]">
        <div className="flex items-center gap-3">
          <div className="h-5 w-5 animate-spin rounded-full border-2 border-[#8B7DFF] border-t-transparent" />
          <span className="text-sm font-medium text-[#8F9BC4]">Loading monostudy…</span>
        </div>
      </div>
    );
  }

  if (!store.user) {
    return (
      <AuthScreen
        isSigningIn={store.isSigningIn}
        errorMessage={store.authError}
        onGoogleSignIn={() => void store.signInWithGoogle()}
      />
    );
  }

  return (
    <StudyLayout
      currentRoute={currentRoute}
      onNavigate={setCurrentRoute}
      userEmail={store.user.email}
      toast={store.toast}
    >
      {store.dataError && (
        <div
          role="alert"
          className="mb-4 rounded-[12px] border border-[#FF6B7A]/40 bg-[#FF6B7A]/10 px-4 py-3 text-sm text-[#FF6B7A]"
        >
          {store.dataError}
        </div>
      )}

      {store.isLoadingData && (
        <div className="mb-4 flex items-center gap-2 text-xs text-[#8F9BC4]">
          <div className="h-3.5 w-3.5 animate-spin rounded-full border-2 border-[#8B7DFF] border-t-transparent" />
          <span>Syncing your workspace…</span>
        </div>
      )}

      {currentRoute === 'dashboard' && (
        <DashboardScreen store={store} onNavigate={setCurrentRoute} />
      )}
      {currentRoute === 'syllabus' && <SyllabusScreen store={store} />}
      {currentRoute === 'pyq' && <PyqVaultScreen store={store} />}
      {currentRoute === 'flashcards' && <FlashcardsScreen store={store} />}
      {currentRoute === 'errors' && <ErrorLogScreen store={store} />}
      {currentRoute === 'notes' && <NotesScreen store={store} />}
      {currentRoute === 'planner' && <PlannerScreen store={store} />}
      {currentRoute === 'analytics' && <AnalyticsScreen store={store} />}
      {currentRoute === 'settings' && <SettingsScreen store={store} />}
    </StudyLayout>
  );
}

export default App;
