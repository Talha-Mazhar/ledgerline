package com.ledgerline.wallets;

import com.ledgerline.identity.CurrentUser;
import com.ledgerline.ledger.Money;
import com.ledgerline.wallets.WalletResponses.OpenWalletRequest;
import com.ledgerline.wallets.WalletResponses.WalletListResponse;
import com.ledgerline.wallets.WalletResponses.WalletResponse;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** HTTP endpoints for {@code /api/v1/wallets}, as specified in {@code api/openapi.yaml}. */
@RestController
@RequestMapping("/api/v1/wallets")
class WalletController {

    private final WalletService wallets;
    private final CurrentUser currentUser;

    WalletController(WalletService wallets, CurrentUser currentUser) {
        this.wallets = wallets;
        this.currentUser = currentUser;
    }

    @GetMapping
    public WalletListResponse listWallets() {
        List<Wallet> owned = wallets.walletsOf(currentUser.id());
        Map<UUID, Money> balances = wallets.balancesOf(owned);
        return new WalletListResponse(owned.stream()
                .map(wallet -> WalletResponse.from(wallet, balanceIn(balances, wallet)))
                .toList());
    }

    @PostMapping
    public ResponseEntity<WalletResponse> openWallet(@RequestBody OpenWalletRequest request) {
        Wallet wallet = wallets.open(currentUser.id(), request.currency());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{walletId}")
                .buildAndExpand(wallet.id())
                .toUri();
        return ResponseEntity.created(location).body(toResponse(wallet));
    }

    @GetMapping("/{walletId}")
    public WalletResponse getWallet(@PathVariable UUID walletId) {
        return toResponse(wallets.walletOf(currentUser.id(), walletId));
    }

    private WalletResponse toResponse(Wallet wallet) {
        return WalletResponse.from(wallet, wallets.balanceOf(wallet));
    }

    private static Money balanceIn(Map<UUID, Money> balances, Wallet wallet) {
        Money balance = balances.get(wallet.id());
        return balance != null ? balance : Money.zero(wallet.currency());
    }
}
