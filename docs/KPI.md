# KPI Reference

Precise definitions and formulas for every metric and quality score computed by `:core-analysis`.
This is the canonical reference linked from `KpiCalculator`, `RepBasedAnalyzer`, `PlankAnalyzer`,
`SessionMetrics`, and `RepMetrics` — if a formula changes, update it here too.

All angles are computed from **3D world landmarks** (metric space, hip-centered), not normalized
image landmarks, so results are invariant to camera distance and subject scale.

## Contents

- [Confidence gating](#confidence-gating)
- [Angle computation](#angle-computation)
- [Signal smoothing](#signal-smoothing)
- [Rep-based exercises](#rep-based-exercises-squat-sit-up-push-up)
  - [FSM & hysteresis](#fsm--hysteresis)
  - [Per-exercise primary angle](#per-exercise-primary-angle)
  - [Rejected reps](#rejected-reps)
  - [Per-rep quality score](#per-rep-quality-score)
  - [Form-score components](#form-score-components)
- [Plank (hold-based)](#plank-hold-based)
- [Session-level KPIs](#session-level-kpis)
- [Quality-score color tiers](#quality-score-color-tiers)
- [Body normalization (reserved)](#body-normalization-reserved)
- [Config reference](#config-reference)

---

## Confidence gating

`ConfidenceGate` (`core-analysis/confidence/ConfidenceGate.kt`) guards every angle computation
from unreliable landmarks (occlusion, out-of-frame, motion blur).

A landmark is **reliable** only if both hold:

```
visibility ≥ visibilityThreshold   (default 0.5)
presence   ≥ presenceThreshold     (default 0.5)
```

For a 3-point angle (e.g. hip–knee–ankle), **all three** landmarks must be reliable or the whole
frame's angle is `null` — that frame becomes `AnalyzerState.LowConfidence` and is excluded from
every downstream computation (FSM, min/max tracking, quality scoring). Partial/garbage data never
propagates into a rep.

## Angle computation

`AngleMath.angleDegrees(a, b, c)` (`core-common/math/AngleMath.kt`) computes the angle at vertex
`b` formed by rays `b→a` and `b→c`, via the dot-product formula:

```
BA = a - b
BC = c - b
cosθ = (BA · BC) / (|BA| |BC|),  clamped to [-1, 1]
θ = degrees(acos(cosθ))
```

Returns `NaN` (treated as unavailable) if either arm vector has near-zero length. Result range:
`[0°, 180°]`.

## Signal smoothing

Every primary angle / body-line angle signal passes through a **One-Euro filter**
(`OneEuroFilter`, `core-analysis/filter/`) before being used, to suppress landmark jitter without
adding excessive lag:

| Parameter | Default | Meaning |
|---|---|---|
| `minCutoff` | 1.0 Hz (Plank: 0.5 Hz) | Lower = more smoothing on slow signals |
| `beta` | 0.007 (Plank: 0.001) | Higher = less lag on fast movements |
| `dCutoff` | 1.0 Hz | Cutoff for the internal derivative estimator |

This is a pre-processing step, not a KPI itself, but every threshold/formula below operates on the
**filtered** angle, not the raw landmark angle.

---

## Rep-based exercises (squat, sit-up, push-up)

Implemented by `RepBasedAnalyzer`, a finite-state machine shared by all three rep-counted
exercises; each subclass (`SquatAnalyzer`, `SitUpAnalyzer`, `PushUpAnalyzer`) only supplies the
primary angle extraction and the form-score component.

### FSM & hysteresis

```
AWAITING_EXTENSION ──→ EXTENDED ──→ FLEXED ──→ EXTENDED  (rep counted or rejected)
                          ↑                        │
                          └────────────────────────┘
```

- **AWAITING_EXTENSION**: waiting for the first `angle ≥ extendThreshold`. Prevents counting a
  partial rep from a mid-exercise start.
- **EXTENDED**: `angle ≥ extendThreshold`. Tracks `maxAngle`. Drops to **FLEXED** once
  `angle ≤ flexThreshold`.
- **FLEXED**: tracks `minAngle`. Returns to **EXTENDED** once `angle ≥ extendThreshold` again —
  this transition is when the rep is scored (see [Rejected reps](#rejected-reps)).

`extendThreshold` is always strictly greater than `flexThreshold`. The gap `(flexThreshold,
extendThreshold)` is a **dead-band**: angles inside it never trigger a state transition, so jitter
straddling a single boundary cannot produce spurious rep counts.

### Per-exercise primary angle

| Exercise | Primary angle | Landmarks (vertex in **bold**) | Extended (top) | Flexed (bottom) |
|---|---|---|---|---|
| Squat | Knee flexion | hip – **knee** – ankle | ≥ 160° | ≤ 100° |
| Sit-up | Hip flexion | shoulder – **hip** – knee | ≥ 140° (lying) | ≤ 70° (up) |
| Push-up | Elbow flexion | shoulder – **elbow** – wrist | ≥ 150° | ≤ 95° |

The primary angle is the **average of the left and right side** when both are reliable, otherwise
whichever single side is available (`null` only when neither side is reliable that frame).

### Rejected reps

On the FLEXED → EXTENDED transition:

```
rom = maxAngle - minAngle
if rom ≥ minRom (40° for all three exercises):
    rep is counted, RepMetrics built
else:
    rejectedCount += 1   (no RepMetrics recorded)
```

This filters out shallow, partial-motion "reps" that would otherwise inflate the rep count.

### Per-rep quality score

`KpiCalculator.repQualityScore(...)` — the same formula for all three rep-based exercises, only
the `formScore` input differs per exercise:

```
depthRange  = flexThreshold - idealDepth
depthScore  = clamp((flexThreshold - minAngle) / depthRange, 0, 1)      [depthRange ≤ 0 → 1.0]

threshRange = extendThreshold - flexThreshold
romScore    = clamp((maxAngle - minAngle) / threshRange, 0, 1)          [threshRange ≤ 0 → 1.0]

formScore   = exercise-specific, clamped to [0, 1] (see below)

rawScore      = depthScore × 0.50 + romScore × 0.30 + formScore × 0.20
qualityScore  = round(rawScore × 100), clamped to [0, 100]
```

Weighting: **depth 50%, range-of-motion 30%, form 20%**.

- `depthScore` = 1.0 when `minAngle ≤ idealDepth` (went at least as deep as the ideal target),
  scaling linearly to 0 as `minAngle` approaches `flexThreshold`.
- `romScore` = 1.0 when the rep's actual ROM (`maxAngle - minAngle`) fills the whole hysteresis
  band (`extendThreshold - flexThreshold`), scaling linearly to 0 for a zero-ROM rep.

### Form-score components

| Exercise | Form component | Formula |
|---|---|---|
| Squat | Left/right knee symmetry | `symmetryScore` |
| Sit-up | Fixed | `formScore = 1.0` (form fully captured by the angle itself) |
| Push-up | Body-line straightness | `bodyLineScore` |

**`KpiCalculator.symmetryScore(leftAngle, rightAngle, maxDeviation)`** (squat: `maxDeviation` =
`maxSymmetryDeviation`, default 15°) — uses each side's *minimum* angle reached during the rep:

```
deviation = |leftAngle - rightAngle|
score = clamp(1 - deviation / maxDeviation, 0, 1)     [maxDeviation ≤ 0 → 1.0]
```

**`KpiCalculator.bodyLineScore(bodyLineAngle, tolerance)`** (push-up: `tolerance` = 25°; also used
directly by Plank, see below) — `bodyLineAngle` is the shoulder–hip–ankle angle, ideally 180°:

```
deviation = |bodyLineAngle - 180|
score = clamp(1 - deviation / tolerance, 0, 1)     [tolerance ≤ 0 → 1.0]
```

### Form alerts (not scored, informational only)

Raised alongside — but independent of — the quality score:

- `GO_DEEPER`: `minAngle > flexThreshold + 5°`
- `UNEVEN_SIDES` (squat only): `|leftMin - rightMin| > maxSymmetryDeviation`
- `SAGGING_HIPS` / `PIKING_HIPS` (push-up): body-line deviation exceeds ±`bodyLineTolerance` (25°)
- `STRAIGHTEN_BODY_LINE` (push-up, per-rep only): `|deviation| > bodyLineTolerance × 0.5` but within
  tolerance

---

## Plank (hold-based)

`PlankAnalyzer` doesn't count reps — it tracks **duration of correct form**.

**Body-line angle**: shoulder–hip–ankle, averaged across both sides when both are reliable
(single side otherwise). Filtered with the plank-specific One-Euro settings (`minCutoff = 0.5`,
`beta = 0.001` — more smoothing, since a plank hold moves far less than a rep).

**Form gate** (per frame):

```
deviation = |bodyLineAngle - 180|
isGood    = deviation ≤ bodyLineTolerance   (default 15°)
```

**Valid-hold accumulation**: `validHoldMs` only advances between two *consecutive* good frames —
accumulation pauses the instant form breaks and resumes seamlessly once it's restored (no penalty
for the broken segment beyond simply not counting that time):

```
if isGood:
    if previous frame was also good:
        validHoldMs += (this frame's timestamp - previous good frame's timestamp)
```

**Session quality score**:

```
formRatio    = validHoldMs / totalDurationMs     (0.0 if totalDurationMs ≤ 0)
qualityScore = round(formRatio × 100), clamped to [0, 100]
```

i.e. the percentage of the whole session spent in acceptable form.

**`avgBodyLineAngle`** = mean of the (filtered) body-line angle across all *good* frames only
(180.0 if there were none).

**Plank form alerts**: `SAGGING_HIPS` (deviation < −tolerance), `PIKING_HIPS` (deviation >
+tolerance), `STRAIGHTEN_BODY_LINE` (`|deviation| > tolerance × 0.5` but still within tolerance).

---

## Session-level KPIs

Computed once per session by `KpiCalculator`, from the completed rep list (or plank state).

**Tempo** — valid reps per minute:

```
tempoRpm = repCount / (durationMs / 60000)     (0.0 if repCount = 0 or durationMs ≤ 0)
```

**Rhythm consistency** — 100 = perfectly even inter-rep timing, lower = erratic pace:

```
intervals    = consecutive differences between rep-completion timestamps (ms)
mean         = average(intervals)
variance     = average((interval - mean)²)
stdDev       = √variance
CV           = (stdDev / mean) × 100            [coefficient of variation, %]
consistency  = round(clamp(100 - CV, 0, 100))
```

Returns 100 when fewer than 2 reps were completed (no interval exists to measure) or when
`mean ≤ 0`.

**Average ROM** — arithmetic mean of each valid rep's `rom` (`maxAngle - minAngle`); 0.0 for an
empty rep list.

**Session quality score** (rep-based exercises) — arithmetic mean of every valid rep's
`qualityScore`, rounded; 0 for an empty rep list:

```
sessionQualityScore = round(average(rep.qualityScore for rep in reps))
```

For **Plank**, `sessionQualityScore` is instead the hold-based `formRatio × 100` described above
(there are no per-rep scores to average).

**Rejected rep count** — attempts that entered FLEXED but whose `rom < minRom` on return to
EXTENDED (see [Rejected reps](#rejected-reps)); these do **not** contribute to tempo, rhythm,
average ROM, or the quality score.

---

## Quality-score color tiers

The 0–100 quality score (session or per-rep) is mapped to a 3-tier color for UI presentation.
Two independent thresholds exist in the codebase for different surfaces:

| Surface | Success (green) | Warning (amber) | Danger (red) |
|---|---|---|---|
| Live/Video HUD (`MetricGauge`) | ≥ 75 | 50–74 | < 50 |
| Details / History screens (`ExtendedColors.scoreColor`) | ≥ 85 | 60–84 | < 60 |

The Details/History thresholds (85/60) come from the current design spec
(`doc/CLAUDE_CODE_PROMPT_DESIGN.md`) and are the ones to follow for any new score-color UI; the
Live/Video HUD's looser 75/50 split predates it and hasn't been reconciled.

---

## Body normalization (reserved)

`BodyNormalization` (`core-analysis/normalization/`) is **not currently called by any analyzer** —
every quality score above is angle-based, which is already scale-invariant and needs no
normalization. It's kept for a future distance- or velocity-based metric:

```
torsoLength = distance(midpoint(leftShoulder, rightShoulder), midpoint(leftHip, rightHip))
normalize(value) = value / torsoLength
```

Returns `null` if any of the four landmarks is below the visibility threshold or if
`torsoLength < 0.01 m` (implausibly small — guards divide-by-near-zero).

---

## Config reference

All threshold constants, by exercise (`RepAnalyzerConfig` / `PlankConfig` defaults):

| Exercise | flexThreshold | extendThreshold | minRom | idealDepth | maxSymmetryDeviation | bodyLineTolerance |
|---|---|---|---|---|---|---|
| Squat | 100° | 160° | 40° | 80° | 15° | — |
| Sit-up | 70° | 140° | 40° | 55° | 15° (unused; form fixed at 1.0) | — |
| Push-up | 95° | 150° | 40° | 80° | — | 25° |
| Plank | — | — | — | — | — | 15° |

Shared across all: `visibilityThreshold = 0.5`, `presenceThreshold = 0.5` (`ConfidenceGate`
defaults).
