-- ATW-xtf0: a show template can require one or more expansions to be enabled
-- before its shows are created (e.g. All Time Rumble needs the RUMBLE
-- expansion). Shows of a template whose requirements are not met are skipped
-- by the scheduler entirely — no empty shells either.
CREATE TABLE IF NOT EXISTS show_template_required_expansion (
  template_id BIGINT NOT NULL,
  expansion_code VARCHAR(64) NOT NULL,
  PRIMARY KEY (template_id, expansion_code),
  CONSTRAINT fk_stre_template FOREIGN KEY (template_id)
    REFERENCES show_template (template_id) ON DELETE CASCADE
);
