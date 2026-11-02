package com.ledgerline.audit;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** The {@code audit_log} table. Rows are only ever inserted; a trigger refuses updates and deletes. */
@Repository
class AuditLog {

    /** One entry. */
    record Entry(
            UUID id,
            @Nullable UUID actorId,
            String action,
            String targetType,
            UUID targetId,
            String details,
            Instant occurredAt) {}

    private final JdbcClient jdbc;

    AuditLog(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    void record(@Nullable UUID actor, String action, String targetType, UUID targetId, String details, Instant at) {
        jdbc.sql("""
                        INSERT INTO audit_log (id, actor_id, action, target_type, target_id, details, occurred_at)
                        VALUES (:id, :actor, :action, :type, :target, :details, :at)
                        """)
                .param("id", UUID.randomUUID())
                .param("actor", actor)
                .param("action", action)
                .param("type", targetType)
                .param("target", targetId)
                .param("details", details)
                .param("at", OffsetDateTime.ofInstant(at, ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS))
                .update();
    }

    List<Entry> latest(int limit) {
        return jdbc.sql("""
                        SELECT id, actor_id, action, target_type, target_id, details, occurred_at
                        FROM audit_log ORDER BY occurred_at DESC LIMIT :limit
                        """)
                .param("limit", limit)
                .query((rs, row) -> new Entry(
                        rs.getObject("id", UUID.class),
                        rs.getObject("actor_id", UUID.class),
                        rs.getString("action"),
                        rs.getString("target_type"),
                        rs.getObject("target_id", UUID.class),
                        rs.getString("details"),
                        rs.getObject("occurred_at", OffsetDateTime.class).toInstant()))
                .list();
    }
}
