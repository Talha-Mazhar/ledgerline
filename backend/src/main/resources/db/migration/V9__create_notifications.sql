-- In-app notifications, written by the consumer of the Kafka topics. The unique (event, user) pair
-- makes the consumer idempotent: Kafka may deliver an event twice, the user sees it once.
CREATE TABLE notifications
(
    id         UUID         NOT NULL PRIMARY KEY,
    user_id    UUID         NOT NULL,
    event_id   UUID         NOT NULL,
    kind       VARCHAR(40)  NOT NULL,
    title      VARCHAR(200) NOT NULL,
    body       VARCHAR(1000) NOT NULL,
    link       VARCHAR(200),
    created_at TIMESTAMPTZ  NOT NULL,
    read_at    TIMESTAMPTZ,
    CONSTRAINT uq_notifications_event_user UNIQUE (event_id, user_id)
);

CREATE INDEX ix_notifications_user ON notifications (user_id, created_at DESC);
