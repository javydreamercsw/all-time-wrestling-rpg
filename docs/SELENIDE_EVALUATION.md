# Selenide Migration Evaluation (ATW-lzh9)

**Date:** 2026-08-29 · **Verdict: do not migrate.** Keep the Selenium suite and
its custom infrastructure; adopt two cheap guardrails instead. A bounded pilot
recipe is included in case the decision is revisited.

## 1. What we have today

- **~93 concrete `*E2ETest` classes, ~242 `@Test` methods**, sharded 2-way at
  class level by `E2EShardCondition` across three CI job families (E2E, docs
  screenshots, video validation). Failsafe retries: `rerunFailingTestsCount=2`.
- **`AbstractE2ETest` (1,654 LOC)** is effectively an in-house framework on raw
  Selenium 4.47:
  - shared ChromeDriver with retried creation, JVM-shutdown cleanup, overlay
    teardown;
  - `login()` with 3 retries and account-lockout self-repair;
  - `navigateToAndWaitForElement()` — 3-attempt navigation that absorbs the
    login-redirect race and transient Vaadin route-init failures;
  - `waitForVaadinClientToLoad()` — 4-phase JS wait (readyState, Vaadin Flow
    client idle, layout, double rAF), which no off-the-shelf framework knows
    how to do;
  - hardened `clickElement()` (scroll, drawer auto-close, native → JS →
    synthetic-event fallbacks, stale-element retry);
  - deep Vaadin shadow-DOM helpers (`selectFromVaadinComboBox`,
    `selectFromVaadinMenuBar`, `findButtonInGridRow`, grid settle/populate
    waits) built on `JavascriptExecutor`;
  - docs screenshot capture (`documentFeature` → `docs/manifest.json`) and a
    full video pipeline (8 fps screenshot polling, Java2D caption burn-in,
    optional TTS, external `ffmpeg` assembly).
- 47 files still use raw `WebDriverWait`/`ExpectedConditions` for waits *after*
  arrival; 17 use the retry-navigation helper; 23 use `JavascriptExecutor`
  directly.

## 2. Evidence from the 2026-08 flake campaign

Every deterministic or recurring E2E failure in the recent stabilization work
had one of these root causes:

|                             Root cause                              |  Count (classes)   |                                                             Would Selenide have prevented it?                                                             |
|---------------------------------------------------------------------|--------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------|
| Login-redirect navigation race (browser stranded on Home)           | 13 classes fixed   | **No** — `Selenide.open()` is the same single-shot navigation; the fix was our retrying `navigateToAndWaitForElement`, which had to be written either way |
| Runner overload (broad timeouts, recover on rerun)                  | many, transient    | **No** — infrastructure, not API                                                                                                                          |
| Data/fixture bugs (`Campaign.hashCode` lazy init, missing universe) | 2                  | **No**                                                                                                                                                    |
| Statistical assertion (`shouldFavorFactionWithHighAffinity`)        | 1                  | **No**                                                                                                                                                    |
| Element-level wait misses (stale element, missing explicit wait)    | ~0 after hardening | Partially — this is Selenide's core value, and it is the one category we no longer suffer from                                                            |

The suite's flakiness was never primarily an element-wait problem — the
category Selenide is best at. Our custom Vaadin-aware waits are *stronger* than
Selenide's generic 4-second polling (Selenide cannot know when the Vaadin Flow
client is idle).

## 3. What Selenide would buy — and cost

**Buys:** automatic element re-lookup (`$()` lazy proxies) eliminating the
remaining stale-element retries; terser assertions (`$.shouldHave(text(...))`)
in the 47 files with raw waits; automatic failure screenshots (we already have
`UITestWatcher` + sequenced screenshots); nicer error messages.

**Costs:**
- Touch ~93 classes / ~242 tests, or run a mixed suite indefinitely.
- Re-plumb the 1,654-LOC base class: driver lifecycle must move to Selenide's
`WebDriverRunner`/config (conflicts with our retried creation + shared-driver
reuse strategy); every helper that takes/returns `WebElement` (click
fallbacks, combo-box/grid JS helpers, video capture's `TakesScreenshot`
polling) needs adaptation.
- The Vaadin-specific 60% of the base class transfers as-is *at best* — Selenide
adds nothing to shadow-DOM JS piercing, Flow-client idle detection, or grid
virtualization handling.
- Risk window: every migrated test needs re-verification with retries disabled;
a half-migrated suite has two wait models to reason about during CI triage.
- Team/agent knowledge: the current helpers are documented by usage in 90+
classes and in CLAUDE.md guidance.

Estimated effort: 2–4 focused weeks including verification — for a suite whose
deterministic flake sources are already fixed.

**Alternatives considered:** Vaadin TestBench (vendor-aware waits and element
API — the technically best fit, but commercial licensing); Playwright (superior
auto-waiting and trace viewer, but a full rewrite plus Java bindings maturity
trade-offs). Neither clears the cost bar today either.

## 4. Recommendation

1. **Stay on Selenium + the in-house helpers.** The investment already made is
   Vaadin-aware in ways no generic framework matches.
2. **Guardrail A — ban raw first-navigation:** add a lightweight ArchUnit-style
   or grep-based test asserting no `*E2ETest` calls single-shot
   `navigateTo(`/`driver.get(` as the first navigation in a test (allow-list
   for special cases). This locks in the flake campaign permanently.
3. **Guardrail B — wait through helpers:** prefer `waitForVaadinElement*` and
   the grid/combo helpers over inline `WebDriverWait` in new tests; note it in
   CLAUDE.md's test guidance.
4. **Re-evaluate only on a trigger:** a new class of element-level flake that
   the helpers cannot absorb, a Vaadin upgrade that breaks the JS waits, or a
   decision to buy TestBench.

## 5. Bounded pilot recipe (if the decision is revisited)

- Add `com.codeborne:selenide` (test scope) pinned alongside Selenium 4.47;
  configure `WebDriverRunner.setWebDriver(driver)` to reuse the existing shared
  driver rather than Selenide's lifecycle.
- Convert exactly one mid-complexity class (`HolidayListViewE2ETest` — dialogs,
  grid, tabs) keeping `AbstractE2ETest` navigation/login untouched.
- Success criteria: 20 consecutive local runs with retries disabled, no wait
  regressions, net LOC reduction ≥ 25% in the class, zero new base-class
  adaptations required. Anything less confirms the "do not migrate" verdict.

