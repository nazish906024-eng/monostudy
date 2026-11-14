# monostudy (Android)

Native Android study companion for **JEE & NEET** preparation (Classes 11–12), built with **Kotlin**, **Jetpack Compose**, **Material Design 3**, and **Firebase Firestore + Google Sign-In**.

## Features
- **Authentication & Cloud Sync**: Google Sign-In via Jetpack Credential Manager backed by Firebase Authentication and per-user Cloud Firestore storage (`users/{uid}`).
- **Dashboard**: Syllabus completion %, today's study minutes vs daily goal, task progress, PYQ accuracy, study streak, high-yield weak spots, continue-learning chapters, and quick-toggle daily tasks.
- **Syllabus Tracker**: 106 bundled chapters across Physics, Chemistry, Mathematics, and Biology with Class (`11`/`12`), Exam (`JEE`/`NEET`/`Both`), subject filters, provisional weightage & 10-yr frequency bands, and per-chapter status (`Not Started`, `In Progress`, `Revised`, `Mastered`).
- **PYQ Practice Vault**: Bundled demo questions plus custom JSON question import, multi-facet filtering (subject, chapter, difficulty, exam, tag, search), confidence ratings, timed attempts, explanations, and error categorization.
- **Flashcards**: 14 bundled flashcards across core subjects with 3D card flip and swipe gestures (`Knew it` / `Review again`).
- **Error Log Vault**: Tracks missed and unconfident attempts with `Open` / `Resolved` filtering, explanations, reopen/resolve actions, and deletion.
- **Notes & Formulas**: Markdown + formula preview, subject tagging, search, adaptive List-Detail layout, and up to 6 image attachments per note via the zero-permission Android Photo Picker.
- **Study Planner & Pomodoro**: Daily task checklist, 25/45/50/60-minute focus timer presets, session tags, optional completion tone, daily goal customization, and manual study-time logging.
- **Analytics & Weekly Summary**: Chapter weak-spot heatmap, 7-day study bar chart with goal line, focus distribution by tag, and 900×500 PNG weekly summary card generation & system share.
- **Backup & Custom Data**: Export/import full study backup JSON, import custom PYQ JSON files, download a question template JSON, and reset user data.

## Data Caveats
- `app/src/main/assets/pyq-data.json` contains 12 demo practice questions, not official past papers.
- Chapter weightage and frequency bands in `app/src/main/assets/chapter-data.json` are provisional planning estimates.
