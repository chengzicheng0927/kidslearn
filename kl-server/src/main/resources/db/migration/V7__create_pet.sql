-- 宠物定义表
CREATE TABLE pet_evolutions (
    id                  INT PRIMARY KEY,                  -- 进化阶段 0-4
    emoji               VARCHAR(20) NOT NULL,
    name                VARCHAR(50) NOT NULL,
    label               VARCHAR(100) NOT NULL,
    background_style    VARCHAR(255) NOT NULL,
    questions_required  INT NOT NULL DEFAULT 0
);

-- 宠物道具定义表
CREATE TABLE pet_food_items (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    emoji           VARCHAR(20) NOT NULL,
    name            VARCHAR(50) NOT NULL,
    cost            INT NOT NULL,
    stat_index      INT NOT NULL,                         -- 0=饱食 1=快乐 2=聪明
    color           VARCHAR(20) NOT NULL,
    unlock_requirement INT,                                -- 解锁所需累计答题数
    locked          BOOLEAN NOT NULL DEFAULT TRUE
);

-- 儿童宠物状态表
CREATE TABLE child_pets (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    child_id            UUID NOT NULL UNIQUE REFERENCES children(id) ON DELETE CASCADE,
    evolution_index     INT NOT NULL DEFAULT 0,
    stat_satiety        INT NOT NULL DEFAULT 50,            -- 饱食度 0-100
    stat_happiness      INT NOT NULL DEFAULT 50,            -- 快乐度 0-100
    stat_intelligence   INT NOT NULL DEFAULT 30,            -- 聪明度 0-100
    emotion             VARCHAR(20) NOT NULL DEFAULT 'idle',
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 宠物喂食记录
CREATE TABLE pet_feed_logs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    child_pet_id    UUID NOT NULL REFERENCES child_pets(id) ON DELETE CASCADE,
    food_item_id    UUID NOT NULL REFERENCES pet_food_items(id),
    stars_spent     INT NOT NULL,
    fed_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- V7__create_pet.sql 中插入宠物进化数据
INSERT INTO pet_evolutions (id, emoji, name, label, background_style, questions_required) VALUES
(0, '🥚', '星蛋',   '蛋 · 第 1 阶 · 即将孵化', 'linear-gradient(135deg, #E8EAF6, #C5CAE9)', 0),
(1, '🐣', '破壳橙', '幼崽 · 第 2 阶 · 成长中', 'linear-gradient(135deg, #FFF9C4, #FFE082)', 20),
(2, '🐱', '小橙子', '少年 · 第 3 阶 · 再答 28 题进化', 'linear-gradient(135deg, #B3E5FC, #C8E6C9)', 50),
(3, '🐯', '橙虎王', '成年 · 第 4 阶 · 威猛无比', 'linear-gradient(135deg, #FFE0B2, #FFCC02)', 100),
(4, '🦁', '星际狮王', '传说 · 第 5 阶 · 最终形态', 'linear-gradient(135deg, #F3E5F5, #E1BEE7)', 200);

-- V7__create_pet.sql 中插入宠物道具数据
INSERT INTO pet_food_items (emoji, name, cost, stat_index, color, unlock_requirement, locked) VALUES
('🍎', '苹果',   10, 0, '#66BB6A', NULL,    FALSE),
('🥛', '牛奶',   15, 1, '#29B6F6', NULL,    FALSE),
('📚', '书本',   20, 2, '#CE93D8', NULL,    FALSE),
('🍰', '蛋糕',   30, 0, '#F48FB1', NULL,    FALSE),
('✏️', '铅笔',   25, 2, '#FF8A65', 50,      TRUE),
('🎒', '书包',   50, 0, '#40C4FF', 100,      TRUE),
('🏆', '奖杯',   80, 1, '#FFD54F', 150,      TRUE),
('🌟', '星星糖', 40, 1, '#FFCA28', 200,      TRUE);
