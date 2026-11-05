# Doubt Board — project spec for Claude Code

Doubt Board is an Android app. A student snaps a maths problem, and a teacher solves it on a whiteboard:
handwriting that draws itself stroke by stroke, a natural voice explaining each step, and the student can
tap any step to have it explained again. When the student keeps getting stuck, the app finds the real
weak skill and teaches that first.

v1 subject: **engineering-level calculus**, starting with **first-order ODEs** (separable, linear)
and the derivative/integration skills they depend on.

Read this whole file before writing code. When this file and a request conflict, ask.

---

## 1. Hard constraints (never break these)

1. **Zero running cost.** No paid API, ever. No API keys committed to the repo. Everything runs on-device
   or on a free tier (Supabase free, Cloudflare Workers free, Gemini API free tier).
2. **Maths comes from the algebra engine, never from an LLM.** Every number, expression and answer shown
   on the board is computed and verified by the CAS. An LLM may only write or rephrase *words*.
3. **Never show an unverified step.** If verification fails, show "I couldn't verify this one yet" rather
   than a possibly wrong solution.
4. **Requests can never hang.** Every network call: 15 s timeout, exponential backoff with jitter,
   honour HTTP 429 `Retry-After`, max 3 retries, circuit breaker that drops to offline mode. The UI never
   waits on the network to start drawing.
5. **Offline first.** The core flow (input → solve → board → voice) must work in airplane mode.
6. **Low battery.** No continuous background work. Models load lazily and unload when the lesson ends.

## 2. Tech stack

| Area | Choice | Notes |
|---|---|---|
| Language / UI | Kotlin, Jetpack Compose | minSdk 26, targetSdk latest stable |
| Architecture | Single-activity, MVVM, Kotlin coroutines + Flow | Hilt for DI |
| CAS | Symja (`matheclipse-core`) on the JVM | Check APK size impact before committing; flag if > 25 MB |
| Step engine | Our own Kotlin code over Symja expressions | See §5 |
| OCR | ML Kit Text Recognition v2 (on-device) | Followed by a confirm/edit screen (§6.1) |
| Voice | Piper voices via `sherpa-onnx` Android | Fallback: Android `TextToSpeech` |
| On-device LLM (later) | ML Kit GenAI / Gemini Nano where supported, else skip | Optional milestone; app must be complete without it |
| Local storage | Room | Mastery, events, cached lessons |
| Cloud (later) | Supabase (Postgres + pgvector), Cloudflare Worker | Optional milestone |
| Tests | JUnit5, Kotest assertions, Compose UI tests | Math code is test-first |

Ask before adding any dependency larger than 5 MB or any network SDK.

## 3. Repo layout

```
app/                      Android app module (UI, navigation, DI)
core/board/               Board script model, renderer, handwriting, typesetter
core/math/                Symja wrapper, parser, step engine, method recipes, verifier
core/narration/           Rule template library, narration builder, (optional) LLM rephraser
core/voice/               TTS engine abstraction, Piper + system TTS implementations
core/adapt/               Skill graph, BKT mastery, decision ladder, next-problem picker
core/capture/             Camera, OCR, LaTeX/plain-text normalizer, confirm screen
core/data/                Room DB, repositories
core/net/                 (later) gateway client with timeout/retry/circuit breaker
content/                  Skill graph JSON, narration templates, sample problems
tools/                    Scripts (e.g. validate templates, generate fixtures)
reference/adaptive_tutor.py   Python v0 of the adaptive engine — port this to core/adapt
```

## 4. The board script (the central contract)

Everything that appears on the board is a `BoardScript`. Solvers produce it, the verifier checks it,
the renderer plays it. The renderer never sees raw model text.

```json
{
  "id": "ode-linear-0001",
  "problem": "dy/dx + 2y = e^x",
  "topic": "ode_first_order_linear",
  "method": "integrating_factor",
  "steps": [
    {
      "id": "s1",
      "skill": "identify_linear_ode",
      "narration_key": "linear.identify",
      "narration_vars": {"P": "2", "Q": "e^x"},
      "board": [
        {"action": "equation", "math": "dy/dx + 2y = e^x", "row": 0},
        {"action": "arrow_note", "text": "P(x) = 2,  Q(x) = e^x", "row": 0}
      ],
      "marks": [{"action": "underline", "target": 0}],
      "check": {"kind": "identity", "expr": "..."}
    }
  ],
  "answer": "y = e^x/3 + C*e^(-2x)",
  "verification": {"method": "substitute_back", "passed": true}
}
```

- **Actions (closed set):** `write`, `equation`, `arrow_note`, `underline`, `box`, `highlight`, `plot`,
  `region`. Unknown actions fail validation.
- **Layout:** a row grid. Each board item has a `row`; the renderer decides pixels. Long equations wrap
  at operators. The board auto-scrolls to keep the current step visible.
- **`math`** uses a small plain-text maths syntax (§7.2), not full LaTeX.
- **`narration_key`** points into the template library (§8). `narration_vars` fills it.
- Put a JSON Schema for this in `core/board/schema/board_script.schema.json` and validate every script
  produced in tests.

## 5. Maths core

### 5.1 Parsing
- Input: normalized plain text from the confirm screen, e.g. `dy/dx + 2*y = exp(x)`.
- Convert to a Symja expression. Unparseable input → back to the confirm screen with the error shown.

### 5.2 Step engine
Symja returns answers, not teacher-style steps. We record steps ourselves:
- **Derivatives:** recursive rule application (constant, power, sum, product, quotient, chain, exp,
  log, trig). Each rule application emits one step tagged with its skill.
- **Integrals (v1 subset):** power rule, exp, 1/x, basic trig, linearity, simple u-substitution
  (inner derivative appears as a factor). Anything else: Symja computes the result and the step is
  shown as one step tagged `integral_other`.

### 5.3 Method recipes (engineering topics)
Each problem type is a recipe: an ordered list of steps; Symja computes each sub-result.

v1 recipes:
1. **Separable ODE** `dy/dx = f(x)g(y)`: separate → integrate both sides → solve for y (if possible) → constant.
2. **Linear first-order ODE** `dy/dx + P(x)y = Q(x)`: identify P, Q → IF = e^∫P → multiply →
   left side as d/dx(y·IF) → integrate → solve for y.

A **method classifier** decides which recipe applies, using rule checks (is it of form X?). The chosen
method gets its own explanation step ("This is linear because…"). If nothing matches, say so plainly.

### 5.4 Verification
- Every step: check the transformation is valid (expressions equivalent via `Simplify[a - b] == 0`,
  or the documented rule holds).
- Final ODE answers: substitute back into the original equation and simplify to 0.
- Store the verification result in the script. Failed → don't render; log the problem for later.

Test-first: for each rule and recipe, write fixture problems with known answers before the code.
Target at least 30 fixtures per recipe.

## 6. Capture

### 6.1 Flow
1. Camera screen opens directly on launch (no onboarding before the first lesson).
2. Snap → ML Kit OCR → normalizer produces plain-text maths.
3. **Confirm screen:** shows the parsed problem rendered nicely plus an editable text field with a small
   maths key row (`^ / ( ) dy/dx ∫ e ln sin cos`). The student taps "Solve".
4. Also allow typing a problem from scratch on the same screen.

Handwritten maths OCR will be imperfect; the confirm screen is what makes it reliable.

## 7. Board renderer

### 7.1 Handwriting
- Draw with **single-stroke (centerline) glyphs** (e.g. Hershey Simplex or a similar single-line font,
  checked for licence) so letters are drawn like a pen, not faded in.
- Animate with `PathMeasure` segment extraction on a Compose `Canvas` at 60 fps.
- Pen speed constant in dp/second; step duration is the longer of drawing time and narration length.
- Respect "reduce motion": draw instantly.

### 7.2 Mini typesetter
A small layout engine for the maths syntax subset: exponents, subscripts, fractions, square roots,
`∫` with limits, `d/dx`, `dy/dx`, Greek letters, `e^x`, function names. Output = list of glyph strokes
with positions. Keep it small and well tested; no full LaTeX.

### 7.3 Interaction
- Bottom thumb zone: play/pause, step scrubber, speed.
- Tap anywhere on a drawn step → pause + three chips: *Explain simpler*, *Why this step?*,
  *Show another way*.
- Captions always on, synced with narration.

## 8. Narration

### 8.1 Rule template library (the main source of explanations)
- `content/narration/en/*.json`: per `narration_key`, three styles: `board`, `analogy`, `worked_example`,
  each a short spoken sentence with `{vars}`.
- Write templates for every skill in the v1 skill graph. Plain, spoken English, like a good teacher.
  Hinglish can be added later as a second language folder.
- A validator script in `tools/` checks every `narration_key` used by recipes exists in all styles.

### 8.2 Optional LLM rephraser (later milestone)
- Input: the verified step + the template sentence. Output: rephrased words only.
- Post-check: reject output that contains numbers or expressions not present in the verified step.

## 9. Voice
- `VoiceEngine` interface: `speak(text): Flow<Progress>`, `stop()`, `estimateDuration(text)`.
- Piper (via sherpa-onnx) as default; ship one English voice model as an optional download after install.
- Fallback to Android `TextToSpeech` if Piper isn't downloaded or fails.
- Synthesize the next step's audio while the current step is playing (one step ahead, no more).

## 10. Adaptive engine (on-device)

Port `reference/adaptive_tutor.py` to Kotlin in `core/adapt`. Keep behaviour identical and port its demo
as a unit test.

- **Skill graph:** `content/skills/engineering_calculus.json` — each skill lists prerequisites.
- **Mastery:** Bayesian Knowledge Tracing per skill. Defaults: know 0.30, learn 0.15, slip 0.10, guess 0.20.
- **Decision ladder** when stuck on a step:
  1. Skill < 0.40 and a prerequisite < 0.60 → go back to the weakest prerequisite.
  2. 1st time stuck → hint. 2nd → re-explain in a different style (prefer the style that has worked best
     for this student). 3rd → easier worked example.
- **Next problem:** weakest skill whose prerequisites are ≥ 0.60. Skills ≥ 0.85 go to spaced review.
- **Your turn:** after a lesson, offer a similar problem; the student enters their working line by line
  (typed or snapped); the verifier finds the **first wrong line**, blames its skill, updates mastery,
  and the board redraws only that step.
- Store mastery and every event (stuck tap, explain-again, wrong line) in Room.

## 11. Cloud (optional, later)

Only after the offline app works end to end.
- **Shared lesson cache** in Supabase: normalized problem hash + embedding → verified BoardScript.
  App checks local cache → cloud cache → solves locally.
- **Gateway:** Cloudflare Worker in front of the Gemini free tier, used only for problems the classifier
  can't handle (e.g. word problems). The Worker holds the key; the app never does.
- All §1.4 rules apply. A failing cloud must never block a lesson.

## 12. Milestones (one PR each, with tests passing)

| # | Milestone | Done when |
|---|---|---|
| M0 | Project skeleton | Modules build, CI runs unit tests, empty Compose screen launches |
| M1 | Renderer spike | 3 hand-written BoardScripts in `content/samples/` play with stroke animation and synced voice at 60 fps; tap-to-pause works |
| M2 | Maths core: derivatives | Parser + derivative step engine + verifier; ≥ 30 fixtures pass; output validates against the schema |
| M3 | ODE recipes | Separable + linear recipes with classifier and substitute-back verification; ≥ 30 fixtures each |
| M4 | Narration | Template library for all v1 skills in 3 styles; validator passes; explain-again chips work |
| M5 | Capture | Camera → OCR → confirm/edit → lesson, fully offline |
| M6 | Adaptive + your turn | Kotlin port matches the Python demo; first-wrong-line detection; mastery persisted |
| M7 | Cloud (optional) | Cache + gateway with timeout/retry/circuit breaker; airplane-mode test still passes |

Do milestones in order. Don't start M(n+1) until M(n)'s "done when" is met.

## 13. Working rules for Claude Code

- Write tests first for anything in `core/math` and `core/adapt`.
- Small, focused commits. Explain any deviation from this spec in the PR description.
- No secrets in code or git history. Config via `local.properties` / environment, gitignored.
- Prefer boring, well-known libraries. Check licences (GPL in particular) before adding any.
- When unsure about pedagogy (how a step should be explained), leave a `// PEDAGOGY:` note and ask.
- Keep the app usable on a mid-range Android phone with 4 GB RAM.

## 14. Out of scope for v1

iOS, free-form chat with the tutor, user accounts and payments (add after M6), subjects beyond
engineering calculus, teacher dashboards, social features.
