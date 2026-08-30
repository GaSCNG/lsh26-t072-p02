# P02 — Pharmacy Expiry Shelf Check

| | |
|---|---|
| **Team ID** | `<TEAM_ID>` |
| **Problem ID** | P02 — Pharmacy Expiry Shelf Check |
| **Live URL** | `<LIVE_URL>` |
| **Language / Runtime** | Java (JDK 21, standard library only — Swing/AWT for the UI, no third-party frameworks) |

---

## 1. What's in this repo

| File | Purpose |
|---|---|
| `Medicine.java` | Domain model for a single stock item. Computes its status (`EXPIRED`, `EXPIRING_SOON`, `EXPIRING_90`, `SAFE`, `RETURNED`) relative to a supplied reference date. |
| `SampleData.java` | The required ≥40-item stock list (44 medicines), with expiry dates generated relative to "today" so the four groups are populated correctly whenever it's run. |
| `StockManager.java` | Holds the stock list, exposes counts/values per group, handles marking an item returned, search/filter, and the bonus 6‑month chart data. |
| `PharmacyApp.java` | Swing GUI: summary cards, tabbed group views, mark‑as‑returned action, search/filter, bonus value‑at‑risk chart. |
| `CaseRunner.java` | Headless command‑line runner used for judging: loads a JSON test file, builds a `StockManager` per case with a **fixed** `today`, applies `mark_returned`, and prints the dashboard for each case. Includes a small hand‑rolled JSON parser (no external library). |
| `testcases.json` | Sample/public test cases in the competition's schema. |
| `run.sh` | Compiles and launches the GUI app. |

---

## 2. Setup & run steps

**Requirements:** JDK 17+ (tested on JDK 21). No external dependencies, no internet access needed to build.

### Option A — Run the GUI (interactive demo)

```bash
chmod +x run.sh
./run.sh
```

This compiles `Medicine.java`, `SampleData.java`, `StockManager.java`, `PharmacyApp.java` and launches the desktop window, pre-loaded with the 44‑item sample stock.

### Option B — Run the judged/headless case runner

```bash
javac -encoding UTF-8 Medicine.java SampleData.java StockManager.java CaseRunner.java
java CaseRunner testcases.json
```

This prints a dashboard block for every case in the JSON file (counts + taka value per group, plus the returned list), using each case's own fixed `today` rather than the system clock.

> **Note:** the console output uses box‑drawing characters. If you see `?` marks instead of `┌─│└`, your terminal locale isn't UTF‑8 — this is a display-only issue (`export LANG=en_US.UTF-8` before running fixes it), not a program bug.

---

## 3. Proof that each requirement is met

Verified by compiling both `PharmacyApp.java` and `CaseRunner.java` clean, and running `CaseRunner` against the full `testcases.json` (25 cases, all processed with no errors: `=== All 25 cases processed ===`).

**Requirement 1 — Stock list of ≥40 medicines, mixed expiry states**
`SampleData.java` hard-codes 44 medicines across companies/batches, split by construction into expired (8), expiring 0–30 days (9), expiring 31–90 days (10), and safe/>90 days (17), all generated relative to today so the mix is preserved on any run date.

**Requirement 2 — Dashboard split into four groups with counts**
`StockManager.countByStatus(...)` / `valueByStatus(...)` compute per-group counts and values from `Medicine.getStatus(today)`. Sample real output for case `PUB-01` (today = 2026‑08‑16, 47 input items, 1 returned):

```
Expired                  10 items   Tk  21,218.90
Expiring Soon 0-30d      10 items   Tk  96,934.25
Expiring 31-90d          11 items   Tk  91,160.70
Safe >90d                15 items   Tk  45,760.35
Returned                  1 items   (IDs: M006)
TOTAL (active)           46 items   Tk 255,074.20
```
(10+10+11+15 = 46 active items; 46+1 returned = 47 input items — the count reconciles.)

**Requirement 3 — Mark as returned leaves active groups, moves to a separate list**
`StockManager.markReturned(Medicine)` sets `Medicine.returned = true`; `getStatus(today)` then always returns `RETURNED` regardless of expiry, so the item is excluded from every active group's count and value the moment it's marked, and only appears via `countByStatus(RETURNED)` / the Returned tab. Confirmed above: item `M006` is out of all four active buckets and listed only under `Returned`.

**Requirement 4 — Total taka value shown for Expired and Expiring‑soon**
`StockManager.valueByStatus(EXPIRED)` and `valueByStatus(EXPIRING_SOON)` sum `quantity × unit_price_bdt` for each active item in that group (`Medicine.getTotalValue()`). Shown as "Value at Risk" on the GUI's summary cards, and printed directly by `CaseRunner` (see `Tk 21,218.90` / `Tk 96,934.25` above).

---

## 4. Major decisions

- **Fixed "today" for judging, real clock for the GUI.** `StockManager` has two constructors: `StockManager()` uses `LocalDate.now()` for the interactive app, `StockManager(LocalDate today)` pins the reference date so `CaseRunner` produces deterministic, reproducible results against each test case's own `today`.
- **Groups are a strict partition**, not overlapping/cumulative buckets: expired, 0–30 days, 31–90 days, and safe are mutually exclusive, matching the "four groups" / "value means the within-30-days group only" wording in the spec.
- **"Returned" removes an item from active calculations entirely**, rather than just hiding it in the UI — `getStatus()` returning `RETURNED` is the single source of truth used by both counts and value sums, so there's no way for a returned item to leak into a total.
- **No external JSON library.** `CaseRunner` includes a small hand-rolled recursive-descent JSON parser so the project has zero third-party dependencies and builds with nothing but the JDK.
- **Bonus features** (not required, included anyway): search/filter by name or company across all tabs, and a 6-month value-at-risk bar chart.

---

## 5. Known limitations

- The Swing GUI (`PharmacyApp.java`) is a desktop application; it is not itself accessible via the live URL — see the URL description above for what is actually hosted there.
- Console output from `CaseRunner` relies on UTF‑8 box‑drawing characters; on a POSIX/`C`-locale terminal these render as `?`. Purely cosmetic (see note in section 2).
- `Medicine.Status` colors are defined using `java.awt.Color`, so `Medicine.java` pulls in `java.awt` even though the headless `CaseRunner` path never renders anything — harmless, but means the "headless" build still has AWT on the classpath.
- No persistence: all state (including `mark_returned` from a test case) lives only in memory for the duration of a single run.

---

## 6. Contributions

See `LICENSES.md` for third-party assets/dependencies (none beyond the JDK standard library) and the team contribution statement below.

| Member | Contribution |
|---|---|
| `<NAME_1>` | `<CONTRIBUTION_1>` |
| `<NAME_2>` | `<CONTRIBUTION_2>` |
| `<NAME_3>` | `<CONTRIBUTION_3>` |

---

## 7. Approach summary

`<ONE-TWO PARAGRAPH SUMMARY OF HOW YOUR TEAM APPROACHED THE PROBLEM>`
