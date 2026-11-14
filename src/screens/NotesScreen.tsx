import { Fragment, useMemo, useRef, useState } from 'react';
import { FileText, ImagePlus, Plus, Search, X } from 'lucide-react';
import { EmptyStatePanel, PageHeader, SectionPanel } from '../components/ui/Primitives';
import { StudyStore } from '../hooks/useStudyStore';
import { StudyConstants, StudyNote } from '../types/study';
import { formatMathText } from '../utils/mathFormat';

interface NotesScreenProps {
  store: StudyStore;
}

function renderInlineMarkdown(line: string) {
  const tokens = line.split(/(\*\*[^*]+\*\*|`[^`]+`|\*[^*]+\*)/g);
  return tokens.map((tok, i) => {
    if (tok.startsWith('**') && tok.endsWith('**') && tok.length > 4) {
      return (
        <strong key={i} className="font-semibold text-[#EEF1FF]">
          {tok.slice(2, -2)}
        </strong>
      );
    }
    if (tok.startsWith('`') && tok.endsWith('`') && tok.length > 2) {
      return (
        <code
          key={i}
          className="font-mono-data rounded bg-[#0B1020] px-1.5 py-0.5 text-xs text-[#8B7DFF]"
        >
          {tok.slice(1, -1)}
        </code>
      );
    }
    if (tok.startsWith('*') && tok.endsWith('*') && tok.length > 2) {
      return (
        <em key={i} className="italic">
          {tok.slice(1, -1)}
        </em>
      );
    }
    return <Fragment key={i}>{tok}</Fragment>;
  });
}

function MarkdownPreview({ markdown }: { markdown: string }) {
  if (!markdown.trim()) {
    return <p className="text-sm text-[#8F9BC4]">Nothing to preview yet.</p>;
  }
  return (
    <div className="space-y-2 text-sm leading-relaxed text-[#EEF1FF]">
      {markdown.split('\n').map((raw, idx) => {
        const line = formatMathText(raw);
        if (line.startsWith('### ')) {
          return (
            <h4 key={idx} className="font-display text-base font-bold text-[#EEF1FF]">
              {line.slice(4)}
            </h4>
          );
        }
        if (line.startsWith('## ')) {
          return (
            <h3 key={idx} className="font-display text-lg font-bold text-[#EEF1FF]">
              {line.slice(3)}
            </h3>
          );
        }
        if (line.startsWith('# ')) {
          return (
            <h2 key={idx} className="font-display text-xl font-bold text-[#EEF1FF]">
              {line.slice(2)}
            </h2>
          );
        }
        const trimmed = line.trimStart();
        if (trimmed.startsWith('- ') || trimmed.startsWith('* ')) {
          return (
            <div key={idx} className="flex gap-2">
              <span className="text-[#8B7DFF]">•</span>
              <span>{renderInlineMarkdown(trimmed.slice(2))}</span>
            </div>
          );
        }
        if (!line.trim()) return <div key={idx} className="h-2" />;
        return <p key={idx}>{renderInlineMarkdown(line)}</p>;
      })}
    </div>
  );
}

function NoteEditor({
  note,
  noteImages,
  onSave,
  onAttachImage,
  onRemoveImage,
  onDelete,
}: {
  note: StudyNote;
  noteImages: Record<string, string>;
  onSave: (title: string, subject: string, body: string) => void;
  onAttachImage: (file: File) => void;
  onRemoveImage: (imgId: string) => void;
  onDelete: () => void;
}) {
  const [title, setTitle] = useState(note.title);
  const [subject, setSubject] = useState(note.subject);
  const [body, setBody] = useState(note.body);
  const [isPreview, setIsPreview] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const saveTimerRef = useRef<number | null>(null);

  function scheduleSave(nextTitle: string, nextSubject: string, nextBody: string) {
    if (saveTimerRef.current) window.clearTimeout(saveTimerRef.current);
    saveTimerRef.current = window.setTimeout(() => {
      onSave(nextTitle, nextSubject, nextBody);
    }, 350);
  }

  return (
    <div className="space-y-3">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <span className="text-xs font-semibold tracking-wider text-[#8B7DFF]">
          PERSONAL NOTE
        </span>
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => {
              onSave(title, subject, body);
              setIsPreview((p) => !p);
            }}
            className="min-h-[36px] rounded-[8px] border border-[#27325A] bg-[#1B2442] px-3 py-1 text-xs font-semibold text-[#EEF1FF] hover:border-[#8B7DFF]"
          >
            {isPreview ? 'Edit' : 'Preview'}
          </button>
          {confirmDelete ? (
            <div className="flex items-center gap-1.5">
              <button
                type="button"
                onClick={onDelete}
                className="min-h-[36px] rounded-[8px] bg-[#FF6B7A] px-3 py-1 text-xs font-semibold text-[#1A0509]"
              >
                Confirm delete
              </button>
              <button
                type="button"
                onClick={() => setConfirmDelete(false)}
                className="min-h-[36px] rounded-[8px] border border-[#27325A] px-2.5 py-1 text-xs text-[#8F9BC4]"
              >
                Cancel
              </button>
            </div>
          ) : (
            <button
              type="button"
              onClick={() => setConfirmDelete(true)}
              className="min-h-[36px] rounded-[8px] px-3 py-1 text-xs font-semibold text-[#FF6B7A] hover:bg-[#1B2442]"
            >
              Delete
            </button>
          )}
        </div>
      </div>

      <input
        type="text"
        value={title}
        maxLength={120}
        onChange={(e) => {
          const val = e.target.value;
          setTitle(val);
          scheduleSave(val, subject, body);
        }}
        placeholder="Untitled note"
        className="font-display min-h-[44px] w-full rounded-[10px] border border-[#27325A] bg-[#0B1020] px-3.5 py-2 text-lg font-semibold text-[#EEF1FF] placeholder-[#8F9BC4] focus:border-[#8B7DFF] focus:outline-none"
      />

      <div className="flex flex-wrap items-center gap-2.5">
        <select
          aria-label="Note subject"
          value={subject}
          onChange={(e) => {
            const val = e.target.value;
            setSubject(val);
            onSave(title, val, body);
          }}
          className="min-h-[40px] rounded-[9px] border border-[#27325A] bg-[#1B2442] px-3 py-1.5 text-xs font-medium text-[#EEF1FF] focus:border-[#8B7DFF] focus:outline-none"
        >
          {StudyConstants.TASK_SUBJECTS.map((s) => (
            <option key={s} value={s}>
              {s}
            </option>
          ))}
        </select>

        <input
          ref={fileInputRef}
          type="file"
          accept="image/*"
          className="hidden"
          onChange={(e) => {
            const file = e.target.files?.[0];
            if (file) onAttachImage(file);
            e.target.value = '';
          }}
        />
        <button
          type="button"
          onClick={() => fileInputRef.current?.click()}
          className="inline-flex min-h-[40px] items-center gap-1.5 rounded-[9px] border border-[#27325A] bg-[#1B2442] px-3 py-1.5 text-xs font-semibold text-[#EEF1FF] hover:border-[#8B7DFF]"
        >
          <ImagePlus className="h-4 w-4" aria-hidden="true" />
          <span>Attach image</span>
        </button>
      </div>

      {isPreview ? (
        <div className="min-h-[240px] rounded-[10px] border border-[#27325A] bg-[#1B2442] p-4">
          <MarkdownPreview markdown={body} />
        </div>
      ) : (
        <textarea
          value={body}
          maxLength={20000}
          onChange={(e) => {
            const val = e.target.value;
            setBody(val);
            scheduleSave(title, subject, val);
          }}
          placeholder="Write a formula, concept or revision note…"
          rows={11}
          className="w-full rounded-[10px] border border-[#27325A] bg-[#0B1020] p-3.5 text-sm leading-relaxed text-[#EEF1FF] placeholder-[#8F9BC4] focus:border-[#8B7DFF] focus:outline-none"
        />
      )}

      {note.imageIds.length > 0 && (
        <div className="flex flex-wrap gap-3 pt-1">
          {note.imageIds.map((imgId) => {
            const src = noteImages[imgId];
            if (!src) return null;
            return (
              <div
                key={imgId}
                className="relative h-[92px] w-[124px] overflow-hidden rounded-[8px] border border-[#27325A]"
              >
                <img
                  src={src}
                  alt="Note diagram attachment"
                  referrerPolicy="no-referrer"
                  className="h-full w-full object-cover"
                />
                <button
                  type="button"
                  onClick={() => onRemoveImage(imgId)}
                  aria-label="Remove image"
                  className="absolute top-1 right-1 flex h-6 w-6 items-center justify-center rounded-full bg-[#FF6B7A] text-[#1A0509]"
                >
                  <X className="h-3.5 w-3.5" />
                </button>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

export function NotesScreen({ store }: NotesScreenProps) {
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedNoteId, setSelectedNoteId] = useState<string | null>(null);

  const filteredNotes = useMemo(() => {
    const q = searchQuery.trim().toLowerCase();
    if (!q) return store.notes;
    return store.notes.filter(
      (n) => n.title.toLowerCase().includes(q) || n.body.toLowerCase().includes(q)
    );
  }, [store.notes, searchQuery]);

  const selectedNote = useMemo(
    () => store.notes.find((n) => n.id === selectedNoteId) ?? null,
    [store.notes, selectedNoteId]
  );

  async function handleCreate() {
    const newId = await store.createNote();
    if (newId) setSelectedNoteId(newId);
  }

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="Personal knowledge base"
        title="Notes & formulas"
        subtitle="Markdown supported. Use $…$ for formulas. Attach images for diagrams."
        trailing={
          <button
            type="button"
            onClick={() => void handleCreate()}
            className="inline-flex min-h-[42px] items-center gap-1.5 whitespace-nowrap rounded-[10px] bg-[#8B7DFF] px-4 py-2 text-sm font-semibold text-[#0B1020] transition-colors hover:bg-[#7A6BFF]"
          >
            <Plus className="h-4 w-4" aria-hidden="true" />
            <span>New note</span>
          </button>
        }
      />

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-12">
        <SectionPanel className="lg:col-span-4">
          <div className="relative">
            <Search
              className="pointer-events-none absolute top-1/2 left-3 h-4 w-4 -translate-y-1/2 text-[#8F9BC4]"
              aria-hidden="true"
            />
            <input
              type="search"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search notes…"
              className="min-h-[40px] w-full rounded-[10px] border border-[#27325A] bg-[#0B1020] pr-3 pl-9 text-sm text-[#EEF1FF] placeholder-[#8F9BC4] focus:border-[#8B7DFF] focus:outline-none"
            />
          </div>

          <div className="mt-3 space-y-2">
            {filteredNotes.length === 0 ? (
              <EmptyStatePanel
                icon={FileText}
                message="No notes yet. Click New note to create one."
              />
            ) : (
              filteredNotes.map((note) => {
                const active = note.id === selectedNoteId;
                return (
                  <button
                    key={note.id}
                    type="button"
                    onClick={() => setSelectedNoteId(note.id)}
                    className={`w-full rounded-[11px] border p-3 text-left transition-colors ${
                      active
                        ? 'border-[#8B7DFF] bg-[#1B2442]'
                        : 'border-[#27325A] bg-[#1B2442]/60 hover:border-[#8B7DFF]/50'
                    }`}
                  >
                    <p className="truncate text-sm font-semibold text-[#EEF1FF]">
                      {note.title.trim() || 'Untitled note'}
                    </p>
                    <p className="mt-0.5 truncate text-xs text-[#8F9BC4]">
                      {note.subject} · {note.body.slice(0, 50).replace(/\n/g, ' ')}
                    </p>
                  </button>
                );
              })
            )}
          </div>
        </SectionPanel>

        <SectionPanel className="lg:col-span-8">
          {!selectedNote ? (
            <div className="flex min-h-[320px] items-center justify-center">
              <EmptyStatePanel
                icon={FileText}
                message="Select a note or create a new one."
              />
            </div>
          ) : (
            <NoteEditor
              key={selectedNote.id}
              note={selectedNote}
              noteImages={store.noteImages}
              onSave={(t, s, b) => void store.updateNote(selectedNote, t, s, b)}
              onAttachImage={(file) => void store.attachNoteImage(selectedNote, file)}
              onRemoveImage={(imgId) => void store.removeNoteImage(selectedNote, imgId)}
              onDelete={() => {
                void store.deleteNote(selectedNote);
                setSelectedNoteId(null);
              }}
            />
          )}
        </SectionPanel>
      </div>
    </div>
  );
}
