package com.ledgerline;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Turns on Spring's cache abstraction; the store is Redis ({@code spring.cache.type=redis}), so
 * every backend instance shares one cache. Used for exchange rates, which change once a day but
 * are read on every quote.
 */
@Configuration(proxyBeanMethods = false)
@EnableCaching
class CachingConfiguration {}
