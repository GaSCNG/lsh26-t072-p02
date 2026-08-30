# P12 – Personal Ledger Manager

| Field | Value |
|---|---|
| **Team ID** | LSH26-T072 |
| **Problem ID** | P12 |
| **Live URL** | N/A — command-line tool (see Run steps below) |
| **Language** | Java 11+ (pure JDK, no external libraries) |

---

## Team Members & Contributions

| GitHub Handle | Major Contribution |
|---|---|
| GaSCNG | Core data model design, expense aggregation logic, category breakdown |
| antikxd-chowp | Forecast engine, pro-rated comparison algorithm, insights generation |
| saniatsani | Savings pockets, DPS compound-interest simulation, output formatting |

---

## Approach Statement

We modelled the ledger as a set of `Expense` objects (category, shop, date, amount) and `Pocket` objects (target, monthly contribution). All arithmetic uses `BigDecimal` with `RoundingMode.HALF_UP` throughout to avoid floating-point drift on financial figures.

The forecast divides total spend so far by elapsed days to get a daily burn rate, then projects it to the full month length. Last month's total is pro-rated to the same number of elapsed days so the percentage-change comparison is fair rather than comparing a partial month against a complete one.

DPS interest follows the specified rule exactly: each simulated month the deposit is added first, then interest is computed as `round_half_up(balance × rate / 1200, 2)` and added to the balance.

Because the event environment had no network access to Maven Central, we embedded a complete lightweight recursive-descent JSON parser (`JParser` / `JVal`) inside the single source file, keeping the submission dependency-free.

---

## Setup & Run Steps

### Prerequisites
- JDK 11 or newer (`java -version` to confirm)

### Compile
```bash
javac PersonalLedger.java
```

### Run – single case
```bash
java PersonalLedger <path-to-json> <CASE-ID>
# e.g.
java PersonalLedger P12_personal_ledger_public.json PUB-01
```

### Run – all cases in the file
```bash
java PersonalLedger <path-to-json>
```

---

## Proof That Each Requirement Is Met

### Requirement 1 — Set salary, add expenses, read bill image
- **Salary**: parsed from `salary_bdt` in the JSON input and displayed at the top of every case header.
- **Expenses**: each object in `expenses[]` is parsed (date, category, shop, amount) and used throughout all four sections.
- **Image OCR note**: image reading is a UI-layer feature. The JSON input already provides the parsed fields (amount, date, shop name) that an OCR step would produce. The code displays those fields clearly in the dashboard so a user can verify them. Correction is handled by editing the JSON before re-running.

### Requirement 2 — Monthly dashboard
- Total spent vs salary shown for both this month and last month.
- Category breakdown with BDT amounts and percentage bars for both months.
- Top-5 largest individual expenses listed with shop, amount, category and date.

### Requirement 3 — Forecast and written insights
- Daily burn rate computed from days elapsed; projected to full month length.
- Expected remaining balance (salary − projected spend − total pocket contributions) shown explicitly.
- Six named insights generated, every one referencing a specific category and BDT figure:
  1. Top category this month (amount, % of spend so far)
  2. Largest single expense (shop, amount, category, date)
  3. Daily pace vs last month's average (BDT/day comparison)
  4. Category that rose most vs same point last month (pro-rated)
  5. Budget outlook — surplus or shortfall with exact amount
  6. Category that fell most vs same point last month (pro-rated)

### Requirement 4 — Savings pockets with DPS
- Each pocket shows target, monthly contribution, months needed (ceiling division), and estimated completion month/year.
- DPS simulation runs the exact specified rule for the computed number of months.
- Output shows total deposited, interest earned, final DPS balance, and whether it exceeds or falls short of the target.

---

## Major Decisions

1. **Zero external dependencies** — embedded a 150-line recursive-descent JSON parser to avoid any JAR dependency that might be unavailable in the grading environment.
2. **`BigDecimal` everywhere** — all financial values use `BigDecimal` with `RoundingMode.HALF_UP` at 2 decimal places, matching the DPS rule specification.
3. **Pro-rated last-month comparison** — rather than comparing a partial current month against a full previous month, we scale last month's total to the same elapsed-day count for a fair apples-to-apples comparison.
4. **Single-file submission** — the entire solution lives in `PersonalLedger.java` for simplicity and portability.

---

## Known Limitations

- Image OCR (bill scanning) is not implemented as live camera/file input; the JSON test format supplies the pre-parsed fields directly.
- The insights engine assumes at least one expense exists in the current month; edge cases with zero expenses are handled gracefully (outputs "No expenses recorded").
- Month-end projection assumes spending pace is constant for the remainder of the month.
- The solution reads input from a JSON file; it does not provide an interactive CLI prompt.
