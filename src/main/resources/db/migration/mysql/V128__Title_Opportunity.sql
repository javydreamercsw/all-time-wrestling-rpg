-- Money in the Bank-style briefcase (ATW-8p72): a held, cashable prize awarded to the
-- winner of a briefcase-deciding tournament (e.g. "Time Vault"). NOT a title — no reigns,
-- defenses, contenders or rankings. The holder may cash it in exactly once, at any time,
-- for a title match against the reigning champion(s) of any active championship.
-- status: HELD → CASHED_IN | EXPIRED | VOIDED
CREATE TABLE title_opportunity (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(16) NOT NULL,
    wrestler_id BIGINT NOT NULL,
    universe_id BIGINT NULL,
    earned_at DATE NOT NULL,
    earned_from_tournament_id BIGINT NULL,
    expiry_date DATE NULL,
    cashed_at DATE NULL,
    cashed_at_segment_id BIGINT NULL,
    cashed_against_title_id BIGINT NULL,
    CONSTRAINT fk_opp_wrestler FOREIGN KEY (wrestler_id) REFERENCES wrestler(wrestler_id),
    CONSTRAINT fk_opp_universe FOREIGN KEY (universe_id) REFERENCES universe(id),
    CONSTRAINT fk_opp_tournament FOREIGN KEY (earned_from_tournament_id) REFERENCES tournament(tournament_id),
    CONSTRAINT fk_opp_segment FOREIGN KEY (cashed_at_segment_id) REFERENCES segment(segment_id),
    CONSTRAINT fk_opp_title FOREIGN KEY (cashed_against_title_id) REFERENCES title(title_id)
);

CREATE INDEX idx_opp_wrestler ON title_opportunity(wrestler_id);
CREATE INDEX idx_opp_tournament ON title_opportunity(earned_from_tournament_id);
CREATE INDEX idx_opp_status ON title_opportunity(status);

-- Briefcase-deciding tournament payoff (ATW-8p72): the winner earns a cashable
-- TitleOpportunity instead of a title or a contender slot. Briefcase tournaments never
-- link a title — the winner may cash in against any reigning champion.
ALTER TABLE tournament ADD COLUMN briefcase_deciding TINYINT(1) NOT NULL DEFAULT 0;
