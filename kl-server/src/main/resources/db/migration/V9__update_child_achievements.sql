-- V9: Update child_achievements to match the Achievement entity
-- 1. Rename achievement_id to definition_id
ALTER TABLE child_achievements RENAME COLUMN achievement_id TO definition_id;

-- 2. Add missing fields (icon, title, description, progress_pct, created_at)
ALTER TABLE child_achievements 
    ADD COLUMN icon VARCHAR(10),
    ADD COLUMN title VARCHAR(100),
    ADD COLUMN description VARCHAR(200),
    ADD COLUMN progress_pct INT NOT NULL DEFAULT 0,
    ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT NOW();
