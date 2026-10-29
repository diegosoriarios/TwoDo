<p align="center">
  <img src="icon.png" width="96" alt="TwoDo icon" />
</p>

<h1 align="center">Two.do</h1>

<p align="center">
  An offline-first Android to-do app that sorts every task into <b>2 min</b>, <b>30 min</b> or <b>long</b> —
  so you always know what fits the time you have right now.
</p>

<p align="center">
  <a href="https://github.com/diegosoriarios/TwoDo/actions/workflows/release.yml">
    <img src="https://github.com/diegosoriarios/TwoDo/actions/workflows/release.yml/badge.svg" alt="Release" />
  </a>
</p>

---

## Why Two.do

Two.do is built around David Allen's *Getting Things Done* **2-minute rule**. Instead of one
undifferentiated list, every task is triaged into three time buckets the moment you capture it:

| Bucket | Meaning | Examples |
|---|---|---|
| ⚡ **2 min** | Micro-tasks you should do right now | "reply to mom", "pay parking ticket" |
| ⏱️ **30 min** | A single focused session (~15–30 min) | "draft project summary" |
| 🏗️ **Long** | Complex, multi-step work that needs planning | "organize the garage" |

Sorting is instant (a rule engine classifies on capture) and gets **smarter over time**: an optional
on-device AI model re-classifies anything the rules couldn't. Nothing ever leaves your phone.

## Features

- **Instant triage** — every captured task lands in a bucket immediately, no tagging required.
- **Dual classification engine**
  - `HeuristicClassifier`: regex/keyword rules (verbs like *call, pay, reply*, word count, etc.) — zero latency, works offline out of the box.
  - Local LLM classifier for ambiguous tasks, refined in the background.
- **Manual override** — pick a bucket yourself when capturing; user overrides are never re-classified automatically.
- **Re-evaluate categories** — re-run classification for a single task or all active tasks; single-item re-evaluation can replace a manual choice, while the bulk action keeps manual categories.
- **Labels** — organize tasks with custom labels (many-to-many).
- **Due dates & reminders** — schedule reminders per task; local notifications via WorkManager.
- **Calendar view** — browse tasks by day, including completed work.
- **Home-screen widget** (Jetpack Glance) — today's tasks with ⚡ 2-minute items first, check tasks off
  directly from the widget, and quick-add shortcut.
- **Quick Add** — translucent capture dialog that can be launched from the widget or externally.
- **Onboarding** — a two-step flow (welcome → on-device AI setup) with a stateful download sheet.
- **Offline-first & private** — all data lives in a local Room database; the AI model runs entirely on-device.

## On-device AI setup

The setup screen offers a one-time model download backed by **WorkManager**:

- Wi-Fi (unmetered) by default, optional mobile-data fallback.
- **Resumable** downloads (HTTP `Range` requests) — pause/interruption keeps the partial file.
- **SHA-256 checksum** and expected-size verification before the model is marked ready.
- Foreground-service downloads with progress notification, storage checks, backoff, and automatic retry.
- A dedicated **AI model** screen in the task-list overflow menu supports download, pause, resume, retry, and delete.
- **Low-RAM and 32-bit devices** gracefully fall back to the instant rules engine.
- Release APKs target **arm64-v8a**; debug builds also include x86_64 for emulators.

The default model is [Qwen3 0.6B dynamic INT4 (LiteRT Community)](https://huggingface.co/litert-community/Qwen3-0.6B):
about 329 MB, ungated, supports Range requests for resume, and runs fully offline.
Inference uses the [LiteRT-LM](https://developers.google.com/edge/litert-lm/android) Android Kotlin API
with the CPU backend and constrained JSON decoding. The model is stored in app-private storage and
excluded from Android backups.

## Architecture

Clean Architecture + MVVM, 100% Kotlin, reactive end to end (`Flow` / `StateFlow`).

```
ui/            Compose screens, ViewModels (StateFlow), reusable components, theme/tokens
domain/        Pure Kotlin: models (Task, TaskBucket, ModelStatus...), repository & classifier
               interfaces, ModelManager
data/          Room (entities, DAOs, converters), repositories impl, classifiers,
               WorkManager workers, SharedPreferences
di/            Hilt modules (Database, Classifier)
widget/        Glance app widget + receiver + toggle action
notifications/ Reminder scheduling, worker and notification helper
```

**Classification pipeline** (`ClassificationRepositoryImpl`):

```
capture(title)
 ├─ user picked a bucket?  → save with BucketSource.USER (never re-classified)
 └─ else HeuristicClassifier.classify()          ← instant, on the capture path
      └─ result == UNCLASSIFIED?
           └─ ClassificationScheduler            ← capture returns immediately
                └─ ClassificationWorker (WorkManager)
                     └─ LlmClassifier.classifyTask()   ← only if model is ready
                          └─ update bucket with BucketSource.LLM
```

Conservative defaults apply when an LLM response is malformed (falls back to `30m`).

**Key classes**

| Class | Role |
|---|---|
| `TasksViewModel` | Task list state, capture, filters, completion, re-evaluation |
| `OnboardingViewModel` | Setup sheet state machine (`SetupUiState`: idle / downloading / done / skipped / error) |
| `ModelManager` | Model lifecycle, download orchestration, device support, engine warm-up |
| `ModelManagerViewModel` | AI model screen state and download/delete actions |
| `LiteRtLmInferenceEngine` | Native LiteRT-LM engine lifecycle and JSON-constrained inference |
| `ClassificationWorker` | Background refinement of ambiguous tasks through WorkManager |
| `ModelManager` | Model file storage, download orchestration, `StateFlow<ModelStatus>` |
| `ModelDownloadWorker` | Resumable download with progress + checksum verification |
| `TwoDoDatabase` | Room database (`TaskDao`, `LabelDao`) |
| `TasksWidget` / `TasksWidgetReceiver` | Glance widget, event-driven updates, checkbox toggles |
| `QuickAddActivity` | Translucent capture entry point |

## Design system

Dark-only visual language with a single accent color. All values live in shared tokens
(`ui/theme/Tokens.kt`, `Color.kt`, `Type.kt`) — no hard-coded values in screens.

| Token | Value |
|---|---|
| Background | `#121212` |
| Surface / elevated surface | `#252525` / `#333333` |
| Accent (coral) | `#D35D47` |
| Text primary / secondary | `#FFFFFF` / `#A0A0A0` |
| Illustration navy | `#0A1A32` / `#16305A` |

- Type scale: title 30/36 bold (-0.01em), body 14/22, caption 13/20, labels 13/500–600.
- Radius: 8 (buttons/chips), 16 (sheet top corners). Vertical rhythm: 4 / 8 / 12 / 16 / 24 / 32.
- Reusable components: `PrimaryButton` (incl. progress-track variant), `TextButton`,
  `BucketChip`, `BottomSheet`, `BucketIllustration`, `TaskRow`, `SummaryCard`, `CalendarCard`, …

## Tech stack

| | |
|---|---|
| Language | Kotlin 2.1.20 |
| UI | Jetpack Compose (BOM 2026.02.01) + Material 3 |
| Widget | Jetpack Glance 1.1.1 |
| Database | Room 2.8.5 (Flow-based reactive DAOs) |
| DI | Hilt 2.60.1 (KSP) |
| Background work | WorkManager 2.9.1 |
| Min / target SDK | 24 (Android 7.0) / 35 (Android 15) |

## Getting started

### Prerequisites

- Android Studio (Ladybug or newer recommended)
- JDK 17+ (CI uses Temurin 21)
- Android SDK Platform 35

### Build & run

```bash
git clone git@github.com:diegosoriarios/TwoDo.git
cd TwoDo

# Debug build
./gradlew :app:assembleDebug

# Install on a connected device/emulator
./gradlew :app:installDebug

# Unit tests
./gradlew :app:testDebugUnitTest
```

Or simply open the project in Android Studio and press **Run**.

### Release builds & signing

Release signing reads from environment variables (no secrets in the repo):

```
CI_KEYSTORE_PATH, CI_KEYSTORE_PASSWORD, CI_KEY_ALIAS, CI_KEY_PASSWORD
```

If they are absent, release builds assemble unsigned — fine for local verification.

### CI / releases

[`.github/workflows/release.yml`](.github/workflows/release.yml) runs on every `v*` tag:

1. Decodes the signing keystore from the `KEYSTORE_BASE64` secret (plus `KEYSTORE_PASSWORD`,
   `KEY_ALIAS`, `KEY_PASSWORD`).
2. Builds `:app:assembleRelease` with JDK 21.
3. Publishes `TwoDo-<version>.apk` to GitHub Releases with generated notes.

To cut a release:

```bash
git tag -a v0.3.2 -m "v0.3.2: <summary>"
git push origin v0.3.2
```

Required repository secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.

## Project structure

```
app/src/main/java/com/diego/twodo/
├── MainActivity.kt            # Single-activity entry, edge-to-edge + splash
├── TwoDoApplication.kt        # Hilt application
├── data/
│   ├── classifier/            # HeuristicClassifier, LocalLlmClassifier, LiteRtLmInferenceEngine
│   ├── local/                 # Room database, DAOs, entities, converters
│   ├── preferences/           # Onboarding state (welcome, dismissed, skipped)
│   ├── repository/            # Task & classification repository implementations
│   └── worker/                # ModelDownloadWorker, ClassificationWorker, scheduler
├── di/                        # Hilt modules
├── domain/
│   ├── classifier/            # LlmClassifier and LlmInferenceEngine interfaces
│   ├── manager/               # ModelManager
│   ├── model/                 # Task, TaskBucket, Label, ModelStatus, ModelSpec, ...
│   └── repository/            # Repository and scheduler interfaces
├── notifications/             # Reminder and model-download notifications
├── ui/
│   ├── calendar/  composer/  model/  onboarding/  quickadd/  tasks/
│   ├── components/            # Shared composables (buttons, chips, sheets, cards)
│   ├── theme/                 # Color, Type, Tokens (design system)
│   └── TwoDoApp.kt            # Top-level navigation & chrome
└── widget/                    # Glance widget, receiver, toggle action
```

## Localization

- English (`values/strings.xml`) — source of truth
- Portuguese, Brazil (`values-pt-rBR/strings.xml`)

## Documentation

- [`PLAN.md`](PLAN.md) — original architecture & development plan (GTD philosophy, step-by-step build order)

## Roadmap

- [x] Bind the native inference runner (LiteRT-LM Kotlin API) into `LocalLlmClassifier`
- [x] Background refinement queue for ambiguous tasks after the model becomes ready
- [ ] Task breakdown suggestions for 🏗️ long bucket items
- [ ] Cloud backup/restore rules (`data_extraction_rules.xml` TODOs)

## License

Not yet specified. All rights reserved by the author until a license is added.
