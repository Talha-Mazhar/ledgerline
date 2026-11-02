package com.ledgerline.statements;

import com.ledgerline.identity.CurrentUser;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** A wallet's monthly statements: which months exist, and each one as a PDF. */
@RestController
@RequestMapping("/api/v1/wallets/{walletId}/statements")
class StatementController {

    record StatementMonth(String month) {}

    record StatementMonthList(List<StatementMonth> items) {}

    private final Statements statements;
    private final StatementPdf pdf;
    private final CurrentUser currentUser;

    StatementController(Statements statements, StatementPdf pdf, CurrentUser currentUser) {
        this.statements = statements;
        this.pdf = pdf;
        this.currentUser = currentUser;
    }

    @GetMapping
    StatementMonthList listStatements(@PathVariable UUID walletId) {
        return new StatementMonthList(statements.monthsOf(currentUser.id(), walletId).stream()
                .map(month -> new StatementMonth(month.toString()))
                .toList());
    }

    @GetMapping("/{month}")
    ResponseEntity<byte[]> getStatement(@PathVariable UUID walletId, @PathVariable String month) {
        YearMonth period;
        try {
            period = YearMonth.parse(month);
        } catch (DateTimeParseException notAMonth) {
            throw new InvalidMonthException();
        }
        MonthlyStatement statement = statements.statement(currentUser.id(), walletId, period);
        String fileName = "ledgerline-" + statement.opening().currency() + "-" + period + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(fileName)
                                .build()
                                .toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(pdf.render(statement));
    }
}
