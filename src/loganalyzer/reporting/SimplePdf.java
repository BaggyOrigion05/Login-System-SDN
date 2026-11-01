package loganalyzer.reporting;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class SimplePdf {

    private static final int MAX_CHARS = 85;
    private static final int LINES_PER_PAGE = 50;

    private ArrayList<String> lines = new ArrayList<String>();

    public void addLine(String line) {
        while (line.length() > MAX_CHARS) {
            int cut = line.lastIndexOf(" ", MAX_CHARS);
            if (cut < 10) {
                cut = MAX_CHARS;
            }
            lines.add(line.substring(0, cut));
            line = "   " + line.substring(cut).trim();
        }
        lines.add(line);
    }

    public boolean save(String path) {
        int pageCount = (lines.size() + LINES_PER_PAGE - 1) / LINES_PER_PAGE;
        if (pageCount < 1) {
            pageCount = 1;
        }

        int[] offsets = new int[4 + pageCount * 2];
        String pdf = "%PDF-1.4\n";

        offsets[1] = pdf.length();
        pdf += "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n";

        String kids = "";
        for (int i = 0; i < pageCount; i++) {
            kids += (4 + i * 2) + " 0 R ";
        }
        offsets[2] = pdf.length();
        pdf += "2 0 obj\n<< /Type /Pages /Kids [" + kids + "] /Count " + pageCount + " >>\nendobj\n";

        offsets[3] = pdf.length();
        pdf += "3 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n";

        for (int i = 0; i < pageCount; i++) {
            int pageObj = 4 + i * 2;
            int contentObj = 5 + i * 2;

            String content = "BT\n/F1 11 Tf\n14 TL\n50 800 Td\n";
            int start = i * LINES_PER_PAGE;
            int end = Math.min(start + LINES_PER_PAGE, lines.size());
            for (int j = start; j < end; j++) {
                content += "(" + clean(lines.get(j)) + ") Tj\nT*\n";
            }
            content += "ET\n";

            offsets[pageObj] = pdf.length();
            pdf += pageObj + " 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents "
                    + contentObj + " 0 R /Resources << /Font << /F1 3 0 R >> >> >>\nendobj\n";

            offsets[contentObj] = pdf.length();
            pdf += contentObj + " 0 obj\n<< /Length " + content.length() + " >>\nstream\n"
                    + content + "endstream\nendobj\n";
        }

        int xrefStart = pdf.length();
        int objectCount = offsets.length;
        pdf += "xref\n0 " + objectCount + "\n";
        pdf += "0000000000 65535 f \n";
        for (int n = 1; n < objectCount; n++) {
            pdf += String.format("%010d", offsets[n]) + " 00000 n \n";
        }
        pdf += "trailer\n<< /Size " + objectCount + " /Root 1 0 R >>\n";
        pdf += "startxref\n" + xrefStart + "\n%%EOF\n";

        try {
            FileWriter writer = new FileWriter(path);
            writer.write(pdf);
            writer.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private String clean(String text) {
        String result = "";
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '(' || c == ')' || c == '\\') {
                result += "\\" + c;
            } else if (c < 32 || c > 126) {
                result += "?";
            } else {
                result += c;
            }
        }
        return result;
    }
}