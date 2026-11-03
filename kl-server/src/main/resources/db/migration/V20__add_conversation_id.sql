-- 为 chat_conversations 表添加 conversation_id 字段
ALTER TABLE chat_conversations ADD COLUMN conversation_id VARCHAR(50);
CREATE INDEX idx_chat_conversations_conv_id ON chat_conversations(parent_id, conversation_id, created_at DESC);
COMMENT ON COLUMN chat_conversations.conversation_id IS '会话ID，用于关联同一对话线程的所有消息';
