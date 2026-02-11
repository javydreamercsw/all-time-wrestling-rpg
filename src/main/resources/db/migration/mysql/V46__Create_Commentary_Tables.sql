-- Commentary System Tables

CREATE TABLE IF NOT EXISTS commentary_team (
    team_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    external_id VARCHAR(255) UNIQUE,
    last_sync TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS commentator (
    commentator_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    npc_id BIGINT NOT NULL UNIQUE,
    style VARCHAR(255),
    catchphrase VARCHAR(255),
    persona_description TEXT,
    external_id VARCHAR(255) UNIQUE,
    last_sync TIMESTAMP NULL,
    FOREIGN KEY (npc_id) REFERENCES npc(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS commentary_team_members (
    team_id BIGINT NOT NULL,
    commentator_id BIGINT NOT NULL,
    PRIMARY KEY (team_id, commentator_id),
    FOREIGN KEY (team_id) REFERENCES commentary_team(team_id) ON DELETE CASCADE,
    FOREIGN KEY (commentator_id) REFERENCES commentator(commentator_id) ON DELETE CASCADE
);

-- Update NPC table with gender and alignment
ALTER TABLE npc ADD COLUMN IF NOT EXISTS gender VARCHAR(255);
ALTER TABLE npc ADD COLUMN IF NOT EXISTS alignment VARCHAR(255);
-- last_sync already exists in some versions but let's be safe
-- ALTER TABLE npc ADD COLUMN IF NOT EXISTS last_sync TIMESTAMP NULL; 

-- Ensure npc.description is long enough
ALTER TABLE npc MODIFY COLUMN description LONGTEXT;

-- Update Show and ShowTemplate with commentary team reference
ALTER TABLE show_template ADD COLUMN commentary_team_id BIGINT;
ALTER TABLE show_template ADD CONSTRAINT fk_show_template_commentary_team FOREIGN KEY (commentary_team_id) REFERENCES commentary_team(team_id) ON DELETE SET NULL;

ALTER TABLE wrestling_show ADD COLUMN commentary_team_id BIGINT;
ALTER TABLE wrestling_show ADD CONSTRAINT fk_show_commentary_team FOREIGN KEY (commentary_team_id) REFERENCES commentary_team(team_id) ON DELETE SET NULL;
