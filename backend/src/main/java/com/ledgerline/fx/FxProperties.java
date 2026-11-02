package com.ledgerline.fx;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code ledgerline.fx.*}: where rates come from.
 *
 * @param feedUrl the Frankfurter API (ECB reference rates), e.g. {@code https://api.frankfurter.dev/v1}
 * @param timeout how long one call to the feed may take
 */
@ConfigurationProperties("ledgerline.fx")
record FxProperties(String feedUrl, Duration timeout) {}
