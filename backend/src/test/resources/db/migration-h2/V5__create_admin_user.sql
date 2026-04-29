CREATE TABLE admin_user (
    id            UUID                     NOT NULL DEFAULT gen_random_uuid(),
    username      VARCHAR(100)             NOT NULL,
    password_hash VARCHAR(255)             NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT pk_admin_user PRIMARY KEY (id),
    CONSTRAINT uq_admin_user_username UNIQUE (username)
);

-- adminPasswordHash MUST be a bcrypt-hashed value — never a plaintext password.
-- Set via application.properties:
--   spring.flyway.placeholders.adminUsername=${ADMIN_USERNAME}
--   spring.flyway.placeholders.adminPasswordHash=${ADMIN_PASSWORD_HASH}
-- Generate a bcrypt hash with: htpasswd -bnBC 10 "" yourpassword | tr -d ':\n'
INSERT INTO admin_user (username, password_hash)
VALUES ('${adminUsername}', '${adminPasswordHash}');
