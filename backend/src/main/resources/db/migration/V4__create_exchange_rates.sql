-- Exchange rates, one row per currency, quoted against the euro: how many units of the currency
-- one euro buys. A rate between any two currencies is derived from their two rows, so four rows
-- give all twelve pairs.
--
-- These are fixed demo rates. A scheduled job fetching the European Central Bank's daily rates
-- would update the same rows.

CREATE TABLE exchange_rates
(
    currency      VARCHAR(3)     NOT NULL PRIMARY KEY CHECK (currency IN ('EUR', 'USD', 'CZK', 'GBP')),
    units_per_eur NUMERIC(19, 8) NOT NULL CHECK (units_per_eur > 0),
    as_of         TIMESTAMPTZ    NOT NULL
);

INSERT INTO exchange_rates (currency, units_per_eur, as_of)
VALUES ('EUR', 1.00000000, '2026-10-30T16:00:00Z'),
       ('USD', 1.08500000, '2026-10-30T16:00:00Z'),
       ('CZK', 25.15000000, '2026-10-30T16:00:00Z'),
       ('GBP', 0.85500000, '2026-10-30T16:00:00Z');
