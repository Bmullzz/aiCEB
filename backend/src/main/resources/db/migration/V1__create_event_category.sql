-- gen_random_uuid() requires the pgcrypto extension on PostgreSQL.
-- H2 (test) provides this function natively; this line is omitted in the H2 migration overlay.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE event_category (
    id         UUID                     NOT NULL DEFAULT gen_random_uuid(),
    name       VARCHAR(100)             NOT NULL,
    color      VARCHAR(7)               NOT NULL,
    active     BOOLEAN                  NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT pk_event_category PRIMARY KEY (id),
    CONSTRAINT uq_event_category_name UNIQUE (name)
);
