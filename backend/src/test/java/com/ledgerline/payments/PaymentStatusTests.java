package com.ledgerline.payments;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** The payment state machine: every allowed move, and a sample of refused ones. */
class PaymentStatusTests {

    @ParameterizedTest
    @CsvSource({
        "HELD, AWAITING_SECOND_APPROVAL",
        "HELD, REJECTED",
        "AWAITING_SECOND_APPROVAL, COMPLETED",
        "AWAITING_SECOND_APPROVAL, REJECTED",
        "COMPLETED, REFUNDED"
    })
    void allows(PaymentStatus from, PaymentStatus to) {
        assertThat(from.canMoveTo(to)).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
        "HELD, COMPLETED",
        "COMPLETED, REJECTED",
        "REJECTED, COMPLETED",
        "REFUNDED, REFUNDED",
        "AWAITING_SECOND_APPROVAL, HELD"
    })
    void refuses(PaymentStatus from, PaymentStatus to) {
        assertThat(from.canMoveTo(to)).isFalse();
    }

    @Test
    void finalStatesAreFinal() {
        for (PaymentStatus next : PaymentStatus.values()) {
            assertThat(PaymentStatus.REJECTED.canMoveTo(next)).isFalse();
            assertThat(PaymentStatus.REFUNDED.canMoveTo(next)).isFalse();
        }
    }
}
