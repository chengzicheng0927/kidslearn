-- 题目表（由 LLM 动态生成，存储供缓存和去重）
CREATE TABLE questions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- 题目基本信息
    subject             VARCHAR(5) NOT NULL CHECK (subject IN ('en', 'ma')),
    topic               VARCHAR(100) NOT NULL,                   -- 题目所属主题
    topic_key           VARCHAR(50) NOT NULL,                   -- LearningStage 枚举 key
    difficulty          VARCHAR(10) NOT NULL CHECK (difficulty IN ('easy','medium','hard')),

    -- 题干
    label               VARCHAR(255) NOT NULL,                   -- 如 英语·动物词汇·⭐⭐
    question            TEXT NOT NULL,                          -- 题干正文

    -- 选项（JSONB）
    options             JSONB NOT NULL,
    -- [{"id": "a", "text": "Shark", "emoji": "🦈"}, ...]

    -- 答案
    correct_answer_id    VARCHAR(10) NOT NULL,

    -- 解析
    explanation         TEXT NOT NULL,
    explanation_steps   TEXT[] NOT NULL,

    -- 鼓励语
    encouragement       TEXT NOT NULL,

    -- 预估用时 & 奖励
    estimated_minutes  INT NOT NULL DEFAULT 3,
    reward              JSONB NOT NULL DEFAULT '{}',
    -- {"stars": 8, "xp": 18, "xpNeeded": 100, "level": 4, "badge": null}

    -- 成功类型
    success_result_mode VARCHAR(20) CHECK (success_result_mode IN ('correct', 'reward')),

    -- 阶段桥梁标记
    stage_bridge        JSONB,
    -- {"from": "EN_VOCAB_ANIMALS", "to": "EN_VOCAB_FRUITS", "bridgeRatio": 0.4}

    -- LLM 生成元数据
    generation_model    VARCHAR(50),
    generation_prompt   TEXT,
    llm_response_id    VARCHAR(100),

    -- 质量审核
    audit_status        VARCHAR(20) NOT NULL DEFAULT 'pending'
                            CHECK (audit_status IN ('pending','approved','rejected','flagged')),
    audit_note          TEXT,
    audited_at          TIMESTAMPTZ,
    audited_by          UUID,

    -- 正确率统计（定时更新）
    times_shown         INT NOT NULL DEFAULT 0,
    times_correct       INT NOT NULL DEFAULT 0,
    accuracy            DECIMAL(5,2) GENERATED ALWAYS AS (
        CASE WHEN times_shown > 0
             THEN ROUND(times_correct * 100.0 / times_shown, 2)
             ELSE 0 END
    ) STORED,

    tags                TEXT[] NOT NULL DEFAULT '{}',

    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_question_subject ON questions(subject);
CREATE INDEX idx_question_topic_key ON questions(topic_key);
CREATE INDEX idx_question_difficulty ON questions(difficulty);
CREATE INDEX idx_question_subject_difficulty ON questions(subject, difficulty);
CREATE INDEX idx_question_audit ON questions(audit_status) WHERE audit_status = 'pending';
CREATE INDEX idx_question_accuracy ON questions(accuracy) WHERE accuracy > 0;

-- 题目答题明细表（每次答题的完整记录）
-- FK 顺序：questions(id) 在 V4，plan_questions 在 V5，故此处先不加 FK 约束
CREATE TABLE quiz_answers (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id          UUID NOT NULL REFERENCES learning_sessions(id) ON DELETE CASCADE,
    question_id         UUID NOT NULL,                           -- 题目ID，FK 约束由 V5 补充
    plan_question_id    UUID,                                    -- 计划题目关联ID，FK 约束由 V5 补充

    selected_answer     VARCHAR(10) NOT NULL,                    -- 学生选择的选项ID
    is_correct          BOOLEAN NOT NULL,
    time_spent_sec      INT NOT NULL DEFAULT 0,

    -- 答题时的上下文快照（即使后续题目/计划被修改，也能保留历史状态）
    snapshot_topic      VARCHAR(100),                            -- 答题时快照：题目主题
    snapshot_stage_key   VARCHAR(50),                            -- 答题时快照：阶段 key
    snapshot_purpose     VARCHAR(20),                            -- 答题时快照：practice/bridge/challenge

    -- 连击快照（记录答题时的 combo 值，便于后续统计连击分布）
    combo_at_answer     INT NOT NULL DEFAULT 0,                -- 答这道题时的当前 combo 值

    answered_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_answer_session ON quiz_answers(session_id);
CREATE INDEX idx_answer_question ON quiz_answers(question_id);
CREATE INDEX idx_answer_plan_question ON quiz_answers(plan_question_id);
-- V5 会为 plan_question_id 和 question_id 补加 FK 约束
