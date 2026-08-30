import java.time.*;
import java.util.*;
import java.util.stream.*;

/**
 * Manages the medicine stock list.
 *
 * Two constructors:
 *   new StockManager()            — loads SampleData; uses real system date (Swing app).
 *   new StockManager(LocalDate)   — starts empty; uses the supplied fixed date (test cases).
 *
 * All grouping logic is based on the configured reference date, not the system clock,
 * so competition test cases with a fixed "today" produce deterministic results.
 */
public class StockManager {

    public interface ChangeListener { void onChanged(); }

    private final List<Medicine>       stock;
    private final List<ChangeListener> listeners = new ArrayList<>();

    /** Reference date used for all status / value calculations. Null = use real LocalDate.now(). */
    private final LocalDate today;

    // ── Constructors ───────────────────────────────────────────────

    /** Swing-app constructor: pre-loads SampleData, uses the real system clock. */
    public StockManager() {
        this.today = null;
        this.stock = new ArrayList<>(SampleData.load());
    }

    /**
     * Test-case constructor: starts with an EMPTY stock list and uses
     * the supplied fixed date for all expiry calculations.
     * Caller must add items via addMedicine().
     */
    public StockManager(LocalDate today) {
        this.today = today;
        this.stock = new ArrayList<>();
    }

    /** Returns the reference date for calculations — fixed or real system clock. */
    private LocalDate today() { return today != null ? today : LocalDate.now(); }

    // ── Listener wiring ───────────────────────────────────────────
    public void addListener(ChangeListener l) { listeners.add(l); }
    private void fire() { listeners.forEach(ChangeListener::onChanged); }

    // ── Mutations ─────────────────────────────────────────────────
    public void addMedicine(Medicine m) { stock.add(m); fire(); }

    public void markReturned(Medicine m) { m.setReturned(true); fire(); }

    // ── Queries ───────────────────────────────────────────────────

    public List<Medicine> getByStatus(Medicine.Status s) {
        LocalDate t = today();
        return stock.stream()
                    .filter(m -> m.getStatus(t) == s)
                    .collect(Collectors.toList());
    }

    public int countByStatus(Medicine.Status s) {
        LocalDate t = today();
        return (int) stock.stream().filter(m -> m.getStatus(t) == s).count();
    }

    public double valueByStatus(Medicine.Status s) {
        LocalDate t = today();
        return stock.stream()
                    .filter(m -> m.getStatus(t) == s)
                    .mapToDouble(Medicine::getTotalValue)
                    .sum();
    }

    /** Look up a medicine by its explicit ID (e.g. "M001"). */
    public Optional<Medicine> getById(String id) {
        return stock.stream().filter(m -> m.getId().equals(id)).findFirst();
    }

    /** All companies present in the stock (sorted), prefixed with "All". */
    public Set<String> companies() {
        TreeSet<String> set = new TreeSet<>();
        set.add("All");
        stock.forEach(m -> set.add(m.getCompany()));
        return set;
    }

    /**
     * Returns the subset of medicines that match the search text and company
     * filter, restricted to the given status.
     */
    public List<Medicine> filtered(Medicine.Status status, String query, String company) {
        String q = query.toLowerCase(Locale.ROOT);
        return getByStatus(status).stream()
            .filter(m -> q.isEmpty()
                      || m.getName().toLowerCase(Locale.ROOT).contains(q)
                      || m.getCompany().toLowerCase(Locale.ROOT).contains(q))
            .filter(m -> "All".equals(company) || m.getCompany().equals(company))
            .collect(Collectors.toList());
    }

    /**
     * Bonus chart: taka value of non-expired stock expiring in each of the
     * next 6 calendar months (starting from the reference date's month).
     */
    public Map<YearMonth, Double> chartData() {
        Map<YearMonth, Double> map = new LinkedHashMap<>();
        LocalDate ref = today();
        for (int i = 0; i < 6; i++) {
            YearMonth ym = YearMonth.from(ref).plusMonths(i);
            double val = stock.stream()
                .filter(m -> !m.isReturned())
                .filter(m -> !m.getExpiryDate().isBefore(ref))
                .filter(m -> YearMonth.from(m.getExpiryDate()).equals(ym))
                .mapToDouble(Medicine::getTotalValue)
                .sum();
            map.put(ym, val);
        }
        return map;
    }
}
