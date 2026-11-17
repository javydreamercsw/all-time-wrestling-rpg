-- Replace the requiresHighHeat boolean with a 0-100 intensity rating per segment rule (ATW-czai).
-- The rating is the gate: a rule is bookable for a rivalry whose heat >= intensity_rating.
-- Band membership (SIMMERING/HEATED/INTENSE/EXPLOSIVE) is derived from the rating at runtime.
ALTER TABLE segment_rule ADD COLUMN intensity_rating INT NOT NULL DEFAULT 0;

UPDATE segment_rule
SET intensity_rating = CASE name
    WHEN 'Normal' THEN 0
    WHEN 'Promo' THEN 10
    WHEN 'Performance Review' THEN 10
    WHEN 'GM Office Confrontation' THEN 10
    WHEN 'Faction Beatdown' THEN 10
    WHEN 'No DQ' THEN 25
    WHEN 'Elimination' THEN 30
    WHEN 'Over the Top Rope Elimination' THEN 35
    WHEN 'Gauntlet' THEN 40
    WHEN 'Submission' THEN 50
    WHEN 'Free-For-All' THEN 50
    WHEN 'Cage' THEN 60
    WHEN 'Ladder Match' THEN 60
    WHEN 'I Quit Match' THEN 80
    WHEN 'Last Man Standing' THEN 80
    WHEN 'Survivor War Match' THEN 80
    WHEN 'Tables, Ladders and Chairs (TLC)' THEN 85
    WHEN '🪖 War Games' THEN 85
    WHEN 'Barbwire Exploding Deathmatch' THEN 90
    WHEN 'Casket Match' THEN 95
    -- Non-seed rules keep their legacy flag: former high-heat rules enter at the EXPLOSIVE floor.
    ELSE CASE WHEN requires_high_heat THEN 75 ELSE 0 END
END;

ALTER TABLE segment_rule DROP COLUMN requires_high_heat;
