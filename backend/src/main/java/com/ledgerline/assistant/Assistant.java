package com.ledgerline.assistant;

import com.ledgerline.wallets.WalletReader;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * Answers a customer's question about their money with the language model and {@link MoneyTools}.
 *
 * <p>Grounding: the system prompt tells the model to use the tools for every figure and to say so
 * when they don't have the answer, rather than guess. The model is small and local, so answers are
 * short and sometimes plain, but never about another customer's data: the tools can't reach it.
 */
@Service
class Assistant {

    private static final Logger log = LoggerFactory.getLogger(Assistant.class);

    static final String SYSTEM = """
            You are Ledgerline's assistant. Ledgerline is a demo multi-currency wallet app.
            You answer the signed-in customer's questions about their own wallets and payments.

            Rules:
            - Get every number from the tools. Never estimate, invent or round amounts.
            - If the tools return nothing that answers the question, say you don't have that
              information. Don't guess.
            - Only talk about this customer's money. Refuse anything else politely in one sentence.
            - Answer in at most four short sentences. Write amounts with their currency, e.g. 25.00 EUR.
            - Dates: today is %s. "This month" starts on the 1st of the current month.
            """;

    private final ChatClient chat;
    private final WalletReader wallets;
    private final Clock clock;

    Assistant(ChatClient.Builder chat, WalletReader wallets, Clock clock) {
        this.chat = chat.build();
        this.wallets = wallets;
        this.clock = clock;
    }

    String ask(UUID customer, String question) {
        String asked = question.strip();
        if (asked.isEmpty() || asked.length() > 500) {
            throw new InvalidQuestionException();
        }
        String answer;
        try {
            answer = chat.prompt()
                    .system(SYSTEM.formatted(LocalDate.now(clock.withZone(ZoneOffset.UTC))))
                    .user(asked)
                    .tools(new MoneyTools(customer, wallets))
                    .call()
                    .content();
        } catch (RuntimeException modelUnavailable) {
            log.warn("The assistant's model didn't answer: {}", modelUnavailable.getMessage());
            throw new AssistantUnavailableException(modelUnavailable);
        }
        if (answer == null || answer.isBlank()) {
            throw new AssistantUnavailableException(null);
        }
        return answer.strip();
    }
}
