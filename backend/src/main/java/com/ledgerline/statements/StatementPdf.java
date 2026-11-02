package com.ledgerline.statements;

import com.ledgerline.ledger.CurrencyCode;
import com.ledgerline.ledger.Money;
import com.ledgerline.ledger.StatementLine;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

/**
 * Draws a statement as an A4 PDF with Apache PDFBox: a header, a summary box, then the lines in a
 * table, continuing on as many pages as needed.
 *
 * <p>Uses PDF's built-in Helvetica, which only covers Latin-1, so text is reduced to it ("Kč"
 * becomes "CZK", other characters are dropped). A production version would embed a full font.
 */
@Component
class StatementPdf {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.UK);
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.UK);
    private static final float MARGIN = 50;
    private static final float ROW = 16;
    private static final float[] COLUMNS = {MARGIN, 130, 380, 470};

    private final PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private final PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    byte[] render(MonthlyStatement statement) {
        try (PDDocument document = new PDDocument();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDDocumentInformation info = document.getDocumentInformation();
            info.setTitle("Ledgerline statement " + statement.month());
            info.setAuthor("Ledgerline");

            Page page = new Page(document);
            page.text(bold, 18, MARGIN, "Ledgerline statement");
            page.down(24);
            page.text(regular, 10, MARGIN, plain(statement.ownerName()) + " <" + plain(statement.ownerEmail()) + ">");
            page.down(ROW);
            page.text(regular, 10, MARGIN, currencyName(statement.opening().currency()) + " wallet, "
                    + statement.month().format(MONTH) + "  |  Wallet " + statement.walletId());
            page.down(28);

            summary(page, "Opening balance", statement.opening());
            summary(page, "Money in", statement.moneyIn());
            summary(page, "Money out", statement.moneyOut());
            summary(page, "Closing balance", statement.closing());
            page.down(18);

            header(page);
            if (statement.lines().isEmpty()) {
                page.text(regular, 10, MARGIN, "No activity this month.");
            }
            for (StatementLine line : statement.lines()) {
                if (page.full()) {
                    page.close();
                    page = new Page(document);
                    header(page);
                }
                page.text(regular, 9, COLUMNS[0], line.postedAt().atZone(ZoneOffset.UTC).format(DAY));
                page.text(regular, 9, COLUMNS[1], plain(line.memo()));
                page.right(regular, 9, COLUMNS[3] - 10, signed(line.amount()));
                page.right(regular, 9, PDRectangle.A4.getWidth() - MARGIN, amount(line.balanceAfter()));
                page.down(ROW);
            }
            page.down(ROW);
            page.text(regular, 8, MARGIN, "Ledgerline is a demo application. No real money is held or moved.");
            page.close();
            document.save(out);
            return out.toByteArray();
        } catch (IOException failed) {
            throw new UncheckedIOException("The statement PDF could not be written", failed);
        }
    }

    private void summary(Page page, String label, Money money) throws IOException {
        page.text(regular, 10, MARGIN, label);
        page.right(bold, 10, 300, amount(money));
        page.down(ROW);
    }

    private void header(Page page) throws IOException {
        page.text(bold, 9, COLUMNS[0], "Date");
        page.text(bold, 9, COLUMNS[1], "Description");
        page.right(bold, 9, COLUMNS[3] - 10, "Amount");
        page.right(bold, 9, PDRectangle.A4.getWidth() - MARGIN, "Balance");
        page.down(ROW);
    }

    /** E.g. {@code EUR 1,250.00}. */
    static String amount(Money money) {
        return money.currency() + " " + String.format(Locale.ROOT, "%,.2f", money.amount());
    }

    static String signed(Money money) {
        return (money.amount().signum() > 0 ? "+" : "") + amount(money);
    }

    private static String currencyName(CurrencyCode currency) {
        return switch (currency) {
            case EUR -> "Euro";
            case USD -> "US dollar";
            case CZK -> "Czech koruna";
            case GBP -> "British pound";
        };
    }

    /** Text Helvetica can draw: Latin-1 only. */
    static String plain(String text) {
        StringBuilder out = new StringBuilder();
        for (char c : text.replace("Kč", "CZK").replace('–', '-').toCharArray()) {
            if (c >= 32 && c <= 255) {
                out.append(c);
            }
        }
        return out.toString();
    }

    /** One page being written, top to bottom. */
    private static final class Page {

        private final PDPageContentStream content;
        private float y;

        Page(PDDocument document) throws IOException {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            this.content = new PDPageContentStream(document, page);
            this.y = PDRectangle.A4.getHeight() - MARGIN;
        }

        void text(PDType1Font font, float size, float x, String text) throws IOException {
            content.beginText();
            content.setFont(font, size);
            content.newLineAtOffset(x, y);
            content.showText(text);
            content.endText();
        }

        void right(PDType1Font font, float size, float rightEdge, String text) throws IOException {
            float width = font.getStringWidth(text) / 1000 * size;
            text(font, size, rightEdge - width, text);
        }

        void down(float by) {
            y -= by;
        }

        boolean full() {
            return y < MARGIN + 2 * ROW;
        }

        void close() throws IOException {
            content.close();
        }
    }
}
