import java.io.*;
import java.math.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.time.temporal.*;
import java.util.*;
import java.util.stream.*;

/**
 * P12 – Personal Ledger Manager
 *
 * Pure-Java solution (no external libraries).
 *
 * Usage:
 *   javac PersonalLedger.java
 *   java PersonalLedger <json-file> [case-id]
 *
 * If case-id is omitted every case is processed.
 */
public class PersonalLedger {

    // ════════════════════════════════════════════════════════════════════════
    //  ENTRY POINT
    // ════════════════════════════════════════════════════════════════════════
    public static void main(String[] args) throws Exception {
        String filename = args.length > 0 ? args[0] : "P12_personal_ledger_public.json";
        String targetId = args.length > 1 ? args[1] : null;

        String content = new String(Files.readAllBytes(Paths.get(filename)));
        JVal root = JParser.parse(content);

        List<JVal> cases = root.obj("cases").arr();
        boolean first = true;
        for (JVal c : cases) {
            String id = c.obj("case_id").str();
            if (targetId == null || id.equals(targetId)) {
                if (!first) { line(""); line(""); }
                first = false;
                processCase(c);
                if (targetId != null) break;
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  CORE LOGIC
    // ════════════════════════════════════════════════════════════════════════
    static void processCase(JVal c) {
        String    caseId       = c.obj("case_id").str();
        LocalDate today        = LocalDate.parse(c.obj("today").str());
        JVal      months       = c.obj("months");
        String    lastMonthStr = months.obj("last").str();   // "2026-03"
        String    thisMonthStr = months.obj("this").str();   // "2026-04"
        BigDecimal salary      = new BigDecimal(c.obj("salary_bdt").str());
        BigDecimal dpsRate     = new BigDecimal(c.obj("dps_annual_rate_percent").str());

        // ── expenses ─────────────────────────────────────────────────────────
        List<Expense> lastMonthExp = new ArrayList<>();
        List<Expense> thisMonthExp = new ArrayList<>();

        for (JVal ej : c.obj("expenses").arr()) {
            LocalDate d  = LocalDate.parse(ej.obj("date").str());
            Expense   e  = new Expense(
                    ej.obj("id").str(), d,
                    ej.obj("category").str(),
                    ej.obj("shop").str(),
                    new BigDecimal(ej.obj("amount_bdt").str()));
            String ym = d.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            if (ym.equals(lastMonthStr))                          lastMonthExp.add(e);
            else if (ym.equals(thisMonthStr) && !d.isAfter(today)) thisMonthExp.add(e);
        }

        // ── pockets ──────────────────────────────────────────────────────────
        List<Pocket> pockets = new ArrayList<>();
        BigDecimal   totalPocketContrib = BigDecimal.ZERO;
        for (JVal pj : c.obj("pockets").arr()) {
            Pocket p = new Pocket(
                    pj.obj("id").str(), pj.obj("name").str(), pj.obj("item").str(),
                    new BigDecimal(pj.obj("target_bdt").str()),
                    new BigDecimal(pj.obj("monthly_contribution_bdt").str()));
            pockets.add(p);
            totalPocketContrib = totalPocketContrib.add(p.monthlyContrib);
        }

        // ── aggregates ───────────────────────────────────────────────────────
        BigDecimal lastTotal = sum(lastMonthExp);
        BigDecimal thisTotal = sum(thisMonthExp);

        Map<String, BigDecimal> lastCats = byCategory(lastMonthExp);
        Map<String, BigDecimal> thisCats = byCategory(thisMonthExp);

        List<Expense> topThis = new ArrayList<>(thisMonthExp);
        topThis.sort((a, b) -> b.amount.compareTo(a.amount));

        // ── timing ───────────────────────────────────────────────────────────
        int daysElapsed   = today.getDayOfMonth();
        int totalDays     = today.lengthOfMonth();
        int daysLeft      = totalDays - daysElapsed;
        int lastMonthDays = YearMonth.parse(lastMonthStr).lengthOfMonth();

        BigDecimal dailyRate = daysElapsed > 0
                ? thisTotal.divide(new BigDecimal(daysElapsed), 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal projTotal = dailyRate
                .multiply(new BigDecimal(totalDays))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal projBalance = salary.subtract(projTotal).subtract(totalPocketContrib)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal lastAtSamePace = (lastMonthDays > 0)
                ? lastTotal.multiply(new BigDecimal(daysElapsed))
                           .divide(new BigDecimal(lastMonthDays), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        String thisMonthName = monthLabel(thisMonthStr);
        String lastMonthName = monthLabel(lastMonthStr);

        // ════════════════════════════════════════════════════════════════════
        //  HEADER
        // ════════════════════════════════════════════════════════════════════
        String sep  = "═".repeat(64);
        String thin = "─".repeat(64);
        line(sep);
        line(String.format("  PERSONAL LEDGER  |  Case: %-8s  |  Date: %s", caseId, today));
        line(String.format("  Monthly Salary: %s", fmtBDT(salary)));
        line(sep);

        // ════════════════════════════════════════════════════════════════════
        //  SECTION 1 – MONTHLY DASHBOARD
        // ════════════════════════════════════════════════════════════════════
        nl();
        sectionHead("1. MONTHLY DASHBOARD");

        line(String.format("  Last month  (%s)    total : %s",
                padR(lastMonthName, 16), fmtBDT(lastTotal)));
        line(String.format("  This month  (%s)  so far : %s  (%d of %d days)",
                padR(thisMonthName, 16), fmtBDT(thisTotal), daysElapsed, totalDays));

        if (lastAtSamePace.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal pctChg = thisTotal.subtract(lastAtSamePace)
                    .multiply(new BigDecimal("100"))
                    .divide(lastAtSamePace, 1, RoundingMode.HALF_UP);
            line(String.format(
                    "  vs last month at same point: %s%s%%  (last month had %s by day %d)",
                    pctChg.signum() >= 0 ? "+" : "", pctChg.toPlainString(),
                    fmtBDT(lastAtSamePace), daysElapsed));
        }

        nl();
        line("  CATEGORY BREAKDOWN — THIS MONTH (" + thisMonthName + ")");
        line("  " + thin);
        printCategoryTable(thisCats, thisTotal);

        nl();
        line("  CATEGORY BREAKDOWN — LAST MONTH (" + lastMonthName + ")");
        line("  " + thin);
        printCategoryTable(lastCats, lastTotal);

        nl();
        int topN = Math.min(5, topThis.size());
        line("  TOP " + topN + " EXPENSES — THIS MONTH");
        line("  " + thin);
        for (int i = 0; i < topN; i++) {
            Expense e = topThis.get(i);
            line(String.format("  %d. %-22s %s  %-14s  %s",
                    i + 1, e.shop, fmtBDT(e.amount),
                    "(" + e.category + ")", e.date));
        }

        // ════════════════════════════════════════════════════════════════════
        //  SECTION 2 – FORECAST
        // ════════════════════════════════════════════════════════════════════
        nl();
        sectionHead("2. FORECAST — " + thisMonthName.toUpperCase());

        line(String.format("  Days elapsed this month   : %d  (today: %s)", daysElapsed, today));
        line(String.format("  Days remaining            : %d", daysLeft));
        line(String.format("  Daily spend rate          : %s / day",
                fmtBDT(dailyRate.setScale(2, RoundingMode.HALF_UP))));
        line(String.format("  Projected month-end spend : %s", fmtBDT(projTotal)));
        line(String.format("  Monthly pocket savings    : %s", fmtBDT(totalPocketContrib)));
        line(String.format("  Expected salary remaining : %s  (%s)",
                fmtBDT(projBalance.abs()),
                projBalance.signum() >= 0 ? "SURPLUS" : "SHORTFALL"));

        nl();
        line("  PROJECTED CATEGORY SPEND BY MONTH-END");
        line("  " + thin);
        for (Map.Entry<String, BigDecimal> entry : thisCats.entrySet()) {
            BigDecimal proj = daysElapsed > 0
                    ? entry.getValue()
                           .divide(new BigDecimal(daysElapsed), 4, RoundingMode.HALF_UP)
                           .multiply(new BigDecimal(totalDays))
                           .setScale(2, RoundingMode.HALF_UP)
                    : entry.getValue();
            line(String.format("  %-14s  so far %s  →  projected %s",
                    entry.getKey() + ":", fmtBDT(entry.getValue()), fmtBDT(proj)));
        }

        // ════════════════════════════════════════════════════════════════════
        //  SECTION 3 – INSIGHTS
        // ════════════════════════════════════════════════════════════════════
        nl();
        sectionHead("3. INSIGHTS");

        generateInsights(thisCats, lastCats, thisTotal, lastTotal, topThis,
                salary, projTotal, totalPocketContrib, projBalance,
                daysElapsed, totalDays, lastMonthDays,
                thisMonthName, lastMonthName);

        // ════════════════════════════════════════════════════════════════════
        //  SECTION 4 – SAVINGS POCKETS
        // ════════════════════════════════════════════════════════════════════
        nl();
        sectionHead("4. SAVINGS POCKETS  (DPS rate: " + dpsRate.toPlainString() + "% p.a.)");

        YearMonth startYM = YearMonth.parse(thisMonthStr);

        for (Pocket p : pockets) {
            nl();
            line(String.format("  [%s] %s  —  %s", p.id, p.name, p.item));
            line("  " + "─".repeat(58));
            line(String.format("  Target amount    : %s", fmtBDT(p.target)));
            line(String.format("  Monthly saving   : %s / month", fmtBDT(p.monthlyContrib)));

            // ceiling division: months needed = ceil(target / contribution)
            BigDecimal[] dr = p.target.divideAndRemainder(p.monthlyContrib);
            int monthsNeeded = dr[0].intValue() + (dr[1].compareTo(BigDecimal.ZERO) > 0 ? 1 : 0);

            YearMonth completionYM = startYM.plusMonths(monthsNeeded);
            String compLabel = completionYM.getMonth()
                    .getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                    + " " + completionYM.getYear();

            line(String.format("  Months needed    : %d", monthsNeeded));
            line(String.format("  Est. completion  : %s", compLabel));

            // DPS simulation
            BigDecimal dpsBalance     = computeDPS(p.monthlyContrib, dpsRate, monthsNeeded);
            BigDecimal totalDeposited = p.monthlyContrib.multiply(new BigDecimal(monthsNeeded));
            BigDecimal interestEarned = dpsBalance.subtract(totalDeposited);
            BigDecimal vsTarget       = dpsBalance.subtract(p.target);

            nl();
            line(String.format("  DPS @ %.2f%% p.a. for %d months:",
                    dpsRate.doubleValue(), monthsNeeded));
            line(String.format("    Total deposited : %s", fmtBDT(totalDeposited)));
            line(String.format("    Interest earned : %s", fmtBDT(interestEarned)));
            line(String.format("    DPS balance     : %s", fmtBDT(dpsBalance)));
            line(String.format("    vs target       : %s %s",
                    fmtBDT(vsTarget.abs()),
                    vsTarget.signum() >= 0 ? "OVER TARGET" : "still short"));
        }

        nl();
        line("═".repeat(64));
    }

    // ════════════════════════════════════════════════════════════════════════
    //  DPS SIMULATION
    // ════════════════════════════════════════════════════════════════════════
    /**
     * Each month: balance += deposit; then add interest = round_half_up(balance * rate / 1200, 2).
     * Returns final balance after `months` cycles.
     */
    static BigDecimal computeDPS(BigDecimal deposit, BigDecimal annualRate, int months) {
        BigDecimal balance = BigDecimal.ZERO;
        for (int m = 0; m < months; m++) {
            balance = balance.add(deposit);
            BigDecimal interest = balance.multiply(annualRate)
                    .divide(new BigDecimal("1200"), 2, RoundingMode.HALF_UP);
            balance = balance.add(interest);
        }
        return balance.setScale(2, RoundingMode.HALF_UP);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  INSIGHTS
    // ════════════════════════════════════════════════════════════════════════
    static void generateInsights(
            Map<String, BigDecimal> thisCats, Map<String, BigDecimal> lastCats,
            BigDecimal thisTotal,  BigDecimal lastTotal,
            List<Expense> topThis,
            BigDecimal salary, BigDecimal projTotal, BigDecimal pocketContribs,
            BigDecimal projBalance,
            int daysElapsed, int totalDays, int lastMonthDays,
            String thisMonthName, String lastMonthName) {

        int n = 1;

        // 1. Top category this month
        Optional<Map.Entry<String,BigDecimal>> topCatOpt =
                thisCats.entrySet().stream().max(Map.Entry.comparingByValue());
        if (topCatOpt.isPresent()) {
            Map.Entry<String,BigDecimal> top = topCatOpt.get();
            BigDecimal p = pct(top.getValue(), thisTotal);
            line(String.format(
                    "  %d. %s is your top category this month at %s (%.1f%% of spending so far).",
                    n++, top.getKey(), fmtBDT(top.getValue()), p.doubleValue()));
        }

        // 2. Largest single expense
        if (!topThis.isEmpty()) {
            Expense e = topThis.get(0);
            line(String.format(
                    "  %d. Largest single expense: %s → %s (%s) on %s.",
                    n++, e.shop, fmtBDT(e.amount), e.category, e.date));
        }

        // 3. Daily spending pace vs last month
        if (lastTotal.compareTo(BigDecimal.ZERO) > 0 && lastMonthDays > 0 && daysElapsed > 0) {
            BigDecimal lastDaily = lastTotal.divide(new BigDecimal(lastMonthDays), 4, RoundingMode.HALF_UP);
            BigDecimal thisDaily = thisTotal.divide(new BigDecimal(daysElapsed),   4, RoundingMode.HALF_UP);
            BigDecimal diff = thisDaily.subtract(lastDaily).setScale(2, RoundingMode.HALF_UP);
            String dir = diff.signum() >= 0 ? "faster" : "slower";
            line(String.format(
                    "  %d. Daily spending pace (%s/day) is running %s than %s's average (%s/day) by %s/day.",
                    n++,
                    fmtBDT(thisDaily.setScale(2, RoundingMode.HALF_UP)), dir, lastMonthName,
                    fmtBDT(lastDaily.setScale(2, RoundingMode.HALF_UP)), fmtBDT(diff.abs())));
        }

        // 4. Category that rose most vs last month at same pace (pro-rated)
        String   riseCategory = null;
        BigDecimal biggestRise = BigDecimal.ZERO;
        Set<String> allCats = new LinkedHashSet<>(thisCats.keySet());
        allCats.addAll(lastCats.keySet());
        for (String cat : allCats) {
            BigDecimal thisAmt = thisCats.getOrDefault(cat, BigDecimal.ZERO);
            BigDecimal lastPro = lastMonthDays > 0
                    ? lastCats.getOrDefault(cat, BigDecimal.ZERO)
                              .multiply(new BigDecimal(daysElapsed))
                              .divide(new BigDecimal(lastMonthDays), 2, RoundingMode.HALF_UP)
                    : lastCats.getOrDefault(cat, BigDecimal.ZERO);
            BigDecimal rise = thisAmt.subtract(lastPro);
            if (rise.compareTo(biggestRise) > 0) { biggestRise = rise; riseCategory = cat; }
        }
        if (riseCategory != null && biggestRise.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal thisAmt = thisCats.getOrDefault(riseCategory, BigDecimal.ZERO);
            BigDecimal lastPro = lastMonthDays > 0
                    ? lastCats.getOrDefault(riseCategory, BigDecimal.ZERO)
                              .multiply(new BigDecimal(daysElapsed))
                              .divide(new BigDecimal(lastMonthDays), 2, RoundingMode.HALF_UP)
                    : lastCats.getOrDefault(riseCategory, BigDecimal.ZERO);
            line(String.format(
                    "  %d. %s spending is up %s vs same point last month (%s now vs %s pro-rated last month).",
                    n++, riseCategory, fmtBDT(biggestRise), fmtBDT(thisAmt), fmtBDT(lastPro)));
        }

        // 5. Budget outlook
        if (projBalance.signum() >= 0) {
            line(String.format(
                    "  %d. At current pace you project a %s SURPLUS after expenses (%s)" +
                    " and pocket contributions (%s).",
                    n++, fmtBDT(projBalance), fmtBDT(projTotal), fmtBDT(pocketContribs)));
        } else {
            line(String.format(
                    "  %d. WARNING: Projected %s SHORTFALL — projected expenses (%s)" +
                    " + pocket savings (%s) exceed salary (%s). Cut discretionary spend.",
                    n++, fmtBDT(projBalance.abs()),
                    fmtBDT(projTotal), fmtBDT(pocketContribs), fmtBDT(salary)));
        }

        // 6. Category that fell most (savings win)
        String   dropCategory = null;
        BigDecimal biggestDrop = BigDecimal.ZERO;
        for (String cat : allCats) {
            BigDecimal thisAmt = thisCats.getOrDefault(cat, BigDecimal.ZERO);
            BigDecimal lastPro = lastMonthDays > 0
                    ? lastCats.getOrDefault(cat, BigDecimal.ZERO)
                              .multiply(new BigDecimal(daysElapsed))
                              .divide(new BigDecimal(lastMonthDays), 2, RoundingMode.HALF_UP)
                    : lastCats.getOrDefault(cat, BigDecimal.ZERO);
            BigDecimal drop = lastPro.subtract(thisAmt);
            if (drop.compareTo(biggestDrop) > 0) { biggestDrop = drop; dropCategory = cat; }
        }
        if (dropCategory != null && biggestDrop.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal thisAmt = thisCats.getOrDefault(dropCategory, BigDecimal.ZERO);
            BigDecimal lastPro = lastMonthDays > 0
                    ? lastCats.getOrDefault(dropCategory, BigDecimal.ZERO)
                              .multiply(new BigDecimal(daysElapsed))
                              .divide(new BigDecimal(lastMonthDays), 2, RoundingMode.HALF_UP)
                    : lastCats.getOrDefault(dropCategory, BigDecimal.ZERO);
            line(String.format(
                    "  %d. Good news: %s is down %s vs same point last month (%s now vs %s pro-rated).",
                    n, dropCategory, fmtBDT(biggestDrop), fmtBDT(thisAmt), fmtBDT(lastPro)));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  UTILITIES
    // ════════════════════════════════════════════════════════════════════════
    static BigDecimal sum(List<Expense> list) {
        return list.stream().map(e -> e.amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    static Map<String, BigDecimal> byCategory(List<Expense> list) {
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        for (Expense e : list) map.merge(e.category, e.amount, BigDecimal::add);
        return map.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey, Map.Entry::getValue,
                        (x, y) -> x, LinkedHashMap::new));
    }

    static BigDecimal pct(BigDecimal part, BigDecimal total) {
        if (total.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return part.multiply(new BigDecimal("100"))
                   .divide(total, 1, RoundingMode.HALF_UP);
    }

    static void printCategoryTable(Map<String, BigDecimal> cats, BigDecimal total) {
        if (cats.isEmpty()) { line("  (no data)"); return; }
        for (Map.Entry<String, BigDecimal> entry : cats.entrySet()) {
            BigDecimal p = pct(entry.getValue(), total);
            int bars = Math.max(1, p.intValue() / 4);
            line(String.format("  %-14s  %s  (%5.1f%%)  %s",
                    entry.getKey() + ":",
                    fmtBDT(entry.getValue()),
                    p.doubleValue(),
                    "█".repeat(bars)));
        }
        line(String.format("  %-14s  %s", "TOTAL:", fmtBDT(total)));
    }

    /** BDT with thousands separator, 2 decimal places, right-padded to 12 chars. */
    static String fmtBDT(BigDecimal v) {
        return String.format("BDT %,12.2f", v.doubleValue());
    }

    static String monthLabel(String ym) {
        YearMonth m = YearMonth.parse(ym);
        return m.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + m.getYear();
    }

    static String padR(String s, int w) {
        return s.length() >= w ? s : s + " ".repeat(w - s.length());
    }

    static void sectionHead(String title) {
        String bar = "─".repeat(64);
        line("┌" + bar + "┐");
        line("│  " + padR(title, 62) + "│");
        line("└" + bar + "┘");
    }

    static void line(String s) { System.out.println(s); }
    static void nl()           { System.out.println();  }

    // ════════════════════════════════════════════════════════════════════════
    //  DATA CLASSES
    // ════════════════════════════════════════════════════════════════════════
    static class Expense {
        final String id, category, shop;
        final LocalDate date;
        final BigDecimal amount;
        Expense(String id, LocalDate date, String cat, String shop, BigDecimal amount) {
            this.id = id; this.date = date; this.category = cat;
            this.shop = shop; this.amount = amount;
        }
    }

    static class Pocket {
        final String id, name, item;
        final BigDecimal target, monthlyContrib;
        Pocket(String id, String name, String item, BigDecimal target, BigDecimal mc) {
            this.id = id; this.name = name; this.item = item;
            this.target = target; this.monthlyContrib = mc;
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  LIGHTWEIGHT JSON PARSER
    //  Supports: objects {}, arrays [], strings "", numbers, booleans, null.
    //  No external dependencies.
    // ════════════════════════════════════════════════════════════════════════
    static class JVal {
        enum Type { OBJECT, ARRAY, STRING, NUMBER, BOOL, NULL }
        final Type type;
        // OBJECT
        final Map<String, JVal> fields;
        // ARRAY
        final List<JVal> items;
        // STRING / NUMBER / BOOL / NULL
        final String raw;

        JVal(Map<String, JVal> fields)   { type = Type.OBJECT; this.fields = fields; items = null; raw = null; }
        JVal(List<JVal> items)           { type = Type.ARRAY;  this.items  = items;  fields = null; raw = null; }
        JVal(Type t, String raw)         { type = t; this.raw = raw; fields = null; items = null; }

        /** Get object member (throws if key absent). */
        JVal obj(String key) {
            if (type == Type.OBJECT && fields.containsKey(key)) return fields.get(key);
            // For arrays, we re-use the key as an index
            if (type == Type.ARRAY) {
                int idx = Integer.parseInt(key);
                return items.get(idx);
            }
            throw new RuntimeException("Key not found: " + key + " in " + this);
        }

        /** Get list of children (works on array). */
        List<JVal> arr() {
            if (type == Type.ARRAY) return items;
            throw new RuntimeException("Not an array: " + type);
        }

        /** Get string value. */
        String str() {
            if (type == Type.STRING) return raw;
            if (type == Type.NUMBER || type == Type.BOOL || type == Type.NULL) return raw;
            throw new RuntimeException("Not a scalar: " + type);
        }

        @Override public String toString() {
            switch (type) {
                case OBJECT: return fields.toString();
                case ARRAY:  return items.toString();
                default:     return raw;
            }
        }
    }

    static class JParser {
        final String src;
        int pos;

        JParser(String src) { this.src = src; this.pos = 0; }

        static JVal parse(String src) { return new JParser(src).parseValue(); }

        JVal parseValue() {
            skipWS();
            if (pos >= src.length()) throw new RuntimeException("Unexpected end at " + pos);
            char ch = src.charAt(pos);
            if (ch == '{') return parseObject();
            if (ch == '[') return parseArray();
            if (ch == '"') return parseString();
            if (ch == 't' || ch == 'f') return parseBool();
            if (ch == 'n') return parseNull();
            return parseNumber();
        }

        JVal parseObject() {
            expect('{');
            Map<String, JVal> map = new LinkedHashMap<>();
            skipWS();
            if (peek() == '}') { pos++; return new JVal(map); }
            while (true) {
                skipWS();
                String key = parseString().raw;
                skipWS();
                expect(':');
                JVal val = parseValue();
                map.put(key, val);
                skipWS();
                char c = src.charAt(pos);
                if (c == '}') { pos++; break; }
                expect(',');
            }
            return new JVal(map);
        }

        JVal parseArray() {
            expect('[');
            List<JVal> list = new ArrayList<>();
            skipWS();
            if (peek() == ']') { pos++; return new JVal(list); }
            while (true) {
                list.add(parseValue());
                skipWS();
                char c = src.charAt(pos);
                if (c == ']') { pos++; break; }
                expect(',');
            }
            return new JVal(list);
        }

        JVal parseString() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (pos < src.length()) {
                char c = src.charAt(pos++);
                if (c == '"') break;
                if (c == '\\') {
                    char esc = src.charAt(pos++);
                    switch (esc) {
                        case '"': sb.append('"');  break;
                        case '\\':sb.append('\\'); break;
                        case '/': sb.append('/');  break;
                        case 'n': sb.append('\n'); break;
                        case 'r': sb.append('\r'); break;
                        case 't': sb.append('\t'); break;
                        case 'u':
                            String hex = src.substring(pos, pos + 4); pos += 4;
                            sb.append((char) Integer.parseInt(hex, 16));
                            break;
                        default: sb.append(esc);
                    }
                } else {
                    sb.append(c);
                }
            }
            return new JVal(JVal.Type.STRING, sb.toString());
        }

        JVal parseNumber() {
            int start = pos;
            if (peek() == '-') pos++;
            while (pos < src.length() && (Character.isDigit(src.charAt(pos)) || src.charAt(pos) == '.' || src.charAt(pos) == 'e' || src.charAt(pos) == 'E' || src.charAt(pos) == '+' || src.charAt(pos) == '-' && pos > start + 1))
                pos++;
            return new JVal(JVal.Type.NUMBER, src.substring(start, pos));
        }

        JVal parseBool() {
            if (src.startsWith("true",  pos)) { pos += 4; return new JVal(JVal.Type.BOOL, "true"); }
            if (src.startsWith("false", pos)) { pos += 5; return new JVal(JVal.Type.BOOL, "false"); }
            throw new RuntimeException("Not a bool at " + pos);
        }

        JVal parseNull() {
            if (src.startsWith("null", pos)) { pos += 4; return new JVal(JVal.Type.NULL, "null"); }
            throw new RuntimeException("Not null at " + pos);
        }

        void skipWS() { while (pos < src.length() && src.charAt(pos) <= ' ') pos++; }
        char peek()   { skipWS(); return pos < src.length() ? src.charAt(pos) : 0; }
        void expect(char ch) {
            skipWS();
            if (pos >= src.length() || src.charAt(pos) != ch)
                throw new RuntimeException("Expected '" + ch + "' at " + pos + " but got '" + (pos < src.length() ? src.charAt(pos) : "EOF") + "'");
            pos++;
        }
    }
}
