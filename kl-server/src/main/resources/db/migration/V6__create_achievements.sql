-- 成就定义表
CREATE TABLE achievement_definitions (
    id              VARCHAR(50) PRIMARY KEY,
    category        VARCHAR(20) NOT NULL CHECK (category IN ('streak','accuracy','volume','special')),
    icon            VARCHAR(20) NOT NULL,
    title           VARCHAR(100) NOT NULL,
    description     VARCHAR(255) NOT NULL,
    requirement     INT NOT NULL,
    type            VARCHAR(30) NOT NULL,               -- 'streak_days' | 'accuracy_rate' | 'total_questions'
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 儿童成就表
CREATE TABLE child_achievements (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    child_id        UUID NOT NULL REFERENCES children(id) ON DELETE CASCADE,
    achievement_id  VARCHAR(50) NOT NULL REFERENCES achievement_definitions(id),
    earned_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    UNIQUE(child_id, achievement_id)
);
CREATE INDEX idx_ach_child ON child_achievements(child_id);

-- 知识星球徽章定义表
CREATE TABLE badge_definitions (
    id              VARCHAR(50) PRIMARY KEY,
    level           INT NOT NULL UNIQUE CHECK (level BETWEEN 1 AND 10),
    icon            VARCHAR(20) NOT NULL,
    title           VARCHAR(100) NOT NULL,
    description     VARCHAR(255) NOT NULL,
    requirement     INT NOT NULL,                        -- 累计答题数
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 儿童徽章表
CREATE TABLE child_badges (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    child_id        UUID NOT NULL REFERENCES children(id) ON DELETE CASCADE,
    badge_id        VARCHAR(50) NOT NULL REFERENCES badge_definitions(id),
    earned_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    UNIQUE(child_id, badge_id)
);
CREATE INDEX idx_badge_child ON child_badges(child_id);

-- V6__create_achievements.sql 中插入成就数据
INSERT INTO achievement_definitions (id, category, icon, title, description, requirement, type) VALUES
('ach-streak-1',  'streak',    '🔥', '初露锋芒',    '连续答对 3 题',   3,  'streak_days'),
('ach-streak-2',  'streak',    '🔥', '小宇宙爆发',  '连续答对 5 题',   5,  'streak_days'),
('ach-streak-3',  'streak',    '🔥', '连胜达人',    '连续答对 10 题', 10,  'streak_days'),
('ach-acc-1',     'accuracy',   '🎯', '瞄靶精准',    '单次准确率 ≥ 80%', 80, 'accuracy_rate'),
('ach-acc-2',     'accuracy',   '💯', '满分答卷',    '单次准确率 = 100%',100,'accuracy_rate'),
('ach-vol-1',     'volume',     '📝', '学习新手',    '累计完成 10 题',  10, 'total_questions'),
('ach-vol-2',     'volume',     '📚', '学习小将',    '累计完成 50 题',  50, 'total_questions'),
('ach-vol-3',     'volume',     '🏆', '学习达人',    '累计完成 100 题',100,'total_questions'),
('ach-special-1', 'special',    '⭐', '首发命中',    '完成第一道题',    1,  'total_questions'),
('ach-special-2', 'special',    '🌟', '连续 7 天学习','坚持学习 7 天',   7,  'streak_days');

-- V6__create_achievements.sql 中插入徽章数据
INSERT INTO badge_definitions (id, level, icon, title, description, requirement) VALUES
('badge-1',  1,  '🌱', '萌新之星', '完成第 1 关卡', 10),
('badge-2',  2,  '📖', '学习小将', '完成第 2 关卡', 20),
('badge-3',  3,  '🔭', '探索新星', '完成第 3 关卡', 30),
('badge-4',  4,  '⚡', '知识闪电', '完成第 4 关卡', 40),
('badge-5',  5,  '🌟', '智慧之星', '完成第 5 关卡', 50),
('badge-6',  6,  '🚀', '成长火箭', '完成第 6 关卡', 60),
('badge-7',  7,  '🏆', '学霸奖章', '完成第 7 关卡', 70),
('badge-8',  8,  '💎', '钻石智慧', '完成第 8 关卡', 80),
('badge-9',  9,  '👑', '知识之王', '完成第 9 关卡', 90),
('badge-10',10,  '🌌', '星际学者', '完成全部关卡',  100);
