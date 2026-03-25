-- 学习记录表（按日聚合，用于日历热力图统计）
-- 答题明细记录在 quiz_answers（V4），每日聚合由 Service 层定时写入
CREATE TABLE study_records (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    child_id        UUID NOT NULL REFERENCES children(id) ON DELETE CASCADE,
    record_date     DATE NOT NULL,
    subject         VARCHAR(5) NOT NULL CHECK (subject IN ('en', 'ma')),
    topic           VARCHAR(100) NOT NULL,                   -- 当日学习主题
    question_count  INT NOT NULL DEFAULT 0,
    correct_count   INT NOT NULL DEFAULT 0,
    accuracy        DECIMAL(5,2) GENERATED ALWAYS AS (
                        CASE WHEN question_count > 0
                             THEN ROUND(correct_count * 100.0 / question_count, 2)
                             ELSE 0 END
                    ) STORED,
    stars_earned    INT NOT NULL DEFAULT 0,
    minutes         INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    UNIQUE(child_id, record_date, subject)
);
CREATE INDEX idx_study_child_date ON study_records(child_id, record_date);

-- 学习会话表（追踪单次学习，包含本次会话的聚合统计）
CREATE TABLE learning_sessions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    child_id        UUID NOT NULL REFERENCES children(id) ON DELETE CASCADE,
    plan_id         UUID,                                   -- 关联学习计划（可选），V5 加 FK 约束
    started_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ended_at        TIMESTAMPTZ,
    total_questions INT NOT NULL DEFAULT 0,
    correct_count   INT NOT NULL DEFAULT 0,
    xp_earned       INT NOT NULL DEFAULT 0,
    stars_earned    INT NOT NULL DEFAULT 0,

    -- 连击追踪（答对累加，答错归零）
    current_combo   INT NOT NULL DEFAULT 0,                  -- 当前连续答对数
    longest_combo   INT NOT NULL DEFAULT 0                  -- 本会话最长连击
);
CREATE INDEX idx_session_child ON learning_sessions(child_id);
CREATE INDEX idx_session_plan ON learning_sessions(plan_id);
