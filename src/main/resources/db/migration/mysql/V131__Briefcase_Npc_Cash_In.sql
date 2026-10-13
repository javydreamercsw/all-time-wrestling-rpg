-- NPC briefcase cash-in advisor state (ATW-2b9u). last_declined_at drives the re-proposal
-- cooldown after the booker deletes an advisor row from the planning card; cash_in_mode
-- records how the case was cashed in (PLANNED / AMBUSH / RUN_IN — RUN_IN inert until
-- ATW-qsm9). NULL = pre-advisor cash-in or never declined.
ALTER TABLE title_opportunity ADD COLUMN last_declined_at DATE NULL;
ALTER TABLE title_opportunity ADD COLUMN cash_in_mode VARCHAR(16) NULL;
