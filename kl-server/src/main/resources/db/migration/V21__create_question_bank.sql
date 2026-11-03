-- 题库预生成表（与 questions 表独立，专门存储预生成题目）
-- questions 表保留用于实时 LLM 生成（灵活但慢）
-- question_bank 用于预生成题目（固定topic，快）
CREATE TABLE question_bank (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- 题目归属
    grade               VARCHAR(20) NOT NULL,        -- 年级：一年级、二年级...
    subject             VARCHAR(5) NOT NULL CHECK (subject IN ('en', 'ma')),
    topic               VARCHAR(100) NOT NULL,        -- 中文名：100以内加减法
    topic_key           VARCHAR(50) NOT NULL,         -- topic key：MA_ADD_SUB_100（来自 learning_stage_definitions）

    -- 题目内容
    difficulty          VARCHAR(10) NOT NULL CHECK (difficulty IN ('easy', 'medium', 'hard')),
    label               VARCHAR(255) NOT NULL,        -- 如 数学·100以内加减法·基础
    question            TEXT NOT NULL,               -- 题干正文
    options             JSONB NOT NULL,               -- [{"id": "a", "text": "82"}, ...]
    correct_answer_id   VARCHAR(10) NOT NULL,         -- 正确答案选项ID

    -- 解析与鼓励
    explanation         TEXT NOT NULL,
    explanation_steps   TEXT[] NOT NULL,
    encouragement       TEXT NOT NULL,

    -- 元数据
    estimated_minutes   INT NOT NULL DEFAULT 3,
    reward              JSONB NOT NULL DEFAULT '{"stars": 8, "xp": 18}',

    -- 状态：generating(生成中) / approved(可用) / failed(失败)
    status              VARCHAR(20) NOT NULL DEFAULT 'approved'
                            CHECK (status IN ('generating', 'approved', 'failed')),

    -- 生成记录
    generation_model    VARCHAR(50),
    generation_prompt   TEXT,
    generated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- 去重索引：同一 topic+difficulty+question 只存一条
    UNIQUE(topic_key, difficulty, question)
);

-- 按年级+科目查询
CREATE INDEX idx_bank_grade_subject ON question_bank(grade, subject);
-- 按 topic_key 精确匹配（激活计划时用到）
CREATE INDEX idx_bank_topic_key ON question_bank(topic_key);
-- 按 topic_key + difficulty 组合（最常用查询）
CREATE INDEX idx_bank_topic_difficulty ON question_bank(topic_key, difficulty);
-- 按 topic_key + difficulty + grade 组合
CREATE INDEX idx_bank_topic_diff_grade ON question_bank(topic_key, difficulty, grade);
-- 状态索引（generating 用于查看生成进度）
CREATE INDEX idx_bank_status ON question_bank(status) WHERE status = 'generating';
