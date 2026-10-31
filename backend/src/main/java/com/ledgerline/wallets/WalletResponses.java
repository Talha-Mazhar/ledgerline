package com.ledgerline.wallets;

import com.ledgerline.MoneyResponse;
import com.ledgerline.ledger.CurrencyCode;
import com.ledgerline.ledger.Money;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * The JSON shapes of the wallets API, written to match the schemas in {@code api/openapi.yaml}.
 * {@code WalletApiTests} checks every response against that file, so a field that drifts from the
 * contract fails the build.
 *
 * <p>These are separate from the {@link Wallet} entity on purpose: the database can change without
 * changing the API, and no JPA field is ever exposed by accident.
 */
final class WalletResponses {

    private WalletResponses() {}

    /** Request body of {@code POST /wallets}. A missing currency is rejected with 400. */
    record OpenWalletRequest(CurrencyCode currency) {
        OpenWalletRequest {
            Objects.requireNonNull(currency, "currency is required");
        }
    }

    record WalletResponse(UUID id, CurrencyCode currency, MoneyResponse balance, WalletStatus status, Instant openedAt) {
        static WalletResponse from(Wallet wallet, Money balance) {
            return new WalletResponse(
                    wallet.id(), wallet.currency(), MoneyResponse.from(balance), wallet.status(), wallet.openedAt());
        }
    }

    /** Lists are wrapped in an object, so paging fields can be added later without breaking clients. */
    record WalletListResponse(List<WalletResponse> items) {}

    record ActivityListResponse(List<ActivityItemResponse> items) {}
}
