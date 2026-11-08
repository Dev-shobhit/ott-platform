CREATE TABLE users (
                       id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                       email          VARCHAR(255) NOT NULL UNIQUE,
                       password_hash  VARCHAR(255),
                       full_name      VARCHAR(255) NOT NULL,
                       role           VARCHAR(255) NOT NULL,
                       tier           VARCHAR(255) NOT NULL,
                       auth_provider  VARCHAR(255) NOT NULL,
                       email_verified BOOLEAN      NOT NULL DEFAULT FALSE,
                       active         BOOLEAN      NOT NULL DEFAULT TRUE,
                       created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                       updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);