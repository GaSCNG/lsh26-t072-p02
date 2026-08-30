import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * P02 – Pharmacy Expiry Shelf Check
 * Single-window Swing application.
 *
 * Required features:
 *  1. 44-medicine stock list (all dates relative to today)
 *  2. Dashboard: 4 groups with item counts
 *  3. Mark as returned → leaves active groups, moves to Returned list
 *  4. Total taka value shown for Expired and Expiring-Soon groups
 *
 * Bonus features:
 *  + Search / filter by name or company
 *  + 6-month value-at-risk bar chart
 *  + Quick-add form with 2-year default shelf life
 */
public class PharmacyApp extends JFrame {

    // ── Palette ──────────────────────────────────────────────────────
    private static final Color C_EXPIRED  = new Color(220,  53,  69);
    private static final Color C_SOON     = new Color(220, 120,   0);
    private static final Color C_DAYS90   = new Color(170, 130,   0);
    private static final Color C_SAFE     = new Color( 34, 139,  34);
    private static final Color C_RETURNED = new Color( 90, 100, 110);
    private static final Color C_HEADER   = new Color( 28,  32,  38);
    private static final Color C_BG       = new Color(243, 245, 248);

    private static final Font F_TITLE  = new Font("SansSerif", Font.BOLD,  13);
    private static final Font F_BODY   = new Font("SansSerif", Font.PLAIN, 12);
    private static final Font F_SMALL  = new Font("SansSerif", Font.PLAIN, 10);

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd-MMM-yyyy");

    // ── State ────────────────────────────────────────────────────────
    private final StockManager mgr;

    // Summary card labels
    private final JLabel[] cardCount = new JLabel[4];
    private final JLabel[] cardValue = new JLabel[2]; // expired + soon

    // One model per tab (expired / soon / 90d / safe / returned)
    private static final Medicine.Status[] TAB_STATUSES = {
        Medicine.Status.EXPIRED, Medicine.Status.EXPIRING_SOON,
        Medicine.Status.EXPIRING_90, Medicine.Status.SAFE, Medicine.Status.RETURNED
    };
    private MedTableModel[] tabModels;
    private JTabbedPane tabs;

    // Search widgets
    private JTextField   searchField;
    private JComboBox<String> companyBox;

    // ── Constructor ──────────────────────────────────────────────────
    public PharmacyApp(StockManager mgr) {
        this.mgr = mgr;
        buildUI();
        mgr.addListener(this::refresh);
    }

    // ─────────────────────────────────────────────────────────────────
    //  UI construction
    // ─────────────────────────────────────────────────────────────────

    private void buildUI() {
        setTitle("Pharmacy Expiry Shelf Check – Khulna");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1280, 820);
        setMinimumSize(new Dimension(960, 640));
        setLocationRelativeTo(null);
        getContentPane().setBackground(C_BG);
        setLayout(new BorderLayout());

        add(buildHeader(), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 8));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(10, 14, 6, 14));
        body.add(buildSummaryRow(),  BorderLayout.NORTH);
        body.add(buildCenterPanel(), BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);

        add(buildStatusBar(), BorderLayout.SOUTH);
    }

    // ── Header ───────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(C_HEADER);
        p.setBorder(new EmptyBorder(10, 18, 10, 18));

        JLabel title = new JLabel("  Pharmacy Expiry Shelf Check");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setForeground(Color.WHITE);

        JLabel sub = new JLabel("  Khulna Neighbourhood Pharmacy   |   Today: "
                + LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy")));
        sub.setFont(F_BODY);
        sub.setForeground(new Color(170, 175, 185));

        JPanel left = new JPanel(new GridLayout(2, 1, 0, 2));
        left.setOpaque(false);
        left.add(title);
        left.add(sub);
        p.add(left, BorderLayout.WEST);
        return p;
    }

    // ── Summary cards ────────────────────────────────────────────────
    private JPanel buildSummaryRow() {
        Medicine.Status[] sts = { Medicine.Status.EXPIRED, Medicine.Status.EXPIRING_SOON,
                                  Medicine.Status.EXPIRING_90, Medicine.Status.SAFE };
        Color[]  cols  = { C_EXPIRED, C_SOON, C_DAYS90, C_SAFE };
        String[] names = { "EXPIRED", "EXPIRING SOON  0-30 days",
                           "EXPIRING  31-90 days", "SAFE  >90 days" };

        JPanel row = new JPanel(new GridLayout(1, 4, 10, 0));
        row.setOpaque(false);
        for (int i = 0; i < 4; i++) {
            row.add(buildCard(i, sts[i], cols[i], names[i]));
        }
        return row;
    }

    private JPanel buildCard(int idx, Medicine.Status status, Color accent, String title) {
        JPanel card = new JPanel(new BorderLayout(4, 6)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(accent);
                g2.fillRoundRect(0, 0, 6, getHeight(), 4, 4);
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(215, 215, 215), 1, true),
            new EmptyBorder(10, 16, 10, 12)
        ));

        JLabel lTitle = new JLabel(title);
        lTitle.setFont(new Font("SansSerif", Font.BOLD, 10));
        lTitle.setForeground(accent);

        JLabel lCount = new JLabel("—");
        lCount.setFont(new Font("SansSerif", Font.BOLD, 30));
        lCount.setForeground(C_HEADER);
        cardCount[idx] = lCount;

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(lTitle, BorderLayout.NORTH);
        top.add(lCount, BorderLayout.CENTER);
        card.add(top, BorderLayout.CENTER);

        // Value at risk only for expired (idx 0) and expiring-soon (idx 1)
        if (idx == 0 || idx == 1) {
            JLabel vLabel = new JLabel("—");
            vLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
            vLabel.setForeground(accent);
            cardValue[idx] = vLabel;

            JLabel vHead = new JLabel("Value at Risk:");
            vHead.setFont(F_SMALL);
            vHead.setForeground(Color.GRAY);

            JPanel bot = new JPanel(new BorderLayout(0, 1));
            bot.setOpaque(false);
            bot.add(vHead,  BorderLayout.NORTH);
            bot.add(vLabel, BorderLayout.CENTER);
            card.add(bot, BorderLayout.SOUTH);
        }
        return card;
    }

    // ── Center (search + tabs) ───────────────────────────────────────
    private JPanel buildCenterPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setOpaque(false);
        p.add(buildSearchBar(), BorderLayout.NORTH);
        p.add(buildTabs(),      BorderLayout.CENTER);
        return p;
    }

    private JPanel buildSearchBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        bar.setOpaque(false);

        bar.add(lbl("Search name / company:"));
        searchField = new JTextField(22);
        searchField.setFont(F_BODY);
        bar.add(searchField);

        bar.add(lbl("Company:"));
        companyBox = new JComboBox<>();
        companyBox.setFont(F_BODY);
        bar.add(companyBox);

        JButton clear = new JButton("Clear");
        clear.setFont(F_BODY);
        clear.addActionListener(e -> {
            searchField.setText("");
            companyBox.setSelectedIndex(0);
        });
        bar.add(clear);

        // Listeners
        DocumentListener dl = new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { applySearch(); }
            public void removeUpdate(DocumentEvent e)  { applySearch(); }
            public void changedUpdate(DocumentEvent e) { applySearch(); }
        };
        searchField.getDocument().addDocumentListener(dl);
        companyBox.addActionListener(e -> applySearch());
        return bar;
    }

    private JTabbedPane buildTabs() {
        tabs = new JTabbedPane();
        tabs.setFont(F_TITLE);

        tabModels = new MedTableModel[TAB_STATUSES.length];
        Color[] colors = { C_EXPIRED, C_SOON, C_DAYS90, C_SAFE, C_RETURNED };

        for (int i = 0; i < TAB_STATUSES.length; i++) {
            Medicine.Status s = TAB_STATUSES[i];
            tabModels[i] = new MedTableModel(mgr.getByStatus(s));
            tabs.addTab(s.getLabel(), buildStockTab(tabModels[i], s, colors[i]));
        }

        tabs.addTab("Value Chart", buildChartTab());
        tabs.addTab("+ Add Medicine", buildAddTab());
        return tabs;
    }

    // ── Stock table tab ──────────────────────────────────────────────
    private JPanel buildStockTab(MedTableModel model, Medicine.Status status, Color accent) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(4, 0, 0, 0));

        JTable table = new JTable(model);
        table.setFont(F_BODY);
        table.setRowHeight(24);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setGridColor(new Color(228, 228, 228));
        table.getTableHeader().setFont(F_TITLE);
        table.getTableHeader().setBackground(new Color(240, 242, 245));

        // Row renderer: zebra stripe + accent on "Days Left" column
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                if (!sel) {
                    setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 249, 253));
                    setForeground(Color.DARK_GRAY);
                    if (col == 6) { setForeground(accent); setFont(F_TITLE); }
                    else            setFont(F_BODY);
                }
                setHorizontalAlignment(col == 3 || col == 4 || col == 5 || col == 6
                                       ? SwingConstants.RIGHT : SwingConstants.LEFT);
                setBorder(new EmptyBorder(0, 6, 0, 6));
                return this;
            }
        });

        // Column widths
        int[] widths = { 200, 130, 110, 55, 110, 120, 75, 110 };
        for (int i = 0; i < widths.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new LineBorder(new Color(210, 210, 210)));
        p.add(scroll, BorderLayout.CENTER);

        // Return button (not shown on Returned tab)
        if (status != Medicine.Status.RETURNED) {
            JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 3));
            btnRow.setOpaque(false);

            JButton retBtn = new JButton("Return Selected to Distributor");
            retBtn.setFont(F_TITLE);
            retBtn.setForeground(Color.WHITE);
            retBtn.setBackground(C_RETURNED);
            retBtn.setOpaque(true);
            retBtn.setBorderPainted(false);
            retBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            retBtn.addActionListener(e -> {
                int row = table.getSelectedRow();
                if (row < 0) {
                    JOptionPane.showMessageDialog(PharmacyApp.this,
                        "Please select a row first.", "No Selection",
                        JOptionPane.WARNING_MESSAGE);
                    return;
                }
                Medicine m = model.getAt(row);
                int ok = JOptionPane.showConfirmDialog(PharmacyApp.this,
                    "Mark \"" + m.getName() + "\"  (Batch: " + m.getBatch() + ")\nas returned to distributor?",
                    "Confirm Return", JOptionPane.YES_NO_OPTION);
                if (ok == JOptionPane.YES_OPTION) mgr.markReturned(m);
            });

            btnRow.add(retBtn);
            JLabel hint = new JLabel("  Click a row, then click the button.");
            hint.setFont(new Font("SansSerif", Font.ITALIC, 11));
            hint.setForeground(Color.GRAY);
            btnRow.add(hint);
            p.add(btnRow, BorderLayout.SOUTH);
        }
        return p;
    }

    // ── Chart tab ────────────────────────────────────────────────────
    private JPanel buildChartTab() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(10, 12, 10, 12));

        JLabel heading = new JLabel("Value at Risk — Next 6 Calendar Months");
        heading.setFont(new Font("SansSerif", Font.BOLD, 14));
        p.add(heading, BorderLayout.NORTH);

        ValueChartPanel chart = new ValueChartPanel();
        p.add(chart, BorderLayout.CENTER);
        mgr.addListener(chart::repaint);

        JLabel note = new JLabel(
            "Each bar = total purchase value (Qty x Unit Price) of medicines expiring in that calendar month.");
        note.setFont(new Font("SansSerif", Font.ITALIC, 11));
        note.setForeground(Color.GRAY);
        p.add(note, BorderLayout.SOUTH);
        return p;
    }

    // ── Add-medicine tab ─────────────────────────────────────────────
    private JPanel buildAddTab() {
        JPanel outer = new JPanel(new FlowLayout(FlowLayout.LEFT, 24, 20));
        outer.setOpaque(false);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 8, 6, 8);
        g.anchor = GridBagConstraints.WEST;

        String[] labels = {
            "Medicine Name:", "Company:", "Batch No:",
            "Quantity:", "Unit Price (Tk):", "Expiry Date (yyyy-MM-dd):"
        };
        JTextField[] fields = new JTextField[labels.length];
        // Bonus: default shelf life = 2 years
        String defaultExpiry = LocalDate.now().plusYears(2)
                                        .format(DateTimeFormatter.ISO_LOCAL_DATE);

        for (int i = 0; i < labels.length; i++) {
            g.gridx = 0; g.gridy = i; g.fill = GridBagConstraints.NONE; g.weightx = 0;
            form.add(lbl(labels[i]), g);
            g.gridx = 1; g.fill = GridBagConstraints.HORIZONTAL; g.weightx = 1;
            fields[i] = new JTextField(24);
            fields[i].setFont(F_BODY);
            if (i == 5) fields[i].setText(defaultExpiry);
            form.add(fields[i], g);
        }

        g.gridx = 1; g.gridy = labels.length;
        g.fill = GridBagConstraints.NONE; g.weightx = 0;
        JButton addBtn = new JButton("Add to Stock");
        addBtn.setFont(F_TITLE);
        addBtn.setForeground(Color.WHITE);
        addBtn.setBackground(C_SAFE);
        addBtn.setOpaque(true);
        addBtn.setBorderPainted(false);

        addBtn.addActionListener(e -> {
            try {
                String name    = fields[0].getText().trim();
                String company = fields[1].getText().trim();
                String batch   = fields[2].getText().trim();
                int    qty     = Integer.parseInt(fields[3].getText().trim());
                double price   = Double.parseDouble(fields[4].getText().trim());
                LocalDate exp  = LocalDate.parse(fields[5].getText().trim());

                if (name.isEmpty() || company.isEmpty() || batch.isEmpty())
                    throw new IllegalArgumentException("Name, Company and Batch are required.");
                if (qty <= 0 || price <= 0)
                    throw new IllegalArgumentException("Quantity and price must be positive.");

                mgr.addMedicine(new Medicine(name, company, batch, qty, exp, price));
                fields[0].setText(""); fields[1].setText(""); fields[2].setText("");
                fields[3].setText(""); fields[4].setText("");
                fields[5].setText(LocalDate.now().plusYears(2)
                                           .format(DateTimeFormatter.ISO_LOCAL_DATE));
                JOptionPane.showMessageDialog(PharmacyApp.this,
                    name + " added successfully.", "Added", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(PharmacyApp.this,
                    "Input error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        form.add(addBtn, g);

        g.gridy = labels.length + 1;
        JLabel hint = new JLabel("Default expiry is 2 years from today (common shelf life). Adjust as needed.");
        hint.setFont(new Font("SansSerif", Font.ITALIC, 11));
        hint.setForeground(Color.GRAY);
        form.add(hint, g);

        outer.add(form);
        return outer;
    }

    // ── Status bar ───────────────────────────────────────────────────
    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
        bar.setBackground(new Color(225, 227, 230));
        bar.setBorder(new EmptyBorder(2, 8, 2, 8));
        JLabel l = new JLabel("Groups are computed live from today's date: "
                              + LocalDate.now() + "   |   44 pre-loaded medicines");
        l.setFont(F_SMALL);
        bar.add(l);
        return bar;
    }

    // ─────────────────────────────────────────────────────────────────
    //  Refresh (called by StockManager after every change)
    // ─────────────────────────────────────────────────────────────────

    void refresh() {
        // Summary cards
        Medicine.Status[] cardStatuses = {
            Medicine.Status.EXPIRED, Medicine.Status.EXPIRING_SOON,
            Medicine.Status.EXPIRING_90, Medicine.Status.SAFE
        };
        for (int i = 0; i < 4; i++)
            cardCount[i].setText(String.valueOf(mgr.countByStatus(cardStatuses[i])));
        cardValue[0].setText(fmt(mgr.valueByStatus(Medicine.Status.EXPIRED)));
        cardValue[1].setText(fmt(mgr.valueByStatus(Medicine.Status.EXPIRING_SOON)));

        // Tab titles
        if (tabs != null) {
            tabs.setTitleAt(0, "Expired ("       + mgr.countByStatus(Medicine.Status.EXPIRED)       + ")");
            tabs.setTitleAt(1, "0-30 Days ("     + mgr.countByStatus(Medicine.Status.EXPIRING_SOON) + ")");
            tabs.setTitleAt(2, "31-90 Days ("    + mgr.countByStatus(Medicine.Status.EXPIRING_90)   + ")");
            tabs.setTitleAt(3, "Safe ("          + mgr.countByStatus(Medicine.Status.SAFE)          + ")");
            tabs.setTitleAt(4, "Returned ("      + mgr.countByStatus(Medicine.Status.RETURNED)      + ")");
        }

        // Company dropdown
        if (companyBox != null) {
            String sel = (String) companyBox.getSelectedItem();
            companyBox.setModel(new DefaultComboBoxModel<>(mgr.companies().toArray(new String[0])));
            if (sel != null) companyBox.setSelectedItem(sel);
        }

        applySearch();
    }

    // ─────────────────────────────────────────────────────────────────
    //  Search / filter
    // ─────────────────────────────────────────────────────────────────

    private void applySearch() {
        if (tabModels == null) return;
        String query   = searchField != null ? searchField.getText() : "";
        String company = companyBox  != null ? (String) companyBox.getSelectedItem() : "All";
        if (company == null) company = "All";
        for (int i = 0; i < TAB_STATUSES.length; i++)
            tabModels[i].setData(mgr.filtered(TAB_STATUSES[i], query, company));
    }

    // ─────────────────────────────────────────────────────────────────
    //  Inner class: table model
    // ─────────────────────────────────────────────────────────────────

    static class MedTableModel extends AbstractTableModel {
        private static final String[] COLS = {
            "Name", "Company", "Batch", "Qty",
            "Unit Price (Tk)", "Total Value (Tk)", "Days Left", "Expiry Date"
        };
        private List<Medicine> data;

        MedTableModel(List<Medicine> data) { this.data = new ArrayList<>(data); }

        void setData(List<Medicine> d) { this.data = new ArrayList<>(d); fireTableDataChanged(); }
        Medicine getAt(int row)        { return data.get(row); }

        @Override public int    getRowCount()              { return data.size(); }
        @Override public int    getColumnCount()           { return COLS.length; }
        @Override public String getColumnName(int col)     { return COLS[col]; }
        @Override public Class<?> getColumnClass(int col)  {
            return (col == 3 || col == 6) ? Long.class : String.class;
        }

        @Override
        public Object getValueAt(int row, int col) {
            Medicine m = data.get(row);
            if (col == 0) return m.getName();
            if (col == 1) return m.getCompany();
            if (col == 2) return m.getBatch();
            if (col == 3) return (long) m.getQuantity();
            if (col == 4) return String.format("Tk %,.2f", m.getUnitPriceBdt());
            if (col == 5) return String.format("Tk %,.2f", m.getTotalValue());
            if (col == 6) return m.getDaysLeft();
            if (col == 7) return m.getExpiryDate().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy"));
            return "";
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  Inner class: 6-month bar chart
    // ─────────────────────────────────────────────────────────────────

    class ValueChartPanel extends JPanel {
        ValueChartPanel() {
            setBackground(Color.WHITE);
            setBorder(new LineBorder(new Color(210, 210, 210)));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            Map<YearMonth, Double> data = mgr.chartData();
            int W = getWidth(), H = getHeight();
            int padL = 90, padR = 20, padT = 25, padB = 46;
            int cW = W - padL - padR;
            int cH = H - padT - padB;
            if (cW <= 0 || cH <= 0) return;

            double maxVal = data.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
            if (maxVal == 0) maxVal = 1;

            int n = data.size();
            int barW  = cW / (n * 2);
            int gap   = barW;

            // Grid lines + Y labels
            g2.setFont(F_SMALL);
            for (int i = 1; i <= 4; i++) {
                int y = padT + cH - (int)(cH * i / 4.0);
                g2.setColor(new Color(235, 235, 235));
                g2.drawLine(padL, y, padL + cW, y);
                g2.setColor(Color.GRAY);
                String lbl = fmt(maxVal * i / 4.0);
                g2.drawString(lbl, 2, y + 4);
            }

            // Bars
            int idx = 0;
            for (Map.Entry<YearMonth, Double> e : data.entrySet()) {
                double val = e.getValue();
                int barH = val > 0 ? Math.max(4, (int)(cH * val / maxVal)) : 2;
                int bx   = padL + idx * (barW + gap) + gap / 2;
                int by   = padT + cH - barH;

                // Orange → yellow → green gradient across months
                float hue = 0.07f + idx * 0.075f;
                g2.setColor(Color.getHSBColor(hue, 0.82f, 0.82f));
                g2.fillRoundRect(bx, by, barW, barH, 6, 6);

                // Month label
                String month = e.getKey().getMonth()
                                .getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                               + " '" + (e.getKey().getYear() % 100);
                g2.setColor(Color.DARK_GRAY);
                int mw = g2.getFontMetrics().stringWidth(month);
                g2.drawString(month, bx + barW / 2 - mw / 2, padT + cH + 16);

                // Value label inside/above bar
                if (val > 0) {
                    String vs = fmt(val);
                    g2.setFont(new Font("SansSerif", Font.PLAIN, 9));
                    int vw = g2.getFontMetrics().stringWidth(vs);
                    if (barH > 20) {
                        g2.setColor(Color.WHITE);
                        g2.drawString(vs, bx + barW / 2 - vw / 2, by + 14);
                    } else {
                        g2.setColor(Color.DARK_GRAY);
                        g2.drawString(vs, bx + barW / 2 - vw / 2, by - 4);
                    }
                    g2.setFont(F_SMALL);
                }
                idx++;
            }

            // Axes
            g2.setColor(new Color(180, 180, 180));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawLine(padL, padT,        padL, padT + cH);     // Y axis
            g2.drawLine(padL, padT + cH,   padL + cW, padT + cH); // X axis
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────────

    private static JLabel lbl(String text) {
        JLabel l = new JLabel(text);
        l.setFont(F_BODY);
        return l;
    }

    /** Format a taka amount: "Tk 12,450" */
    private static String fmt(double v) {
        return String.format("Tk %,.0f", v);
    }

    // ─────────────────────────────────────────────────────────────────
    //  main
    // ─────────────────────────────────────────────────────────────────

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}

            StockManager mgr = new StockManager();
            PharmacyApp  app = new PharmacyApp(mgr);
            app.refresh();          // populate everything before showing
            app.setVisible(true);
        });
    }
}
