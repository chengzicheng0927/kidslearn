-- 重建题库表：修复 UNIQUE 约束（加 subject）、补 created_at/updated_at
-- 重建生成任务表：补 grade/subject 字段

DROP TABLE IF EXISTS bank_generation_task;
DROP TABLE IF EXISTS question_bank;

CREATE TABLE question_bank (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- 题目归属
    grade               VARCHAR(20) NOT NULL,
    subject             VARCHAR(5) NOT NULL CHECK (subject IN ('en', 'ma')),
    topic               VARCHAR(100) NOT NULL,
    topic_key           VARCHAR(50) NOT NULL,

    -- 题目内容
    difficulty          VARCHAR(10) NOT NULL CHECK (difficulty IN ('easy', 'medium', 'hard')),
    label               VARCHAR(255) NOT NULL,
    question            TEXT NOT NULL,
    options             JSONB NOT NULL,
    correct_answer_id   VARCHAR(10) NOT NULL,

    -- 解析与鼓励
    explanation         TEXT NOT NULL,
    explanation_steps   TEXT[] NOT NULL,
    encouragement       TEXT NOT NULL,

    -- 元数据
    estimated_minutes   INT NOT NULL DEFAULT 3,
    reward              JSONB NOT NULL DEFAULT '{"stars": 8, "xp": 18}',

    -- 状态
    status              VARCHAR(20) NOT NULL DEFAULT 'approved'
                            CHECK (status IN ('generating', 'approved', 'failed')),

    -- 生成记录
    generation_model    VARCHAR(50),
    generation_prompt   TEXT,
    generated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- 时间戳
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- 去重：同一 subject + topic + difficulty + question 只存一条
    UNIQUE(subject, topic_key, difficulty, question)
);

CREATE INDEX idx_bank_grade_subject ON question_bank(grade, subject);
CREATE INDEX idx_bank_topic_key ON question_bank(topic_key);
CREATE INDEX idx_bank_topic_difficulty ON question_bank(topic_key, difficulty);
CREATE INDEX idx_bank_topic_diff_grade ON question_bank(topic_key, difficulty, grade);
CREATE INDEX idx_bank_status ON question_bank(status) WHERE status = 'generating';

CREATE TABLE bank_generation_task (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- 生成范围
    grade               VARCHAR(20),
    subject             VARCHAR(5),
    topic_key           VARCHAR(50) NOT NULL,
    difficulty          VARCHAR(10) NOT NULL,

    -- 生成参数
    target_count        INT NOT NULL DEFAULT 50,
    questions_generated INT NOT NULL DEFAULT 0,

    -- 状态
    status              VARCHAR(20) NOT NULL DEFAULT 'pending'
                            CHECK (status IN ('pending', 'generating', 'completed', 'failed')),

    error_message       TEXT,

    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at        TIMESTAMPTZ
);

CREATE UNIQUE INDEX idx_task_topic_difficulty_pending
    ON bank_generation_task(topic_key, difficulty)
    WHERE status IN ('pending', 'generating');

CREATE INDEX idx_task_topic_key ON bank_generation_task(topic_key);
