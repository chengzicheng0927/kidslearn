-- ============================================================
-- 新增已完成学习计划数据
-- 使用方法: psql -U postgres -d kidslearn -f docs/add_completed_plans.sql
-- ============================================================

BEGIN;

-- 已完成的历史计划 #1 (completed) - 英语
INSERT INTO learning_plans (
    id, child_id, subject, status, difficulty, duration_days, daily_goal,
    phases, current_phase_index, estimated_minutes,
    started_at, completed_at, total_completed, total_correct, current_streak, longest_streak,
    generation_prompt, generation_model,
    created_at, updated_at
) VALUES (
    'd1000001-0001-0001-0001-000000000003',
    '3b9e1dda-7ea4-4200-b311-f11cee9836f5',
    'en',
    'completed',
    'easy',
    7,
    '每天5道英语题',
    '[{"dayRange":"1-7","goal":"学习颜色词汇","topic":"EN_VOCAB_COLORS","questionsPerDay":5,"difficulty":"easy"}]'::jsonb,
    0,
    15,
    '2026-03-10',
    '2026-03-17',
    35,
    30,
    7,
    7,
    '帮我制定一个7天的英语学习计划，学习颜色词汇',
    'gpt-4',
    '2026-03-10',
    NOW()
) ON CONFLICT (id) DO NOTHING;

-- 已完成的历史计划 #2 (completed) - 数学
INSERT INTO learning_plans (
    id, child_id, subject, status, difficulty, duration_days, daily_goal,
    phases, current_phase_index, estimated_minutes,
    started_at, completed_at, total_completed, total_correct, current_streak, longest_streak,
    generation_prompt, generation_model,
    created_at, updated_at
) VALUES (
    'd1000001-0001-0001-0001-000000000004',
    '3b9e1dda-7ea4-4200-b311-f11cee9836f5',
    'ma',
    'completed',
    'medium',
    10,
    '每天5道数学题',
    '[{"dayRange":"1-5","goal":"10以内加法","topic":"MA_ADD_10","questionsPerDay":5,"difficulty":"easy"},{"dayRange":"6-10","goal":"10以内减法","topic":"MA_SUB_10","questionsPerDay":5,"difficulty":"medium"}]'::jsonb,
    0,
    20,
    '2026-02-20',
    '2026-03-02',
    50,
    42,
    10,
    10,
    '帮我制定一个10天的数学学习计划，练习10以内加减法',
    'gpt-4',
    '2026-02-20',
    NOW()
) ON CONFLICT (id) DO NOTHING;

-- 已完成的历史计划 #3 (completed) - 英语
INSERT INTO learning_plans (
    id, child_id, subject, status, difficulty, duration_days, daily_goal,
    phases, current_phase_index, estimated_minutes,
    started_at, completed_at, total_completed, total_correct, current_streak, longest_streak,
    generation_prompt, generation_model,
    created_at, updated_at
) VALUES (
    'd1000001-0001-0001-0001-000000000005',
    '3b9e1dda-7ea4-4200-b311-f11cee9836f5',
    'en',
    'completed',
    'easy',
    5,
    '每天5道英语题',
    '[{"dayRange":"1-5","goal":"学习数字词汇","topic":"EN_VOCAB_NUMBERS","questionsPerDay":5,"difficulty":"easy"}]'::jsonb,
    0,
    15,
    '2026-03-03',
    '2026-03-08',
    25,
    22,
    5,
    5,
    '帮我制定一个5天的英语学习计划，学习数字',
    'gpt-4',
    '2026-03-03',
    NOW()
) ON CONFLICT (id) DO NOTHING;

COMMIT;
