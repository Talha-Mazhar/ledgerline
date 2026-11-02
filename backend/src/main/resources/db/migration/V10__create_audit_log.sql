-- Who did what in the back office, and when. Append-only, like the ledger.
CREATE TABLE audit_log
(
    id          UUID         NOT NULL PRIMARY KEY,
    actor_id    UUID,
    action      VARCHAR(48)  NOT NULL,
    target_type VARCHAR(24)  NOT NULL,
    target_id   UUID         NOT NULL,
    details     VARCHAR(1000) NOT NULL,
    occurred_at TIMESTAMPTZ  NOT NULL
);

CREATE INDEX ix_audit_log_occurred ON audit_log (occurred_at DESC);
CREATE INDEX ix_audit_log_target ON audit_log (target_id);

CREATE TRIGGER trg_audit_log_append_only
    BEFORE UPDATE OR DELETE
    ON audit_log
    FOR EACH ROW
EXECUTE FUNCTION ledger_forbid_change();
