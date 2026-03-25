-- 定时任务执行日志
CREATE TABLE job_execution_logs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_name        VARCHAR(100) NOT NULL,
    started_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ended_at        TIMESTAMPTZ,
    status          VARCHAR(20) NOT NULL,                  -- RUNNING / SUCCESS / FAILED
    error_message   TEXT,
    executed_by     VARCHAR(100)                           -- pod name for distributed
);

-- 推送记录
CREATE TABLE notification_logs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    child_id        UUID NOT NULL REFERENCES children(id) ON DELETE CASCADE,
    type            VARCHAR(30) NOT NULL,                 -- REMINDER / TASK / WEEKLY
    title           VARCHAR(100) NOT NULL,
    body            TEXT NOT NULL,
    sent_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    delivered       BOOLEAN,
    delivered_at    TIMESTAMPTZ
);
CREATE INDEX idx_notif_child ON notification_logs(child_id, sent_at);
