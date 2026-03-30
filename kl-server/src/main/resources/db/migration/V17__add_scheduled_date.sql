-- 给 questions 表增加 scheduled_date 字段，用于预分配题目到具体日期
ALTER TABLE questions ADD COLUMN scheduled_date DATE;

-- 已有题目标记为 null（未分配日期）
-- 新题目在学习计划创建时由代码填充
