-- V20: 补全create_at
ALTER TABLE question_bank ADD COLUMN created_at TIMESTAMP DEFAULT NOW();
