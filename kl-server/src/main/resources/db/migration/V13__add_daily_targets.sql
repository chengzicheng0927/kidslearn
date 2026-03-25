-- 添加每日任务目标字段
ALTER TABLE children ADD COLUMN daily_target_en INTEGER NOT NULL DEFAULT 5;
ALTER TABLE children ADD COLUMN daily_target_ma INTEGER NOT NULL DEFAULT 3;
