-- V16: 补全错题集表的 answered_at 字段
ALTER TABLE child_wrong_questions ADD COLUMN answered_at TIMESTAMPTZ;
