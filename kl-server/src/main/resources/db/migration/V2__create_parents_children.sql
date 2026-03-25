-- 家长账户表
CREATE TABLE parents (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    password_hash       VARCHAR(255) NOT NULL,
    password_salt       VARCHAR(64) NOT NULL,
    password_attempts   INT NOT NULL DEFAULT 0,
    locked_until        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    disabled            BOOLEAN NOT NULL DEFAULT FALSE
);

-- 家长设置表
CREATE TABLE parent_settings (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    parent_id               UUID NOT NULL UNIQUE REFERENCES parents(id) ON DELETE CASCADE,
    daily_limit_minutes     INT NOT NULL DEFAULT 30,
    dnd_enabled             BOOLEAN NOT NULL DEFAULT FALSE,
    dnd_start               TIME NOT NULL DEFAULT '21:00',
    dnd_end                 TIME NOT NULL DEFAULT '08:00',
    eye_care_reminder       BOOLEAN NOT NULL DEFAULT TRUE,
    break_reminder          BOOLEAN NOT NULL DEFAULT TRUE,
    teacher_name            VARCHAR(50) NOT NULL DEFAULT '小星老师',
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 儿童账户表
CREATE TABLE children (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    parent_id           UUID NOT NULL REFERENCES parents(id) ON DELETE CASCADE,
    name                VARCHAR(50) NOT NULL,
    age                 INT NOT NULL CHECK (age BETWEEN 3 AND 14),
    age_group           VARCHAR(10) NOT NULL,             -- '3-4岁' / '5-6岁' 等
    stars               INT NOT NULL DEFAULT 0,
    streak              INT NOT NULL DEFAULT 0,
    today_minutes       INT NOT NULL DEFAULT 0,
    today_date          DATE NOT NULL DEFAULT CURRENT_DATE,
    xp                  INT NOT NULL DEFAULT 0,
    xp_needed           INT NOT NULL DEFAULT 100,
    level               INT NOT NULL DEFAULT 1,
    total_answered      INT NOT NULL DEFAULT 0,
    total_days          INT NOT NULL DEFAULT 0,
    study_time          TIME[],                          -- 学习提醒时间点
    study_days          INT[],                          -- 学习提醒星期
    reminder_enabled    BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 设备表
CREATE TABLE devices (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    child_id       UUID NOT NULL REFERENCES children(id) ON DELETE CASCADE,
    device_id      VARCHAR(255) NOT NULL,
    push_token     VARCHAR(512),
    platform       VARCHAR(20) NOT NULL DEFAULT 'ios',
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    UNIQUE(child_id, device_id)
);
CREATE INDEX idx_device_child ON devices(child_id);
