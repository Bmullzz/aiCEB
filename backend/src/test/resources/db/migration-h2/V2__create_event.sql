CREATE TABLE event (
    id                    UUID                     NOT NULL DEFAULT gen_random_uuid(),
    category_id           UUID                     NOT NULL,
    title                 VARCHAR(255)             NOT NULL,
    description           TEXT,
    location              VARCHAR(255)             NOT NULL,
    start_time            TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time              TIMESTAMP WITH TIME ZONE NOT NULL,
    status                VARCHAR(20)              NOT NULL DEFAULT 'UPCOMING',
    alert_offset_minutes  INTEGER                  NOT NULL DEFAULT 60,
    alert_offset_minutes_2 INTEGER,
    visible               BOOLEAN                  NOT NULL DEFAULT true,
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT pk_event PRIMARY KEY (id),
    CONSTRAINT fk_event_category FOREIGN KEY (category_id)
        REFERENCES event_category (id) ON DELETE RESTRICT
);

CREATE INDEX idx_event_start_time   ON event (start_time);
CREATE INDEX idx_event_status       ON event (status);
CREATE INDEX idx_event_visible_start ON event (visible, start_time);
