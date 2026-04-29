CREATE TABLE subscription (
    id           UUID                     NOT NULL DEFAULT gen_random_uuid(),
    event_id     UUID                     NOT NULL,
    phone_number VARCHAR(20)              NOT NULL,
    status       VARCHAR(20)              NOT NULL DEFAULT 'ACTIVE',
    subscribed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    opted_out_at  TIMESTAMP WITH TIME ZONE,

    CONSTRAINT pk_subscription PRIMARY KEY (id),
    CONSTRAINT fk_subscription_event FOREIGN KEY (event_id)
        REFERENCES event (id) ON DELETE RESTRICT,
    CONSTRAINT uq_subscription_event_phone UNIQUE (event_id, phone_number)
);

CREATE INDEX idx_subscription_event_id     ON subscription (event_id);
CREATE INDEX idx_subscription_event_status ON subscription (event_id, status);
