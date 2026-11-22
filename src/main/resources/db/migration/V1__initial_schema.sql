-- ==============================================================================
-- Flyway Migration V1: Initial Schema for Humanitarians AI Publication Pipeline
-- Preserves existing PostgreSQL schema, foreign keys, check constraints & indexes
-- ==============================================================================

CREATE TABLE IF NOT EXISTS projects (
    project_id INTEGER PRIMARY KEY,
    project_name VARCHAR(255) NOT NULL,
    project_type VARCHAR(50) NOT NULL DEFAULT 'internal'
);

CREATE TABLE IF NOT EXISTS pms (
    pm_id INTEGER PRIMARY KEY,
    project_id INTEGER NOT NULL REFERENCES projects(project_id) ON DELETE CASCADE,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'pm',
    status VARCHAR(50) NOT NULL DEFAULT 'active',
    CONSTRAINT chk_pm_status CHECK (status IN ('active', 'inactive'))
);

CREATE TABLE IF NOT EXISTS fellows (
    fellow_id INTEGER PRIMARY KEY,
    project_id INTEGER NOT NULL REFERENCES projects(project_id) ON DELETE CASCADE,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'active',
    display_label VARCHAR(255),
    CONSTRAINT chk_fellow_status CHECK (status IN ('active', 'inactive'))
);

CREATE TABLE IF NOT EXISTS videos (
    video_id INTEGER PRIMARY KEY,
    fellow_id INTEGER REFERENCES fellows(fellow_id) ON DELETE SET NULL,
    title VARCHAR(500),
    youtube_link VARCHAR(500),
    drive_link VARCHAR(500),
    drive_file_id VARCHAR(255),
    youtube_video_id VARCHAR(255),
    current_status VARCHAR(50) NOT NULL DEFAULT 'submitted',
    status_note TEXT,
    submitted_on TIMESTAMP WITH TIME ZONE,
    published_on TIMESTAMP WITH TIME ZONE,
    CONSTRAINT chk_video_status CHECK (
        current_status IN ('submitted', 'in_review', 'needs_modification', 'queued', 'published')
    )
);

CREATE INDEX IF NOT EXISTS idx_video_yt_id ON videos(youtube_video_id);
CREATE INDEX IF NOT EXISTS idx_video_status ON videos(current_status);
CREATE INDEX IF NOT EXISTS idx_video_status_yt ON videos(current_status, youtube_video_id);

CREATE TABLE IF NOT EXISTS review_events (
    event_id SERIAL PRIMARY KEY,
    video_id INTEGER NOT NULL REFERENCES videos(video_id) ON DELETE CASCADE,
    event_type VARCHAR(50) NOT NULL,
    reviewer VARCHAR(255) NOT NULL,
    occurred_on TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes TEXT,
    CONSTRAINT chk_event_type CHECK (
        event_type IN ('submitted', 'review_started', 'modification_requested', 'resubmitted', 'approved', 'queued', 'published')
    )
);

CREATE INDEX IF NOT EXISTS idx_review_events_video ON review_events(video_id);

CREATE TABLE IF NOT EXISTS notifications (
    notification_id SERIAL PRIMARY KEY,
    video_id INTEGER NOT NULL REFERENCES videos(video_id) ON DELETE CASCADE,
    recipient_email VARCHAR(255) NOT NULL,
    sent_on TIMESTAMP WITH TIME ZONE,
    delivery_status VARCHAR(50) NOT NULL DEFAULT 'pending',
    reason TEXT,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT chk_notif_status CHECK (
        delivery_status IN ('pending', 'sending', 'sent', 'failed', 'skipped')
    )
);

CREATE INDEX IF NOT EXISTS idx_notif_video_id ON notifications(video_id);
CREATE INDEX IF NOT EXISTS idx_notif_delivery_status ON notifications(delivery_status);
CREATE INDEX IF NOT EXISTS idx_notif_video_status ON notifications(video_id, delivery_status);
