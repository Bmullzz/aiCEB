CREATE TABLE notification_log (
    id              UUID                     NOT NULL DEFAULT gen_random_uuid(),
    subscription_id UUID                     NOT NULL,
    offset_minutes  INTEGER                  NOT NULL,
    message_sid     VARCHAR(64),
    delivery_status VARCHAR(20)              NOT NULL DEFAULT 'QUEUED',
    sent_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT pk_notification_log PRIMARY KEY (id),
    CONSTRAINT fk_notification_log_subscription FOREIGN KEY (subscription_id)
        REFERENCES subscription (id) ON DELETE RESTRICT,
    CONSTRAINT uq_notification_log_subscription_offset UNIQUE (subscription_id, offset_minutes)
);

CREATE INDEX idx_notification_log_subscription_id ON notification_log (subscription_id);
