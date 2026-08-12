-- Short-lived single-use codes for email verification, password reset and
-- email change. Specification section 10.10.

CREATE TABLE verification_tokens (
    id             UUID         NOT NULL,
    user_id        UUID         NOT NULL,
    type           VARCHAR(32)  NOT NULL,
    token_hash     VARCHAR(255) NOT NULL,
    target_value   VARCHAR(320),
    expires_at     TIMESTAMPTZ  NOT NULL,
    used_at        TIMESTAMPTZ,
    invalidated_at TIMESTAMPTZ,
    attempt_count  INTEGER      NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_verification_tokens PRIMARY KEY (id),
    CONSTRAINT fk_verification_tokens_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_verification_tokens_type CHECK (
        type IN ('EMAIL_VERIFICATION', 'PASSWORD_RESET', 'EMAIL_CHANGE'))
);

-- Every flow looks up the newest usable code for one account and one purpose.
CREATE INDEX ix_verification_tokens_user_type ON verification_tokens (user_id, type);