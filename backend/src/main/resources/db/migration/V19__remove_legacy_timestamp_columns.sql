-- V19: Remove legacy created_at column from fiscalbill table
-- We now use the 'created' column added in V15, with proper defaults
-- The old created_at column had no default and was causing NULL constraint violations

ALTER TABLE fiscalbill
  DROP COLUMN IF EXISTS created_at;

ALTER TABLE fiscalbill
  DROP COLUMN IF EXISTS updated_at;
