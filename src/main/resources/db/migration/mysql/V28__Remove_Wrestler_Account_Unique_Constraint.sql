-- Drop the unique constraint on wrestler(account_id) if it exists
-- In MySQL, we can use DROP INDEX if it was created as a unique index/constraint
-- We'll try to drop it by the common name 'unique_wrestler_account' if it exists.

-- Using a stored procedure to safely drop the index if it exists
DELIMITER //

CREATE PROCEDURE DropWrestlerAccountUnique()
BEGIN
    DECLARE index_count INT;

    SELECT COUNT(*) INTO index_count
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'wrestler'
      AND index_name = 'unique_wrestler_account';

    IF index_count > 0 THEN
        ALTER TABLE wrestler DROP INDEX unique_wrestler_account;
    END IF;
END //

DELIMITER ;

CALL DropWrestlerAccountUnique();
DROP PROCEDURE DropWrestlerAccountUnique;
