-- ============================================================
-- 模拟数据生成脚本 - 计划和题目
-- 假充孩子 ID: b1eebc99-9a4b-4b2e-9a0b-1c3f5e7d8b2b
-- 当前日期: 2026-03-27
-- 使用方法: psql -U postgres -d kidslearn -f mock_data_for_testing.sql
-- ============================================================

BEGIN;

-- ---------------------------------------------------
-- 今日题目 (2026-03-27)
-- 4道英语 + 3道数学
-- ---------------------------------------------------

-- 英语题目
INSERT INTO questions (
    id, subject, topic, topic_key, difficulty, label, question, options, correct_answer_id,
    explanation, explanation_steps, encouragement, estimated_minutes, reward, success_result_mode,
    audit_status, scheduled_date, created_at
) VALUES
(
    'c1000001-0001-0001-0001-000000000001', 'en', '动物词汇', 'EN_VOCAB_ANIMALS', 'easy',
    '英语 · 动物词汇 · ⭐',
    'What animal says "Woof woof"?',
    '[{"id":"a","text":"Cat"},{"id":"b","text":"Dog"},{"id":"c","text":"Bird"},{"id":"d","text":"Fish"}]'::jsonb,
    'b',
    'Dog says "Woof woof".',
    ARRAY['Think about what pet makes this sound', 'Cat says meow', 'Dog says woof woof'],
    '很棒！狗狗的叫声你一定很熟悉吧！',
    3, '{"stars":6,"xp":15}'::jsonb, 'correct',
    'approved', '2026-03-27', NOW()
),
(
    'c1000001-0001-0001-0001-000000000002', 'en', '水果词汇', 'EN_VOCAB_FRUITS', 'easy',
    '英语 · 水果词汇 · ⭐',
    'What fruit is yellow and long like a弯月?',
    '[{"id":"a","text":"Apple"},{"id":"b","text":"Banana"},{"id":"c","text":"Orange"},{"id":"d","text":"Grape"}]'::jsonb,
    'b',
    'Banana is yellow and has a curved shape.',
    ARRAY['Think about yellow fruits', 'Banana is yellow and curved'],
    '香蕉的形状很特别，对吧！',
    3, '{"stars":6,"xp":15}'::jsonb, 'correct',
    'approved', '2026-03-27', NOW()
),
(
    'c1000001-0001-0001-0001-000000000003', 'en', '颜色词汇', 'EN_VOCAB_COLORS', 'easy',
    '英语 · 颜色词汇 · ⭐',
    'What color is the sun?',
    '[{"id":"a","text":"Red"},{"id":"b","text":"Yellow"},{"id":"c","text":"Blue"},{"id":"d","text":"Green"}]'::jsonb,
    'b',
    'The sun is yellow!',
    ARRAY['Think about the sun', 'The sun looks yellow'],
    '太阳是黄色的，你答对啦！',
    2, '{"stars":5,"xp":12}'::jsonb, 'correct',
    'approved', '2026-03-27', NOW()
),
(
    'c1000001-0001-0001-0001-000000000004', 'en', '家庭称谓', 'EN_VOCAB_FAMILY', 'medium',
    '英语 · 家庭称谓 · ⭐⭐',
    'Who is your "mom''s husband"?',
    '[{"id":"a","text":"Brother"},{"id":"b","text":"Dad"},{"id":"c","text":"Grandpa"},{"id":"d","text":"Uncle"}]'::jsonb,
    'b',
    'Mom''s husband is your Dad.',
    ARRAY['Think about family members', 'Mom is married to Dad', 'So mom''s husband is Dad'],
    '家庭成员很重要，你记得很清楚！',
    4, '{"stars":8,"xp":18}'::jsonb, 'correct',
    'approved', '2026-03-27', NOW()
)

ON CONFLICT (id) DO NOTHING;

-- 数学题目
INSERT INTO questions (
    id, subject, topic, topic_key, difficulty, label, question, options, correct_answer_id,
    explanation, explanation_steps, encouragement, estimated_minutes, reward, success_result_mode,
    audit_status, scheduled_date, created_at
) VALUES
(
    'c2000001-0001-0001-0001-000000000001', 'ma', '10以内加法', 'MA_ADD_10', 'easy',
    '数学 · 10以内加法 · ⭐',
    '5 + 3 = ?',
    '[{"id":"a","text":"6"},{"id":"b","text":"7"},{"id":"c","text":"8"},{"id":"d","text":"9"}]'::jsonb,
    'c',
    '5 + 3 = 8',
    ARRAY['从5开始往后数3个：6、7、8', '合起来是8'],
    '5+3=8，你做对了！',
    2, '{"stars":5,"xp":12}'::jsonb, 'correct',
    'approved', '2026-03-27', NOW()
),
(
    'c2000001-0001-0001-0001-000000000002', 'ma', '10以内减法', 'MA_SUB_10', 'easy',
    '数学 · 10以内减法 · ⭐',
    '9 - 4 = ?',
    '[{"id":"a","text":"4"},{"id":"b","text":"5"},{"id":"c","text":"6"},{"id":"d","text":"7"}]'::jsonb,
    'b',
    '9 - 4 = 5',
    ARRAY['从9往前数4个：8、7、6、5', '答案是5'],
    '减法学得不错！',
    2, '{"stars":5,"xp":12}'::jsonb, 'correct',
    'approved', '2026-03-27', NOW()
),
(
    'c2000001-0001-0001-0001-000000000003', 'ma', '比大小', 'MA_COMPARE', 'easy',
    '数学 · 比大小 · ⭐',
    'Which is bigger: 6 or 4?',
    '[{"id":"a","text":"6"},{"id":"b","text":"4"},{"id":"c","text":"一样大"},{"id":"d","text":"不知道"}]'::jsonb,
    'a',
    '6 is bigger than 4!',
    ARRAY['比较两个数字', '6比4大'],
    '比大小你很在行！',
    2, '{"stars":5,"xp":12}'::jsonb, 'correct',
    'approved', '2026-03-27', NOW()
)

ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------
-- 明日题目 (2026-03-28)
-- ---------------------------------------------------
INSERT INTO questions (
    id, subject, topic, topic_key, difficulty, label, question, options, correct_answer_id,
    explanation, explanation_steps, encouragement, estimated_minutes, reward, success_result_mode,
    audit_status, scheduled_date, created_at
) VALUES
(
    'c1000001-0001-0001-0002-000000000001', 'en', '动物词汇', 'EN_VOCAB_ANIMALS', 'easy',
    '英语 · 动物词汇 · ⭐',
    'What animal has long ears and hops?',
    '[{"id":"a","text":"Dog"},{"id":"b","text":"Rabbit"},{"id":"c","text":"Cat"},{"id":"d","text":"Fish"}]'::jsonb,
    'b',
    'Rabbits have long ears and hop!',
    ARRAY['Think about hopping animals', 'Long ears + hopping = Rabbit'],
    '兔子的特征你记住了！',
    3, '{"stars":6,"xp":15}'::jsonb, 'correct',
    'approved', '2026-03-28', NOW()
),
(
    'c2000001-0001-0001-0002-000000000001', 'ma', '10以内加法', 'MA_ADD_10', 'easy',
    '数学 · 10以内加法 · ⭐',
    '4 + 4 = ?',
    '[{"id":"a","text":"6"},{"id":"b","text":"7"},{"id":"c","text":"8"},{"id":"d","text":"9"}]'::jsonb,
    'c',
    '4 + 4 = 8',
    ARRAY['4+4就是两个4合起来', '4、8'],
    '4+4=8，很棒！',
    2, '{"stars":5,"xp":12}'::jsonb, 'correct',
    'approved', '2026-03-28', NOW()
)

ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------
-- 学习计划
-- ---------------------------------------------------

-- 待确认的计划 (pending)
INSERT INTO learning_plans (
    id, child_id, subject, status, difficulty, duration_days, daily_goal,
    phases, current_phase_index, estimated_minutes,
    generation_prompt, generation_model,
    created_at, updated_at
) VALUES (
    'd1000001-0001-0001-0001-000000000001',
    'b1eebc99-9a4b-4b2e-9a0b-1c3f5e7d8b2b',
    'ma',
    'pending',
    'easy',
    7,
    '每天5道数学题',
    '[{"dayRange":"1-3","goal":"掌握10以内加减法","topic":"MA_ADD_10","questionsPerDay":5,"difficulty":"easy"},{"dayRange":"4-7","goal":"巩固练习","topic":"MA_SUB_10","questionsPerDay":5,"difficulty":"easy"}]'::jsonb,
    0,
    15,
    '帮我制定一个7天的数学学习计划，主要练习10以内加减法',
    'gpt-4',
    NOW(),
    NOW()
) ON CONFLICT (id) DO NOTHING;

-- 进行中的计划 (active)
INSERT INTO learning_plans (
    id, child_id, subject, status, difficulty, duration_days, daily_goal,
    phases, current_phase_index, estimated_minutes,
    started_at, total_completed, total_correct, current_streak, longest_streak,
    generation_prompt, generation_model,
    created_at, updated_at
) VALUES (
    'd1000001-0001-0001-0001-000000000002',
    'b1eebc99-9a4b-4b2e-9a0b-1c3f5e7d8b2b',
    'en',
    'active',
    'easy',
    14,
    '每天5道英语题',
    '[{"dayRange":"1-7","goal":"学习动物词汇","topic":"EN_VOCAB_ANIMALS","questionsPerDay":5,"difficulty":"easy"},{"dayRange":"8-14","goal":"学习水果词汇","topic":"EN_VOCAB_FRUITS","questionsPerDay":5,"difficulty":"easy"}]'::jsonb,
    0,
    20,
    '2026-03-25',  -- started_at
    10,     -- total_completed
    8,      -- total_correct
    2,      -- current_streak
    3,      -- longest_streak
    '帮我制定一个14天的英语学习计划，从动物词汇开始',
    'gpt-4',
    '2026-03-25',
    NOW()
) ON CONFLICT (id) DO NOTHING;

-- 已完成的历史计划 #1 (completed) - 英语
INSERT INTO learning_plans (
    id, child_id, subject, status, difficulty, duration_days, daily_goal,
    phases, current_phase_index, estimated_minutes,
    started_at, completed_at, total_completed, total_correct, current_streak, longest_streak,
    generation_prompt, generation_model,
    created_at, updated_at
) VALUES (
    'd1000001-0001-0001-0001-000000000003',
    'b1eebc99-9a4b-4b2e-9a0b-1c3f5e7d8b2b',
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
    35,     -- total_completed
    30,     -- total_correct
    7,      -- current_streak
    7,      -- longest_streak
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
    'b1eebc99-9a4b-4b2e-9a0b-1c3f5e7d8b2b',
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
    50,     -- total_completed
    42,     -- total_correct
    10,     -- current_streak
    10,     -- longest_streak
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
    'b1eebc99-9a4b-4b2e-9a0b-1c3f5e7d8b2b',
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
    25,     -- total_completed
    22,     -- total_correct
    5,      -- current_streak
    5,      -- longest_streak
    '帮我制定一个5天的英语学习计划，学习数字',
    'gpt-4',
    '2026-03-03',
    NOW()
) ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------
-- 计划题目关联 (plan_questions)
-- ---------------------------------------------------
INSERT INTO plan_questions (id, plan_id, question_id, purpose, phase_index, added_at)
VALUES
    ('e1000001-0001-0001-0001-000000000001', 'd1000001-0001-0001-0001-000000000002', 'c1000001-0001-0001-0001-000000000001', 'practice', 0, NOW()),
    ('e1000001-0001-0001-0001-000000000002', 'd1000001-0001-0001-0001-000000000002', 'c1000001-0001-0001-0001-000000000002', 'practice', 0, NOW()),
    ('e1000001-0001-0001-0001-000000000003', 'd1000001-0001-0001-0001-000000000002', 'c1000001-0001-0001-0001-000000000003', 'practice', 0, NOW())
ON CONFLICT (id) DO NOTHING;

COMMIT;
