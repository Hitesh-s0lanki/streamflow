-- Multiple sprite sheets per video asset (was 1:1).
-- For existing DBs with single-sheet schema: run this once to add columns,
-- backfill, drop unique constraint, and add composite index.
-- Fresh installs: Hibernate (ddl-auto=update) creates the new schema; no need to run.

-- 1. Add new columns (nullable first for backfill)
ALTER TABLE sprite_sheet ADD COLUMN IF NOT EXISTS sheet_index integer;
ALTER TABLE sprite_sheet ADD COLUMN IF NOT EXISTS start_frame integer;
ALTER TABLE sprite_sheet ADD COLUMN IF NOT EXISTS end_frame integer;
ALTER TABLE sprite_sheet ADD COLUMN IF NOT EXISTS frames_count integer;

-- 2. Backfill existing single-sheet rows (only if total_frames exists)
DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'sprite_sheet' AND column_name = 'total_frames'
  ) THEN
    UPDATE sprite_sheet
    SET sheet_index = 0,
        start_frame = 0,
        end_frame = GREATEST(0, total_frames - 1),
        frames_count = COALESCE(total_frames, 0)
    WHERE sheet_index IS NULL;
  END IF;
END $$;

-- 3. Enforce NOT NULL on new columns (for any new rows)
ALTER TABLE sprite_sheet
    ALTER COLUMN sheet_index SET DEFAULT 0,
    ALTER COLUMN start_frame SET NOT NULL,
    ALTER COLUMN end_frame SET NOT NULL,
    ALTER COLUMN frames_count SET NOT NULL;

UPDATE sprite_sheet SET sheet_index = 0 WHERE sheet_index IS NULL;
ALTER TABLE sprite_sheet ALTER COLUMN sheet_index SET NOT NULL;

-- 4. Drop unique constraint on video_asset_id (allows multiple rows per video)
DROP INDEX IF EXISTS idx_sprite_sheet_video_asset_id;

-- 5. Add composite index for fast ordered lookup
CREATE INDEX IF NOT EXISTS idx_sprite_sheet_video_asset_sheet
    ON sprite_sheet (video_asset_id, sheet_index);

-- 6. Drop total_frames column (replaced by frames_count per sheet)
ALTER TABLE sprite_sheet DROP COLUMN IF EXISTS total_frames;
