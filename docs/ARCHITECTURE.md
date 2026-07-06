# Architecture

Module responsibilities, data-flow detail, and dependency rules for IO Motion.

## Contents

- [Module graph](#module-graph)
- [Module responsibilities](#module-responsibilities)
- [Dependency rules](#dependency-rules)
- [Data flow](#data-flow)
  - [Live camera pipeline](#live-camera-pipeline)
  - [Offline video pipeline](#offline-video-pipeline)
  - [Persistence & history](#persistence--history)
  - [Export & share](#export--share)
- [State management](#state-management)
- [Dependency injection](#dependency-injection)
- [Navigation](#navigation)
- [Theming](#theming)
- [Persistence schema](#persistence-schema)
- [Testing boundaries](#testing-boundaries)

---

## Module graph

```
                              ┌──────────┐
                              │   app    │  (composition root)
                              └────┬─────┘
             ┌───────────┬─────────┼─────────┬───────────┐
             ▼           ▼         ▼         ▼           ▼
      feature-live  feature-video  feature-history   core-pose
             │           │              │
             └─────┬─────┴──────┬───────┘
                   ▼             ▼
                 data        core-export
                   │             │
                   └──────┬──────┘
                          ▼
                    core-analysis
                          │
                          ▼
                     core-common
                          ▲
                          │
                       core-ui ───────────────┘
                (also depended on directly by app + every feature module)
```

`core-pose` also depends on `core-common` only (parallel branch, not on `core-analysis` — pose
detection has no notion of exercises or scoring).

## Module responsibilities

| Module | Type | Responsibility |
|---|---|---|
| `:core-common` | pure Kotlin JVM | Zero-dependency domain primitives: `PoseFrame`, `Landmark`, `Vec3`, `AngleMath`, `ExerciseType`, `AnalysisMode`, `ThemeMode`, `AccentTheme`. No Android, no framework code. Everything else in the app can see these types. |
| `:core-analysis` | pure Kotlin JVM | The biomechanical engine — most important module. `ExerciseAnalyzer` FSM implementations (squat/sit-up/push-up/plank), `KpiCalculator`, confidence gating, One-Euro filtering, body normalization. Consumes `PoseFrame`, produces `AnalyzerState` (live) and `SessionMetrics` (final). No Android dependency — fully unit-testable on the JVM. See `docs/KPI.md` for every formula. |
| `:core-pose` | Android lib | MediaPipe Pose Landmarker + CameraX integration. `PoseFrameSource` (live camera → `Flow<PoseFrameResult>`) and `VideoAnalysisSession` (offline video → `Flow<ProgressEvent>`). Both converge on the same `PoseResultConverter` to produce `PoseFrame`, so live and video modes feed identical data into `:core-analysis`. |
| `:core-ui` | Android lib | Design-system layer: `Theme.kt`/`Color.kt`/`Type.kt`/`Shapes.kt` (fonts, accent-color tokens, cut-corner shape), plus shared composables (`SegmentedControl`, `SectionLabel`, `MetricGauge`, `SessionMetricCards`, `SkeletonOverlay`). Depends on `:core-analysis` because some shared composables (`RepCard`, `RepMetricsGrid`) take `RepMetrics`/`SessionMetrics` directly as parameters — a pragmatic layering choice (UI reads domain models straight, no intermediate UI-model mapping for these shared cards). |
| `:data` | Android lib | Persistence + settings. Room (`AppDatabase`, `SessionDao`, `SessionEntity`/`RepEntity`) for session history; DataStore (`SettingsPreferences`) for theme mode / accent color / default model variant. Exposes `SessionRepository` and `SettingsRepository` interfaces; everything above depends on the interface, never the impl. |
| `:core-export` | pure Kotlin JVM | Stateless `SessionJsonExporter` / `SessionCsvExporter` — serialize a `SessionMetrics` to a shareable string. No Android, no file I/O (writing to disk and sharing the `Uri` is the calling feature module's job). |
| `:feature-live` | Android lib | Home screen (exercise/mode picker + CTA), Settings screen, and the live camera analysis screen. Orchestrates `PoseFrameSource` + `ExerciseAnalyzer` + `SessionRepository` in `LiveViewModel`. |
| `:feature-video` | Android lib | Offline video analysis screen (pick a video → process → show result). Orchestrates `VideoAnalysisSession` + `ExerciseAnalyzer` + `SessionRepository` in `VideoViewModel`. |
| `:feature-history` | Android lib | Session history list and the post-session Details report screen, plus export/share. |
| `:app` | Android application | Composition root only: `MainActivity`, `MainViewModel` (theme/accent StateFlows), `AppNavHost` (nav graph wiring every feature screen together), `IoMotionApp` (`@HiltAndroidApp`). Contains no business logic or reusable UI of its own. |

## Dependency rules

1. **`:core-common` has zero project dependencies.** Every other module can depend on it; it can
   depend on nothing in this repo. It's the one type-sharing module every layer agrees on.
2. **`:core-analysis` and `:core-export` are pure Kotlin JVM modules** (`kotlin.jvm` plugin, no
   Android Gradle plugin). They cannot reference `android.*` APIs even by accident — this is a
   deliberate boundary that keeps the biomechanical engine unit-testable without an emulator/Robolectric
   and keeps exporters free of file-I/O concerns.
3. **Feature modules depend downward only**, never on each other. `feature-live`, `feature-video`,
   and `feature-history` each depend on some subset of `core-common` / `core-analysis` / `core-pose`
   / `core-ui` / `data` / `core-export`, but never on one another. Screens hand off between features
   exclusively through `:app`'s `AppNavHost` (a route string + args), not a direct function call.
4. **`:app` is the only module allowed to depend on every feature module.** It is the composition
   root: it wires navigation and constructs the Hilt graph, and should contain no business logic or
   reusable composables of its own — if a screen or component belongs anywhere else, it doesn't
   belong in `:app`.
5. **Repository interfaces, not implementations, cross module boundaries.** `:data` exposes
   `SessionRepository` / `SettingsRepository` as interfaces with `@Binds` implementations
   (`SessionRepositoryImpl`, `SettingsPreferences`) — feature modules and ViewModels inject the
   interface. This is what makes `:data`'s Room/DataStore choice swappable without touching
   consumers.
6. **No circular module dependencies** — enforced structurally by Gradle (a cycle is a build error),
   not by convention.

## Data flow

Two independent pipelines — live camera and offline video — both terminate in the *same*
`ExerciseAnalyzer` contract, so a squat performed live and a squat analyzed from a video file
produce metrics with identical definitions and formulas.

### Live camera pipeline

```
CameraX ImageAnalysis frame
   → PoseImageAnalyzer (core-pose, on a dedicated single-thread executor)
   → PoseLandmarkerHelper.detectAsync (MediaPipe, RunningMode.LIVE_STREAM)
   → PoseResultConverter.convert(...)               → PoseFrame
   → PoseFrameSource._frames (MutableSharedFlow)     → PoseFrameResult
   → LiveViewModel.onFrameResult(...)
        ├─ if session active: ExerciseAnalyzer.update(frame) → AnalyzerState.Tracking / HoldTracking
        └─ updates LiveUiState (StateFlow) → LiveScreen recomposes (skeleton overlay, rep counter, form alerts)
   → [Stop tapped, or ViewModel cleared while active]
        ExerciseAnalyzer.finish() → SessionMetrics → SessionRepository.save(...)
```

`PoseFrameSource` and `PoseLandmarkerHelper` are `@Singleton`-scoped — one camera/inference
pipeline lives for the app's process lifetime, rebindable per visit to the Live screen, rather than
being recreated (and leaking an inference thread) on every navigation.

### Offline video pipeline

```
User picks a video Uri
   → VideoAnalysisSession.process(uri, config)   (core-pose)
        MediaMetadataRetriever extracts frames at a fixed interval
        → PoseLandmarker.detectForVideo (MediaPipe, RunningMode.VIDEO, CPU delegate only —
          GPU delegate doesn't reliably support the strictly-monotonic timestamps VIDEO mode needs)
        → PoseResultConverter.convert(...)         → PoseFrame
        → emits ProgressEvent.Frame(progress, poseFrame)
   → VideoViewModel.processVideo(...)
        for each Frame event: ExerciseAnalyzer.update(poseFrame) (same analyzer as the live path)
        → VideoUiState.Processing (StateFlow) → VideoScreen shows progress
   → ProgressEvent.Complete
        → ExerciseAnalyzer.finish() → SessionMetrics → SessionRepository.save(...)
        → VideoUiState.Result → VideoScreen shows the report
```

A fresh `PoseLandmarker` is created and released per `process()` call (unlike the live path's
persistent helper) since video analysis is a one-shot batch job with no between-call state to keep
warm.

### Persistence & history

```
SessionRepository.save(metrics, mode, modelVariant)
   → runs on ApplicationScope (a process-lifetime CoroutineScope, NOT viewModelScope) —
     a session ended by the user backgrounding the app or the ViewModel being torn down
     immediately after Stop must still complete its write.
   → SessionMetrics.toEntity(...) → SessionEntity + List<RepEntity>   (data/repository/SessionRepositoryImpl.kt)
   → SessionDao.insertSession(...) / insertReps(...)   (Room, AppDatabase)

HistoryScreen  → HistoryViewModel.uiState ← SessionRepository.sessions (Flow<List<SessionRecord>>, observed live)
SessionReportScreen → SessionReportViewModel.load(id) ← SessionRepository.getById(id)
```

`SessionWithReps.toRecord()` returns `null` (dropping that row) rather than throwing if a stored
`analysisMode`/`exerciseType` string doesn't match a current enum constant — e.g. after a future
rename — so one corrupted/stale row can't crash every observer of the history list.

### Export & share

```
SessionReportViewModel.exportJson() / .exportCsv()
   → SessionJsonExporter / SessionCsvExporter.export(...)   (core-export, pure string serialization)
   → written to a cache file, wrapped in a FileProvider content:// Uri
   → emitted on a Channel<ShareContent> → SessionReportScreen launches ACTION_SEND
```

## State management

- Every screen-level ViewModel is a `@HiltViewModel` exposing a single `StateFlow<UiState>` (or a
  sealed-class state for multi-phase screens like `VideoUiState`: `Idle` / `Processing` / `Result` /
  `Error`). Composables `collectAsState()` it; there is no business logic inside a `@Composable`.
- Nav-arg-derived initial state (`exerciseType`, `modelVariant`) is read directly from the nav
  backstack's `SavedStateHandle` in the ViewModel constructor, not via a separate `initialize()`
  call from the composable — the latter left a window where the UI could observe default state
  before initialization landed (see `LiveViewModel`/`VideoViewModel` kdoc).
- One-shot events that shouldn't survive recomposition or be replayed (share intents) go through a
  `Channel`, not the state `StateFlow`.

## Dependency injection

Hilt, with exactly one `@Module` in the whole codebase: `DataModule` (`:data`), providing:

- `AppDatabase` / `SessionDao` (Room, `fallbackToDestructiveMigration()` — safe only because
  `AppDatabase.version` has never been bumped; a real `Migration` is required the moment it is)
- The settings `DataStore<Preferences>` (file name `theme_prefs`, kept from before the accent-color
  and default-model-variant additions so existing persisted preferences aren't lost)
- `@Binds SessionRepositoryImpl → SessionRepository` and `SettingsPreferences → SettingsRepository`
- `@ApplicationScope CoroutineScope` — a `SupervisorJob() + Dispatchers.Default` scope that is
  never itself cancelled, used for persistence writes that must outlive the caller

Everything else Hilt-related is `@HiltViewModel` constructor injection or `@Singleton` on a plain
class (`PoseFrameSource`, `VideoAnalysisSession` in `:core-pose`) — no additional `@Module`s.

## Navigation

Single `NavHost` in `:app` (`AppNavHost.kt`) with a private `Routes` object as the only place route
strings are constructed:

```
home
history
settings
report/{sessionId}
live/{exerciseType}/{modelVariant}
video/{exerciseType}/{modelVariant}
```

`exerciseType`/`modelVariant` travel as string nav args rather than a shared in-memory object —
each screen re-parses them (`parseEnumOrDefault`, falling back to a sane default rather than
throwing on a stale/renamed value) directly from its own `SavedStateHandle`.

## Theming

`:core-ui`'s `Theme.kt` (`IO_motionTheme`) is the single place light/dark colors and the selected
accent color (`AccentTheme` → `Color`, plus the luminance-based accent-on contrast rule) resolve
into a `MaterialTheme.colorScheme` + `ExtendedColors` (muted text tiers, hairline, score-color
tiers). Screens read `MaterialTheme.colorScheme.primary` for "the accent," never a hardcoded color,
so switching accent in Settings repaints every screen without per-screen changes. See
`doc/CLAUDE_CODE_PROMPT_DESIGN.md` for the full visual spec this implements.

## Persistence schema

`AppDatabase` (Room, version 1): `SessionEntity` (one row per completed session) + `RepEntity`
(one row per valid rep, foreign-keyed to its session). Schema JSON is exported to `data/schemas/`
and committed — Room diffs future `Migration`s against these snapshots. `SessionRepository` is the
error boundary: it never lets a Room/DataStore-shaped exception or a stale enum string escape as
anything other than a dropped row or a sane default.

## Testing boundaries

`:core-common`, `:core-analysis`, and `:core-export` are plain `kotlin.jvm` modules — their tests
(`core-analysis/src/test/...`) run as fast, dependency-free JVM unit tests with no Android
framework, emulator, or Robolectric needed. This is the direct payoff of dependency rule #2: the
most important module (the biomechanical engine) is also the cheapest and fastest to test.
