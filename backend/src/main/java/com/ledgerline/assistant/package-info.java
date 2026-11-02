/**
 * Assistant module: "where did my money go?" in plain language, answered by a language model
 * (Ollama, running locally) through Spring AI.
 *
 * <p>The model never sees the database. It gets tools ({@link com.ledgerline.assistant.MoneyTools})
 * that return the signed-in customer's own balances and activity, and instructions to answer
 * only from what those tools return. The tools are bound to one customer per question, so the
 * model can't ask about anyone else.
 *
 * <p>May depend on {@code wallets} (balances, activity), {@code ledger} (money types) and {@code
 * identity} (who is asking).
 */
@NullMarked
@ApplicationModule(
        displayName = "Assistant",
        allowedDependencies = {"wallets", "ledger", "identity"})
package com.ledgerline.assistant;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
