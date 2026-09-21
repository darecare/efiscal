-- Persist Tax Authority dateAndTimeOfIssue (advance payment moment) on the fiscal bill.
ALTER TABLE fiscalbill
    ADD COLUMN IF NOT EXISTS dateandtimeofissue VARCHAR(50);
