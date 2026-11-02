-- Identity verification (KYC). A customer uploads an ID document; staff review it. Verified
-- customers get higher limits. The document itself lives in object storage (S3 / MinIO); this
-- row keeps only its key.
CREATE TABLE verifications
(
    user_id          UUID         NOT NULL PRIMARY KEY,
    status           VARCHAR(24)  NOT NULL CHECK (status IN ('PENDING_REVIEW', 'VERIFIED', 'REJECTED')),
    document_key     VARCHAR(200) NOT NULL,
    document_name    VARCHAR(200) NOT NULL,
    content_type     VARCHAR(100) NOT NULL,
    submitted_at     TIMESTAMPTZ  NOT NULL,
    reviewed_by      UUID,
    reviewed_at      TIMESTAMPTZ,
    rejection_reason VARCHAR(500),
    version          BIGINT       NOT NULL
);

CREATE INDEX ix_verifications_status ON verifications (status, submitted_at);
