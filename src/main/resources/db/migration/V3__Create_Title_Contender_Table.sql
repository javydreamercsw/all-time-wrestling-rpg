-- Drop the existing contender_id column from the title table
ALTER TABLE title DROP COLUMN contender_id;

-- Create the title_contender join table
CREATE TABLE title_contender (
    title_id BIGINT NOT NULL,
    wrestler_id BIGINT NOT NULL,
    PRIMARY KEY (title_id, wrestler_id),
    FOREIGN KEY (title_id) REFERENCES title(title_id) ON DELETE CASCADE,
    FOREIGN KEY (wrestler_id) REFERENCES wrestler(wrestler_id) ON DELETE CASCADE
);