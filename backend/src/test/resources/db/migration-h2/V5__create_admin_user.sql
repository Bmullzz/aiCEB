CREATE TABLE admin_user (
    id            UUID                     NOT NULL DEFAULT gen_random_uuid(),
    username      VARCHAR(100)             NOT NULL,
    password_hash VARCHAR(255)             NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT pk_admin_user PRIMARY KEY (id),
    CONSTRAINT uq_admin_user_username UNIQUE (username)
);

-- adminPasswordHash MUST be a bcrypt-hashed value — never a plaintext password.
-- Configure in application.properties using env vars ADMIN_USERNAME and ADMIN_PASSWORD_HASH:
--   spring.flyway.placeholders.adminUsername=<from env>
--   spring.flyway.placeholders.adminPasswordHash=<bcrypt hash from env>
-- Generate a bcrypt hash with: htpasswd -bnBC 10 "" yourpassword | tr -d ':\n'
INSERT INTO admin_user (username, password_hash)
VALUES ('${adminUsername}', '${adminPasswordHash}');
