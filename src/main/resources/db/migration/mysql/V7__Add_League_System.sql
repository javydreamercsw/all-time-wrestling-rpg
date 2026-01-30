CREATE TABLE league (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    commissioner_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    max_picks_per_player INTEGER NOT NULL DEFAULT 1,
    CONSTRAINT fk_league_commissioner FOREIGN KEY (commissioner_id) REFERENCES account(id)
);

CREATE TABLE league_excluded_wrestler (
    league_id BIGINT NOT NULL,
    wrestler_id BIGINT NOT NULL,
    PRIMARY KEY (league_id, wrestler_id),
    CONSTRAINT fk_excluded_league FOREIGN KEY (league_id) REFERENCES league(id) ON DELETE CASCADE,
    CONSTRAINT fk_excluded_wrestler FOREIGN KEY (wrestler_id) REFERENCES wrestler(wrestler_id) ON DELETE CASCADE
);

CREATE TABLE league_membership (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    league_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    role VARCHAR(50) NOT NULL,
    CONSTRAINT fk_membership_league FOREIGN KEY (league_id) REFERENCES league(id),
    CONSTRAINT fk_membership_member FOREIGN KEY (member_id) REFERENCES account(id),
    CONSTRAINT uk_league_member UNIQUE (league_id, member_id)
);

CREATE TABLE league_roster (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    league_id BIGINT NOT NULL,
    owner_id BIGINT NOT NULL,
    wrestler_id BIGINT NOT NULL,
    CONSTRAINT fk_roster_league FOREIGN KEY (league_id) REFERENCES league(id),
    CONSTRAINT fk_roster_owner FOREIGN KEY (owner_id) REFERENCES account(id),
    CONSTRAINT fk_roster_wrestler FOREIGN KEY (wrestler_id) REFERENCES wrestler(wrestler_id),
    CONSTRAINT uk_league_wrestler UNIQUE (league_id, wrestler_id)
);

CREATE TABLE draft (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    league_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    current_turn_user_id BIGINT,
    current_round INTEGER NOT NULL DEFAULT 1,
    current_pick_number INTEGER NOT NULL DEFAULT 1,
    direction INTEGER NOT NULL DEFAULT 1,
    CONSTRAINT fk_draft_league FOREIGN KEY (league_id) REFERENCES league(id),
    CONSTRAINT fk_draft_turn_user FOREIGN KEY (current_turn_user_id) REFERENCES account(id)
);

CREATE TABLE draft_pick (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    draft_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    wrestler_id BIGINT NOT NULL,
    pick_number INTEGER NOT NULL,
    round INTEGER NOT NULL,
    CONSTRAINT fk_pick_draft FOREIGN KEY (draft_id) REFERENCES draft(id),
    CONSTRAINT fk_pick_user FOREIGN KEY (user_id) REFERENCES account(id),
    CONSTRAINT fk_pick_wrestler FOREIGN KEY (wrestler_id) REFERENCES wrestler(wrestler_id),
    CONSTRAINT uk_draft_wrestler UNIQUE (draft_id, wrestler_id),
    CONSTRAINT uk_draft_pick_number UNIQUE (draft_id, pick_number)
);

CREATE TABLE match_fulfillment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    segment_id BIGINT NOT NULL,
    league_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    winner_id BIGINT,
    submitted_by_id BIGINT,
    CONSTRAINT fk_fulfillment_segment FOREIGN KEY (segment_id) REFERENCES segment(segment_id),
    CONSTRAINT fk_fulfillment_league FOREIGN KEY (league_id) REFERENCES league(id),
    CONSTRAINT fk_fulfillment_winner FOREIGN KEY (winner_id) REFERENCES wrestler(wrestler_id),
    CONSTRAINT fk_fulfillment_submitter FOREIGN KEY (submitted_by_id) REFERENCES account(id)
);

ALTER TABLE wrestling_show ADD COLUMN league_id BIGINT;
ALTER TABLE wrestling_show ADD CONSTRAINT fk_show_league FOREIGN KEY (league_id) REFERENCES league(id);

ALTER TABLE account ADD COLUMN active_wrestler_id BIGINT;

ALTER TABLE inbox_item_target ADD COLUMN target_type VARCHAR(20) DEFAULT 'ACCOUNT' NOT NULL;
