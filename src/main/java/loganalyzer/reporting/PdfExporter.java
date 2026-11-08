package loganalyzer.reporting;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import loganalyzer.detection.Finding;
import loganalyzer.service.AnalysisResult;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Writes the styled PDF report. Severities are HIGH, MEDIUM and LOW only. */
public final class PdfExporter {

    private PdfExporter() { }

    public static void write(Path destination, List<AnalysisResult> results) throws IOException {
        Path target = destination.toAbsolutePath();
        Path temporary = Files.createTempFile(target.getParent(), "log-report-", ".tmp");
        try {
            try (OutputStream out = Files.newOutputStream(temporary)) {
                render(results, out);
            }
            try {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IOException("Could not build PDF report: " + ex.getMessage(), ex);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private enum Sev {
        HIGH("High", new Color(0xC62828), 8),
        MEDIUM("Medium", new Color(0xEF6C00), 3),
        LOW("Low", new Color(0x1E88E5), 1);

        final String label;
        final Color color;
        final double weight;

        Sev(String label, Color color, double weight) {
            this.label = label;
            this.color = color;
            this.weight = weight;
        }
    }

    private static final Color INK = new Color(0x1F2933);
    private static final Color NAVY = new Color(0x0F2540);
    private static final Color ACCENT = new Color(0x0B7BC1);
    private static final Color GREY = new Color(0x6B7785);
    private static final Color TRACK = new Color(0xE6EBF0);
    private static final Color CARD = new Color(0xF5F7FA);
    private static final Color RULE = new Color(0xD5DCE3);

    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneOffset.UTC);

    // ---------- data helpers ----------

    private static Sev sevOf(String s) {
        if (s != null) {
            for (Sev v : Sev.values()) {
                if (v.name().equalsIgnoreCase(s.trim())) {
                    return v;
                }
            }
        }
        return Sev.LOW;
    }

    private static String safe(String s) {
        return (s == null || s.isEmpty()) ? "-" : s;
    }

    private static String cut(String s, int n) {
        return s.length() > n ? s.substring(0, n - 3) + "..." : s;
    }

    private static String fmt(Instant i) {
        return i == null ? "-" : STAMP.format(i) + " UTC";
    }

    private static String fmtShort(Instant i) {
        return i == null ? "-" : STAMP.format(i);
    }

    private static Map<String, Long> top(List<String> items, int n) {
        return items.stream()
                .collect(Collectors.groupingBy(x -> x, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(n)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
    }

    private static int riskScore(Map<Sev, Long> counts) {
        double sum = 0;
        for (Sev s : Sev.values()) {
            sum += s.weight * counts.getOrDefault(s, 0L);
        }
        return (int) Math.round(100 * (1 - Math.exp(-sum / 16.0)));
    }

    // ---------- java2d charts ----------

    private static final int S = 3; // supersampling so charts stay sharp in the pdf

    private static BufferedImage canvas(int w, int h) {
        return new BufferedImage(w * S, h * S, BufferedImage.TYPE_INT_RGB);
    }

    private static Graphics2D g2(BufferedImage bi) {
        Graphics2D g = bi.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, bi.getWidth(), bi.getHeight());
        g.scale(S, S);
        return g;
    }

    private static Font sf(boolean bold, int size) {
        return new Font("SansSerif", bold ? Font.BOLD : Font.PLAIN, size);
    }

    private static void text(Graphics2D g, String s, double x, double y, Font f, Color c) {
        g.setFont(f);
        g.setColor(c);
        g.drawString(s, (float) x, (float) y);
    }

    private static void textC(Graphics2D g, String s, double cx, double y, Font f, Color c) {
        g.setFont(f);
        double w = g.getFontMetrics().getStringBounds(s, g).getWidth();
        text(g, s, cx - w / 2, y, f, c);
    }

    private static void textR(Graphics2D g, String s, double rx, double y, Font f, Color c) {
        g.setFont(f);
        double w = g.getFontMetrics().getStringBounds(s, g).getWidth();
        text(g, s, rx - w, y, f, c);
    }

    private static Image toImage(BufferedImage bi) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(bi, "png", bos);
        return Image.getInstance(bos.toByteArray());
    }

    private static BufferedImage donut(Map<Sev, Long> c, long total) {
        int W = 520, H = 300;
        BufferedImage bi = canvas(W, H);
        Graphics2D g = g2(bi);
        text(g, "Severity distribution", 0, 20, sf(true, 16), INK);

        double cx = 125, cy = 168, th = 28, r = 100 - th / 2;
        g.setStroke(new BasicStroke((float) th, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        g.setColor(TRACK);
        g.draw(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r));

        double start = 90;
        for (Sev s : Sev.values()) {
            long n = c.getOrDefault(s, 0L);
            if (n == 0 || total == 0) {
                continue;
            }
            double ext = -360.0 * n / total;
            double e = ext < -4 ? ext + 1.6 : ext; // small gap between slices
            g.setColor(s.color);
            g.draw(new Arc2D.Double(cx - r, cy - r, 2 * r, 2 * r, start, e, Arc2D.OPEN));
            start += ext;
        }
        textC(g, String.valueOf(total), cx, cy + 12, sf(true, 36), INK);
        textC(g, "findings", cx, cy + 32, sf(false, 13), GREY);

        int y = 118;
        for (Sev s : Sev.values()) {
            long n = c.getOrDefault(s, 0L);
            g.setColor(s.color);
            g.fill(new RoundRectangle2D.Double(270, y - 12, 14, 14, 4, 4));
            text(g, s.label, 294, y, sf(false, 15), INK);
            textR(g, String.valueOf(n), 440, y, sf(true, 15), INK);
            textR(g, (total == 0 ? 0 : Math.round(100.0 * n / total)) + "%", 505, y, sf(false, 14), GREY);
            y += 38;
        }
        g.dispose();
        return bi;
    }

    private static BufferedImage gauge(int score) {
        int W = 300, H = 300;
        BufferedImage bi = canvas(W, H);
        Graphics2D g = g2(bi);
        text(g, "Overall risk", 0, 20, sf(true, 16), INK);

        double cx = 150, cy = 190, th = 24, r = 105 - th / 2;
        g.setStroke(new BasicStroke((float) th, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(TRACK);
        g.draw(new Arc2D.Double(cx - r, cy - r, 2 * r, 2 * r, 180, -180, Arc2D.OPEN));

        Color bc;
        String band;
        if (score < 34) {
            bc = new Color(0x2E7D32);
            band = "Low risk";
        } else if (score < 67) {
            bc = Sev.MEDIUM.color;
            band = "Medium risk";
        } else {
            bc = Sev.HIGH.color;
            band = "High risk";
        }

        g.setColor(bc);
        g.draw(new Arc2D.Double(cx - r, cy - r, 2 * r, 2 * r, 180, -180.0 * score / 100, Arc2D.OPEN));

        textC(g, String.valueOf(score), cx, cy - 8, sf(true, 46), INK);
        textC(g, "out of 100", cx, cy + 24, sf(false, 13), GREY);
        textC(g, band, cx, cy + 58, sf(true, 18), bc);
        textC(g, "0", cx - r, cy + 34, sf(false, 12), GREY);
        textC(g, "100", cx + r, cy + 34, sf(false, 12), GREY);
        g.dispose();
        return bi;
    }

    private static BufferedImage hbars(String title, Map<String, Long> data, Color color) {
        int W = 420, H = 278;
        BufferedImage bi = canvas(W, H);
        Graphics2D g = g2(bi);
        text(g, title, 0, 20, sf(true, 16), INK);

        long max = data.values().stream().mapToLong(Long::longValue).max().orElse(1);
        double barX = 150, maxW = W - barX - 36;
        int y = 68;
        for (Map.Entry<String, Long> e : data.entrySet()) {
            text(g, cut(e.getKey(), 20), 0, y + 5, sf(false, 13), INK);
            g.setColor(TRACK);
            g.fill(new RoundRectangle2D.Double(barX, y - 8, maxW, 16, 16, 16));
            double w = Math.max(10, maxW * e.getValue() / max);
            g.setColor(color);
            g.fill(new RoundRectangle2D.Double(barX, y - 8, w, 16, 16, 16));
            text(g, String.valueOf(e.getValue()), barX + maxW + 8, y + 5, sf(true, 13), INK);
            y += 36;
        }
        g.dispose();
        return bi;
    }

    private static BufferedImage trend(List<Finding> fs) {
        int W = 900, H = 280;
        BufferedImage bi = canvas(W, H);
        Graphics2D g = g2(bi);
        text(g, "Findings over time", 0, 22, sf(true, 18), INK);

        TreeMap<LocalDate, EnumMap<Sev, Long>> days = new TreeMap<>();
        for (Finding f : fs) {
            if (f.detectedAt() == null) {
                continue;
            }
            LocalDate d = LocalDate.ofInstant(f.detectedAt(), ZoneOffset.UTC);
            days.computeIfAbsent(d, k -> new EnumMap<>(Sev.class)).merge(sevOf(f.severity()), 1L, Long::sum);
        }

        long maxDay = 1;
        for (EnumMap<Sev, Long> m : days.values()) {
            long sum = 0;
            for (long v : m.values()) {
                sum += v;
            }
            maxDay = Math.max(maxDay, sum);
        }
        int yMax = (int) Math.max(4, ((maxDay + 3) / 4) * 4);

        double left = 44, right = W - 10, top = 56, bottom = H - 38;
        g.setStroke(new BasicStroke(1f));
        for (int i = 0; i <= 4; i++) {
            double y = bottom - (bottom - top) * i / 4.0;
            g.setColor(i == 0 ? RULE : TRACK);
            g.draw(new Line2D.Double(left, y, right, y));
            textR(g, String.valueOf(yMax * i / 4), left - 10, y + 5, sf(false, 13), GREY);
        }

        double lx = W - 3 * 90;
        for (Sev s : Sev.values()) {
            g.setColor(s.color);
            g.fill(new RoundRectangle2D.Double(lx, 12, 12, 12, 3, 3));
            text(g, s.label, lx + 18, 23, sf(false, 13), INK);
            lx += 90;
        }

        int n = Math.max(days.size(), 1);
        double slot = (right - left) / n;
        double bw = Math.min(70, slot * 0.6);
        int step = Math.max(1, (int) Math.ceil(n / 14.0));
        Sev[] order = Sev.values();
        int i = 0;
        for (Map.Entry<LocalDate, EnumMap<Sev, Long>> d : days.entrySet()) {
            double x = left + slot * i + (slot - bw) / 2;
            double yc = bottom;
            for (int k = order.length - 1; k >= 0; k--) { // low at the bottom, high on top
                long cnt = d.getValue().getOrDefault(order[k], 0L);
                if (cnt == 0) {
                    continue;
                }
                double h = (bottom - top) * cnt / yMax;
                g.setColor(order[k].color);
                g.fill(new Rectangle2D.Double(x, yc - h, bw, h));
                yc -= h;
            }
            if (i % step == 0) {
                textC(g, d.getKey().toString().substring(5), left + slot * i + slot / 2, bottom + 24, sf(false, 13), GREY);
            }
            i++;
        }
        g.dispose();
        return bi;
    }

    // ---------- pdf pieces ----------

    private static com.lowagie.text.Font pf(boolean bold, float size, Color c) {
        return FontFactory.getFont(bold ? FontFactory.HELVETICA_BOLD : FontFactory.HELVETICA, size, c);
    }

    private static class Pages extends PdfPageEventHelper {
        private final String title;
        private PdfTemplate total;
        private BaseFont bf;

        Pages(String title) {
            this.title = title;
        }

        @Override
        public void onOpenDocument(PdfWriter w, Document d) {
            try {
                bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, false);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            total = w.getDirectContent().createTemplate(30, 12);
        }

        @Override
        public void onEndPage(PdfWriter w, Document d) {
            PdfContentByte cb = w.getDirectContent();
            float y = d.bottom() - 22;
            cb.setColorStroke(RULE);
            cb.setLineWidth(0.5f);
            cb.moveTo(d.left(), y + 13);
            cb.lineTo(d.right(), y + 13);
            cb.stroke();

            cb.beginText();
            cb.setFontAndSize(bf, 8);
            cb.setColorFill(GREY);
            cb.setTextMatrix(d.left(), y);
            cb.showText(title);
            cb.endText();

            String t = "Page " + w.getPageNumber() + " of ";
            float len = bf.getWidthPoint(t, 8);
            float x = d.right() - len - 12;
            cb.beginText();
            cb.setFontAndSize(bf, 8);
            cb.setColorFill(GREY);
            cb.setTextMatrix(x, y);
            cb.showText(t);
            cb.endText();
            cb.addTemplate(total, x + len, y);
        }

        @Override
        public void onCloseDocument(PdfWriter w, Document d) {
            total.beginText();
            total.setFontAndSize(bf, 8);
            total.setColorFill(GREY);
            total.setTextMatrix(0, 0);
            total.showText(String.valueOf(w.getPageNumber() - 1));
            total.endText();
        }
    }

    private static PdfPTable section(String title) {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingBefore(14);
        t.setSpacingAfter(8);
        PdfPCell c = new PdfPCell(new Phrase(title, pf(true, 13, INK)));
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColorBottom(ACCENT);
        c.setBorderWidthBottom(1.5f);
        c.setPaddingLeft(0);
        c.setPaddingBottom(5);
        t.addCell(c);
        return t;
    }

    private static PdfPCell imgCell(BufferedImage bi, float padL, float padR) throws Exception {
        PdfPCell c = new PdfPCell(toImage(bi), true);
        c.setBorder(Rectangle.NO_BORDER);
        c.setPaddingLeft(padL);
        c.setPaddingRight(padR);
        c.setPaddingTop(0);
        c.setPaddingBottom(0);
        return c;
    }

    private static PdfPCell plain(String text, com.lowagie.text.Font f, Color bg, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setBackgroundColor(bg);
        c.setBorder(Rectangle.NO_BORDER);
        c.setPadding(6);
        c.setHorizontalAlignment(align);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return c;
    }

    private static void kpi(PdfPTable t, String label, long n, Color color) {
        PdfPCell c = new PdfPCell();
        c.setBackgroundColor(CARD);
        c.setBorder(Rectangle.TOP);
        c.setBorderColorTop(color);
        c.setBorderWidthTop(3f);
        c.setPadding(9);
        Paragraph v = new Paragraph(String.valueOf(n), pf(true, 24, color));
        v.setAlignment(Element.ALIGN_CENTER);
        Paragraph l = new Paragraph(label.toUpperCase(), pf(true, 8, GREY));
        l.setAlignment(Element.ALIGN_CENTER);
        c.addElement(v);
        c.addElement(l);
        t.addCell(c);
    }

    private static PdfPCell gap() {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        return c;
    }

    private static void header(PdfPTable t, String... names) {
        for (String h : names) {
            t.addCell(plain(h.toUpperCase(), pf(true, 8, Color.WHITE), NAVY, Element.ALIGN_LEFT));
        }
    }

    // ---------- document ----------

    private static void render(List<AnalysisResult> results, OutputStream out) throws Exception {
        List<Finding> fs = results.stream().flatMap(r -> r.findings().stream()).collect(Collectors.toList());
        fs.sort(Comparator.comparingInt((Finding f) -> sevOf(f.severity()).ordinal())
                .thenComparing(Finding::detectedAt, Comparator.nullsLast(Comparator.<Instant>reverseOrder())));

        long parsed = 0, malformed = 0, blank = 0;
        for (AnalysisResult r : results) {
            parsed += r.events().size();
            malformed += r.malformedLineCount();
            blank += r.blankLineCount();
        }

        Map<Sev, Long> bySev = new EnumMap<>(Sev.class);
        for (Finding f : fs) {
            bySev.merge(sevOf(f.severity()), 1L, Long::sum);
        }
        String highest = "None";
        for (Sev s : Sev.values()) {
            if (bySev.getOrDefault(s, 0L) > 0) {
                highest = s.label;
                break;
            }
        }

        Document doc = new Document(PageSize.A4, 40, 40, 36, 56);
        PdfWriter w = PdfWriter.getInstance(doc, out);
        w.setPageEvent(new Pages("Log Analysis Report"));
        doc.open();

        // banner
        PdfPTable banner = new PdfPTable(new float[]{3f, 2f});
        banner.setWidthPercentage(100);
        PdfPCell left = new PdfPCell();
        left.setBackgroundColor(NAVY);
        left.setBorder(Rectangle.NO_BORDER);
        left.setPadding(20);
        left.addElement(new Paragraph("SECURITY ASSESSMENT", pf(true, 8, new Color(0x7FC4F0))));
        Paragraph title = new Paragraph("Log Analysis Report", pf(true, 26, Color.WHITE));
        title.setSpacingBefore(4);
        left.addElement(title);
        left.addElement(new Paragraph("Intrusion detection findings", pf(false, 11, new Color(0xA9B7C6))));

        PdfPCell right = new PdfPCell();
        right.setBackgroundColor(NAVY);
        right.setBorder(Rectangle.NO_BORDER);
        right.setPadding(20);
        right.setVerticalAlignment(Element.ALIGN_MIDDLE);
        String[][] meta = {
                {"FILES ANALYZED", String.valueOf(results.size())},
                {"GENERATED", fmt(Instant.now())},
                {"HIGHEST SEVERITY", highest}
        };
        for (String[] kv : meta) {
            Paragraph k = new Paragraph(kv[0], pf(true, 7, new Color(0x7FC4F0)));
            k.setAlignment(Element.ALIGN_RIGHT);
            Paragraph v = new Paragraph(kv[1], pf(false, 10, Color.WHITE));
            v.setAlignment(Element.ALIGN_RIGHT);
            v.setSpacingAfter(6);
            right.addElement(k);
            right.addElement(v);
        }
        banner.addCell(left);
        banner.addCell(right);
        PdfPCell strip = new PdfPCell();
        strip.setColspan(2);
        strip.setBackgroundColor(ACCENT);
        strip.setBorder(Rectangle.NO_BORDER);
        strip.setFixedHeight(4);
        banner.addCell(strip);
        doc.add(banner);

        // cards
        PdfPTable cards = new PdfPTable(new float[]{1, .12f, 1, .12f, 1, .12f, 1, .12f, 1});
        cards.setWidthPercentage(100);
        cards.setSpacingBefore(14);
        kpi(cards, "Files", results.size(), NAVY);
        cards.addCell(gap());
        kpi(cards, "Parsed lines", parsed, NAVY);
        for (Sev s : Sev.values()) {
            cards.addCell(gap());
            kpi(cards, s.label, bySev.getOrDefault(s, 0L), s.color);
        }
        doc.add(cards);

        // donut + gauge
        PdfPTable row = new PdfPTable(new float[]{63, 37});
        row.setWidthPercentage(100);
        row.setSpacingBefore(14);
        row.addCell(imgCell(donut(bySev, fs.size()), 0, 10));
        row.addCell(imgCell(gauge(riskScore(bySev)), 10, 0));
        doc.add(row);

        // trend
        PdfPTable tr = new PdfPTable(1);
        tr.setWidthPercentage(100);
        tr.setSpacingBefore(10);
        tr.addCell(imgCell(trend(fs), 0, 0));
        doc.add(tr);

        // page 2
        doc.newPage();
        if (!fs.isEmpty()) {
            doc.add(section("Where findings concentrate"));
            PdfPTable bars = new PdfPTable(2);
            bars.setWidthPercentage(100);
            bars.addCell(imgCell(hbars("Top sources",
                    top(fs.stream().map(f -> safe(f.source())).collect(Collectors.toList()), 6), ACCENT), 0, 10));
            bars.addCell(imgCell(hbars("Top rules",
                    top(fs.stream().map(f -> safe(f.rule())).collect(Collectors.toList()), 6), NAVY), 10, 0));
            doc.add(bars);
        }

        // files table
        doc.add(section("Files analyzed"));
        PdfPTable ft = new PdfPTable(new float[]{4f, 1.2f, 1.4f, 1.1f, 1.2f, 1.2f});
        ft.setWidthPercentage(100);
        ft.setHeaderRows(1);
        header(ft, "File", "Parsed", "Malformed", "Blank", "Findings", "Time");
        int i = 0;
        for (AnalysisResult r : results) {
            Color bg = i++ % 2 == 0 ? Color.WHITE : CARD;
            ft.addCell(plain(safe(r.fileName()), pf(true, 9, INK), bg, Element.ALIGN_LEFT));
            ft.addCell(plain(String.valueOf(r.events().size()), pf(false, 9, INK), bg, Element.ALIGN_RIGHT));
            ft.addCell(plain(String.valueOf(r.malformedLineCount()), pf(false, 9, INK), bg, Element.ALIGN_RIGHT));
            ft.addCell(plain(String.valueOf(r.blankLineCount()), pf(false, 9, INK), bg, Element.ALIGN_RIGHT));
            ft.addCell(plain(String.valueOf(r.findings().size()), pf(true, 9, INK), bg, Element.ALIGN_RIGHT));
            ft.addCell(plain(r.analysisTimeMillis() + " ms", pf(false, 9, GREY), bg, Element.ALIGN_RIGHT));
        }
        doc.add(ft);

        if (fs.isEmpty()) {
            Paragraph none = new Paragraph("No findings were detected in the analyzed files.", pf(false, 10, GREY));
            none.setSpacingBefore(12);
            doc.add(none);
        } else {
            // overview table
            doc.add(section("Findings overview"));
            PdfPTable t = new PdfPTable(new float[]{1.2f, 2.2f, 3.4f, 1.8f, 1.9f});
            t.setWidthPercentage(100);
            t.setHeaderRows(1);
            header(t, "Severity", "Rule", "Title", "Source", "Detected (UTC)");
            i = 0;
            for (Finding f : fs) {
                Color bg = i++ % 2 == 0 ? Color.WHITE : CARD;
                Sev sev = sevOf(f.severity());
                PdfPCell pill = plain(sev.label.toUpperCase(), pf(true, 7.5f, Color.WHITE), sev.color, Element.ALIGN_CENTER);
                pill.setBorder(Rectangle.BOX);
                pill.setBorderColor(bg);
                pill.setBorderWidth(3.5f);
                pill.setPadding(3);
                t.addCell(pill);
                t.addCell(plain(safe(f.rule()), pf(false, 9, INK), bg, Element.ALIGN_LEFT));
                t.addCell(plain(safe(f.title()), pf(false, 9, INK), bg, Element.ALIGN_LEFT));
                t.addCell(plain(safe(f.source()), pf(false, 9, INK), bg, Element.ALIGN_LEFT));
                t.addCell(plain(fmtShort(f.detectedAt()), pf(false, 8.5f, GREY), bg, Element.ALIGN_LEFT));
            }
            doc.add(t);

            // details
            doc.newPage();
            doc.add(section("Finding details"));
            for (Finding f : fs) {
                Sev sev = sevOf(f.severity());
                PdfPTable card = new PdfPTable(new float[]{0.8f, 60f});
                card.setWidthPercentage(100);
                card.setKeepTogether(true);
                card.setSpacingAfter(8);

                PdfPCell bar = new PdfPCell();
                bar.setBackgroundColor(sev.color);
                bar.setBorder(Rectangle.NO_BORDER);

                PdfPCell body = new PdfPCell();
                body.setBackgroundColor(CARD);
                body.setBorder(Rectangle.NO_BORDER);
                body.setPadding(10);

                body.addElement(new Paragraph(safe(f.title()), pf(true, 11, INK)));

                Paragraph head = new Paragraph();
                head.add(new Chunk(sev.label.toUpperCase(), pf(true, 8, sev.color)));
                head.add(new Chunk("   |   " + safe(f.rule()) + "   |   " + safe(f.source())
                        + "   |   " + fmt(f.detectedAt()), pf(false, 8, GREY)));
                head.setSpacingAfter(6);
                body.addElement(head);

                body.addElement(new Paragraph(safe(f.description()), pf(false, 9.5f, INK)));

                card.addCell(bar);
                card.addCell(body);
                doc.add(card);
            }
        }

        // malformed lines, capped
        if (malformed > 0) {
            doc.add(section("Malformed lines (" + malformed + ")"));
            PdfPTable mt = new PdfPTable(new float[]{2.2f, 0.8f, 6f});
            mt.setWidthPercentage(100);
            mt.setHeaderRows(1);
            header(mt, "File", "Line", "Content");
            int shown = 0;
            outer:
            for (AnalysisResult r : results) {
                for (var line : r.malformedLines()) {
                    if (shown >= 25) {
                        break outer;
                    }
                    Color bg = shown++ % 2 == 0 ? Color.WHITE : CARD;
                    mt.addCell(plain(safe(r.fileName()), pf(false, 8.5f, INK), bg, Element.ALIGN_LEFT));
                    mt.addCell(plain(String.valueOf(line.lineNumber()), pf(false, 8.5f, GREY), bg, Element.ALIGN_LEFT));
                    mt.addCell(plain(cut(String.valueOf(line.content()), 110), pf(false, 8.5f, INK), bg, Element.ALIGN_LEFT));
                }
            }
            doc.add(mt);
            if (malformed > shown) {
                Paragraph more = new Paragraph("and " + (malformed - shown) + " more", pf(false, 8.5f, GREY));
                more.setSpacingBefore(4);
                doc.add(more);
            }
        }

        doc.close();
    }
}
