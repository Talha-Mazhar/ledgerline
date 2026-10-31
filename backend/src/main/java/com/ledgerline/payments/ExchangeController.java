package com.ledgerline.payments;

import com.ledgerline.identity.CurrentUser;
import com.ledgerline.wallets.ActivityItemResponse;
import com.ledgerline.wallets.MoneyResponse;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** {@code POST /api/v1/exchanges/quote} (a preview) and {@code POST /api/v1/exchanges}. */
@RestController
@RequestMapping("/api/v1/exchanges")
class ExchangeController {

    record ExchangeRequest(UUID fromWalletId, UUID toWalletId, String amount) {
        ExchangeRequest {
            Objects.requireNonNull(fromWalletId, "fromWalletId is required");
            Objects.requireNonNull(toWalletId, "toWalletId is required");
            Objects.requireNonNull(amount, "amount is required");
        }
    }

    record ExchangeQuoteResponse(MoneyResponse sell, MoneyResponse buy, String rate) {}

    record ExchangeResponse(UUID entryId, ActivityItemResponse sell, ActivityItemResponse buy, String rate) {}

    private final Exchanges exchanges;
    private final CurrentUser currentUser;

    ExchangeController(Exchanges exchanges, CurrentUser currentUser) {
        this.exchanges = exchanges;
        this.currentUser = currentUser;
    }

    @PostMapping("/quote")
    public ExchangeQuoteResponse quoteExchange(@RequestBody ExchangeRequest request) {
        Exchanges.Quote quote =
                exchanges.quote(currentUser.id(), request.fromWalletId(), request.toWalletId(), request.amount());
        return new ExchangeQuoteResponse(
                MoneyResponse.from(quote.sell()),
                MoneyResponse.from(quote.buy()),
                quote.rate().customerRate().toPlainString());
    }

    @PostMapping
    public ResponseEntity<ExchangeResponse> exchangeMoney(
            @RequestHeader("Idempotency-Key") UUID idempotencyKey, @RequestBody ExchangeRequest request) {
        Exchanges.Completed done = exchanges.exchange(
                currentUser.id(), request.fromWalletId(), request.toWalletId(), request.amount(), idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ExchangeResponse(
                        done.entryId(),
                        ActivityItemResponse.from(done.sell()),
                        ActivityItemResponse.from(done.buy()),
                        done.rate().customerRate().toPlainString()));
    }
}
