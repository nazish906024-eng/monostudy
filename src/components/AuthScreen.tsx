import { Cloud } from 'lucide-react';

interface AuthScreenProps {
  isSigningIn: boolean;
  errorMessage: string | null;
  onGoogleSignIn: () => void;
}

export function AuthScreen({ isSigningIn, errorMessage, onGoogleSignIn }: AuthScreenProps) {
  return (
    <div
      className="flex min-h-screen items-center justify-center px-5 py-10"
      style={{
        background:
          'radial-gradient(circle at top, rgba(27, 36, 66, 0.72) 0%, #0B1020 65%)',
      }}
    >
      <div className="w-full max-w-[420px] rounded-[20px] border border-[#27325A] bg-[#141B33] px-6 py-8 shadow-2xl">
        <div className="flex items-center justify-center gap-3">
          <div className="flex h-11 w-11 items-center justify-center rounded-[12px] bg-[#8B7DFF] text-2xl font-extrabold text-[#0B1020]">
            m
          </div>
          <div>
            <p className="font-display text-xl font-bold tracking-tight text-[#EEF1FF]">
              mono<span className="text-[#8B7DFF]">study</span>
            </p>
            <p className="text-[11px] font-medium tracking-wider text-[#8F9BC4]">
              JEE · NEET · CLASS 11–12
            </p>
          </div>
        </div>

        <h1 className="font-display mt-6 text-center text-2xl font-bold text-[#EEF1FF]">
          Welcome back
        </h1>
        <p className="mt-1.5 text-center text-sm text-[#8F9BC4]">
          Sign in with Google to keep your prep synced securely across your devices.
        </p>

        <button
          type="button"
          onClick={onGoogleSignIn}
          disabled={isSigningIn}
          className="mt-6 flex min-h-[48px] w-full items-center justify-center gap-2 rounded-[12px] bg-[#8B7DFF] px-5 py-3 text-sm font-semibold text-[#0B1020] transition-colors hover:bg-[#7A6BFF] disabled:opacity-60"
        >
          {isSigningIn ? 'Signing in…' : 'Continue with Google'}
        </button>

        {errorMessage && (
          <p className="mt-3.5 text-center text-xs font-medium text-[#FF6B7A]">
            {errorMessage}
          </p>
        )}

        <div className="mt-5 flex items-center justify-center gap-2 text-xs text-[#8F9BC4]">
          <Cloud className="h-4 w-4" aria-hidden="true" />
          <span>Synced securely with Cloud Firestore</span>
        </div>
      </div>
    </div>
  );
}
