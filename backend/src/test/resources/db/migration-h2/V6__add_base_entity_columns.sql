-- event_category was created without updated_at; add it so the entity can extend BaseEntity.
ALTER TABLE event_category
    ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now();

-- subscription was created without created_at / updated_at; add both for BaseEntity.
-- subscribed_at is retained as a subscription-specific business field.
ALTER TABLE subscription ADD COLUMN created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now();
ALTER TABLE subscription ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now();
