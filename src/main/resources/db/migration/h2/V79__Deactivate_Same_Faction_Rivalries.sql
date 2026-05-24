-- ATW-bhp: Deactivate rivalries where both wrestlers currently share the same faction.
-- These were created before the same-faction heat guard was added and represent dirty data.
-- Heat event history is preserved for audit purposes.
MERGE INTO rivalry r
USING (
    SELECT DISTINCT r2.rivalry_id
    FROM rivalry r2
    JOIN wrestler_state ws1 ON ws1.wrestler_id = r2.wrestler1_id
        AND ws1.faction_id IS NOT NULL
    JOIN wrestler_state ws2 ON ws2.wrestler_id = r2.wrestler2_id
        AND ws2.faction_id = ws1.faction_id
        AND ws2.universe_id = ws1.universe_id
    WHERE r2.is_active = TRUE
) src ON r.rivalry_id = src.rivalry_id
WHEN MATCHED THEN UPDATE SET
    r.is_active        = FALSE,
    r.ended_date       = NOW(),
    r.storyline_notes  = CONCAT(
        COALESCE(r.storyline_notes, ''),
        ' [Auto-closed: wrestlers share the same faction]'
    );
