# TwoDo - Architecture & Development Plan

## Overview & Philosophy
**TwoDo** is an offline-first, local LLM-powered Android todo app designed around David Allen’s *Getting Things Done (GTD)* 2-minute rule.
The core concept is to instantly triage tasks into three distinct time buckets so users can knock out 2-minute micro-tasks immediately:
1. ⚡ **"2m" (≤ 2 minutes):** Micro-tasks to perform right away (e.g., "reply to mom", "pay parking ticket").
2. ⏱️ **"30m" (~15–30 minutes):** Single focused sessions (e.g., "draft project summary").
3. 🏗️ **"long" (> 30 minutes / multi-step):** Tasks requiring planning or breakdown (e.g., "organize garage").

---

## Technical Stack
- **Language:** 100% Modern Idiomatic Kotlin
- **UI:** Jetpack Compose + Material 3
- **App Widget:** Jetpack Glance
- **Database:** Room (SQLite) with Flow-based reactive streams
- **Dependency Injection:** Hilt (`hilt-android`, `hilt-compiler`, `ksp`)
- **Concurrency & Background Work:** Kotlin Coroutines, StateFlow, WorkManager
- **Local AI Engine:** MediaPipe LLM Inference API / Local GGUF/bin quantized model interface with heuristic fallback
- **Architecture:** Clean Architecture (Data, Domain, Presentation) + MVVM

---

## Roadmap & Steps

### Step 1: Data Layer & Room Database
- `TaskEntity`: Database table for tasks with `id`, `title`, `bucket` (`TWO_MIN`, `THIRTY_MIN`, `LONG`, `UNCLASSIFIED`), `bucketSource` (`HEURISTIC`, `LLM`, `USER`), `isCompleted`, `createdAt`, `updatedAt`.
- `TaskDao`: Reactive Room DAO providing queries for task listing, filtering by completion/bucket, optimistic insertion, completion toggles, and bucket overrides.
- `TwoDoDatabase`: Room database definition and converters.
- Hilt Dependency Injection Modules (`DatabaseModule`, `RepositoryModule`).
- Repository implementation (`TaskRepository`).

### Step 2: Dual Classifier Architecture (Heuristic + LLM Interface)
- `TaskBucket` and `BucketSource` domain enums.
- `HeuristicClassifier`: Instant regex/keyword engine matching verb patterns ("call", "pay", "reply", "email", "unsub", etc.).
- `LlmClassifier`: Interface for local LLM text classification returning structured classification (`category`, `reason`).
- `TaskClassificationRepository`: Orchestrates instant heuristic tagging followed by background queuing for ambiguous/unclassified tasks.

### Step 3: Model Downloader & Manager
- `ModelDownloadWorker`: `WorkManager` background job with Wi-Fi constraint, pause/resume support, SHA-256 verification, and progress updates.
- `ModelManager`: Exposes `StateFlow<ModelStatus>` (`NotDownloaded`, `Downloading(progress)`, `Ready`, `Error`) and manages model file storage.
- Device compatibility & low-RAM check to fall back gracefully to heuristics.

### Step 4: Jetpack Compose UI
- Material 3 Compose theme and components.
- Main Screen:
  - Collapsible GTD buckets (⚡ 2m with prominent "Do it now" action, ⏱️ 30m, 🏗️ Long).
  - Quick-add input bar / bottom sheet with instant optimistic Room insertion.
  - Manual bucket override sheet/dialog (sets `bucketSource = USER`).
- Onboarding & Model Download screen with progress indicators and "Skip for now" option.

### Step 5: Jetpack Glance App Widget
- Home screen widget displaying pending tasks prioritized by 2m bucket.
- Direct checkbox toggle to mark tasks as complete via `ActionCallback`.
- Quick-add shortcut launching the capture view.
- Real-time sync with Room database changes.
