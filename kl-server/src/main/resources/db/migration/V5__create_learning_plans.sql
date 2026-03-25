-- 学习计划主表
CREATE TABLE learning_plans (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    child_id            UUID NOT NULL REFERENCES children(id) ON DELETE CASCADE,
    subject             VARCHAR(5) NOT NULL CHECK (subject IN ('en', 'ma')),
    status              VARCHAR(20) NOT NULL DEFAULT 'pending'
                                        CHECK (status IN ('pending','active','paused','completed','cancelled')),
    difficulty          VARCHAR(10) NOT NULL DEFAULT 'medium'
                                        CHECK (difficulty IN ('easy','medium','hard')),
    duration_days       INT NOT NULL,                              -- 计划总天数
    daily_goal          VARCHAR(255) NOT NULL,                    -- 每日目标描述
    phases              JSONB NOT NULL DEFAULT '[]',              -- 阶段数组（JSON）
    current_phase_index INT NOT NULL DEFAULT 0,                  -- 当前进行到第几个阶段（从0开始）
    estimated_minutes   INT NOT NULL DEFAULT 20,                -- 每日预计时长（分钟）
    total_questions     INT GENERATED ALWAYS AS (                  -- 计划总题目数（计算列）
        (phases->0->>'questionsPerDay')::int * duration_days
    ) STORED,

    -- AI 生成元数据
    generation_prompt   TEXT,                                    -- 触发生成时的原始 prompt
    generation_model    VARCHAR(50),                             -- 使用的 LLM 模型
    generation_params   JSONB,                                   -- 生成参数（temperature 等）

    -- 阶段进度追踪
    started_at          TIMESTAMPTZ,
    paused_at           TIMESTAMPTZ,
    completed_at        TIMESTAMPTZ,
    cancelled_at        TIMESTAMPTZ,

    -- 统计数据（按日快照）
    total_completed     INT NOT NULL DEFAULT 0,                  -- 累计完成题目数
    total_correct       INT NOT NULL DEFAULT 0,                  -- 累计正确数
    current_streak      INT NOT NULL DEFAULT 0,                  -- 当前连续学习天数
    longest_streak     INT NOT NULL DEFAULT 0,                  -- 最长连续学习天数

    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_plan_child ON learning_plans(child_id);
CREATE INDEX idx_plan_status ON learning_plans(status);
CREATE INDEX idx_plan_child_status ON learning_plans(child_id, status);

-- 学习计划阶段进度表（每个阶段独立记录，更新更灵活）
CREATE TABLE plan_phase_progress (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id             UUID NOT NULL REFERENCES learning_plans(id) ON DELETE CASCADE,
    phase_index         INT NOT NULL,                            -- 阶段序号（从0开始）
    phase_key           VARCHAR(50) NOT NULL,                    -- LearningStage 枚举 key，如 EN_VOCAB_ANIMALS
    phase_topic         VARCHAR(100) NOT NULL,                   -- 阶段主题，如 动物词汇

    -- 开始/结束时间
    started_at          TIMESTAMPTZ,
    completed_at        TIMESTAMPTZ,

    -- 阶段内答题统计
    questions_planned   INT NOT NULL DEFAULT 0,                  -- 计划题目数（questionsPerDay * 天数）
    questions_done      INT NOT NULL DEFAULT 0,                  -- 已完成题目数
    correct_count       INT NOT NULL DEFAULT 0,                  -- 正确数
    accuracy            DECIMAL(5,2) GENERATED ALWAYS AS (
        CASE WHEN questions_done > 0
             THEN ROUND(correct_count * 100.0 / questions_done, 2)
             ELSE 0 END
    ) STORED,

    -- 难度（随阶段变化）
    difficulty          VARCHAR(10) NOT NULL DEFAULT 'medium',

    -- 附加数据
    extra               JSONB,                                  -- 预留扩展字段

    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    UNIQUE(plan_id, phase_index)
);
CREATE INDEX idx_phase_plan ON plan_phase_progress(plan_id);

-- 学习计划调整历史（记录每次调整的内容）
CREATE TABLE plan_adjustment_log (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id             UUID NOT NULL REFERENCES learning_plans(id) ON DELETE CASCADE,
    adjustment_prompt   TEXT NOT NULL,                           -- 家长调整描述
    llm_response        TEXT,                                    -- LLM 调整后的回复
    phases_before       JSONB,                                   -- 调整前 phases
    phases_after        JSONB,                                   -- 调整后 phases
    adjustment_type     VARCHAR(20) CHECK (adjustment_type IN (
                            'duration_change',                    -- 周期变化
                            'difficulty_change',                 -- 难度变化
                            'questions_change',                   -- 题目数变化
                            'topic_change',                      -- 主题变化
                            'pause',
                            'resume',
                            'cancel'
                        )),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_adj_plan ON plan_adjustment_log(plan_id);

-- 学习阶段定义表
CREATE TABLE learning_stage_definitions (
    id              VARCHAR(50) PRIMARY KEY,                    -- 如 EN_VOCAB_ANIMALS
    subject         VARCHAR(5) NOT NULL CHECK (subject IN ('en', 'ma')),
    topic           VARCHAR(100) NOT NULL,                   -- 如 动物词汇
    min_age         INT NOT NULL,
    max_age         INT NOT NULL,
    next_stage_keys TEXT[] NOT NULL,                         -- 下一可选阶段列表，如 {EN_VOCAB_FRUITS, EN_CONVERSATION_BASIC}
    sort_order      INT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 学习计划阶段初始化数据
INSERT INTO learning_stage_definitions
    (id, subject, topic, min_age, max_age, next_stage_keys, sort_order) VALUES
('EN_VOCAB_ANIMALS',   'en', '动物词汇',     3, 4, ARRAY['EN_VOCAB_FRUITS','EN_VOCAB_COLORS'],          1),
('EN_VOCAB_FRUITS',    'en', '水果词汇',     4, 5, ARRAY['EN_VOCAB_FOOD','EN_CONVERSATION_BASIC'],      2),
('EN_VOCAB_FOOD',      'en', '食物词汇',     5, 6, ARRAY['EN_VOCAB_COLORS','EN_VOCAB_NUMBERS'],         3),
('EN_VOCAB_COLORS',    'en', '颜色词汇',     3, 5, ARRAY['EN_VOCAB_NUMBERS','EN_VOCAB_ANIMALS'],        4),
('EN_VOCAB_NUMBERS',   'en', '数字词汇',     4, 6, ARRAY['EN_VOCAB_COLORS','EN_CONVERSATION_BASIC'],   5),
('EN_CONVERSATION_BASIC','en','简单对话',      6, 8, ARRAY['EN_READING_INTRO','EN_GRAMMAR_BASIC'],       6),
('EN_READING_INTRO',   'en', '阅读入门',     8,10, ARRAY['EN_GRAMMAR_BASIC','EN_READING_ADV'],           7),
('EN_GRAMMAR_BASIC',   'en', '语法基础',    10,12, ARRAY['EN_READING_ADV'],                              8),
('EN_READING_ADV',     'en', '阅读进阶',    12,14, ARRAY['EN_GRAMMAR_BASIC'],                              9),
('MA_NUMBER_10',       'ma', '10以内数字',    3, 4, ARRAY['MA_ADD_SUB_10','MA_NUMBER_10'],               10),
('MA_ADD_SUB_10',      'ma', '10以内加减法',  4, 6, ARRAY['MA_ADD_SUB_20','MA_APPLICATION'],             11),
('MA_ADD_SUB_20',      'ma', '20以内加减法',  5, 7, ARRAY['MA_ADD_SUB_100','MA_APPLICATION'],            12),
('MA_ADD_SUB_100',     'ma', '100以内加减法', 7, 9, ARRAY['MA_MULTIPLY','MA_DIVIDE'],                     13),
('MA_MULTIPLY',        'ma', '乘法入门',      8,10, ARRAY['MA_DIVIDE','MA_APPLICATION'],                   14),
('MA_DIVIDE',          'ma', '除法入门',      9,11, ARRAY['MA_MULTIPLY','MA_APPLICATION'],                 15),
('MA_FRACTION',        'ma', '分数入门',     10,12, ARRAY['MA_DECIMAL','MA_APPLICATION'],                16),
('MA_DECIMAL',         'ma', '小数入门',     11,13, ARRAY['MA_FRACTION','MA_APPLICATION'],                17),
('MA_APPLICATION',     'ma', '综合应用',     12,14, ARRAY['MA_FRACTION','MA_DECIMAL'],                   18);

-- 题目-计划关联表（计划分配题目时建立关联，含题目用途分类）
CREATE TABLE plan_questions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id             UUID NOT NULL REFERENCES learning_plans(id) ON DELETE CASCADE,
    question_id         UUID NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    purpose             VARCHAR(20) NOT NULL DEFAULT 'practice'
                            CHECK (purpose IN ('practice', 'bridge', 'challenge')),
    phase_index         INT NOT NULL DEFAULT 0,                -- 属于哪个阶段（从0开始）
    added_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(plan_id, question_id)
);
CREATE INDEX idx_pq_plan ON plan_questions(plan_id);
CREATE INDEX idx_pq_question ON plan_questions(question_id);
CREATE INDEX idx_pq_plan_purpose ON plan_questions(plan_id, purpose);

-- V5 FK 约束补全
ALTER TABLE learning_sessions
    ADD CONSTRAINT fk_session_plan
    FOREIGN KEY (plan_id) REFERENCES learning_plans(id) ON DELETE SET NULL;

ALTER TABLE learning_sessions
    ADD COLUMN plan_phase_id UUID REFERENCES plan_phase_progress(id) ON DELETE SET NULL;

ALTER TABLE quiz_answers
    ADD CONSTRAINT fk_answer_question
    FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE RESTRICT;

ALTER TABLE quiz_answers
    ADD CONSTRAINT fk_answer_plan_question
    FOREIGN KEY (plan_question_id) REFERENCES plan_questions(id) ON DELETE SET NULL;
