import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;

/**
 * Competition test-case runner for P02 — Pharmacy Expiry Shelf Check.
 *
 * Usage:
 *   javac Medicine.java SampleData.java StockManager.java CaseRunner.java
 *   java CaseRunner testcases.json
 *
 * The JSON file must follow the competition schema:
 *   {
 *     "cases": [
 *       {
 *         "case_id": "PUB-01",
 *         "today": "2026-08-16",
 *         "items": [
 *           { "id":"M001", "name":"...", "company":"...", "batch":"...",
 *             "quantity":149, "unit_price_bdt":"1.00", "expiry":"2028-08-05" }
 *         ],
 *         "mark_returned": ["M006"]
 *       },
 *       ...
 *     ]
 *   }
 *
 * No third-party libraries required — uses a built-in minimal JSON parser.
 */
public class CaseRunner {

    public static void main(String[] args) throws Exception {
        String filename = (args.length > 0) ? args[0] : "testcases.json";
        File f = new File(filename);
        if (!f.exists()) {
            System.err.println("ERROR: File not found — " + f.getAbsolutePath());
            System.err.println("Put your JSON file in the same folder and run:");
            System.err.println("  java CaseRunner testcases.json");
            System.exit(1);
        }

        String json = new String(Files.readAllBytes(f.toPath()), java.nio.charset.StandardCharsets.UTF_8);
        Object root = JsonParser.parse(json);

        @SuppressWarnings("unchecked")
        List<Object> cases = (List<Object>) ((Map<?,?>) root).get("cases");

        if (cases == null || cases.isEmpty()) {
            System.err.println("ERROR: No 'cases' array found in JSON.");
            System.exit(1);
        }

        int totalCases = cases.size();
        System.out.println("=== Pharmacy Expiry Shelf Check — Case Runner ===");
        System.out.println("    File   : " + f.getAbsolutePath());
        System.out.println("    Cases  : " + totalCases);
        System.out.println();

        Medicine.Status[] ACTIVE = {
            Medicine.Status.EXPIRED,
            Medicine.Status.EXPIRING_SOON,
            Medicine.Status.EXPIRING_90,
            Medicine.Status.SAFE
        };

        for (Object cObj : cases) {
            @SuppressWarnings("unchecked")
            Map<String, Object> c = (Map<String, Object>) cObj;

            String    caseId  = str(c.get("case_id"));
            LocalDate today   = LocalDate.parse(str(c.get("today")));

            @SuppressWarnings("unchecked")
            List<Object> itemsRaw = (List<Object>) c.get("items");

            @SuppressWarnings("unchecked")
            List<Object> returnedRaw = c.containsKey("mark_returned")
                                       ? (List<Object>) c.get("mark_returned")
                                       : Collections.emptyList();

            // ── Build stock ──────────────────────────────────────────
            StockManager mgr = new StockManager(today);

            for (Object iObj : itemsRaw) {
                @SuppressWarnings("unchecked")
                Map<String, Object> item = (Map<String, Object>) iObj;

                String    id       = str(item.get("id"));
                String    name     = str(item.get("name"));
                String    company  = str(item.get("company"));
                String    batch    = str(item.get("batch"));
                int       qty      = toInt(item.get("quantity"));
                double    price    = toDouble(item.get("unit_price_bdt"));
                LocalDate expiry   = LocalDate.parse(str(item.get("expiry")));

                mgr.addMedicine(new Medicine(id, name, company, batch, qty, expiry, price));
            }

            // ── Apply mark_returned ──────────────────────────────────
            List<String> returnedIds = new ArrayList<>();
            for (Object rid : returnedRaw) {
                String id = str(rid);
                returnedIds.add(id);
                mgr.getById(id).ifPresent(mgr::markReturned);
            }

            // ── Print results ────────────────────────────────────────
            System.out.println("┌─ " + caseId + "  (today = " + today + ") " +
                               "─────────────────────────────────────────────");

            int total = 0;
            double totalValue = 0;
            for (Medicine.Status s : ACTIVE) {
                int    cnt = mgr.countByStatus(s);
                double val = mgr.valueByStatus(s);
                total      += cnt;
                totalValue += val;
                System.out.printf("│  %-22s  %3d items   Tk %,10.2f%n",
                                  s.getLabel(), cnt, val);
            }

            int retCnt = mgr.countByStatus(Medicine.Status.RETURNED);
            System.out.printf("│  %-22s  %3d items   (IDs: %s)%n",
                              Medicine.Status.RETURNED.getLabel(),
                              retCnt,
                              returnedIds.isEmpty() ? "none" : String.join(", ", returnedIds));

            System.out.printf("│  %-22s  %3d items   Tk %,10.2f%n", "TOTAL (active)", total, totalValue);
            System.out.println("└─────────────────────────────────────────────────────────────────");
            System.out.println();
        }

        System.out.println("=== All " + totalCases + " cases processed ===");
    }

    // ── Helpers ────────────────────────────────────────────────────

    private static String str(Object o) {
        return o == null ? "" : o.toString();
    }

    private static int toInt(Object o) {
        if (o instanceof Number) return ((Number) o).intValue();
        return Integer.parseInt(str(o).trim());
    }

    private static double toDouble(Object o) {
        if (o instanceof Number) return ((Number) o).doubleValue();
        return Double.parseDouble(str(o).trim());
    }

    // ══════════════════════════════════════════════════════════════════
    // Minimal self-contained JSON parser
    // Handles: objects {}, arrays [], strings "", numbers, true/false/null
    // ══════════════════════════════════════════════════════════════════
    static final class JsonParser {

        private final String s;
        private int pos;

        private JsonParser(String s) { this.s = s; }

        static Object parse(String json) {
            return new JsonParser(json.trim()).parseValue();
        }

        private Object parseValue() {
            skipWs();
            if (pos >= s.length()) throw new RuntimeException("Unexpected end of JSON at pos " + pos);
            char c = s.charAt(pos);
            if (c == '{') return parseObject();
            if (c == '[') return parseArray();
            if (c == '"') return parseString();
            if (c == 't') { expect("true");  return Boolean.TRUE;  }
            if (c == 'f') { expect("false"); return Boolean.FALSE; }
            if (c == 'n') { expect("null");  return null;          }
            if (c == '-' || Character.isDigit(c)) return parseNumber();
            throw new RuntimeException("Unexpected character '" + c + "' at pos " + pos);
        }

        private Map<String, Object> parseObject() {
            Map<String, Object> map = new LinkedHashMap<>();
            pos++; // skip '{'
            skipWs();
            while (pos < s.length() && s.charAt(pos) != '}') {
                String key = parseString();
                skipWs();
                consume(':');
                skipWs();
                Object val = parseValue();
                map.put(key, val);
                skipWs();
                if (pos < s.length() && s.charAt(pos) == ',') { pos++; skipWs(); }
            }
            consume('}');
            return map;
        }

        private List<Object> parseArray() {
            List<Object> list = new ArrayList<>();
            pos++; // skip '['
            skipWs();
            while (pos < s.length() && s.charAt(pos) != ']') {
                list.add(parseValue());
                skipWs();
                if (pos < s.length() && s.charAt(pos) == ',') { pos++; skipWs(); }
            }
            consume(']');
            return list;
        }

        private String parseString() {
            consume('"');
            StringBuilder sb = new StringBuilder();
            while (pos < s.length()) {
                char c = s.charAt(pos++);
                if (c == '"') return sb.toString();
                if (c == '\\') {
                    if (pos >= s.length()) break;
                    char esc = s.charAt(pos++);
                    switch (esc) {
                        case '"':  sb.append('"');  break;
                        case '\\': sb.append('\\'); break;
                        case '/':  sb.append('/');  break;
                        case 'n':  sb.append('\n'); break;
                        case 'r':  sb.append('\r'); break;
                        case 't':  sb.append('\t'); break;
                        case 'b':  sb.append('\b'); break;
                        case 'f':  sb.append('\f'); break;
                        case 'u':
                            String hex = s.substring(pos, Math.min(pos + 4, s.length()));
                            sb.append((char) Integer.parseInt(hex, 16));
                            pos += 4;
                            break;
                        default:   sb.append(esc);
                    }
                } else {
                    sb.append(c);
                }
            }
            throw new RuntimeException("Unterminated string");
        }

        private Number parseNumber() {
            int start = pos;
            if (pos < s.length() && s.charAt(pos) == '-') pos++;
            while (pos < s.length() && Character.isDigit(s.charAt(pos))) pos++;
            boolean decimal = false;
            if (pos < s.length() && s.charAt(pos) == '.') {
                decimal = true;
                pos++;
                while (pos < s.length() && Character.isDigit(s.charAt(pos))) pos++;
            }
            if (pos < s.length() && (s.charAt(pos) == 'e' || s.charAt(pos) == 'E')) {
                decimal = true;
                pos++;
                if (pos < s.length() && (s.charAt(pos) == '+' || s.charAt(pos) == '-')) pos++;
                while (pos < s.length() && Character.isDigit(s.charAt(pos))) pos++;
            }
            String num = s.substring(start, pos);
            return decimal ? Double.parseDouble(num) : Long.parseLong(num);
        }

        private void expect(String keyword) {
            if (s.startsWith(keyword, pos)) { pos += keyword.length(); }
            else throw new RuntimeException("Expected '" + keyword + "' at pos " + pos);
        }

        private void consume(char expected) {
            if (pos >= s.length() || s.charAt(pos) != expected)
                throw new RuntimeException("Expected '" + expected + "' at pos " + pos
                                           + ", found '" + (pos < s.length() ? s.charAt(pos) : "EOF") + "'");
            pos++;
        }

        private void skipWs() {
            while (pos < s.length() && s.charAt(pos) <= ' ') pos++;
        }
    }
}
