-- Execution history for one-time Java data migrations (DataMigration framework).
-- Mirrors flyway_schema_history on the Java side: one row per migration id, ever.
-- Global by design — data repairs are database-wide, so there is NO universe_id column.
-- status: SUCCESS | FAILED | LEGACY_SEEDED (repair already applied via ad-hoc game_setting flag).
CREATE TABLE data_migration_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    migration_id VARCHAR(255) NOT NULL,
    app_version VARCHAR(100) NOT NULL,
    executed_at TIMESTAMP NOT NULL,
    duration_ms BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    CONSTRAINT pk_data_migration_history PRIMARY KEY (id),
    CONSTRAINT uq_data_migration_history_migration_id UNIQUE (migration_id)
);

CREATE INDEX idx_data_migration_history_status ON data_migration_history(status);
