CREATE TABLE tier_boundary (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tier VARCHAR(255) NOT NULL,
    gender VARCHAR(255) NOT NULL,
    min_fans BIGINT NOT NULL,
    max_fans BIGINT NOT NULL,
    challenge_cost BIGINT NOT NULL,
    contender_entry_fee BIGINT NOT NULL,
    CONSTRAINT uc_tier_boundary_tier_gender UNIQUE (tier, gender)
);
