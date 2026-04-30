-- H2 (PostgreSQL-compatibility mode) provides gen_random_uuid() natively.
-- CREATE EXTENSION is omitted — H2 does not support PostgreSQL extensions.

CREATE TABLE event_category (
    id         UUID                     NOT NULL DEFAULT gen_random_uuid(),
    name       VARCHAR(100)             NOT NULL,
    color      VARCHAR(7)               NOT NULL,
    active     BOOLEAN                  NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT pk_event_category PRIMARY KEY (id),
    CONSTRAINT uq_event_category_name UNIQUE (name)
);
