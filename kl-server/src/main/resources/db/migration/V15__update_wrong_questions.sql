-- V15: 增强错题集巩固逻辑
ALTER TABLE child_wrong_questions ADD COLUMN wrong_count INT NOT NULL DEFAULT 1;
ALTER TABLE child_wrong_questions ADD COLUMN correct_count INT NOT NULL DEFAULT 0;
ALTER TABLE child_wrong_questions ADD COLUMN threshold INT NOT NULL DEFAULT 3;

-- 学习计划题目关联错题集
ALTER TABLE plan_questions ADD COLUMN wrong_question_id UUID REFERENCES child_wrong_questions(id) ON DELETE SET NULL;
