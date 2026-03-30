-- 对话历史表
CREATE TABLE chat_conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    parent_id UUID NOT NULL,
    child_id UUID,
    role VARCHAR(20) NOT NULL,           -- 'user' / 'assistant'
    content TEXT NOT NULL,               -- 对话内容
    tool_calls JSONB,                    -- tool 调用记录（可选）
    tool_results JSONB,                  -- tool 返回结果（可选）
    created_at TIMESTAMP DEFAULT NOW()
);

-- 索引
CREATE INDEX idx_chat_conversations_parent ON chat_conversations(parent_id, created_at DESC);
CREATE INDEX idx_chat_conversations_parent_child ON chat_conversations(parent_id, child_id, created_at DESC);

COMMENT ON TABLE chat_conversations IS '家长聊天对话历史';
COMMENT ON COLUMN chat_conversations.tool_calls IS 'tool 调用记录，格式: [{"name":"xxx","input":{}}]';
COMMENT ON COLUMN chat_conversations.tool_results IS 'tool 返回结果，格式: {"success":true,"..."}';
