-- V11: 初始化 10 个测试邀请码（包含前端 mock 数据）
-- 由于部分开发环境中 V1 已在此前执行，可能缺少 DEFAULT 约束，
-- 因此在此显式补充 max_uses 和 expires_at 值以确保向下兼容。

INSERT INTO invite_codes (code, max_uses, expires_at, note) VALUES
('KL-ABCD-1234', 1, NOW() + INTERVAL '30 days', '前端 Mock 专用测试码 1'),
('KL-1234-5678', 1, NOW() + INTERVAL '30 days', '前端 Mock 专用测试码 2'),
('KL-XYZW-9876', 1, NOW() + INTERVAL '30 days', '前端 Mock 专用测试码 3'),
('KL-TEST-0004', 1, NOW() + INTERVAL '30 days', '附加测试码 4'),
('KL-TEST-0005', 1, NOW() + INTERVAL '30 days', '附加测试码 5'),
('KL-TEST-0006', 1, NOW() + INTERVAL '30 days', '附加测试码 6'),
('KL-TEST-0007', 1, NOW() + INTERVAL '30 days', '附加测试码 7'),
('KL-TEST-0008', 1, NOW() + INTERVAL '30 days', '附加测试码 8'),
('KL-TEST-0009', 1, NOW() + INTERVAL '30 days', '附加测试码 9'),
('KL-TEST-0010', 1, NOW() + INTERVAL '30 days', '附加测试码 10');
