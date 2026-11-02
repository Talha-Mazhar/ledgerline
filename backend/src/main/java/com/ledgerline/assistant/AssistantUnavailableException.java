package com.ledgerline.assistant;

import com.ledgerline.DomainException;
import org.jspecify.annotations.Nullable;

class AssistantUnavailableException extends DomainException {

    AssistantUnavailableException(@Nullable Throwable cause) {
        super(
                Kind.UNAVAILABLE,
                "Assistant unavailable",
                "The assistant isn't available right now. Start it with the 'ai' Compose profile, then try again.",
                cause);
    }
}
