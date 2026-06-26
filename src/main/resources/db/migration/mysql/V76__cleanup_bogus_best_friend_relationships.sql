-- Remove BEST_FRIEND relationships that have no corresponding team registration.
-- Preserves the 7 legitimate tag-team-based bonds; deletes the ~195 bogus entries
-- created by the pre-fix improveGameplayRelationships which bonded all segment pairs.
DELETE FROM wrestler_relationship
WHERE relationship_type = 'BEST_FRIEND'
AND NOT EXISTS (
  SELECT 1 FROM team t
  WHERE (t.wrestler1_id = wrestler_relationship.wrestler1_id AND t.wrestler2_id = wrestler_relationship.wrestler2_id)
     OR (t.wrestler1_id = wrestler_relationship.wrestler2_id AND t.wrestler2_id = wrestler_relationship.wrestler1_id)
);
