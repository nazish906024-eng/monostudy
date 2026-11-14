import { ReactNode } from 'react';
import { ArrowRight, LucideIcon } from 'lucide-react';

export function PageHeader({
  eyebrow,
  title,
  subtitle,
  trailing,
}: {
  eyebrow: string;
  title: string;
  subtitle: string;
  trailing?: ReactNode;
}) {
  return (
    <header className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
      <div>
        <p className="text-xs font-semibold tracking-wider text-[#8B7DFF]">{eyebrow}</p>
        <h1 className="font-display mt-1 text-2xl font-bold tracking-tight text-[#EEF1FF] sm:text-3xl">
          {title}
        </h1>
        <p className="mt-1 text-sm text-[#8F9BC4]">{subtitle}</p>
      </div>
      {trailing && <div className="shrink-0">{trailing}</div>}
    </header>
  );
}

export function SectionPanel({
  children,
  className = '',
}: {
  children: ReactNode;
  className?: string;
}) {
  return (
    <section
      className={`rounded-[14px] border border-[#27325A] bg-[#141B33] p-4 sm:p-5 ${className}`}
    >
      {children}
    </section>
  );
}

export function PanelHeader({
  eyebrow,
  title,
  actionLabel,
  onAction,
}: {
  eyebrow?: string;
  title: string;
  actionLabel?: string;
  onAction?: () => void;
}) {
  return (
    <div className="flex items-center justify-between gap-3">
      <div>
        {eyebrow && <p className="text-xs font-medium text-[#8B7DFF]">{eyebrow}</p>}
        <h2 className="font-display text-lg font-semibold text-[#EEF1FF]">{title}</h2>
      </div>
      {actionLabel && onAction && (
        <button
          type="button"
          onClick={onAction}
          className="inline-flex min-h-[40px] shrink-0 items-center gap-1.5 whitespace-nowrap rounded-[10px] px-3 py-1.5 text-xs font-semibold text-[#8B7DFF] transition-colors hover:bg-[#1B2442]"
        >
          <span>{actionLabel}</span>
          <ArrowRight className="h-3.5 w-3.5" aria-hidden="true" />
        </button>
      )}
    </div>
  );
}

export function StudyProgressBar({
  fraction,
  color = '#8B7DFF',
}: {
  fraction: number;
  color?: string;
}) {
  const pct = Math.max(0, Math.min(100, Math.round(fraction * 100)));
  return (
    <div className="h-2 w-full overflow-hidden rounded-full bg-[#1B2442]">
      <div
        className="h-full rounded-full transition-all duration-300"
        style={{ width: `${pct}%`, backgroundColor: color }}
      />
    </div>
  );
}

export function StatMetricCard({
  label,
  value,
  caption,
  progressFraction,
  accentColor = '#8B7DFF',
}: {
  label: string;
  value: string;
  caption: string;
  progressFraction?: number;
  accentColor?: string;
}) {
  return (
    <div className="rounded-[14px] border border-[#27325A] bg-[#141B33] p-4">
      <p className="truncate text-xs font-medium text-[#8F9BC4]">{label}</p>
      <p className="font-display tabular-nums mt-1.5 text-3xl font-bold text-[#EEF1FF]">
        {value}
      </p>
      {progressFraction !== undefined && (
        <div className="mt-2.5">
          <StudyProgressBar fraction={progressFraction} color={accentColor} />
        </div>
      )}
      <p className="mt-2 text-xs text-[#8F9BC4]">{caption}</p>
    </div>
  );
}

export function EmptyStatePanel({
  icon: Icon,
  message,
}: {
  icon: LucideIcon;
  message: string;
}) {
  return (
    <div className="flex flex-col items-center justify-center gap-2 rounded-[12px] border border-[#27325A] bg-[#1B2442]/60 p-6 text-center">
      <Icon className="h-6 w-6 text-[#8F9BC4]" aria-hidden="true" />
      <p className="text-sm text-[#8F9BC4]">{message}</p>
    </div>
  );
}

export function WeightageLabel({
  band,
  labelPrefix = '',
}: {
  band: string;
  labelPrefix?: string;
}) {
  const colorClass =
    band === 'High'
      ? 'text-[#FF6B7A]'
      : band === 'Medium'
        ? 'text-[#FFA94D]'
        : band === 'Low'
          ? 'text-[#3DDC97]'
          : 'text-[#8F9BC4]';
  const text = labelPrefix ? `${band} ${labelPrefix}` : band;
  return <span className={`text-xs font-semibold ${colorClass}`}>{text}</span>;
}
