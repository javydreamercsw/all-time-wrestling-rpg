-- Outcome chart definitions: named dice-roll lookup tables (e.g. Highlight Reel, Finisher Chart)
CREATE TABLE outcome_matrix (
    outcome_matrix_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name              VARCHAR(255) NOT NULL,
    description       LONGTEXT,
    category          VARCHAR(50)  NOT NULL
);

CREATE TABLE outcome_matrix_entry (
    outcome_matrix_entry_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    outcome_matrix_id       BIGINT       NOT NULL,
    dice_roll               INT          NOT NULL,
    template_text           LONGTEXT     NOT NULL,
    heat_delta              INT,
    fan_delta               BIGINT,
    tv_grade_delta          INT,
    grudge_grade_delta      INT,
    injury_caused           BOOLEAN      NOT NULL DEFAULT FALSE,
    redirect_matrix_id      BIGINT,
    FOREIGN KEY (outcome_matrix_id)  REFERENCES outcome_matrix(outcome_matrix_id) ON DELETE CASCADE,
    FOREIGN KEY (redirect_matrix_id) REFERENCES outcome_matrix(outcome_matrix_id) ON DELETE SET NULL,
    UNIQUE KEY uq_entry_roll (outcome_matrix_id, dice_roll)
);
