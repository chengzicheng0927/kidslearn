-- V10: Sync child_pets with PetState entity
ALTER TABLE child_pets RENAME COLUMN evolution_index TO evolution_id;
ALTER TABLE child_pets RENAME COLUMN stat_happiness TO happiness;
ALTER TABLE child_pets RENAME COLUMN stat_satiety TO hunger;

ALTER TABLE child_pets DROP COLUMN IF EXISTS stat_intelligence;
ALTER TABLE child_pets DROP COLUMN IF EXISTS emotion;

ALTER TABLE child_pets ADD COLUMN pet_name VARCHAR(50) NOT NULL DEFAULT '小星星';
ALTER TABLE child_pets ADD COLUMN xp INT NOT NULL DEFAULT 0;
ALTER TABLE child_pets ADD COLUMN xp_to_next INT NOT NULL DEFAULT 100;
ALTER TABLE child_pets ADD COLUMN last_fed_at TIMESTAMPTZ;
ALTER TABLE child_pets ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT NOW();
