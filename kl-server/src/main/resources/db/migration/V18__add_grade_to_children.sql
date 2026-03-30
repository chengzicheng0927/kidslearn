-- V18__add_grade_to_children.sql
ALTER TABLE children ADD COLUMN grade VARCHAR(20);
COMMENT ON COLUMN children.grade IS '年级，如"2年级"、"一年级"';
