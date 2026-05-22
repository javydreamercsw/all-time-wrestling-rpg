-- ATW-bhp: Deactivate rivalries where both wrestlers currently share the same faction.
-- These were created before the same-faction heat guard was added and represent dirty data.
-- Heat event history is preserved for audit purposes.
UPDATE rivalry r
JOIN wrestler_state ws1 ON ws1.wrestler_id = r.wrestler1_id
    AND ws1.faction_id IS NOT NULL
JOIN wrestler_state ws2 ON ws2.wrestler_id = r.wrestler2_id
    AND ws2.faction_id = ws1.faction_id
    AND ws2.universe_id = ws1.universe_id
SET
    r.is_active   = 0,
    r.ended_date  = NOW(),
    r.storyline_notes = CONCAT(
        COALESCE(r.storyline_notes, ''),
        ' [Auto-closed: wrestlers share the same faction]'
    )
WHERE r.is_active = 1;
