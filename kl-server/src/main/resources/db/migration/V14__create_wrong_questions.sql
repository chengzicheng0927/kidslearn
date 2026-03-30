-- V14: 创建错题集表
CREATE TABLE child_wrong_questions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    child_id            UUID NOT NULL REFERENCES children(id) ON DELETE CASCADE,
    question_id         UUID NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    wrong_answer_id     VARCHAR(10) NOT NULL, -- 记录当时选错的选项 ID
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    -- 核心约束：同一个学生同一个题目在错题集中只出现一次
    UNIQUE(child_id, question_id)
);

-- 索引：加速按孩子查询错题集
CREATE INDEX idx_wrong_questions_child ON child_wrong_questions(child_id);
