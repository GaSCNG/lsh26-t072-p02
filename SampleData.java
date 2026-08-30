import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * 44 pre-loaded medicines.
 * All expiry dates are relative to LocalDate.now() so the groups
 * are always correct regardless of when the program is run.
 */
public class SampleData {

    public static List<Medicine> load() {
        LocalDate t = LocalDate.now();

        return Arrays.asList(

            // ── EXPIRED (past today) ────────────────────────────────
            new Medicine("Napa 500mg",          "Beximco Pharma", "BX2024-001",  120, t.minusDays(5),   4.50),
            new Medicine("Amoxicillin 250mg",   "Square Pharma",  "SQ2024-002",   80, t.minusDays(15), 12.00),
            new Medicine("Metformin 500mg",     "ACI Limited",    "ACI2024-003", 200, t.minusDays(30),  8.50),
            new Medicine("Omeprazole 20mg",     "Incepta Pharma", "IN2024-004",   60, t.minusDays(45), 15.00),
            new Medicine("Losartan 50mg",       "Renata Ltd",     "RL2024-005",   90, t.minusDays(60), 22.00),
            new Medicine("Atorvastatin 20mg",   "Eskayef",        "ES2024-006",   45, t.minusDays(3),  35.00),
            new Medicine("Cetirizine 10mg",     "Beximco Pharma", "BX2024-007",  150, t.minusDays(20),  6.00),
            new Medicine("Pantoprazole 40mg",   "Square Pharma",  "SQ2024-008",   70, t.minusDays(10), 18.00),

            // ── EXPIRING SOON  0-30 days ────────────────────────────
            new Medicine("Azithromycin 500mg",  "ACI Limited",    "ACI2025-009",  55, t.plusDays(5),   45.00),
            new Medicine("Vitamin C 500mg",     "Incepta Pharma", "IN2025-010",  200, t.plusDays(12),   5.00),
            new Medicine("Clopidogrel 75mg",    "Renata Ltd",     "RL2025-011",   60, t.plusDays(18),  28.00),
            new Medicine("Doxycycline 100mg",   "Eskayef",        "ES2025-012",   40, t.plusDays(25),  20.00),
            new Medicine("Amlodipine 5mg",      "Beximco Pharma", "BX2025-013",  110, t.plusDays(7),   14.00),
            new Medicine("Ranitidine 150mg",    "Square Pharma",  "SQ2025-014",   85, t.plusDays(22),   9.00),
            new Medicine("Metronidazole 400mg", "ACI Limited",    "ACI2025-015", 130, t.plusDays(29),   7.50),
            new Medicine("Montelukast 10mg",    "Incepta Pharma", "IN2025-016",   50, t.plusDays(3),   32.00),
            new Medicine("Fexofenadine 120mg",  "Renata Ltd",     "RL2025-017",   75, t.plusDays(15),  24.00),

            // ── EXPIRING IN 31-90 DAYS ──────────────────────────────
            new Medicine("Lisinopril 10mg",     "Eskayef",        "ES2025-018",   95, t.plusDays(35),  18.50),
            new Medicine("Ciprofloxacin 500mg", "Beximco Pharma", "BX2025-019",   65, t.plusDays(45),  25.00),
            new Medicine("Diclofenac 50mg",     "Square Pharma",  "SQ2025-020",  180, t.plusDays(60),   6.00),
            new Medicine("Fluoxetine 20mg",     "ACI Limited",    "ACI2025-021",  40, t.plusDays(75),  38.00),
            new Medicine("Simvastatin 40mg",    "Incepta Pharma", "IN2025-022",   55, t.plusDays(50),  22.00),
            new Medicine("Warfarin 5mg",        "Renata Ltd",     "RL2025-023",   30, t.plusDays(88),  42.00),
            new Medicine("Salbutamol 4mg",      "Eskayef",        "ES2025-024",  100, t.plusDays(40),  11.00),
            new Medicine("Furosemide 40mg",     "Beximco Pharma", "BX2025-025",   70, t.plusDays(65),   9.50),
            new Medicine("Clonazepam 0.5mg",    "Square Pharma",  "SQ2025-026",   45, t.plusDays(80),  15.00),
            new Medicine("Ibuprofen 400mg",     "ACI Limited",    "ACI2025-027", 160, t.plusDays(55),   8.00),

            // ── SAFE  more than 90 days ─────────────────────────────
            new Medicine("Paracetamol 500mg",   "Incepta Pharma", "IN2026-028",  300, t.plusDays(120),  4.50),
            new Medicine("Amoxiclav 625mg",     "Renata Ltd",     "RL2026-029",   80, t.plusDays(180), 55.00),
            new Medicine("Aspirin 75mg",        "Eskayef",        "ES2026-030",  250, t.plusDays(150),  5.00),
            new Medicine("Vitamin B Complex",   "Beximco Pharma", "BX2026-031",  120, t.plusDays(200), 10.00),
            new Medicine("Calcium 500mg",       "Square Pharma",  "SQ2026-032",   90, t.plusDays(365), 12.00),
            new Medicine("Zinc 20mg",           "ACI Limited",    "ACI2026-033", 150, t.plusDays(270),  7.00),
            new Medicine("Folic Acid 5mg",      "Incepta Pharma", "IN2026-034",  200, t.plusDays(300),  3.50),
            new Medicine("Metoprolol 50mg",     "Renata Ltd",     "RL2026-035",   75, t.plusDays(240), 16.00),
            new Medicine("Esomeprazole 40mg",   "Eskayef",        "ES2026-036",   60, t.plusDays(195), 28.00),
            new Medicine("Levofloxacin 500mg",  "Beximco Pharma", "BX2026-037",   50, t.plusDays(160), 48.00),
            new Medicine("Insulin Glargine",    "Square Pharma",  "SQ2026-038",   20, t.plusDays(400),350.00),
            new Medicine("Rabeprazole 20mg",    "ACI Limited",    "ACI2026-039",  85, t.plusDays(280), 22.00),
            new Medicine("Telmisartan 40mg",    "Incepta Pharma", "IN2026-040",  110, t.plusDays(320), 18.00),
            new Medicine("Pregabalin 75mg",     "Renata Ltd",     "RL2026-041",   35, t.plusDays(210), 65.00),
            new Medicine("Levothyroxine 50mcg", "Eskayef",        "ES2026-042",  120, t.plusDays(180), 12.00),
            new Medicine("Glimepiride 2mg",     "Beximco Pharma", "BX2026-043",   90, t.plusDays(250), 14.00),
            new Medicine("Bisoprolol 5mg",      "Square Pharma",  "SQ2026-044",   60, t.plusDays(330), 20.00)
        );
    }
}
