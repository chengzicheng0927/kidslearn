-- 邀请码表
CREATE TABLE invite_codes (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code            VARCHAR(12) NOT NULL UNIQUE,
    max_uses        INT NOT NULL DEFAULT 1,            -- 只能使用一次
    used_count      INT NOT NULL DEFAULT 0,
    expires_at      TIMESTAMPTZ NOT NULL DEFAULT NOW() + INTERVAL '30 days', -- 默认有效期 30 天
    note            TEXT,
    created_by      UUID,                              -- admin id
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    used_at         TIMESTAMPTZ,
    used_by         UUID,                              -- parent id (FK later)

    CONSTRAINT chk_used_count CHECK (used_count <= max_uses)
);

CREATE INDEX idx_invite_code ON invite_codes(code);
CREATE INDEX idx_invite_expires ON invite_codes(expires_at);
