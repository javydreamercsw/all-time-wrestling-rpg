-- Seed-update review (2.13.0): park seed-data collisions as pending admin decisions on
-- version-upgrade boots instead of silently overwriting user-modified data.
-- decision_type: FIELD_CONFLICT (a tracked field differs) | REMOVAL (seed no longer carries
--   the entity; field_name/current_value/new_value are NULL for removals).
-- status: PENDING → APPLIED_KEEP | APPLIED_NEW | APPLIED_KEEP_AS_CUSTOM | APPLIED_REMOVE
--   (outcomes mapped from SeedFieldDecision: KEEP_MINE, USE_NEW, KEEP_AS_CUSTOM, REMOVE).
CREATE TABLE seed_update_decision (
    id                  BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    domain_key          VARCHAR(64)  NOT NULL,
    entity_key          VARCHAR(255) NOT NULL,
    entity_display_name VARCHAR(255) NOT NULL,
    field_name          VARCHAR(128) NULL,
    decision_type       VARCHAR(32)  NOT NULL,
    current_value       TEXT NULL,
    new_value           TEXT NULL,
    new_fingerprint     VARCHAR(64)  NOT NULL,
    status              VARCHAR(32)  NOT NULL,
    app_version         VARCHAR(32)  NOT NULL,
    created_at          TIMESTAMP    NOT NULL
);

CREATE INDEX idx_sud_status ON seed_update_decision(status);
CREATE INDEX idx_sud_domain ON seed_update_decision(domain_key, entity_key);

-- NOTE: an at-most-one-PENDING-row rule per (domain_key, entity_key, field_name) is
-- deliberately NOT expressed as a DB constraint. H2 and MySQL lack partial indexes, so a
-- unique index on PENDING rows only is not expressible here. Uniqueness is instead enforced
-- by a query-level check (findByDomainKeyAndEntityKeyAndFieldNameAndStatus for the PENDING
-- row) plus service-layer dedupe at insert time. A full (non-partial) unique constraint
-- would wrongly block re-detection of the same coordinate after an earlier decision was
-- applied and a later upgrade collides again.
