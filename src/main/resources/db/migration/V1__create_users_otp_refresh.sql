-- ============================================================
-- Users
-- ============================================================
CREATE TABLE users
(
    id              UUID         NOT NULL DEFAULT gen_random_uuid(),
    identifier      VARCHAR(150) NOT NULL,
    identifier_type VARCHAR(10)  NOT NULL,
    password_hash   VARCHAR(60)  NOT NULL,
    balance         NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    status          VARCHAR(30)  NOT NULL DEFAULT 'PENDING_VERIFICATION',
    is_verified     BOOLEAN      NOT NULL DEFAULT FALSE,
    version         BIGINT       NOT NULL DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_users
        PRIMARY KEY (id),
    CONSTRAINT uq_users_identifier
        UNIQUE (identifier, identifier_type),
    CONSTRAINT chk_users_identifier_type
        CHECK (identifier_type IN ('EMAIL', 'PHONE')),
    CONSTRAINT chk_users_status
        CHECK (status IN (
                          'PENDING_VERIFICATION',
                          'ACTIVE',
                          'SUSPENDED',
                          'DELETED'
            )),
    CONSTRAINT chk_users_balance
        CHECK (balance >= 0)
);

CREATE INDEX idx_users_status
    ON users (status)
    WHERE status = 'ACTIVE';

-- ============================================================
-- OTP verifications
-- ============================================================
CREATE TABLE otp_verifications
(
    id            UUID        NOT NULL DEFAULT gen_random_uuid(),
    user_id       UUID        NOT NULL,
    code_hash     VARCHAR(64) NOT NULL,
    purpose       VARCHAR(30) NOT NULL,
    attempt_count INTEGER     NOT NULL DEFAULT 0,
    is_used       BOOLEAN     NOT NULL DEFAULT FALSE,
    expires_at    TIMESTAMP   NOT NULL,
    created_at    TIMESTAMP   NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_otp_verifications
        PRIMARY KEY (id),
    CONSTRAINT fk_otp_user
        FOREIGN KEY (user_id) REFERENCES users (id)
            ON DELETE CASCADE,
    CONSTRAINT chk_otp_purpose
        CHECK (purpose IN (
                           'REGISTRATION',
                           'PASSWORD_RESET',
                           'LOGIN_2FA',
                           'CHANGE_IDENTIFIER'
            )),
    CONSTRAINT chk_otp_attempt_count
        CHECK (attempt_count >= 0 AND attempt_count <= 10)
);

CREATE INDEX idx_otp_active
    ON otp_verifications (user_id, purpose)
    WHERE is_used = FALSE;

CREATE INDEX idx_otp_expires
    ON otp_verifications (expires_at)
    WHERE is_used = FALSE;

-- ============================================================
-- Refresh tokens
-- ============================================================
CREATE TABLE refresh_tokens
(
    id         UUID        NOT NULL DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    revoked    BOOLEAN     NOT NULL DEFAULT FALSE,
    expires_at TIMESTAMP   NOT NULL,
    created_at TIMESTAMP   NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_refresh_tokens
        PRIMARY KEY (id),
    CONSTRAINT uq_refresh_token_hash
        UNIQUE (token_hash),
    CONSTRAINT fk_refresh_token_user
        FOREIGN KEY (user_id) REFERENCES users (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_refresh_token_user
    ON refresh_tokens (user_id)
    WHERE revoked = FALSE;

CREATE INDEX idx_refresh_token_expires
    ON refresh_tokens (expires_at)
    WHERE revoked = FALSE;