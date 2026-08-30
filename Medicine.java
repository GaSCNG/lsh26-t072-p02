import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public class Medicine {

    public enum Status {
        EXPIRED      ("Expired",             new java.awt.Color(220,  53,  69)),
        EXPIRING_SOON("Expiring Soon 0-30d", new java.awt.Color(255, 140,   0)),
        EXPIRING_90  ("Expiring 31-90d",     new java.awt.Color(180, 140,   0)),
        SAFE         ("Safe >90d",           new java.awt.Color( 40, 167,  69)),
        RETURNED     ("Returned",            new java.awt.Color(108, 117, 125));

        private final String label;
        private final java.awt.Color color;

        Status(String label, java.awt.Color color) {
            this.label = label;
            this.color = color;
        }
        public String getLabel()         { return label; }
        public java.awt.Color getColor() { return color; }
    }

    private final String    id;
    private String          name;
    private String          company;
    private String          batch;
    private int             quantity;
    private LocalDate       expiryDate;
    private double          unitPriceBdt;
    private boolean         returned;

    // ── Constructors ───────────────────────────────────────────────

    /** Standard constructor — generates a short random ID (used by the Swing app). */
    public Medicine(String name, String company, String batch,
                    int quantity, LocalDate expiryDate, double unitPriceBdt) {
        this("M-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
             name, company, batch, quantity, expiryDate, unitPriceBdt);
    }

    /**
     * Constructor with an explicit ID (used when loading competition test cases,
     * e.g. id = "M001").
     */
    public Medicine(String id, String name, String company, String batch,
                    int quantity, LocalDate expiryDate, double unitPriceBdt) {
        this.id          = id;
        this.name        = name;
        this.company     = company;
        this.batch       = batch;
        this.quantity    = quantity;
        this.expiryDate  = expiryDate;
        this.unitPriceBdt= unitPriceBdt;
        this.returned    = false;
    }

    // ── Status calculation ─────────────────────────────────────────

    /**
     * Status computed relative to the given reference date.
     * Injecting the date lets test cases use a fixed "today" rather than the
     * real system clock.
     */
    public Status getStatus(LocalDate today) {
        if (returned) return Status.RETURNED;
        long d = getDaysLeft(today);
        if (d <  0)  return Status.EXPIRED;
        if (d <= 30) return Status.EXPIRING_SOON;
        if (d <= 90) return Status.EXPIRING_90;
        return Status.SAFE;
    }

    /** Status relative to the real system clock — used by the Swing UI. */
    public Status getStatus() { return getStatus(LocalDate.now()); }

    public long getDaysLeft(LocalDate today) {
        return ChronoUnit.DAYS.between(today, expiryDate);
    }

    public long   getDaysLeft()    { return getDaysLeft(LocalDate.now()); }
    public double getTotalValue()  { return quantity * unitPriceBdt; }

    // ── Getters ────────────────────────────────────────────────────
    public String    getId()           { return id; }
    public String    getName()         { return name; }
    public String    getCompany()      { return company; }
    public String    getBatch()        { return batch; }
    public int       getQuantity()     { return quantity; }
    public LocalDate getExpiryDate()   { return expiryDate; }
    public double    getUnitPriceBdt() { return unitPriceBdt; }
    public boolean   isReturned()      { return returned; }

    // ── Setters ────────────────────────────────────────────────────
    public void setName(String v)          { this.name = v; }
    public void setCompany(String v)       { this.company = v; }
    public void setBatch(String v)         { this.batch = v; }
    public void setQuantity(int v)         { this.quantity = v; }
    public void setExpiryDate(LocalDate v) { this.expiryDate = v; }
    public void setUnitPriceBdt(double v)  { this.unitPriceBdt = v; }
    public void setReturned(boolean v)     { this.returned = v; }

    @Override public String toString() { return name + " [" + batch + "]"; }
}
