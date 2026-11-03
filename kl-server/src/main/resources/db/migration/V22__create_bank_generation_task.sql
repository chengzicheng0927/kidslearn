-- 题库生成任务表（跟踪异步生成任务状态）
CREATE TABLE bank_generation_task (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- 生成范围
    topic_key           VARCHAR(50) NOT NULL,         -- 来自 learning_stage_definitions.id
    difficulty          VARCHAR(10) NOT NULL,         -- easy / medium / hard

    -- 生成参数
    target_count        INT NOT NULL DEFAULT 50,      -- 目标数量（补充到 min-waterline）
    questions_generated INT NOT NULL DEFAULT 0,        -- 已生成数量

    -- 状态：pending(待生成) → generating(生成中) → completed/failed
    status              VARCHAR(20) NOT NULL DEFAULT 'pending'
                            CHECK (status IN ('pending', 'generating', 'completed', 'failed')),

    -- 错误信息
    error_message       TEXT,

    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at        TIMESTAMPTZ
);

-- 防止同一 topic+difficulty 并发生成（确保水位检查的原子性）
CREATE UNIQUE INDEX idx_task_topic_difficulty_pending
    ON bank_generation_task(topic_key, difficulty)
    WHERE status IN ('pending', 'generating');

-- 按 topic_key 查询进行中任务
CREATE INDEX idx_task_topic_key ON bank_generation_task(topic_key);
